package eu.greev.dcbot.scheduler;

import eu.greev.dcbot.ticketsystem.service.RatingData;
import eu.greev.dcbot.ticketsystem.service.SupporterRatingStatsHelper;
import eu.greev.dcbot.ticketsystem.service.SupporterSettingsData;
import eu.greev.dcbot.ticketsystem.service.TicketData;
import eu.greev.dcbot.utils.Config;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.awt.*;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RatingStatsScheduler {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    private final Config config;
    private final RatingData ratingData;
    private final TicketData ticketData;
    private final JDA jda;
    private final SupporterSettingsData supporterSettingsData;

    public RatingStatsScheduler(Config config, RatingData ratingData, TicketData ticketData, JDA jda, SupporterSettingsData supporterSettingsData) {
        this.config = config;
        this.ratingData = ratingData;
        this.ticketData = ticketData;
        this.jda = jda;
        this.supporterSettingsData = supporterSettingsData;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::sendDailyReport, getInitialDelayForHour(9), 1, TimeUnit.DAYS);

        scheduler.scheduleAtFixedRate(this::sendWeeklyReport, getInitialDelayForWeekly(), 7, TimeUnit.DAYS);

        scheduler.scheduleAtFixedRate(this::sendMonthlyReport, getInitialDelayForMonthly(), 30, TimeUnit.DAYS);

        log.info("RatingStatsScheduler started - Daily reports at 9:00, Weekly reports on Monday 9:00, Monthly reports on 1st at 9:00");
    }

    private long getInitialDelayForHour(int targetHour) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.withHour(targetHour).withMinute(0).withSecond(0).withNano(0);

        if (now.isAfter(nextRun)) {
            nextRun = nextRun.plusDays(1);
        }

        return Duration.between(now, nextRun).getSeconds();
    }

    private long getInitialDelayForWeekly() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextMonday = now.with(DayOfWeek.MONDAY).withHour(9).withMinute(0).withSecond(0).withNano(0);

        if (now.isAfter(nextMonday)) {
            nextMonday = nextMonday.plusWeeks(1);
        }

        return Duration.between(now, nextMonday).getSeconds();
    }

    private long getInitialDelayForMonthly() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime firstOfMonth = now.withDayOfMonth(1).withHour(9).withMinute(0).withSecond(0).withNano(0);

        if (now.isAfter(firstOfMonth)) {
            firstOfMonth = firstOfMonth.plusMonths(1);
        }

        return Duration.between(now, firstOfMonth).getSeconds();
    }

    private void sendDailyReport() {
        if (config.getRatingStatsChannel() == 0) {
            return;
        }

        TextChannel channel = jda.getTextChannelById(config.getRatingStatsChannel());
        if (channel == null) {
            log.warn("Rating stats channel not found: {}", config.getRatingStatsChannel());
            return;
        }

        List<MessageEmbed> embeds = buildDailyReport();
        if (embeds.isEmpty()) {
            return;
        }

        channel.sendMessageEmbeds(embeds).queue();
        log.info("Sent daily report");
    }

    private void sendWeeklyReport() {
        long channelId = config.getSpecialStatsChannel() != 0
                ? config.getSpecialStatsChannel()
                : config.getRatingStatsChannel();

        if (channelId == 0) {
            return;
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            log.warn("Weekly stats channel not found: {}", channelId);
            return;
        }

        List<MessageEmbed> embeds = buildWeeklyReport();
        if (embeds.isEmpty()) {
            return;
        }

        channel.sendMessageEmbeds(embeds).queue();
        log.info("Sent weekly report to channel {}", channelId);
    }

    private void sendMonthlyReport() {
        long channelId = config.getSpecialStatsChannel() != 0
                ? config.getSpecialStatsChannel()
                : config.getRatingStatsChannel();

        if (channelId == 0) {
            return;
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            log.warn("Monthly stats channel not found: {}", channelId);
            return;
        }

        List<MessageEmbed> embeds = buildMonthlyReport();
        if (embeds.isEmpty()) {
            return;
        }

        channel.sendMessageEmbeds(embeds).queue();
        log.info("Sent monthly report to channel {}", channelId);
    }

    // Public methods for debug commands
    public List<MessageEmbed> buildDailyReport() {
        List<MessageEmbed> embeds = new ArrayList<>();

        // Ticket stats
        int closedTickets = ticketData.countClosedTicketsLastDays(1);
        Map<String, Integer> ticketsBySupporter = ticketData.countClosedTicketsPerSupporterLastDays(1);

        // Rating stats
        int totalRatings = ratingData.countTotalRatingsLastDays(1);
        double avgRating = ratingData.averageRatingLastDays(1);
        Map<String, Double> avgRatings = ratingData.averageRatingPerSupporterLastDays(1);
        Map<String, Integer> countRatings = ratingData.countRatingsPerSupporterLastDays(1);

        if (closedTickets == 0 && totalRatings == 0) {
            return embeds;
        }

        LocalDate yesterday = LocalDate.now().minusDays(1);
        EmbedBuilder builder = new EmbedBuilder()
                .setColor(Color.decode(config.getColor()))
                .setTitle("📊 Täglicher Report - " + yesterday.getDayOfMonth() + "." + yesterday.getMonthValue() + "." + yesterday.getYear())
                .setDescription("Statistiken der letzten 24 Stunden");

        // Ticket overview
        StringBuilder overview = new StringBuilder();
        overview.append("**Tickets geschlossen:** ").append(closedTickets).append("\n");
        overview.append("**Bewertungen erhalten:** ").append(totalRatings);
        if (totalRatings > 0) {
            overview.append(" (Ø ").append(String.format("%.2f", avgRating)).append(" ").append(getStarDisplay(avgRating)).append(")");
        }
        builder.addField("Übersicht", overview.toString(), false);

        // Top supporters by tickets
        if (!ticketsBySupporter.isEmpty()) {
            builder.addField("🎫 Tickets pro Supporter", formatTicketStats(ticketsBySupporter, 5), false);
        }

        // Ratings by supporter
        if (!avgRatings.isEmpty()) {
            builder.addField("⭐ Bewertungen pro Supporter", formatRatingStats(avgRatings, countRatings, 5), false);
        }

        builder.setFooter(config.getServerName(), config.getServerLogo());
        embeds.add(builder.build());
        return embeds;
    }

    public List<MessageEmbed> buildWeeklyReport() {
        List<MessageEmbed> embeds = new ArrayList<>();

        // Ticket stats
        int closedTickets = ticketData.countClosedTicketsLastDays(7);
        Map<String, Integer> ticketsBySupporter = ticketData.countClosedTicketsPerSupporterLastDays(7);

        // Rating stats
        int totalRatings = ratingData.countTotalRatingsLastDays(7);
        double avgRating = ratingData.averageRatingLastDays(7);
        Map<String, Double> avgRatings = ratingData.averageRatingPerSupporterLastDays(7);
        Map<String, Integer> countRatings = ratingData.countRatingsPerSupporterLastDays(7);

        if (closedTickets == 0 && totalRatings == 0) {
            return embeds;
        }

        LocalDate weekStart = LocalDate.now().minusDays(7);
        LocalDate weekEnd = LocalDate.now().minusDays(1);
        EmbedBuilder builder = new EmbedBuilder()
                .setColor(Color.BLUE)
                .setTitle("📊 Wöchentlicher Report")
                .setDescription("Statistiken vom " + weekStart.getDayOfMonth() + "." + weekStart.getMonthValue() + ". bis " + weekEnd.getDayOfMonth() + "." + weekEnd.getMonthValue() + ".");

        // Overview
        StringBuilder overview = new StringBuilder();
        overview.append("**Tickets geschlossen:** ").append(closedTickets).append("\n");
        overview.append("**Bewertungen erhalten:** ").append(totalRatings);
        if (totalRatings > 0) {
            overview.append(" (Ø ").append(String.format("%.2f", avgRating)).append(" ").append(getStarDisplay(avgRating)).append(")");
        }
        builder.addField("Übersicht", overview.toString(), false);

        // Top supporters by tickets
        if (!ticketsBySupporter.isEmpty()) {
            builder.addField("🏆 Top Supporter (Tickets)", formatTicketStatsRanked(ticketsBySupporter, 10), false);
        }

        // Ratings by supporter
        if (!avgRatings.isEmpty()) {
            builder.addField("⭐ Beste Bewertungen", formatRatingStatsRanked(avgRatings, countRatings, 10), false);
        }

        builder.setFooter(config.getServerName(), config.getServerLogo());
        embeds.add(builder.build());
        return embeds;
    }

    public List<MessageEmbed> buildMonthlyReport() {
        List<MessageEmbed> embeds = new ArrayList<>();

        // Ticket stats
        int closedTickets = ticketData.countClosedTicketsLastDays(30);
        Map<String, Integer> ticketsBySupporter = ticketData.countClosedTicketsPerSupporterLastDays(30);

        // Rating stats
        int totalRatings = ratingData.countTotalRatingsLastDays(30);
        double avgRating = ratingData.averageRatingLastDays(30);
        Map<String, Double> avgRatings = ratingData.averageRatingPerSupporterLastDays(30);
        Map<String, Integer> countRatings = ratingData.countRatingsPerSupporterLastDays(30);

        if (closedTickets == 0 && totalRatings == 0) {
            return embeds;
        }

        LocalDate monthStart = LocalDate.now().minusDays(30);
        EmbedBuilder builder = new EmbedBuilder()
                .setColor(Color.MAGENTA)
                .setTitle("📊 Monatlicher Report")
                .setDescription("Statistiken der letzten 30 Tage (seit " + monthStart.getDayOfMonth() + "." + monthStart.getMonthValue() + "." + monthStart.getYear() + ")");

        // Overview
        StringBuilder overview = new StringBuilder();
        overview.append("**Tickets geschlossen:** ").append(closedTickets).append("\n");
        overview.append("**Bewertungen erhalten:** ").append(totalRatings);
        if (totalRatings > 0) {
            overview.append(" (Ø ").append(String.format("%.2f", avgRating)).append(" ").append(getStarDisplay(avgRating)).append(")");
        }
        builder.addField("Übersicht", overview.toString(), false);

        // Top supporters by tickets
        if (!ticketsBySupporter.isEmpty()) {
            builder.addField("🏆 Top Supporter (Tickets)", formatTicketStatsRanked(ticketsBySupporter, 10), false);
        }

        // Ratings by supporter
        if (!avgRatings.isEmpty()) {
            builder.addField("⭐ Beste Bewertungen", formatRatingStatsRanked(avgRatings, countRatings, 10), false);
        }

        builder.setFooter(config.getServerName(), config.getServerLogo());
        embeds.add(builder.build());
        return embeds;
    }

    private String formatTicketStats(Map<String, Integer> ticketsBySupporter, int limit) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Map.Entry<String, Integer> entry : ticketsBySupporter.entrySet()) {
            if (count >= limit) break;
            String userId = entry.getKey();
            boolean hideStats = supporterSettingsData.isHideStats(userId);
            String mention = hideStats ? "Anonym" : getUserMention(userId);
            String ticketCount = hideStats ? "???" : String.valueOf(entry.getValue());
            sb.append(mention).append(": **").append(ticketCount).append("** Tickets\n");
            count++;
        }
        return sb.toString().trim();
    }

    private String formatTicketStatsRanked(Map<String, Integer> ticketsBySupporter, int limit) {
        StringBuilder sb = new StringBuilder();
        int rank = 1;
        for (Map.Entry<String, Integer> entry : ticketsBySupporter.entrySet()) {
            if (rank > limit) break;
            String userId = entry.getKey();
            boolean hideStats = supporterSettingsData.isHideStats(userId);
            String mention = hideStats ? "Anonym" : getUserMention(userId);
            String ticketCount = hideStats ? "???" : String.valueOf(entry.getValue());
            String medal = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : rank + ".";
            sb.append(medal).append(" ").append(mention).append(": **").append(ticketCount).append("** Tickets\n");
            rank++;
        }
        return sb.toString().trim();
    }

    private String formatRatingStats(Map<String, Double> avgRatings, Map<String, Integer> countRatings, int limit) {
        List<SupporterRatingStatsHelper.SupporterRatingEntry> topSupporters
                = SupporterRatingStatsHelper.topSupporters(avgRatings, countRatings, limit);
        StringBuilder sb = new StringBuilder();
        for (SupporterRatingStatsHelper.SupporterRatingEntry entry : topSupporters) {
            String userId = entry.supporterId();
            boolean hideStats = supporterSettingsData.isHideStats(userId);
            String mention = hideStats ? "Anonym" : getUserMention(userId);
            if (hideStats) {
                sb.append(mention).append(": ★★★★★ (Ø ???, ???x)\n");
            } else {
                double avg = entry.avgRating();
                int ratings = entry.ratingCount();
                sb.append(mention)
                        .append(": ")
                        .append(SupporterRatingStatsHelper.starDisplay(avg))
                        .append(" (Ø ")
                        .append(String.format("%.2f", avg))
                        .append(", ")
                        .append(ratings)
                        .append("x)\n");
            }
        }
        return sb.toString().trim();
    }

    private String formatRatingStatsRanked(Map<String, Double> avgRatings, Map<String, Integer> countRatings, int limit) {
        StringBuilder sb = new StringBuilder();
        List<SupporterRatingStatsHelper.SupporterRatingEntry> topSupporters = SupporterRatingStatsHelper.topSupporters(avgRatings, countRatings, limit);
        for (int i = 0; i < topSupporters.size(); i++) {
            SupporterRatingStatsHelper.SupporterRatingEntry entry = topSupporters.get(i);
            int rank = i + 1;
            String userId = entry.supporterId();
            boolean hideStats = supporterSettingsData.isHideStats(userId);
            String mention = hideStats ? "Anonym" : getUserMention(userId);
            String medal = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : rank + ".";
            if (hideStats) {
                sb.append(medal).append(" ").append(mention).append(": ★★★★★ (Ø ???, ???x)\n");
            } else {
                double avg = entry.avgRating();
                int ratings = entry.ratingCount();
                sb.append(medal)
                        .append(" ")
                        .append(mention)
                        .append(": ")
                        .append(SupporterRatingStatsHelper.starDisplay(avg))
                        .append(" (Ø ")
                        .append(String.format("%.2f", avg))
                        .append(", ")
                        .append(ratings)
                        .append("x)\n");
            }
        }
        return sb.toString().trim();
    }

    private String getUserMention(String id) {
        try {
            return Optional.ofNullable(jda.retrieveUserById(id).complete())
                    .map(IMentionable::getAsMention)
                    .orElse("<@" + id + ">");
        } catch (Exception e) {
            return "<@" + id + ">";
        }
    }

    private String getStarDisplay(double avg) {
        return SupporterRatingStatsHelper.starDisplay(avg);
    }
}
