package eu.greev.dcbot.ticketsystem.interactions.modals;

import eu.greev.dcbot.ticketsystem.entities.Rating;
import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.interactions.Interaction;
import eu.greev.dcbot.ticketsystem.service.RatingData;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

import java.awt.*;
import java.time.Instant;

@AllArgsConstructor
public class RatingModal implements Interaction {
    private final TicketService ticketService;
    private final RatingData ratingData;
    private final Config config;
    private final JDA jda;

    @Override
    public void execute(Event evt) {
        ModalInteractionEvent event = (ModalInteractionEvent) evt;
        String modalId = event.getModalId();

        if (!modalId.startsWith("rating-modal-")) {
            return;
        }

        String[] parts = modalId.split("-");
        if (parts.length != 4) {
            event.reply("Invalid rating modal.").setEphemeral(true).queue();
            return;
        }

        int stars;
        int ticketId;
        try {
            stars = Integer.parseInt(parts[2]);
            ticketId = Integer.parseInt(parts[3]);
        } catch (NumberFormatException e) {
            event.reply("Invalid rating modal format.").setEphemeral(true).queue();
            return;
        }

        String message = event.getValue("rating-message") != null
                ? event.getValue("rating-message").getAsString()
                : null;

        if (message != null && message.isBlank()) {
            message = null;
        }

        Ticket ticket = ticketService.getTicketByTicketId(ticketId);
        if (ticket == null) {
            event.reply("Ticket not found.").setEphemeral(true).queue();
            return;
        }

        if (!ticket.isPendingRating()) {
            event.reply("This ticket is no longer awaiting a rating.").setEphemeral(true).queue();
            return;
        }

        if (!event.getUser().getId().equals(ticket.getOwner().getId())) {
            event.reply("Only the ticket owner can submit a rating.").setEphemeral(true).queue();
            return;
        }

        Rating rating = Rating.builder()
                .ticketId(ticketId)
                .ownerId(ticket.getOwner().getId())
                .supporterId(ticket.getSupporter().getId())
                .rating(stars)
                .message(message)
                .createdAt(Instant.now().getEpochSecond())
                .build();

        ratingData.saveRating(rating);

        String starDisplay = getStarDisplay(stars);
        EmbedBuilder confirmation = new EmbedBuilder()
                .setColor(Color.GREEN)
                .setTitle("Thank You!")
                .setDescription("Your rating has been recorded.\n\n" + starDisplay + " (" + stars + "/5)")
                .setFooter(config.getServerName(), config.getServerLogo());

        event.replyEmbeds(confirmation.build()).setEphemeral(true).queue();

        sendRatingNotification(ticket, stars, message);

        ticket.setPendingRating(false);

        Member pendingCloser = null;
        if (ticket.getPendingCloser() != null) {
            pendingCloser = jda.getGuildById(config.getServerId())
                    .getMember(ticket.getPendingCloser());
        }

        if (pendingCloser != null) {
            ticketService.closeTicket(ticket, false, pendingCloser, null);
        } else {
            Member owner = jda.getGuildById(config.getServerId()).getMember(ticket.getOwner());
            if (owner != null) {
                ticketService.closeTicket(ticket, false, owner, null);
            }
        }
    }

    private String getStarDisplay(int stars) {
        return "\u2605".repeat(stars) + "\u2606".repeat(5 - stars);
    }

    private void sendRatingNotification(Ticket ticket, int stars, String message) {
        if (config.getRatingNotificationChannels() == null || config.getRatingNotificationChannels().isEmpty()) {
            return;
        }

        String starDisplay = getStarDisplay(stars);
        Color embedColor = stars >= 4 ? Color.GREEN : stars >= 3 ? Color.YELLOW : Color.RED;

        EmbedBuilder notification = new EmbedBuilder()
                .setColor(embedColor)
                .setTitle("Ticket #" + ticket.getId() + " closed")
                .setDescription(ticket.getSupporter().getAsMention() + " hat **" + stars + " Sterne** " + starDisplay + " erhalten und ein Ticket gelöst!")
                .setThumbnail(ticket.getSupporter().getEffectiveAvatarUrl())
                .setFooter(config.getServerName(), config.getServerLogo());

        if (message != null && !message.isBlank()) {
            notification.addField("Feedback", message, false);
        }

        for (Long channelId : config.getRatingNotificationChannels()) {
            var channel = jda.getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessageEmbeds(notification.build()).queue();
            }
        }
    }
}
