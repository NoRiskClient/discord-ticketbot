package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.interactions.Interaction;
import eu.greev.dcbot.ticketsystem.interactions.InteractionMessages;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public class SetWaitingCommand extends Interaction {
    public SetWaitingCommand(@NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda) {
        super(config, ticketService, jda);
        addCommand("Set the ticket in waiting mode");
    }

    @Override
    public String getIdentifier() {
        return "set-waiting";
    }

    @Override
    public void handleSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if(ticket == null) return;
        if (!ticket.isWaiting()) {
            ticketService.toggleWaiting(ticket, true);
            ticket.setWaitingSince(Instant.now());
            replyAndQueue(InteractionMessages.SET_WAITING_WAITING);
        } else {
            replyEphemeralAndQueue(InteractionMessages.SET_WAITING_ALREADY_WAITING);
        }
    }
}