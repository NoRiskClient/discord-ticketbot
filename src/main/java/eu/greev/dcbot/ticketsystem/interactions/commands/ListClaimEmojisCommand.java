package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.interactions.Interaction;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class ListClaimEmojisCommand extends Interaction {
    public ListClaimEmojisCommand(@NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda) {
        super(config, ticketService, jda);
        addCommand("List all claim emojis");
    }

    @Override
    public String getIdentifier() {
        return "list-claim-emojis";
    }

    @Override
    public void handleSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
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
