package gg.norisk.ticketbot;

import gg.norisk.ticketbot.entities.Ticket;
import gg.norisk.ticketbot.util.Result;
import java.util.Map;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;

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

  public Ticket getTicketByChannel(Channel channel) {
    return database.getTicketByChannelId(channel.getId());
  }
}
