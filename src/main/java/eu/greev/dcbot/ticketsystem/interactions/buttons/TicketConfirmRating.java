package eu.greev.dcbot.ticketsystem.interactions.buttons;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;

@AllArgsConstructor
public class TicketConfirmRating extends AbstractButton {
    private final TicketService ticketService;
    private final Config config;

    @Override
    public void execute(Event evt) {
        ButtonInteractionEvent event = (ButtonInteractionEvent) evt;
        Ticket ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());

        if (ticket == null) {
            event.reply("Ticket not found.").setEphemeral(true).queue();
            return;
        }

        if (ticket.getSupporter() == null) {
            EmbedBuilder error = new EmbedBuilder()
                    .setColor(Color.RED)
                    .setFooter(config.getServerName(), config.getServerLogo())
                    .addField("Cannot request rating", "This ticket has no supporter assigned. Please claim the ticket first or use a different close option.", false);
            event.replyEmbeds(error.build()).setEphemeral(true).queue();
            return;
        }

        if (ticket.isPendingRating()) {
            EmbedBuilder error = new EmbedBuilder()
                    .setColor(Color.RED)
                    .setFooter(config.getServerName(), config.getServerLogo())
                    .addField("Rating already pending", "A rating request has already been sent for this ticket.", false);
            event.replyEmbeds(error.build()).setEphemeral(true).queue();
            return;
        }

        ticket.setPendingRating(true);
        ticket.setPendingCloser(event.getUser());
        ticket.setPendingRatingSince(java.time.Instant.now());
        ticket.setRatingRemindersSent(0);

        // Move ticket to pending rating category (if configured)
        if (config.getPendingRatingCategory() != 0) {
            Category pendingCategory = event.getJDA().getCategoryById(config.getPendingRatingCategory());
            if (pendingCategory != null) {
                ticket.getTextChannel().getManager()
                        .setParent(pendingCategory)
                        .queue();
            }
        }

        // Disable MESSAGE_SEND for owner
        ticket.getTextChannel()
                .upsertPermissionOverride(event.getGuild().getMember(ticket.getOwner()))
                .setDenied(Permission.MESSAGE_SEND)
                .queue();

        sendRatingRequest(ticket, event);
    }

    private void sendRatingRequest(Ticket ticket, ButtonInteractionEvent event) {
        EmbedBuilder ratingEmbed = new EmbedBuilder()
                .setColor(Color.decode(config.getColor()))
                .setTitle("Rate Your Support Experience")
                .setDescription("Your ticket **#" + ticket.getId() + "** is about to be closed.\nPlease rate your experience with our support team!")
                .addField("Supporter", ticket.getSupporter().getAsMention(), true)
                .setFooter(config.getServerName(), config.getServerLogo());

        try {
            ticket.getOwner().openPrivateChannel()
                    .flatMap(channel -> channel.sendMessageEmbeds(ratingEmbed.build())
                            .setActionRow(
                                    Button.secondary("rating-1-" + ticket.getId(), "⭐"),
                                    Button.secondary("rating-2-" + ticket.getId(), "⭐⭐"),
                                    Button.primary("rating-3-" + ticket.getId(), "⭐⭐⭐"),
                                    Button.primary("rating-4-" + ticket.getId(), "⭐⭐⭐⭐"),
                                    Button.success("rating-5-" + ticket.getId(), "⭐⭐⭐⭐⭐")
                            ))
                    .queue(
                            success -> {
                                EmbedBuilder confirmation = new EmbedBuilder()
                                        .setColor(Color.GREEN)
                                        .setFooter(config.getServerName(), config.getServerLogo())
                                        .addField("Rating request sent", "A rating request has been sent to " + ticket.getOwner().getAsMention() + " via DM.\nThe ticket will close once they submit their rating.", false);
                                event.replyEmbeds(confirmation.build()).setEphemeral(true).queue();

                                EmbedBuilder waitingEmbed = new EmbedBuilder()
                                        .setColor(Color.YELLOW)
                                        .setDescription("Waiting for " + ticket.getOwner().getAsMention() + " to submit their rating before closing...");
                                ticket.getTextChannel().sendMessageEmbeds(waitingEmbed.build()).queue();
                            },
                            error -> handleDMFailure(ticket, event)
                    );
        } catch (Exception e) {
            handleDMFailure(ticket, event);
        }
    }

    private void handleDMFailure(Ticket ticket, ButtonInteractionEvent event) {
        EmbedBuilder ratingEmbed = new EmbedBuilder()
                .setColor(Color.decode(config.getColor()))
                .setTitle("Rate Your Support Experience")
                .setDescription(ticket.getOwner().getAsMention() + ", please rate your experience before this ticket closes!")
                .addField("Supporter", ticket.getSupporter().getAsMention(), true)
                .setFooter(config.getServerName(), config.getServerLogo());

        ticket.getTextChannel().sendMessage(ticket.getOwner().getAsMention())
                .setEmbeds(ratingEmbed.build())
                .setActionRow(
                        Button.secondary("rating-1-" + ticket.getId(), "⭐"),
                        Button.secondary("rating-2-" + ticket.getId(), "⭐⭐"),
                        Button.primary("rating-3-" + ticket.getId(), "⭐⭐⭐"),
                        Button.primary("rating-4-" + ticket.getId(), "⭐⭐⭐⭐"),
                        Button.success("rating-5-" + ticket.getId(), "⭐⭐⭐⭐⭐")
                ).queue();

        EmbedBuilder confirmation = new EmbedBuilder()
                .setColor(Color.YELLOW)
                .setFooter(config.getServerName(), config.getServerLogo())
                .addField("Rating request sent", "Could not send DM to ticket owner. Rating request has been sent in this channel.", false);
        event.replyEmbeds(confirmation.build()).setEphemeral(true).queue();
    }
}
