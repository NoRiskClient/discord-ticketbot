package gg.norisk.ticketbot.interaction.mixed;

import gg.norisk.ticketbot.Config;
import gg.norisk.ticketbot.TicketService;
import gg.norisk.ticketbot.embed.EmbedBuildInfo;
import gg.norisk.ticketbot.embed.Embeds;
import gg.norisk.ticketbot.interaction.Interaction;
import gg.norisk.ticketbot.util.Result;
import gg.norisk.ticketbot.util.TranslationUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import org.jetbrains.annotations.NotNull;

public class TicketCloseInteraction extends Interaction {
  public TicketCloseInteraction(
      @NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda) {
    super(config, ticketService, jda);
    this.allowedInPrivateTicketChannel = true;
    addCommand("Close the ticket");
  }

  @Override
  public String getIdentifier() {
    return "close";
  }

  @Override
  public void handleButtonInteraction(@NotNull ButtonInteractionEvent event) {
    handleShared(event);
  }

  @Override
  public void handleSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
    handleShared(event);
  }

  private void handleShared(@NotNull IReplyCallback event) {
    Result<Void> result =
        ticketService.closeTicket(
            Objects.requireNonNull(ticket), Objects.requireNonNull(event.getMember()), null, event);

    if (result.isFailure()) {
      replyEphemeralAndQueue(
          new EmbedBuildInfo(
              Embeds.TICKET_CLOSE_FAILED,
              event.getUserLocale().toLocale(),
              new HashMap<>(
                  Map.of(
                      "ERROR",
                      TranslationUtils.translate(
                          "message.ticket.close.error." + result.getError(),
                          event.getUserLocale().toLocale())))));
    }
  }
}
