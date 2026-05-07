package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class StatsWaiting extends AbstractCommand {
    public StatsWaiting(Config config, TicketService ticketService, EmbedBuilder missingPerm, JDA jda) {
        super(config, ticketService, missingPerm, jda);
    }

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;
        if (!hasStaffPermission(event.getMember())) {
            event.replyEmbeds(missingPerm.setFooter(config.getServerName(), config.getServerLogo()).build()).setEphemeral(true).queue();
            return;
        }

        var data = ticketService.getTicketData();

        Map<String, String> nextTicketsForClosing = data.nextTicketsForClosing(100);
        Map<String, Map<String, String>> longestSinceLastSupporterMessage = data.longestSinceLastSupporterMessage(100);

        EmbedBuilder ticketsWaitingForUserInput = new EmbedBuilder()
                .setColor(Color.decode(config.getColor()))
                .setTitle("Tickets waiting for User input")
                .setFooter(config.getServerName(), config.getServerLogo());

        if (!nextTicketsForClosing.isEmpty()) {
            String longestWaiting = nextTicketsForClosing.entrySet().stream()
                    .map(e -> "• <#%s>: <t:%d:R>".formatted(e.getKey(), Instant.parse(e.getValue()).getEpochSecond()))
                    .collect(Collectors.joining("\n"));
            ticketsWaitingForUserInput.setDescription(longestWaiting);
        }

        EmbedBuilder ticketsWaitingForSupporterInput = new EmbedBuilder()
                .setColor(Color.decode(config.getColor()))
                .setTitle("Tickets waiting for Supporter input")
                .setFooter(config.getServerName(), config.getServerLogo());

        StringBuilder description = new StringBuilder();

        if (!longestSinceLastSupporterMessage.isEmpty()) {
            for (Map.Entry<String, Map<String, String>> entry : longestSinceLastSupporterMessage.entrySet()) {
                description.append("<@").append(entry.getKey()).append(">:\n");

                for (Map.Entry<String, String> ticketEntry : entry.getValue().entrySet()) {
                    description.append(" - <#").append(ticketEntry.getKey()).append(">: ").append("<t:").append(ticketEntry.getValue()).append(":R>").append("\n");
                }
            }
            ticketsWaitingForSupporterInput.setDescription(description);
        }

        event.replyEmbeds(ticketsWaitingForUserInput.build(), ticketsWaitingForSupporterInput.build()).setEphemeral(true).queue();
    }
}
