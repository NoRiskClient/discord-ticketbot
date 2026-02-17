package gg.norisk.ticketbot;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.Gson;
import gg.norisk.ticketbot.entities.Ticket;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import org.jdbi.v3.core.Jdbi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sqlite.SQLiteDataSource;

@Slf4j
public class Database {
  private final String url;
  private Jdbi jdbi;
  private final JDA jda;
  private final Cache<String, Ticket> ticketByChannelCache;
  private final Cache<Integer, Ticket> ticketByIdCache;

  public Database(@NotNull String url, @NotNull JDA jda) {
    this.url = url;
    this.jda = jda;
    this.ticketByChannelCache =
        Caffeine.newBuilder().expireAfterAccess(30, TimeUnit.MINUTES).maximumSize(1000).build();

    this.ticketByIdCache =
        Caffeine.newBuilder().expireAfterAccess(30, TimeUnit.MINUTES).maximumSize(1000).build();
  }

  public void initialize() throws IOException {
    SQLiteDataSource dataSource = new SQLiteDataSource();
    dataSource.setUrl(url);
    jdbi = Jdbi.create(dataSource);

    log.debug(dataSource.getUrl());

    try (InputStream stream = getClass().getClassLoader().getResourceAsStream("dbsetup.sql")) {
      if (stream == null) {
        throw new IOException("Database setup script not found");
      }

      String sql = new String(stream.readAllBytes());

      for (String statement : sql.split(";")) {
        if (!statement.trim().isEmpty()) {
          jdbi.withHandle(handle -> handle.createUpdate(statement).execute());
        }
      }
    }

    migrate();
  }

  private void migrate() {
    log.debug("Checking database migrations...");
    try {
      jdbi.withHandle(
          h ->
              h.createUpdate("ALTER TABLE tickets ADD COLUMN closedAt BIGINT DEFAULT NULL")
                  .execute());
      log.info("Database migration: Added 'closedAt' column to 'tickets' table.");
    } catch (Exception e) {
      // Column already exists, ignore
    }
    log.debug("Database migrations complete.");
  }

  @Nullable
  public String getMiscValue(String key) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT value FROM misc WHERE key = :key")
                .bind("key", key)
                .mapTo(String.class)
                .findOne()
                .orElse(null));
  }

  public void setMiscValue(String key, String value) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    "INSERT INTO misc (key, value) VALUES (:key, :value) "
                        + "ON CONFLICT(key) DO UPDATE SET value = :value")
                .bind("key", key)
                .bind("value", value)
                .execute());
  }

  @Nullable
  public Ticket getTicketByChannelId(String channelId) {
    Ticket cached = ticketByChannelCache.getIfPresent(channelId);
    if (cached != null) {
      return cached;
    }

    Ticket ticket =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery("SELECT * FROM tickets WHERE channelId = :channelId")
                    .bind("channelId", channelId)
                    .map(new Ticket.Mapper(jda))
                    .findOne()
                    .orElse(null));

    if (ticket != null) {
      ticketByChannelCache.put(channelId, ticket);
      ticketByIdCache.put(ticket.getId(), ticket);
    } else {
      ticketByChannelCache.invalidate(channelId);
    }

    return ticket;
  }

  @Nullable
  public Ticket getTicketById(int id) {
    Ticket cached = ticketByIdCache.getIfPresent(id);
    if (cached != null) {
      return cached;
    }

    Ticket ticket =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery("SELECT * FROM tickets WHERE id = :id")
                    .bind("id", id)
                    .map(new Ticket.Mapper(jda))
                    .findOne()
                    .orElse(null));

    if (ticket != null) {
      ticketByIdCache.put(id, ticket);
      ticketByChannelCache.put(
          ticket.getChannel() != null ? ticket.getChannel().getId() : "", ticket);
    } else {
      ticketByIdCache.invalidate(id);
    }

    return ticket;
  }

  public int saveTicket(Ticket ticket) {
    if (ticket.getId() == 0) {
      int id =
          jdbi.withHandle(
              handle ->
                  handle
                      .createUpdate(
                          "INSERT INTO tickets (category, ownerId, info, createdAt, closedAt, channelId, supporterId, closerId) "
                              + "VALUES (:category, :ownerId, :info, :createdAt, :closedAt, :channelId, :supporterId, :closerId)")
                      .bind("category", ticket.getCategory().getId())
                      .bind("info", new Gson().toJson(ticket.getInfo()))
                      .bind("createdAt", ticket.getCreatedAt().toEpochMilli())
                      .bind(
                          "closedAt",
                          ticket.getClosedAt() != null ? ticket.getClosedAt().toEpochMilli() : 0)
                      .bind("ownerId", ticket.getOwner().getId())
                      .bind(
                          "channelId",
                          ticket.getChannel() != null ? ticket.getChannel().getId() : "")
                      .bind(
                          "supporterId",
                          ticket.getSupporter() != null ? ticket.getSupporter().getId() : "")
                      .bind(
                          "closerId", ticket.getCloser() != null ? ticket.getCloser().getId() : "")
                      .executeAndReturnGeneratedKeys("id")
                      .mapTo(int.class)
                      .one());

      ticket.setId(id);
      ticketByIdCache.put(id, ticket);
      return id;
    } else {
      jdbi.useHandle(
          handle ->
              handle
                  .createUpdate(
                      "UPDATE tickets SET category = :category, ownerId = :ownerId, info = :info, createdAt = :createdAt, closedAt = :closedAt channelId = :channelId, "
                          + "supporterId = :supporterId, closerId = :closerId WHERE id = :id")
                  .bind("category", ticket.getCategory().getId())
                  .bind("info", new Gson().toJson(ticket.getInfo()))
                  .bind("createdAt", ticket.getCreatedAt().toEpochMilli())
                  .bind(
                      "closedAt",
                      ticket.getClosedAt() != null ? ticket.getClosedAt().toEpochMilli() : 0)
                  .bind("ownerId", ticket.getOwner().getId())
                  .bind("channelId", ticket.getChannel() != null ? ticket.getChannel().getId() : "")
                  .bind(
                      "supporterId",
                      ticket.getSupporter() != null ? ticket.getSupporter().getId() : "")
                  .bind("closerId", ticket.getCloser() != null ? ticket.getCloser().getId() : "")
                  .bind("id", ticket.getId())
                  .execute());
      return ticket.getId();
    }
  }
}
