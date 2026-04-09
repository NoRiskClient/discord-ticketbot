package gg.norisk.ticketbot.interaction.message;

import gg.norisk.ticketbot.Config;
import gg.norisk.ticketbot.TicketService;
import gg.norisk.ticketbot.embed.EmbedBuildInfo;
import gg.norisk.ticketbot.embed.Embeds;
import gg.norisk.ticketbot.interaction.Interaction;
import gg.norisk.ticketbot.util.Result;
import gg.norisk.ticketbot.util.TranslationUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import org.jetbrains.annotations.NotNull;

public class ForwardMessageInteraction extends Interaction {
  public ForwardMessageInteraction(
      @NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda) {
    super(config, ticketService, jda);
    addMessageCommand("Forward");
  }

  @Override
  public String getIdentifier() {
    return "forward";
  }

  @Override
  public void handleMessageContextInteraction(@NotNull MessageContextInteractionEvent event) {
    Result<Void> result =
        ticketService.forwardSupporterMessage(
            event.getTarget(), event.getTarget().getChannel(), event.getUser());

    if (result.isFailure()) {
      replyEphemeralAndQueue(
          new EmbedBuildInfo(
              Embeds.MESSAGE_FORWARD_FAILED,
              config.getStaffLocale(),
              new HashMap<>(
                  Map.of(
                      "ERROR",
                      TranslationUtils.translate(
                          Optional.ofNullable(result.getError())
                              .orElse("message.generic.error.unknown"),
                          event.getUserLocale().toLocale())))));
    } else {
      replyEphemeralAndQueue(
          new EmbedBuildInfo(
              Embeds.MESSAGE_FORWARD_SUCCESS, config.getStaffLocale(), new HashMap<>()));
    }
  }
}
