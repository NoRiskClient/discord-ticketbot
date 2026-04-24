package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;

public class ListClaimedTickets extends AbstractCommand {
    public ListClaimedTickets(Config config, TicketService ticketService, EmbedBuilder missingPerm, JDA jda) {
        super(config, ticketService, missingPerm, jda);
    }

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;
        Member member = event.getMember();
        if (!hasStaffPermission(member)) {
            event.replyEmbeds(missingPerm.setFooter(config.getServerName(), config.getServerLogo()).build()).setEphemeral(true).queue();
            return;
        }

        StringBuilder stringBuilder = new StringBuilder();
        ticketService.getTicketData().getOpenTicketsChannelIdsBySupporter(member.getId()).forEach(id -> stringBuilder.append("- <#").append(id).append(">\n"));

        event.replyEmbeds(new EmbedBuilder()
                .setColor(Color.decode(config.getColor()))
                .setTitle("Your claimed tickets")
                .setDescription(stringBuilder.isEmpty() ? "You don't have any open tickets." : stringBuilder.toString())
                .setAuthor(member.getEffectiveName(), null, member.getEffectiveAvatarUrl())
                .setFooter(config.getServerName(), config.getServerLogo())
                .build()).setEphemeral(true).queue();
    }
}
