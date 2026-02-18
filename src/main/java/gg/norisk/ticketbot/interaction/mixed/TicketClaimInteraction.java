package gg.norisk.ticketbot.interaction.mixed;

import gg.norisk.ticketbot.Config;
import gg.norisk.ticketbot.TicketService;
import gg.norisk.ticketbot.interaction.Interaction;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import org.jetbrains.annotations.NotNull;

public class TicketClaimInteraction extends Interaction {
  public TicketClaimInteraction(
      @NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda) {
    super(config, ticketService, jda);
  }

  @Override
  public String getIdentifier() {
    return "claim";
  }

  @Override
  public void handleButtonInteraction(@NotNull ButtonInteractionEvent event) {
    handleShared(event);
  }

  @Override
  public void handleSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
    handleShared(event);
  }

  private void handleShared(IReplyCallback event) {}
}
