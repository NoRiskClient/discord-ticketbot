package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.util.Map;

public class Stats extends AbstractCommand {
    public Stats(Config config, TicketService ticketService, EmbedBuilder missingPerm, JDA jda) {
        super(config, ticketService, missingPerm, jda);
    }

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;
        if (!event.getMember().getRoles().contains(jda.getRoleById(config.getStaffId()))) {
            event.replyEmbeds(missingPerm.setFooter(config.getServerName(), config.getServerLogo()).build()).setEphemeral(true).queue();
            return;
        }

        var data = ticketService.getTicketData();
        int total = data.countTotalTickets();
        int open = data.countOpenTickets();
        int waiting = data.countWaitingTickets();

        Map<String, Integer> topClosers = data.topClosers(5);
        Map<String, Integer> topSupporters = data.topSupporters(10);
        Map<String, Long> nextTicketsForClosing = data.nextTicketsForClosing(3);

        EmbedBuilder builder = new EmbedBuilder()
                .setColor(Color.decode(config.getColor()))
                .setTitle("Ticket statistics")
                .setFooter(config.getServerName(), config.getServerLogo());

        builder.addField("Totals", "Total: **" + total + "**\nOpen: **" + open + "**\nWaiting: **" + waiting + "**", false);

        if (!topClosers.isEmpty()) {
            String nameFromUserId = getNameListFromUserId(topClosers);
            builder.addField("Top ticket closers", nameFromUserId, false);
        }

        if (!topSupporters.isEmpty()) {
            String nameFromUserId = getNameListFromUserId(topSupporters);
            builder.addField("Users with most open tickets", nameFromUserId, false);
        }

        if (!nextTicketsForClosing.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Long> e : nextTicketsForClosing.entrySet()) {
                String channelId = e.getKey();
                Long time = e.getValue();
                sb.append("• ").append(channelId).append(": ").append("<t:").append(time).append(":R>").append("\n");
            }
            builder.addField("Longest waiting tickets", sb.toString(), false);
        }

        event.replyEmbeds(builder.build()).setEphemeral(true).queue();
    }

    private String getNameListFromUserId(Map<String, Integer> topClosers) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : topClosers.entrySet()) {
            String userId = e.getKey();
            Integer count = e.getValue();
            String name = userId;
            User u = jda.retrieveUserById(userId).complete();
            if (u != null) {
                name = u.getName();
            }
            sb.append("• ").append(name).append(": ").append(count).append("\n");
        }
        return sb.toString();
    }
}
