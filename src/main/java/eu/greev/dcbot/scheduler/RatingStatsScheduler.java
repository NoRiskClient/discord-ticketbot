package eu.greev.dcbot.scheduler;

import eu.greev.dcbot.ticketsystem.service.RatingData;
import eu.greev.dcbot.utils.Config;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.awt.*;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public class RatingStatsScheduler {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Config config;
    private final RatingData ratingData;
    private final JDA jda;

    public void start() {
        scheduler.scheduleAtFixedRate(this::sendDailyReport, getInitialDelayForHour(9), 24 * 60 * 60, TimeUnit.SECONDS);

        scheduler.scheduleAtFixedRate(this::sendWeeklyReport, getInitialDelayForWeekly(), 7 * 24 * 60 * 60, TimeUnit.SECONDS);

        log.info("RatingStatsScheduler started - Daily reports at 9:00, Weekly reports on Monday 9:00");
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

    private void sendDailyReport() {
        if (config.getRatingStatsChannel() == 0) {
            return;
        }

        TextChannel channel = jda.getTextChannelById(config.getRatingStatsChannel());
        if (channel == null) {
            log.warn("Rating stats channel not found: {}", config.getRatingStatsChannel());
            return;
        }

        Map<String, Double> avgRatings = ratingData.averageRatingPerSupporterLastDays(1);
        Map<String, Integer> countRatings = ratingData.countRatingsPerSupporterLastDays(1);

        if (avgRatings.isEmpty()) {
            return;
        }

        int totalToday = ratingData.countTotalRatingsLastDays(1);
        double avgToday = ratingData.averageRatingLastDays(1);

        EmbedBuilder builder = new EmbedBuilder()
                .setColor(Color.decode(config.getColor()))
                .setTitle("Daily Rating Report")
                .setDescription("Summary of ratings received in the last 24 hours")
                .addField("Overview",
                        "Total Ratings: **" + totalToday + "**\n" +
                                "Average: **" + String.format("%.2f", avgToday) + "** " + getStarDisplay(avgToday),
                        false)
                .addField("By Supporter", formatSupporterStats(avgRatings, countRatings), false)
                .setFooter(config.getServerName(), config.getServerLogo());

        channel.sendMessageEmbeds(builder.build()).queue();
        log.info("Sent daily rating report");
    }

    private void sendWeeklyReport() {
        if (config.getRatingStatsChannel() == 0) {
            return;
        }

        TextChannel channel = jda.getTextChannelById(config.getRatingStatsChannel());
        if (channel == null) {
            log.warn("Rating stats channel not found: {}", config.getRatingStatsChannel());
            return;
        }

        Map<String, Double> avgRatings = ratingData.averageRatingPerSupporterLastDays(7);
        Map<String, Integer> countRatings = ratingData.countRatingsPerSupporterLastDays(7);

        if (avgRatings.isEmpty()) {
            return;
        }

        int totalWeek = ratingData.countTotalRatingsLastDays(7);
        double avgWeek = ratingData.averageRatingLastDays(7);

        EmbedBuilder builder = new EmbedBuilder()
                .setColor(Color.decode(config.getColor()))
                .setTitle("Weekly Rating Report")
                .setDescription("Summary of ratings received in the last 7 days")
                .addField("Overview",
                        "Total Ratings: **" + totalWeek + "**\n" +
                                "Average: **" + String.format("%.2f", avgWeek) + "** " + getStarDisplay(avgWeek),
                        false)
                .addField("By Supporter", formatSupporterStats(avgRatings, countRatings), false)
                .setFooter(config.getServerName(), config.getServerLogo());

        channel.sendMessageEmbeds(builder.build()).queue();
        log.info("Sent weekly rating report");
    }

    private String formatSupporterStats(Map<String, Double> avgRatings, Map<String, Integer> countRatings) {
        return avgRatings.entrySet().stream()
                .map(e -> {
                    String mention = Optional.ofNullable(jda.retrieveUserById(e.getKey()).complete())
                            .map(u -> u.getAsMention())
                            .orElse("<@" + e.getKey() + ">");
                    double avg = e.getValue();
                    int count = countRatings.getOrDefault(e.getKey(), 0);
                    String stars = getStarDisplay(avg);
                    return String.format("%s %s (%.2f avg, %d ratings)", mention, stars, avg, count);
                })
                .collect(Collectors.joining("\n"));
    }

    private String getStarDisplay(double avg) {
        int fullStars = (int) Math.round(avg);
        fullStars = Math.max(0, Math.min(5, fullStars));
        return "\u2605".repeat(fullStars) + "\u2606".repeat(5 - fullStars);
    }
}
