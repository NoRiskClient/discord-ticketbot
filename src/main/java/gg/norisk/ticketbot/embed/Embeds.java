package gg.norisk.ticketbot.embed;

import java.util.List;
import org.jetbrains.annotations.Nullable;

public class Embeds {
  public static final EmbedDefinition INTERACTION_MISSING_PERMISSIONS =
      failure(
          "message.interaction.missing_permissions.title",
          "message.interaction.missing_permissions.description");

  public static final EmbedDefinition INTERACTION_WRONG_CHANNEL =
      failure(
          "message.interaction.wrong_channel.title",
          "message.interaction.wrong_channel.description");

  public static final EmbedDefinition TICKET_CREATION_FAILED =
      failure("message.ticket.creation.failed.title", "message.ticket.creation.failed.description");

  public static final EmbedDefinition TICKET_CREATION_SUCCESS =
      success(
          "message.ticket.creation.success.title", "message.ticket.creation.success.description");

  public static final EmbedDefinition TICKET_BASE_MESSAGE =
      new EmbedDefinition(
          "message.base_message.title",
          "message.base_message.description",
          false,
          true,
          List.of(),
          null);

  private static EmbedDefinition success(@Nullable String title, @Nullable String message) {
    return new EmbedDefinition(title, message, true, true, List.of(), 0x00FF00);
  }

  private static EmbedDefinition failure(@Nullable String title, @Nullable String message) {
    return new EmbedDefinition(title, message, true, true, List.of(), 0xFF0000);
  }
}
