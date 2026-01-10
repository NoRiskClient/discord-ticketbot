package gg.norisk.ticketbot;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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

    try (InputStream stream = getClass().getClassLoader().getResourceAsStream("dbsetup.sql")) {
      if (stream == null) {
        throw new IOException("Database setup script not found");
      }
      String sql = new String(stream.readAllBytes());
      jdbi.useHandle(handle -> handle.execute(sql));
    }

    migrate();
  }

  private void migrate() {
    log.info("Checking database migrations...");
    try {
      jdbi.withHandle(
          h ->
              h.createUpdate("ALTER TABLE tickets ADD COLUMN closedAt BIGINT DEFAULT NULL")
                  .execute());
      log.info("Added closedAt column to tickets table");
    } catch (Exception e) {
      // Column already exists, ignore
    }
    log.info("Database migrations complete.");
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
}
