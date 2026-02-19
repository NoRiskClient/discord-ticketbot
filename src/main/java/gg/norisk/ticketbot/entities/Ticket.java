package gg.norisk.ticketbot.entities;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import gg.norisk.ticketbot.TicketCategory;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
@Builder(toBuilder = true)
public class Ticket {
  private int id;
  private TicketCategory category;
  private User owner;
  private Locale locale;
  private Map<String, String> info;
  private Instant createdAt;
  private @Nullable Instant claimedAt;
  private @Nullable Instant closedAt;
  private @Nullable TextChannel channel;
  private @Nullable User supporter;
  private @Nullable User closer;

  public static class Mapper implements ColumnMapper<Ticket> {
    private final JDA jda;

    public Mapper(JDA jda) {
      this.jda = jda;
    }

    @Override
    public Ticket map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
      TicketCategory category = TicketCategory.fromId(r.getString("category"));
      User owner = jda.getUserById(r.getString("ownerId"));
      Locale locale = Locale.of(r.getString("locale"));
      LinkedHashMap<String, String> info =
          new Gson()
              .fromJson(
                  r.getString("info"), new TypeToken<LinkedHashMap<String, String>>() {}.getType());
      Instant createdAt = Instant.ofEpochMilli(r.getLong("createdAt"));
      Instant claimedAt =
          r.getLong("claimedAt") == 0 ? null : Instant.ofEpochMilli(r.getLong("claimedAt"));
      Instant closedAt =
          r.getLong("closedAt") == 0 ? null : Instant.ofEpochMilli(r.getLong("closedAt"));
      TextChannel channel =
          r.getString("channelId").isEmpty()
              ? null
              : jda.getTextChannelById(r.getString("channelId"));
      User supporter =
          r.getString("supporterId").isEmpty() ? null : jda.getUserById(r.getString("supporter"));
      User closer =
          r.getString("closerId").isEmpty() ? null : jda.getUserById(r.getString("closer"));

      if (owner == null) {
        return null;
      }

      return Ticket.builder()
          .id(r.getInt("id"))
          .category(category)
          .owner(owner)
          .locale(locale)
          .info(info)
          .createdAt(createdAt)
          .claimedAt(claimedAt)
          .closedAt(closedAt)
          .channel(channel)
          .supporter(supporter)
          .closer(closer)
          .build();
    }
  }
}
