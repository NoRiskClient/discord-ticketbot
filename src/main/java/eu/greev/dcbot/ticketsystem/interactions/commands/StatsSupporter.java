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

public class StatsSupporter extends AbstractCommand {
    public StatsSupporter(Config config, TicketService ticketService, EmbedBuilder missingPerm, JDA jda) {
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

        int days = event.getOption("days").getAsInt();
        Map<String, Integer> topClosers = data.topClosers(100, days);

        EmbedBuilder builder = new EmbedBuilder()
                .setColor(Color.decode(config.getColor()))
                .setTitle("Ticket statistics")
                .setFooter(config.getServerName(), config.getServerLogo());

        if (!topClosers.isEmpty()) {
            String nameFromUserId = getNameListFromUserId(topClosers);
            builder.addField("Top ticket closers", nameFromUserId, false);
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
