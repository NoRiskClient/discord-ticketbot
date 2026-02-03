package gg.norisk.ticketbot.interaction.modal;

import gg.norisk.ticketbot.Config;
import gg.norisk.ticketbot.TicketCategory;
import gg.norisk.ticketbot.TicketService;
import gg.norisk.ticketbot.embed.EmbedBuildInfo;
import gg.norisk.ticketbot.embed.Embeds;
import gg.norisk.ticketbot.entities.Ticket;
import gg.norisk.ticketbot.interaction.Interaction;
import gg.norisk.ticketbot.util.Result;
import java.util.Map;
import java.util.Optional;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import org.jetbrains.annotations.NotNull;

public class TicketCreationModalInteraction extends Interaction {
  private final TicketCategory category;

  public TicketCreationModalInteraction(
      @NotNull Config config,
      @NotNull TicketService ticketService,
      @NotNull JDA jda,
      @NotNull TicketCategory category) {
    super(config, ticketService, jda);
    this.category = category;
  }

  @Override
  public String getIdentifier() {
    return category.getId();
  }

  @Override
  public void handleModalInteraction(@NotNull ModalInteractionEvent event) {
    deferEphemeralAndQueue(
        () -> {
          Result<Ticket> result =
              ticketService.createTicket(
                  category.extractInfo(event.getInteraction()),
                  category,
                  event.getInteraction().getUser());

          if (result.getError() == null
              && result.getValue() != null
              && result.getValue().getChannel() != null) {
            return new EmbedBuildInfo(
                Embeds.TICKET_CREATION_SUCCESS,
                event.getInteraction().getUserLocale().toLocale(),
                Map.of("CHANNEL", result.getValue().getChannel().getId()));
          } else {
            return new EmbedBuildInfo(
                Embeds.TICKET_CREATION_FAILED,
                event.getInteraction().getUserLocale().toLocale(),
                Map.of("ERROR", Optional.ofNullable(result.getError()).orElse("Unknown error")));
          }
        });
  }
}
