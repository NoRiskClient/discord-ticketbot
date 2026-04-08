package gg.norisk.ticketbot.interaction.modals;

import gg.norisk.ticketbot.Config;
import gg.norisk.ticketbot.TicketCategory;
import gg.norisk.ticketbot.TicketService;
import gg.norisk.ticketbot.embed.Embeds;
import gg.norisk.ticketbot.entities.Ticket;
import gg.norisk.ticketbot.interaction.Interaction;
import gg.norisk.ticketbot.util.Result;
import gg.norisk.ticketbot.util.TranslationUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
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
    this.allowedAnywhere = true;
  }

  @Override
  public String getIdentifier() {
    return category.getId();
  }

  @Override
  public void handleModalInteraction(@NotNull ModalInteractionEvent event) {
    event.deferReply().queue();

    Result<Ticket> result =
        ticketService.createTicket(
            category.extractInfo(event.getInteraction()),
            category,
            event.getInteraction().getUser(),
            event.getUserLocale().toLocale());

    if (!result.isFailure()
        && result.getValue() != null
        && result.getValue().getChannel() != null) {
      StringBuilder details = new StringBuilder();

      for (Map.Entry<String, String> entry : result.getValue().getInfo().entrySet()) {
        details
            .append("**")
            .append(
                TranslationUtils.translate(
                    "category." + category.getId() + "." + entry.getKey() + ".label",
                    event.getUserLocale().toLocale()))
            .append("**\n")
            .append(entry.getValue())
            .append("\n");
      }

      event
          .getHook()
          .sendMessageEmbeds(
              Embeds.TICKET_CREATION_SUCCESS.toBuilder(
                      config,
                      event.getUserLocale().toLocale(),
                      new HashMap<>(
                          Map.of(
                              "ID",
                              String.valueOf(result.getValue().getId()),
                              "CATEGORY",
                              TranslationUtils.translate(
                                  "category." + result.getValue().getCategory().getId() + ".label",
                                  event.getUserLocale().toLocale()),
                              "DETAILS",
                              details.toString())),
                      config.getGuild(jda),
                      event.getUser())
                  .setImage("https://cdn.norisk.gg/misc/nrc_ticket_banner.png")
                  .build())
          .addActionRow(Button.danger("close", "Close"))
          .queue();
    } else {
      event
          .getHook()
          .sendMessageEmbeds(
              Embeds.TICKET_CREATION_FAILED.toBuilder(
                      config,
                      event.getUserLocale().toLocale(),
                      new HashMap<>(
                          Map.of(
                              "ERROR",
                              TranslationUtils.translate(
                                  Optional.ofNullable(result.getError())
                                      .orElse("message.generic.error.unknown"),
                                  event.getUserLocale().toLocale()))),
                      config.getGuild(jda),
                      event.getUser())
                  .build())
          .queue();
    }
  }
}
