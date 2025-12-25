package gg.norisk.ticketbot;

import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;
import org.sqlite.SQLiteDataSource;

@RequiredArgsConstructor
@Slf4j
public class Database {
  private final String url;
  private Jdbi jdbi;

  public void initialize() throws IOException {
    SQLiteDataSource ds = new SQLiteDataSource();
    ds.setUrl(url);
    jdbi = Jdbi.create(ds);

    try (InputStream is = getClass().getClassLoader().getResourceAsStream("dbsetup.sql")) {
      if (is == null) {
        throw new IOException("Database setup script not found");
      }
      String sql = new String(is.readAllBytes());
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
}
