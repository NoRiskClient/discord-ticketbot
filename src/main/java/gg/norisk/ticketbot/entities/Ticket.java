package gg.norisk.ticketbot.entities;

import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.Builder;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jetbrains.annotations.Nullable;

@Getter
@Builder(toBuilder = true)
public class Ticket {
  private int id;
  private User owner;
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
      User owner = jda.getUserById(r.getString("owner"));
      TextChannel channel =
          r.getString("channel").isEmpty() ? null : jda.getTextChannelById(r.getString("channel"));
      User supporter =
          r.getString("supporter").isEmpty() ? null : jda.getUserById(r.getString("supporter"));
      User closer = r.getString("closer").isEmpty() ? null : jda.getUserById(r.getString("closer"));

      if (owner == null) {
        return null;
      }

      return Ticket.builder()
          .id(r.getInt("id"))
          .owner(owner)
          .channel(channel)
          .supporter(supporter)
          .closer(closer)
          .build();
    }
  }
}
