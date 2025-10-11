package eu.greev.dcbot.ticketsystem.interactions;

import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.JDA;

@FunctionalInterface
public interface InteractionFactory {
    Interaction create(Config config, TicketService ticketService, JDA jda);
}
