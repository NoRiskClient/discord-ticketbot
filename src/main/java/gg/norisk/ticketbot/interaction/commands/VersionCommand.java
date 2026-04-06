package gg.norisk.ticketbot.interaction.commands;

import gg.norisk.ticketbot.Config;
import gg.norisk.ticketbot.Main;
import gg.norisk.ticketbot.TicketService;
import gg.norisk.ticketbot.embed.EmbedBuildInfo;
import gg.norisk.ticketbot.embed.Embeds;
import gg.norisk.ticketbot.interaction.Interaction;
import java.util.HashMap;
import java.util.Map;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

public class VersionCommand extends Interaction {
  public VersionCommand(
      @NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda) {
    super(config, ticketService, jda);
    this.allowedAnywhere = true;
    addCommand("Displays the current version of the bot");
  }

  @Override
  public String getIdentifier() {
    return "version";
  }

  @Override
  public void handleSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
    replyEphemeralAndQueue(
        new EmbedBuildInfo(
            Embeds.VERSION_INFO,
            event.getUserLocale().toLocale(),
            new HashMap<>(Map.of("VERSION", Main.VERSION))));
  }
}
