package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;

public class ListClaimEmojis extends AbstractCommand {
    public ListClaimEmojis(Config config, TicketService ticketService, EmbedBuilder missingPerm, JDA jda) {
        super(config, ticketService, missingPerm, jda);
    }

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;
        if (!hasStaffPermission(event.getMember())) {
            event.replyEmbeds(missingPerm.setFooter(config.getServerName(), config.getServerLogo()).build()).setEphemeral(true).queue();
            return;
        }

        StringBuilder builder = new StringBuilder();

        config.getClaimEmojis().forEach((id, emoji) -> builder.append("<@%s>".formatted(id)).append(": ").append(emoji).append("\n"));

        event.replyEmbeds(new EmbedBuilder()
                .setColor(Color.decode(config.getColor()))
                .setTitle("Claim Emojis")
                .setDescription(builder.isEmpty() ? "No claim emojis set." : builder.toString())
                .setFooter(config.getServerName(), config.getServerLogo())
                .build()
        ).setEphemeral(true).queue();
    }
}
