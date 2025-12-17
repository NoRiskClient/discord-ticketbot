package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.Main;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.util.List;

public class DebugStats extends AbstractCommand {

    public DebugStats(Config config, TicketService ticketService, EmbedBuilder missingPerm, JDA jda) {
        super(config, ticketService, missingPerm, jda);
    }

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;

        if (!hasStaffPermission(event.getMember())) {
            event.replyEmbeds(missingPerm.setFooter(config.getServerName(), config.getServerLogo()).build()).setEphemeral(true).queue();
            return;
        }

        String type = event.getOption("type") != null ? event.getOption("type").getAsString() : "daily";

        List<MessageEmbed> embeds;
        switch (type.toLowerCase()) {
            case "weekly" -> embeds = Main.getRatingStatsScheduler().buildWeeklyReport();
            case "monthly" -> embeds = Main.getRatingStatsScheduler().buildMonthlyReport();
            default -> embeds = Main.getRatingStatsScheduler().buildDailyReport();
        }

        if (embeds.isEmpty()) {
            EmbedBuilder noData = new EmbedBuilder()
                    .setColor(Color.ORANGE)
                    .setTitle("Keine Daten")
                    .setDescription("Für den Zeitraum '" + type + "' sind keine Statistiken verfügbar.")
                    .setFooter(config.getServerName(), config.getServerLogo());
            event.replyEmbeds(noData.build()).setEphemeral(true).queue();
            return;
        }

        event.replyEmbeds(embeds).setEphemeral(true).queue();
    }
}
