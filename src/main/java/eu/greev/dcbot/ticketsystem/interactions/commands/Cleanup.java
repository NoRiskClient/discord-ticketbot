package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;

@Slf4j
public class Cleanup extends AbstractCommand {
    public Cleanup(Config config, TicketService ticketService, EmbedBuilder missingPerm, JDA jda) {
        super(config, ticketService, missingPerm, jda);
    }

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;
        if (!hasStaffPermission(event.getMember())) {
            event.replyEmbeds(missingPerm.setFooter(config.getServerName(), config.getServerLogo()).build()).setEphemeral(true).queue();
            return;
        }

        ticketService.consolidateCategoriesAndCleanup();

        log.info("Manual cleanup triggered by {}", event.getUser().getName());

        event.replyEmbeds(new EmbedBuilder()
                .setColor(Color.decode(config.getColor()))
                .setTitle("Cleanup")
                .setDescription("✅ **Started cleanup...**")
                .setFooter(config.getServerName(), config.getServerLogo())
                .build()
        ).setEphemeral(true).queue();
    }
}
