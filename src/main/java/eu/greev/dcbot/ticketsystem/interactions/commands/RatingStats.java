package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.service.RatingData;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class RatingStats extends AbstractCommand {
    private final RatingData ratingData;

    public RatingStats(Config config, TicketService ticketService, EmbedBuilder missingPerm, JDA jda, RatingData ratingData) {
        super(config, ticketService, missingPerm, jda);
        this.ratingData = ratingData;
    }

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;

        if (!config.isDevMode() && !event.getMember().getRoles().contains(jda.getRoleById(config.getStaffId()))) {
            event.replyEmbeds(missingPerm.setFooter(config.getServerName(), config.getServerLogo()).build()).setEphemeral(true).queue();
            return;
        }

        EmbedBuilder builder = new EmbedBuilder()
                .setColor(Color.decode(config.getColor()))
                .setTitle("Rating Statistics")
                .setFooter(config.getServerName(), config.getServerLogo());

        int totalRatings = ratingData.countTotalRatings();
        double overallAvg = ratingData.averageRatingOverall();

        builder.addField("Overview",
                "Total Ratings: **" + totalRatings + "**\n" +
                        "Overall Average: **" + String.format("%.2f", overallAvg) + "** " + getStarDisplay(overallAvg),
                false);

        Map<String, Double> avgRatings = ratingData.averageRatingPerSupporter();
        Map<String, Integer> countRatings = ratingData.countRatingsPerSupporter();

        if (!avgRatings.isEmpty()) {
            String allTimeStats = formatSupporterStats(avgRatings, countRatings);
            builder.addField("All-Time Ratings (by Supporter)", allTimeStats, false);
        }

        Map<String, Double> weeklyAvg = ratingData.averageRatingPerSupporterLastDays(7);
        Map<String, Integer> weeklyCount = ratingData.countRatingsPerSupporterLastDays(7);

        if (!weeklyAvg.isEmpty()) {
            String weeklyStats = formatSupporterStats(weeklyAvg, weeklyCount);
            builder.addField("Last 7 Days", weeklyStats, false);
        } else {
            builder.addField("Last 7 Days", "No ratings in the last 7 days", false);
        }

        Map<String, Double> dailyAvg = ratingData.averageRatingPerSupporterLastDays(1);
        Map<String, Integer> dailyCount = ratingData.countRatingsPerSupporterLastDays(1);

        if (!dailyAvg.isEmpty()) {
            String dailyStats = formatSupporterStats(dailyAvg, dailyCount);
            builder.addField("Today", dailyStats, false);
        } else {
            builder.addField("Today", "No ratings today", false);
        }

        event.replyEmbeds(builder.build()).setEphemeral(true).queue();
    }

    private String formatSupporterStats(Map<String, Double> avgRatings, Map<String, Integer> countRatings) {
        var sortedEntries = avgRatings.entrySet().stream()
                .sorted((a, b) -> {
                    int cmp = Double.compare(b.getValue(), a.getValue());
                    if (cmp == 0) {
                        return Integer.compare(
                                countRatings.getOrDefault(b.getKey(), 0),
                                countRatings.getOrDefault(a.getKey(), 0)
                        );
                    }
                    return cmp;
                })
                .limit(5)
                .toList();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sortedEntries.size(); i++) {
            var e = sortedEntries.get(i);
            String mention = Optional.ofNullable(jda.retrieveUserById(e.getKey()).complete())
                    .map(u -> u.getAsMention())
                    .orElse("<@" + e.getKey() + ">");
            double avg = e.getValue();
            int count = countRatings.getOrDefault(e.getKey(), 0);
            String stars = getStarDisplay(avg);
            sb.append(String.format("%d. %s %s (%.2f avg, %d ratings)", i + 1, mention, stars, avg, count));
            if (i < sortedEntries.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private String getStarDisplay(double avg) {
        int fullStars = (int) Math.round(avg);
        fullStars = Math.max(0, Math.min(5, fullStars));
        return "\u2605".repeat(fullStars) + "\u2606".repeat(5 - fullStars);
    }
}
