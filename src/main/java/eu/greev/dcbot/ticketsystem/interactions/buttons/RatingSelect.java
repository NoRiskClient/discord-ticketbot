package eu.greev.dcbot.ticketsystem.interactions.buttons;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

@AllArgsConstructor
public class RatingSelect extends AbstractButton {
    private final TicketService ticketService;

    @Override
    public void execute(Event evt) {
        ButtonInteractionEvent event = (ButtonInteractionEvent) evt;
        String buttonId = event.getButton().getId();

        if (buttonId == null || !buttonId.startsWith("rating-")) {
            return;
        }

        String[] parts = buttonId.split("-");
        if (parts.length != 3) {
            event.reply("Invalid rating button.").setEphemeral(true).queue();
            return;
        }

        int stars;
        int ticketId;
        try {
            stars = Integer.parseInt(parts[1]);
            ticketId = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            event.reply("Invalid rating button format.").setEphemeral(true).queue();
            return;
        }

        Ticket ticket = ticketService.getTicketByTicketId(ticketId);
        if (ticket == null) {
            event.reply("Ticket not found.").setEphemeral(true).queue();
            return;
        }

        if (!event.getUser().getId().equals(ticket.getOwner().getId())) {
            event.reply("Only the ticket owner can submit a rating.").setEphemeral(true).queue();
            return;
        }

        if (!ticket.isPendingRating()) {
            event.reply("This ticket is no longer awaiting a rating.").setEphemeral(true).queue();
            return;
        }

        String starDisplay = getStarDisplay(stars);

        TextInput message = TextInput.create("rating-message", "Feedback (Optional)", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Share any additional feedback about your support experience...")
                .setRequired(false)
                .setMaxLength(500)
                .build();

        Modal modal = Modal.create("rating-modal-" + stars + "-" + ticketId, "Submit Rating " + starDisplay)
                .addActionRow(message)
                .build();

        event.replyModal(modal).queue();
    }

    private String getStarDisplay(int stars) {
        return "\u2605".repeat(stars) + "\u2606".repeat(5 - stars);
    }
}
