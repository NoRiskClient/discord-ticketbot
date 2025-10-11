package eu.greev.dcbot.ticketsystem.interactions.impl;

import eu.greev.dcbot.ticketsystem.interactions.Interaction;
import eu.greev.dcbot.ticketsystem.interactions.InteractionMessages;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import eu.greev.dcbot.utils.CustomEmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

public class TranscriptInteraction extends Interaction {
    private static final CustomEmbedBuilder SENT = new CustomEmbedBuilder().setDescription("Sent transcript of Ticket #%ID via DM").serverFooter().userAuthor();

    public TranscriptInteraction(@NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda) {
        super(config, ticketService, jda);
        this.ticketChannelRequired = false;
    }

    @Override
    public String getIdentifier() {
        return "transcript";
    }

    @Override
    public void handleButtonInteraction(@NotNull ButtonInteractionEvent event) {
        int ticketID = Integer.parseInt(event.getMessage().getEmbeds().get(0).getTitle().replace("Ticket #", ""));
        this.ticket = ticketService.getTicketByTicketId(ticketID);

        if (ticket == null) {
            replyEphemeralAndQueue(InteractionMessages.INTERACTION_GENERIC_FAILURE);
            return;
        }

        event.getUser().openPrivateChannel()
                .flatMap(channel -> channel.sendFiles(FileUpload.fromData(ticket.getTranscript().toFile(ticketID))))
                .queue();

        SENT.placeholder("%ID", String.valueOf(ticketID));
        replyEphemeralAndQueue(SENT);
    }
}