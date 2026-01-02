package gg.norisk.ticketbot.interaction;

import gg.norisk.ticketbot.Config;
import gg.norisk.ticketbot.Database;
import gg.norisk.ticketbot.TicketManager;
import net.dv8tion.jda.api.JDA;

@FunctionalInterface
public interface InteractionFactory {
  Interaction create(Config config, TicketManager ticketManager, Database database, JDA jda);
}
