package gg.norisk.ticketbot.interaction;

import gg.norisk.ticketbot.Config;
import gg.norisk.ticketbot.TicketService;
import net.dv8tion.jda.api.JDA;

@FunctionalInterface
public interface InteractionFactory {
  Interaction create(Config config, TicketService ticketService, JDA jda);
}
