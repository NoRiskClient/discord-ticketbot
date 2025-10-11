package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.interactions.Interaction;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class StatsCommand extends Interaction {
    public StatsCommand(@NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda) {
        super(config, ticketService, jda);
        this.ticketChannelRequired = false;
        addCommand("Show general ticket statistics");
    }

    @Override
    public String getIdentifier() {
        return "stats";
    }

    @Override
    public void handleSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        var data = ticketService.getTicketData();
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
