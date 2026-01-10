package gg.norisk.ticketbot.interaction;

import net.dv8tion.jda.api.EmbedBuilder;

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

  private static EmbedBuilder withColor(String title, String message, int color) {
    return new EmbedBuilder().setTitle(title).setDescription(message).setColor(color);
  }

  private static EmbedBuilder success(String title, String message) {
    return withColor(title, message, 0x00FF00);
  }

  private static EmbedBuilder failure(String title, String message) {
    return withColor(title, message, 0xFF0000);
  }
}
