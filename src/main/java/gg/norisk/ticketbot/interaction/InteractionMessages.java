package gg.norisk.ticketbot.interaction;

import net.dv8tion.jda.api.EmbedBuilder;

public class InteractionMessages {
  public static final EmbedBuilder INTERACTION_MISSING_PERMISSIONS =
      failure(
          "message.interaction.missing_permissions.title",
          "message.interaction.missing_permissions.description");

  public static final EmbedBuilder INTERACTION_WRONG_CHANNEL =
      failure(
          "message.interaction.wrong_channel.title",
          "message.interaction.wrong_channel.description");

  private static EmbedBuilder success(String title, String message) {
    return new EmbedBuilder().setTitle(title).setDescription(message).setColor(0x00FF00);
  }

  private static EmbedBuilder failure(String title, String message) {
    return new EmbedBuilder().setTitle(title).setDescription(message).setColor(0xFF0000);
  }
}
