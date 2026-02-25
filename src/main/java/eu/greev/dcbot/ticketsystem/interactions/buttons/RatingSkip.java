package eu.greev.dcbot.ticketsystem.interactions.buttons;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.SupporterSettingsData;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.ticketsystem.service.XpService;
import eu.greev.dcbot.utils.Config;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.awt.*;

@Slf4j
public class RatingSkip extends AbstractButton {
    private final TicketService ticketService;
    private final Config config;
    private final JDA jda;
    private final XpService xpService;
    private final SupporterSettingsData supporterSettingsData;

    public RatingSkip(TicketService ticketService, Config config, JDA jda, XpService xpService, SupporterSettingsData supporterSettingsData) {
        this.ticketService = ticketService;
        this.config = config;
        this.jda = jda;
        this.xpService = xpService;
        this.supporterSettingsData = supporterSettingsData;
    }

    @Override
    public void execute(Event evt) {
        ButtonInteractionEvent event = (ButtonInteractionEvent) evt;
        String buttonId = event.getButton().getId();

        if (buttonId == null || !buttonId.startsWith("rating-skip-")) {
            return;
        }

        String[] parts = buttonId.split("-");
        if (parts.length != 3) {
            event.reply("Invalid button.").setEphemeral(true).queue();
            return;
        }

        int ticketId;
        try {
            ticketId = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            event.reply("Invalid button format.").setEphemeral(true).queue();
            return;
        }

        Ticket ticket = ticketService.getTicketByTicketId(ticketId);
        if (ticket == null) {
            event.reply("Ticket not found.").setEphemeral(true).queue();
            return;
        }

        if (!event.getUser().getId().equals(ticket.getOwner().getId())) {
            event.reply("Only the ticket owner can skip the rating.").setEphemeral(true).queue();
            return;
        }

        if (!ticket.isPendingRating()) {
            event.reply("This ticket is no longer awaiting a rating.").setEphemeral(true).queue();
            return;
        }

        // Generate transcript first to get URL for XP notification
        String transcriptUrl = sendSkipNotification(ticket);

        // Award XP with transcript URL (async - sends full ticket data, no rating since skipped)
        xpService.awardTicketXp(ticket, null, transcriptUrl);

        // Reset pending rating state
        ticket.setPendingRatingSince(null);

        // Send confirmation
        EmbedBuilder confirmation = new EmbedBuilder()
                .setColor(Color.GRAY)
                .setTitle("Rating Skipped")
                .setDescription("You have chosen not to rate this ticket. Thank you for using our support!")
                .setFooter(config.getServerName(), config.getServerLogo());

        event.replyEmbeds(confirmation.build()).setEphemeral(true).queue();

        // Close the ticket
        Member pendingCloser = null;
        if (ticket.getPendingCloser() != null) {
            pendingCloser = jda.getGuildById(config.getServerId())
                    .getMember(ticket.getPendingCloser());
        }

        if (pendingCloser != null) {
            ticketService.closeTicket(ticket, false, pendingCloser, null, transcriptUrl);
        } else {
            Member owner = jda.getGuildById(config.getServerId()).getMember(ticket.getOwner());
            if (owner != null) {
                ticketService.closeTicket(ticket, false, owner, null, transcriptUrl);
            }
        }
    }

    private String sendSkipNotification(Ticket ticket) {
        // Generate transcript (will return null for sensitive categories)
        String transcriptUrl = ticketService.sendTranscript(ticket);

        if (config.getRatingNotificationChannels() == null || config.getRatingNotificationChannels().isEmpty()) {
            return transcriptUrl;
        }

        // Check privacy setting for supporter
        boolean hideStats = supporterSettingsData.isHideStats(ticket.getSupporter().getId());
        String displayName = hideStats ? "Anonym" : ticket.getSupporter().getAsMention();
        String thumbnailUrl = hideStats ? null : ticket.getSupporter().getEffectiveAvatarUrl();

        EmbedBuilder notification = new EmbedBuilder()
                .setColor(Color.GRAY)
                .setTitle("Ticket #" + ticket.getId() + " closed")
                .setDescription(displayName + " hat ein Ticket gelöst! *(Keine Bewertung)*")
                .setFooter(config.getServerName(), config.getServerLogo());

        if (thumbnailUrl != null) {
            notification.setThumbnail(thumbnailUrl);
        }

        ticketService.appendTranscriptLinkAndSendCloseEmbed(transcriptUrl, ticket, notification);

        return transcriptUrl;
    }
}
