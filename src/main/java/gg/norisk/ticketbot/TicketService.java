package gg.norisk.ticketbot;

import gg.norisk.ticketbot.entities.Ticket;
import gg.norisk.ticketbot.util.Result;
import java.util.Map;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public class TicketService {
  private final Database database;

  public TicketService(Database database) {
    this.database = database;
  }

  public Result<Ticket> createTicket(Map<String, String> info, TicketCategory category, User user) {
    return Result.failure("Not implemented");
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
}
