package gg.norisk.ticketbot;

import gg.norisk.ticketbot.entities.Ticket;
import gg.norisk.ticketbot.util.Result;
import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

@AllArgsConstructor
public class TicketService {
  private final Config config;
  private final Database database;
  private final JDA jda;

  public Result<Ticket> createTicket(
      Map<String, String> info, TicketCategory category, User owner) {
    Ticket ticket =
        Ticket.builder().id(0).category(category).owner(owner).createdAt(Instant.now()).build();

    int id = database.saveTicket(ticket);

    ticket.setChannel(
        config
            .getGuild(jda)
            .createTextChannel(generateChannelName(ticket), config.getUnclaimedCategory(jda))
            .complete());

    return Result.success(ticket);
  }

  public boolean isTicketChannel(Channel channel) {
    return database.getTicketByChannelId(channel.getId()) != null;
  }

  public Ticket getTicketByChannelId(String channelId) {
    return database.getTicketByChannelId(channelId);
  }

  @Nullable
  @Contract("null -> null")
  public Ticket getTicketByChannel(@Nullable Channel channel) {
    if (channel == null) {
      return null;
    }

    return database.getTicketByChannelId(channel.getId());
  }

  private String generateChannelName(Ticket ticket) {
    return ticket.getCategory().getId() + "-" + ticket.getId() + "-" + ticket.getOwner().getName();
  }
}
