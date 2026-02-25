package eu.greev.dcbot.ticketsystem.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Shared helper to keep supporter rating leaderboards consistent across commands and schedulers.
 */
public final class SupporterRatingStatsHelper {
    private SupporterRatingStatsHelper() {
    }

    public static List<SupporterRatingEntry> topSupporters(Map<String, Double> avgRatings,
                                                            Map<String, Integer> countRatings,
                                                            int limit) {
        Stream<SupporterRatingEntry> stream = avgRatings.entrySet().stream()
                .map(entry -> new SupporterRatingEntry(
                        entry.getKey(),
                        entry.getValue(),
                        countRatings.getOrDefault(entry.getKey(), 0)
                ))
                .sorted(SUPPORTER_COMPARATOR);

        if (limit > 0) {
            stream = stream.limit(limit);
        }

        return stream.toList();
    }

    public static String starDisplay(double avg) {
        int fullStars = Math.clamp((int) Math.round(avg), 0, 5);
        return "★".repeat(fullStars) + "☆".repeat(5 - fullStars);
    }

    public record SupporterRatingEntry(String supporterId, double avgRating, int ratingCount) {
    }

    private static final Comparator<SupporterRatingEntry> SUPPORTER_COMPARATOR =
            Comparator.comparingDouble(SupporterRatingEntry::avgRating).reversed()
                    .thenComparing(Comparator.comparingInt(SupporterRatingEntry::ratingCount).reversed())
                    .thenComparing(SupporterRatingEntry::supporterId);
}
