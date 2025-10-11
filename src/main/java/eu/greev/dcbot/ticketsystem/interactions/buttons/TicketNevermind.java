package eu.greev.dcbot.ticketsystem.interactions.buttons;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.interactions.Interaction;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class TicketNevermind extends Interaction {
    public TicketNevermind(@NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda) {
        super(config, ticketService, jda);
    }

    @Override
    public String getIdentifier() {
        return "nevermind";
    }

    @Override
    public void handleButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (config.getServerName() == null) {
            EmbedBuilder error = new EmbedBuilder()
                    .setColor(Color.RED)
                    .setDescription("❌ **Ticketsystem wasn't setup, please tell an Admin to use </ticket setup:0>!**");
            event.replyEmbeds(error.build()).setEphemeral(true).queue();
            return;
        }
        Ticket ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());
        if (ticket.getOwner().equals(event.getUser())) {
            ticketService.closeTicket(ticket, true, event.getUser(), null);
        }else {
            EmbedBuilder builder = new EmbedBuilder().setColor(Color.RED)
                    .addField("❌ **Missing access**", "You can not click this button", false)
                    .setFooter(config.getServerName(), config.getServerLogo());
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
        }
    }
}