package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;

public class ForceClose extends AbstractCommand {
    private final EmbedBuilder wrongChannel;

    public ForceClose(Config config, TicketService ticketService, EmbedBuilder missingPerm, EmbedBuilder wrongChannel, JDA jda) {
        super(config, ticketService, missingPerm, jda);
        this.wrongChannel = wrongChannel;
    }

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;

        if (config.getServerName() == null) {
            EmbedBuilder error = new EmbedBuilder()
                    .setColor(Color.RED)
                    .setDescription("**Ticketsystem wasn't setup, please tell an Admin to use </ticket setup:0>!**");
            event.replyEmbeds(error.build()).setEphemeral(true).queue();
            return;
        }

        // Only staff or admins can force close
        if (!hasStaffPermission(event.getMember())) {
            event.replyEmbeds(missingPerm.setFooter(config.getServerName(), config.getServerLogo()).build()).setEphemeral(true).queue();
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

        EmbedBuilder confirmation = new EmbedBuilder()
                .setColor(Color.GREEN)
                .setFooter(config.getServerName(), config.getServerLogo())
                .addField("Ticket force-closed", "This ticket has been closed without rating.", false);
        event.replyEmbeds(confirmation.build()).setEphemeral(true).queue();

        // Close directly without rating flow
        ticketService.closeTicket(ticket, false, event.getMember(), null);
    }
}
