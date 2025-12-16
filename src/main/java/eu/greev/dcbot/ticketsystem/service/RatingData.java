package eu.greev.dcbot.ticketsystem.service;

import eu.greev.dcbot.ticketsystem.entities.Rating;
import org.jdbi.v3.core.Jdbi;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RatingData {
    private final Jdbi jdbi;

    public RatingData(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public int saveRating(Rating rating) {
        return jdbi.withHandle(handle -> handle.createUpdate(
                        "INSERT INTO ratings (ticketID, ownerID, supporterID, rating, message, createdAt) VALUES (?,?,?,?,?,?)")
                .bind(0, rating.getTicketId())
                .bind(1, rating.getOwnerId())
                .bind(2, rating.getSupporterId())
                .bind(3, rating.getRating())
                .bind(4, rating.getMessage())
                .bind(5, rating.getCreatedAt())
                .executeAndReturnGeneratedKeys("ratingID")
                .mapTo(Integer.class)
                .one());
    }

    public boolean hasRating(int ticketId) {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT COUNT(*) FROM ratings WHERE ticketID = ?")
                .bind(0, ticketId)
                .mapTo(Integer.class)
                .findOne()
                .orElse(0) > 0);
    }

    public Rating getRatingByTicketId(int ticketId) {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT * FROM ratings WHERE ticketID = ?")
                .bind(0, ticketId)
                .map((rs, ctx) -> Rating.builder()
                        .id(rs.getInt("ratingID"))
                        .ticketId(rs.getInt("ticketID"))
                        .ownerId(rs.getString("ownerID"))
                        .supporterId(rs.getString("supporterID"))
                        .rating(rs.getInt("rating"))
                        .message(rs.getString("message"))
                        .createdAt(rs.getLong("createdAt"))
                        .build())
                .findFirst()
                .orElse(null));
    }

    public List<Rating> getRatingsFromLastDays(int days) {
        long since = Instant.now().minus(days, ChronoUnit.DAYS).getEpochSecond();
        return jdbi.withHandle(handle -> handle.createQuery("SELECT * FROM ratings WHERE createdAt >= ?")
                .bind(0, since)
                .map((rs, ctx) -> Rating.builder()
                        .id(rs.getInt("ratingID"))
                        .ticketId(rs.getInt("ticketID"))
                        .ownerId(rs.getString("ownerID"))
                        .supporterId(rs.getString("supporterID"))
                        .rating(rs.getInt("rating"))
                        .message(rs.getString("message"))
                        .createdAt(rs.getLong("createdAt"))
                        .build())
                .list());
    }

    public Map<String, Double> averageRatingPerSupporter() {
        return jdbi.withHandle(handle -> handle.createQuery(
                        "SELECT supporterID, AVG(CAST(rating AS REAL)) as avg FROM ratings GROUP BY supporterID ORDER BY avg DESC")
                .reduceRows(new LinkedHashMap<>(), (map, row) -> {
                    map.put(row.getColumn("supporterID", String.class), row.getColumn("avg", Double.class));
                    return map;
                }));
    }

    public Map<String, Integer> countRatingsPerSupporter() {
        return jdbi.withHandle(handle -> handle.createQuery(
                        "SELECT supporterID, COUNT(*) as c FROM ratings GROUP BY supporterID ORDER BY c DESC")
                .reduceRows(new LinkedHashMap<>(), (map, row) -> {
                    map.put(row.getColumn("supporterID", String.class), row.getColumn("c", Integer.class));
                    return map;
                }));
    }

    public Map<String, Double> averageRatingPerSupporterLastDays(int days) {
        long since = Instant.now().minus(days, ChronoUnit.DAYS).getEpochSecond();
        return jdbi.withHandle(handle -> handle.createQuery(
                        "SELECT supporterID, AVG(CAST(rating AS REAL)) as avg FROM ratings WHERE createdAt >= ? GROUP BY supporterID ORDER BY avg DESC")
                .bind(0, since)
                .reduceRows(new LinkedHashMap<>(), (map, row) -> {
                    map.put(row.getColumn("supporterID", String.class), row.getColumn("avg", Double.class));
                    return map;
                }));
    }

    public Map<String, Integer> countRatingsPerSupporterLastDays(int days) {
        long since = Instant.now().minus(days, ChronoUnit.DAYS).getEpochSecond();
        return jdbi.withHandle(handle -> handle.createQuery(
                        "SELECT supporterID, COUNT(*) as c FROM ratings WHERE createdAt >= ? GROUP BY supporterID ORDER BY c DESC")
                .bind(0, since)
                .reduceRows(new LinkedHashMap<>(), (map, row) -> {
                    map.put(row.getColumn("supporterID", String.class), row.getColumn("c", Integer.class));
                    return map;
                }));
    }

    public int countTotalRatings() {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT COUNT(*) FROM ratings")
                .mapTo(Integer.class)
                .findOne()
                .orElse(0));
    }

    public double averageRatingOverall() {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT AVG(CAST(rating AS REAL)) FROM ratings")
                .mapTo(Double.class)
                .findOne()
                .orElse(0.0));
    }

    public int countTotalRatingsLastDays(int days) {
        long since = Instant.now().minus(days, ChronoUnit.DAYS).getEpochSecond();
        return jdbi.withHandle(handle -> handle.createQuery("SELECT COUNT(*) FROM ratings WHERE createdAt >= ?")
                .bind(0, since)
                .mapTo(Integer.class)
                .findOne()
                .orElse(0));
    }

    public double averageRatingLastDays(int days) {
        long since = Instant.now().minus(days, ChronoUnit.DAYS).getEpochSecond();
        return jdbi.withHandle(handle -> handle.createQuery("SELECT AVG(CAST(rating AS REAL)) FROM ratings WHERE createdAt >= ?")
                .bind(0, since)
                .mapTo(Double.class)
                .findOne()
                .orElse(0.0));
    }
}
