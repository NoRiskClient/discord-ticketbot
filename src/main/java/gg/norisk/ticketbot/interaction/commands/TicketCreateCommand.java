package gg.norisk.ticketbot.interaction.commands;

import gg.norisk.ticketbot.Config;
import gg.norisk.ticketbot.TicketCategory;
import gg.norisk.ticketbot.TicketService;
import gg.norisk.ticketbot.embed.EmbedBuildInfo;
import gg.norisk.ticketbot.embed.Embeds;
import gg.norisk.ticketbot.interaction.Interaction;
import gg.norisk.ticketbot.util.TranslationUtils;
import java.util.HashMap;
import java.util.Map;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;

public class TicketCreateCommand extends Interaction {
  public TicketCreateCommand(
      @NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda) {
    super(config, ticketService, jda);
    this.allowedAnywhere = true;
    addCommand("Start the ticket creation process");
  }

  @Override
  public String getIdentifier() {
    return "create";
  }

  @Override
  public void handleSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
    StringSelectMenu.Builder selectionBuilder =
        StringSelectMenu.create("select-category")
            .setPlaceholder(
                TranslationUtils.translate(
                    "selection.category.placeholder", event.getUserLocale().toLocale()));

    for (TicketCategory category : TicketCategory.values()) {
      selectionBuilder.addOption(
          TranslationUtils.translate(
              "category." + category.getId() + ".label", event.getUserLocale().toLocale()),
          category.getId(),
          TranslationUtils.translate(
              "category." + category.getId() + ".description", event.getUserLocale().toLocale()));
    }

    Channel channel = event.getChannel();

    if (channel.getType() == ChannelType.PRIVATE) {
      reply(new EmbedBuildInfo(Embeds.BASE_MESSAGE, event.getUserLocale().toLocale(), null))
          .addActionRow(selectionBuilder.build())
          .queue();
    } else {
      try {
        String channelId =
            event
                .getUser()
                .openPrivateChannel()
                .flatMap(
                    c ->
                        c.sendMessageEmbeds(
                                Embeds.BASE_MESSAGE.toBuilder(
                                        config,
                                        event.getUserLocale().toLocale(),
                                        Map.of(),
                                        config.getGuild(jda),
                                        null)
                                    .build())
                            .addActionRow(selectionBuilder.build()))
                .complete()
                .getChannelId();

        replyEphemeralAndQueue(
            new EmbedBuildInfo(
                Embeds.TICKET_CREATION_START_SUCCESS,
                event.getUserLocale().toLocale(),
                new HashMap<>(Map.of("DM_CHANNEL", "<#" + channelId + ">"))));
      } catch (ErrorResponseException e) {
        replyEphemeralAndQueue(
            new EmbedBuildInfo(
                Embeds.TICKET_CREATION_START_FAILED,
                event.getUserLocale().toLocale(),
                new HashMap<>(
                    Map.of(
                        "ERROR",
                        TranslationUtils.translate(
                            "message.ticket.creation_start.error.dm_disabled",
                            event.getUserLocale().toLocale())))));
      }
    }
  }
}
