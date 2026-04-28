package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.service.TicketData;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Stats extends AbstractCommand {
    public Stats(Config config, TicketService ticketService, EmbedBuilder missingPerm, JDA jda) {
        super(config, ticketService, missingPerm, jda);
    }

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;
        if (!hasStaffPermission(event.getMember())) {
            event.replyEmbeds(missingPerm.setFooter(config.getServerName(), config.getServerLogo()).build()).setEphemeral(true).queue();
            return;
        }

        TicketData data = ticketService.getTicketData();
        int total = data.countTotalTickets();
        int open = data.countOpenTickets();
        int waiting = data.countWaitingTickets();

        Map<String, Integer> topClosers = data.topClosers(5);
        Map<String, Integer> topSupporters = data.topSupporters(10);
        Map<String, String> nextTicketsForClosing = data.nextTicketsForClosing(3);

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
            builder.addField("Helpers with most open tickets", nameFromUserId, false);
        }

        if (!nextTicketsForClosing.isEmpty()) {
            String longestWaiting = nextTicketsForClosing.entrySet().stream()
                    .map(e -> "• <#%s>: <t:%d:R>".formatted(e.getKey(), Instant.parse(e.getValue()).getEpochSecond()))
                    .collect(Collectors.joining("\n"));
            builder.addField("Longest waiting tickets", longestWaiting, false);
        }

        event.replyEmbeds(builder.build()).setEphemeral(true).queue();
    }

    private String getNameListFromUserId(Map<String, Integer> topClosers) {
        return topClosers.entrySet().stream()
                .map(e -> Map.entry(Optional.ofNullable(jda.retrieveUserById(e.getKey()).complete()), e.getValue()))
                .filter(e -> e.getKey().isPresent())
                .map(e -> "• %s: %d".formatted(e.getKey().get().getAsMention(), e.getValue()))
                .collect(Collectors.joining("\n"));
    }
}
