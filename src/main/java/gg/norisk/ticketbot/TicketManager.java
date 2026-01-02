package gg.norisk.ticketbot;

import net.dv8tion.jda.api.entities.channel.Channel;

public class TicketManager {
  private final Database database;

  public TicketManager(Database database) {
    this.database = database;
  }

  public boolean createTicket() {
    return false;
  }

  public boolean isTicketChannel(Channel channel) {
    return database.getTicketByChannelId(channel.getId()) != null;
  }
}
