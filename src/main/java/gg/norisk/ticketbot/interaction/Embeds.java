package gg.norisk.ticketbot.interaction;

import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.Nullable;

public class Embeds {
  public static final EmbedBuilder INTERACTION_MISSING_PERMISSIONS =
      failure(
          "message.interaction.missing_permissions.title",
          "message.interaction.missing_permissions.description");

  public static final EmbedBuilder INTERACTION_WRONG_CHANNEL =
      failure(
          "message.interaction.wrong_channel.title",
          "message.interaction.wrong_channel.description");

  public static final EmbedBuilder TICKET_CREATION_FAILED =
      failure("message.ticket.creation.failed.title", "message.ticket.creation.failed.description");

  public static final EmbedBuilder TICKET_CREATION_SUCCESS =
      success(
          "message.ticket.creation.success.title", "message.ticket.creation.success.description");

  public static final EmbedBuilder TICKET_BASE_MESSAGE =
      create(
          "message.ticket.base_message.title",
          "message.ticket.base_message.description",
          null,
          true,
          false);

  private static EmbedBuilder create(
      @Nullable String title,
      @Nullable String message,
      @Nullable Integer color,
      boolean serverFooter,
      boolean userAuthor) {
    EmbedBuilder builder = new EmbedBuilder();
    builder.setTitle(title);
    builder.setDescription(message);

    if (color != null) {
      builder.setColor(color);
    }

    if (serverFooter) {
      builder.setFooter("$SERVER_NAME", "$SERVER_ICON_URL");
    }

    if (userAuthor) {
      builder.setAuthor("$USER_NAME", null, "$USER_AVATAR_URL");
    }

    return builder;
  }

  private static EmbedBuilder success(@Nullable String title, @Nullable String message) {
    return create(title, message, 0x00FF00, true, true);
  }

  private static EmbedBuilder failure(@Nullable String title, @Nullable String message) {
    return create(title, message, 0xFF0000, true, true);
  }
}
