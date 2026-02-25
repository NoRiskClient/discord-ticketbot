package eu.greev.dcbot.ticketsystem.interactions;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.time.Instant;
import java.util.List;

/**
 * Handles ticket closing with mandatory rating flow.
 *
 * Flow:
 * 1. Supporter clicks "Close" button
 * 2. Confirmation embed is shown with "Confirm" button
 * 3. On confirm: ticket moves to rating category, staff is removed, owner gets rating request
 * 4. Owner rates or skips, ticket is deleted
 */

@AllArgsConstructor
@Slf4j
public class TicketClose implements Interaction {
    private final JDA jda;
    private final Config config;
    private final EmbedBuilder wrongChannel;
    private final EmbedBuilder missingPerm;
    private final TicketService ticketService;

    @Override
    public void execute(Event evt) {
        if (evt instanceof ButtonInteractionEvent event) {
            handleClose(event);
            return;
        }

        if (evt instanceof SlashCommandInteractionEvent event) {
            handleClose(event);
        }
    }

    private void handleClose(IReplyCallback event) {
        if (config.getServerName() == null) {
            EmbedBuilder error = new EmbedBuilder()
                    .setColor(Color.RED)
                    .setDescription("❌ **Ticketsystem wasn't setup, please tell an Admin to use </ticket setup:0>!**");
            event.replyEmbeds(error.build()).setEphemeral(true).queue();
            return;
        }

        Ticket ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());
        if (ticket == null) {
            event.replyEmbeds(wrongChannel
                            .setFooter(config.getServerName(), config.getServerLogo())
                            .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                            .build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        // Permission check: Staff, Admin, DevMode, or Owner (if no helper replied yet)
        boolean isStaff = event.getMember().getRoles().contains(jda.getRoleById(config.getStaffId()));
        boolean isAdmin = event.getMember().hasPermission(Permission.ADMINISTRATOR);
        boolean isOwner = event.getUser().getIdLong() == ticket.getOwner().getIdLong();
        boolean canOwnerClose = isOwner && !hasHelperReplied(ticket);

        if (!config.isDevMode() && !isStaff && !isAdmin && !canOwnerClose) {
            event.replyEmbeds(missingPerm.setFooter(config.getServerName(), config.getServerLogo()).build()).setEphemeral(true).queue();
            return;
        }

        if (ticket.isPendingRating()) {
            EmbedBuilder error = new EmbedBuilder()
                    .setColor(Color.RED)
                    .setFooter(config.getServerName(), config.getServerLogo())
                    .addField("Already closing", "This ticket is already waiting for a rating.", false);
            event.replyEmbeds(error.build()).setEphemeral(true).queue();
            return;
        }

        // Show confirmation dialog
        if (ticket.getSupporter() == null) {
            // Unclaimed ticket - direct close
            EmbedBuilder confirmation = new EmbedBuilder()
                    .setColor(Color.YELLOW)
                    .setTitle("Close Ticket #" + ticket.getId() + "?")
                    .setDescription("This ticket has no supporter assigned.\nIt will be closed without a rating request.")
                    .setFooter(config.getServerName(), config.getServerLogo());
            event.replyEmbeds(confirmation.build())
                    .addActionRow(Button.danger("close-confirm-" + ticket.getId(), "Close Ticket"))
                    .setEphemeral(true)
                    .queue();
        } else {
            // Claimed ticket - rating flow
            EmbedBuilder confirmation = new EmbedBuilder()
                    .setColor(Color.YELLOW)
                    .setTitle("Close Ticket #" + ticket.getId() + "?")
                    .setDescription("**What happens when you close:**\n" +
                            "• You and the staff team will lose access to this ticket\n" +
                            "• " + ticket.getOwner().getAsMention() + " will be asked to rate your support\n" +
                            "• The ticket will be deleted after the rating")
                    .setFooter(config.getServerName(), config.getServerLogo());
            event.replyEmbeds(confirmation.build())
                    .addActionRow(Button.danger("close-confirm-" + ticket.getId(), "Close & Request Rating"))
                    .setEphemeral(true)
                    .queue();
        }
    }

    public void executeClose(ButtonInteractionEvent event, int ticketId) {
        // Defer reply immediately to avoid 3-second timeout
        event.deferReply(true).queue();

        Ticket ticket = ticketService.getTicketByTicketId(ticketId);
        if (ticket == null) {
            event.getHook().sendMessage("Ticket not found.").setEphemeral(true).queue();
            return;
        }

        if (ticket.isPendingRating()) {
            event.getHook().sendMessage("This ticket is already being closed.").setEphemeral(true).queue();
            return;
        }

        // If no supporter, close directly
        if (ticket.getSupporter() == null) {
            EmbedBuilder confirmation = new EmbedBuilder()
                    .setColor(Color.GREEN)
                    .setFooter(config.getServerName(), config.getServerLogo())
                    .addField("Ticket closed", "This unclaimed ticket has been closed.", false);
            event.getHook().sendMessageEmbeds(confirmation.build()).setEphemeral(true).queue();
            ticketService.closeTicket(ticket, false, event.getMember(), null);
            return;
        }

        // Check if owner is still in the guild before requesting rating
        Guild guild = jda.getGuildById(config.getServerId());
        if (guild != null && guild.getMember(ticket.getOwner()) == null) {
            // Owner not in guild, close directly without rating
            EmbedBuilder confirmation = new EmbedBuilder()
                    .setColor(Color.GREEN)
                    .setFooter(config.getServerName(), config.getServerLogo())
                    .addField("Ticket closed", "The ticket owner is no longer in the server. Ticket closed without rating.", false);
            event.getHook().sendMessageEmbeds(confirmation.build()).setEphemeral(true).queue();
            ticketService.closeTicket(ticket, false, event.getMember(), "Closed without rating (member not in server)");
            return;
        }

        // Set pending rating state
        ticket.setPendingCloser(event.getUser());
        ticket.setPendingRatingSince(Instant.now());
        ticket.setRatingRemindersSent(0);

        if (ticket.isWaiting()) {
            ticket.setWaiting(false);
            ticket.setWaitingSince(null);
            ticket.setRemindersSent(0);
            // Update channel name to remove waiting emoji
            ticketService.toggleWaiting(ticket, false);
        }

        // Move ticket to pending rating category
        Category pendingCategory = ticketService.getAvailablePendingRatingCategory();
        if (pendingCategory != null) {
            ticket.getTextChannel().getManager()
                    .setParent(pendingCategory)
                    .queue();
        }

        // Remove supporter from ticket (can't see it anymore)
        ticket.getTextChannel()
                .upsertPermissionOverride(event.getGuild().getMember(ticket.getSupporter()))
                .setDenied(Permission.VIEW_CHANNEL)
                .queue();

        // Remove staff role from ticket (can't see it anymore)
        Role staffRole = jda.getRoleById(config.getStaffId());
        if (staffRole != null) {
            ticket.getTextChannel()
                    .upsertPermissionOverride(staffRole)
                    .setDenied(Permission.VIEW_CHANNEL)
                    .queue();
        }

        // Send rating request
        sendRatingRequest(ticket, event.getHook());
    }

    private void sendRatingRequest(Ticket ticket, InteractionHook hook) {
        EmbedBuilder ratingEmbed = new EmbedBuilder()
                .setColor(Color.decode(config.getColor()))
                .setTitle("Rate Your Support Experience")
                .setDescription("Your ticket **#" + ticket.getId() + "** is about to be closed.\nPlease rate your experience with our support team!")
                .addField("Supporter", ticket.getSupporter().getAsMention(), true)
                .setFooter(config.getServerName(), config.getServerLogo());

        try {
            ticket.getOwner().openPrivateChannel()
                    .flatMap(channel -> channel.sendMessageEmbeds(ratingEmbed.build())
                            .addActionRow(
                                    Button.secondary("rating-1-" + ticket.getId(), "⭐"),
                                    Button.secondary("rating-2-" + ticket.getId(), "⭐⭐"),
                                    Button.primary("rating-3-" + ticket.getId(), "⭐⭐⭐"),
                                    Button.primary("rating-4-" + ticket.getId(), "⭐⭐⭐⭐"),
                                    Button.success("rating-5-" + ticket.getId(), "⭐⭐⭐⭐⭐")
                            )
                            .addActionRow(
                                    Button.danger("rating-skip-" + ticket.getId(), "No thanks")
                            ))
                    .queue(
                            success -> {
                                EmbedBuilder confirmation = new EmbedBuilder()
                                        .setColor(Color.GREEN)
                                        .setFooter(config.getServerName(), config.getServerLogo())
                                        .addField("Ticket closed", "The ticket has been closed and " + ticket.getOwner().getAsMention() + " has been asked to rate their experience.", false);
                                hook.sendMessageEmbeds(confirmation.build()).setEphemeral(true).queue();

                                EmbedBuilder waitingEmbed = new EmbedBuilder()
                                        .setColor(Color.YELLOW)
                                        .setDescription("⏳ Waiting for " + ticket.getOwner().getAsMention() + " to submit their rating...");
                                ticket.getTextChannel().sendMessageEmbeds(waitingEmbed.build()).queue();
                            },
                            error -> handleDMFailure(ticket, hook)
                    );
        } catch (Exception e) {
            handleDMFailure(ticket, hook);
        }
    }

    private void handleDMFailure(Ticket ticket, InteractionHook hook) {
        EmbedBuilder ratingEmbed = new EmbedBuilder()
                .setColor(Color.decode(config.getColor()))
                .setTitle("Rate Your Support Experience")
                .setDescription(ticket.getOwner().getAsMention() + ", please rate your experience before this ticket closes!")
                .addField("Supporter", ticket.getSupporter().getAsMention(), true)
                .setFooter(config.getServerName(), config.getServerLogo());

        ticket.getTextChannel().sendMessage(ticket.getOwner().getAsMention())
                .setEmbeds(ratingEmbed.build())
                .addActionRow(
                        Button.secondary("rating-1-" + ticket.getId(), "⭐"),
                        Button.secondary("rating-2-" + ticket.getId(), "⭐⭐"),
                        Button.primary("rating-3-" + ticket.getId(), "⭐⭐⭐"),
                        Button.primary("rating-4-" + ticket.getId(), "⭐⭐⭐⭐"),
                        Button.success("rating-5-" + ticket.getId(), "⭐⭐⭐⭐⭐")
                )
                .addActionRow(
                        Button.danger("rating-skip-" + ticket.getId(), "Nein danke")
                ).queue();

        EmbedBuilder confirmation = new EmbedBuilder()
                .setColor(Color.YELLOW)
                .setFooter(config.getServerName(), config.getServerLogo())
                .addField("Ticket closed", "Could not send DM to ticket owner. Rating request has been sent in this channel.", false);
        hook.sendMessageEmbeds(confirmation.build()).setEphemeral(true).queue();
    }

    /**
     * Checks if any helper (non-owner, non-bot) has replied in the ticket.
     */
    private boolean hasHelperReplied(Ticket ticket) {
        try {
            List<Message> messages = ticket.getTextChannel().getIterableHistory()
                    .takeAsync(100)
                    .get();

            for (Message message : messages) {
                // Skip bots
                if (message.getAuthor().isBot()) continue;
                // Skip the ticket owner
                if (message.getAuthor().getIdLong() == ticket.getOwner().getIdLong()) continue;
                // Someone else wrote - helper has replied
                return true;
            }
        } catch (Exception e) {
            // If we can't check, assume helper has replied (safer)
            return true;
        }
        return false;
    }
}