package eu.greev.dcbot.ticketsystem.interactions.impl;

import eu.greev.dcbot.ticketsystem.interactions.Interaction;
import eu.greev.dcbot.ticketsystem.interactions.InteractionMessages;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import org.jetbrains.annotations.NotNull;

public class TicketClaimInteraction extends Interaction {
    public TicketClaimInteraction(@NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda) {
        super(config, ticketService, jda);
        addCommand("Claim this ticket");
    }

    @Override
    public String getIdentifier() {
        return "claim";
    }

    @Override
    public void handleButtonInteraction(@NotNull ButtonInteractionEvent event) {
        handleShared(event);
    }

    @Override
    public void handleSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        handleShared(event);
    }

    private void handleShared(@NotNull IReplyCallback event) {
        if(this.ticket == null) return;
        if (ticketService.claim(ticket, event.getUser())) {
            replyAndQueue(InteractionMessages.TICKET_CLAIM_CLAIMED);
        } else {
            replyEphemeralAndQueue(InteractionMessages.TICKET_CLAIM_FAILED);
        }
    }
}