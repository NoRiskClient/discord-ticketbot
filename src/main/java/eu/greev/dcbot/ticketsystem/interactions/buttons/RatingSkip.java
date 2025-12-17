package eu.greev.dcbot.ticketsystem.interactions.buttons;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.ryzeon.transcripts.DiscordHtmlTranscripts;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.*;

@Slf4j
@AllArgsConstructor
public class RatingSkip extends AbstractButton {
    private final TicketService ticketService;
    private final Config config;
    private final JDA jda;

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

        // Send skip notification (with transcript but no rating)
        String transcriptUrl = sendSkipNotification(ticket);

        // Reset pending rating state
        ticket.setPendingRating(false);

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
        // Generate HTML transcript and upload to log channel to get URL
        String transcriptUrl = null;
        try {
            if (ticket.getTextChannel() != null && config.getLogChannel() != 0) {
                FileUpload transcriptUpload = DiscordHtmlTranscripts.getInstance()
                        .createTranscript(ticket.getTextChannel(), "transcript-" + ticket.getId() + ".html");

                var logChannel = jda.getTextChannelById(config.getLogChannel());
                if (logChannel != null) {
                    var uploadMessage = logChannel.sendFiles(transcriptUpload).complete();
                    if (!uploadMessage.getAttachments().isEmpty()) {
                        transcriptUrl = uploadMessage.getAttachments().getFirst().getUrl();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to generate/upload HTML transcript for ticket #{}", ticket.getId(), e);
        }

        if (config.getRatingNotificationChannels() == null || config.getRatingNotificationChannels().isEmpty()) {
            return transcriptUrl;
        }

        EmbedBuilder notification = new EmbedBuilder()
                .setColor(Color.GRAY)
                .setTitle("Ticket #" + ticket.getId() + " closed")
                .setDescription(ticket.getSupporter().getAsMention() + " hat ein Ticket gelöst! *(Keine Bewertung)*")
                .setThumbnail(ticket.getSupporter().getEffectiveAvatarUrl())
                .setFooter(config.getServerName(), config.getServerLogo());

        // Only add transcript link for non-sensitive categories
        if (transcriptUrl != null && !ticket.getCategory().isSensitive()) {
            notification.addField("📝 Transcript", "[Hier klicken](" + transcriptUrl + ")", false);
        }

        for (Long channelId : config.getRatingNotificationChannels()) {
            var channel = jda.getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessageEmbeds(notification.build()).queue();
            }
        }

        return transcriptUrl;
    }
}
