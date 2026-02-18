package gg.norisk.ticketbot.interaction.mixed;

import gg.norisk.ticketbot.Config;
import gg.norisk.ticketbot.TicketService;
import gg.norisk.ticketbot.embed.EmbedBuildInfo;
import gg.norisk.ticketbot.embed.Embeds;
import gg.norisk.ticketbot.interaction.Interaction;
import gg.norisk.ticketbot.util.Result;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import org.jetbrains.annotations.NotNull;

public class TicketClaimInteraction extends Interaction {
  public TicketClaimInteraction(
      @NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda) {
    super(config, ticketService, jda);
    addCommand("Makes you the new supporter of the ticket");
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

  private void handleShared(IReplyCallback event) {
    Result<Void> result =
        ticketService.claimTicket(Objects.requireNonNull(ticket), event.getUser());

    if (result.isFailure()) {
      replyEphemeralAndQueue(
          new EmbedBuildInfo(
              Embeds.TICKET_CLAIM_FAILED,
              ticket.getLocale(),
              new HashMap<>(
                  Map.of(
                      "ERROR", Optional.ofNullable(result.getError()).orElse("Unknown error")))));
    } else {
      replyEphemeralAndQueue(
          new EmbedBuildInfo(Embeds.TICKET_CLAIM_SUCCESS, ticket.getLocale(), null));
    }
  }
}
