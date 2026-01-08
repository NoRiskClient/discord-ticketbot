package eu.greev.dcbot.ticketsystem.interactions.modals;

import eu.greev.dcbot.ticketsystem.entities.Rating;
import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.interactions.Interaction;
import eu.greev.dcbot.ticketsystem.service.RatingData;
import eu.greev.dcbot.ticketsystem.service.SupporterSettingsData;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.ticketsystem.service.XpService;
import eu.greev.dcbot.utils.Config;
import me.ryzeon.transcripts.DiscordHtmlTranscripts;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.*;
import java.time.Instant;
import java.util.List;

@Slf4j
public class RatingModal implements Interaction {
    private final TicketService ticketService;
    private final RatingData ratingData;
    private final Config config;
    private final JDA jda;
    private final XpService xpService;
    private final SupporterSettingsData supporterSettingsData;

    public RatingModal(TicketService ticketService, RatingData ratingData, Config config, JDA jda, XpService xpService, SupporterSettingsData supporterSettingsData) {
        this.ticketService = ticketService;
        this.ratingData = ratingData;
        this.config = config;
        this.jda = jda;
        this.xpService = xpService;
        this.supporterSettingsData = supporterSettingsData;
    }

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

        // Generate transcript first to get URL for XP notification
        String transcriptUrl = sendRatingNotification(ticket, stars, message);

        // Award XP to the supporter with transcript URL (async - sends full ticket data)
        xpService.awardTicketXp(ticket, stars, transcriptUrl);

        ticket.setPendingRatingSince(null);

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

    private String getStarDisplay(int stars) {
        return "\u2605".repeat(stars) + "\u2606".repeat(5 - stars);
    }

    private String sendRatingNotification(Ticket ticket, int stars, String message) {
        String starDisplay = getStarDisplay(stars);
        Color embedColor = stars >= 4 ? Color.GREEN : stars == 3 ? Color.YELLOW : Color.RED;

        // Generate HTML transcript and upload to log channel to get URL
        String transcriptUrl = null;
        try {
            if (ticket.getTextChannel() != null && config.getLogChannel() != 0) {
                // Fetch messages first to check if channel has content
                var messages = ticket.getTextChannel().getIterableHistory()
                        .takeAsync(1000)
                        .get();

                if (messages != null && !messages.isEmpty()) {
                    FileUpload transcriptUpload = DiscordHtmlTranscripts.getInstance()
                            .createTranscript(ticket.getTextChannel(), "transcript-" + ticket.getId() + ".html");

                    var logChannel = jda.getTextChannelById(config.getLogChannel());
                    if (logChannel != null) {
                        var uploadMessage = logChannel.sendFiles(transcriptUpload).complete();
                        // Use message jump URL instead of attachment URL for better Discord navigation
                        transcriptUrl = uploadMessage.getJumpUrl();
                    }
                } else {
                    log.warn("No messages found in ticket #{} channel, skipping transcript generation", ticket.getId());
                }
            }
        } catch (Exception e) {
            log.error("Failed to generate/upload HTML transcript for ticket #{}: {}", ticket.getId(), e.getMessage());
            // Continue without transcript - don't let this block the rating notification
        }

        if (config.getRatingNotificationChannels() == null || config.getRatingNotificationChannels().isEmpty()) {
            return transcriptUrl;
        }

        // Check privacy setting for supporter
        boolean hideStats = supporterSettingsData.isHideStats(ticket.getSupporter().getId());
        String displayName = hideStats ? "Anonym" : ticket.getSupporter().getAsMention();
        String displayStars = hideStats ? "???" : stars + " Sterne";
        String displayStarIcons = hideStats ? "★★★★★" : starDisplay;
        String thumbnailUrl = hideStats ? null : ticket.getSupporter().getEffectiveAvatarUrl();
        String displayFeedback = hideStats ? "Versteckt" : ((message != null && !message.isBlank()) ? message : "Kein Feedback");

        EmbedBuilder notification = new EmbedBuilder()
                .setColor(embedColor)
                .setTitle("Ticket #" + ticket.getId() + " closed")
                .setDescription(displayName + " hat **" + displayStars + "** " + displayStarIcons + " erhalten und ein Ticket gelöst!")
                .setFooter(config.getServerName(), config.getServerLogo());

        if (thumbnailUrl != null) {
            notification.setThumbnail(thumbnailUrl);
        }

        notification.addField("Feedback", displayFeedback, false);

        // Only add transcript link for non-sensitive categories
        if (transcriptUrl != null && !ticket.getCategory().isSensitive()) {
            notification.addField("📝 Transcript", "[Hier klicken](" + transcriptUrl + ")", false);
        }

        List<Long> ratingNotificationChannels = config.getRatingNotificationChannels();

        if (stars <= 3) {
            ratingNotificationChannels.addAll(config.getLowRatingNotificationChannels());
        }

        for (Long channelId : ratingNotificationChannels) {
            var channel = jda.getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessageEmbeds(notification.build()).queue();
            }
        }

        return transcriptUrl;
    }
}
