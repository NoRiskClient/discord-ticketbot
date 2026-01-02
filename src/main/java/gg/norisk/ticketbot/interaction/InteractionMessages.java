package gg.norisk.ticketbot.interaction;

public class InteractionMessages {
  public static final CustomEmbedBuilder INTERACTION_MISSING_PERMISSIONS =
      failure(
          "message.interaction.missing_permissions.title",
          "message.interaction.missing_permissions.description");

  private static CustomEmbedBuilder success(String title, String message) {
    return new CustomEmbedBuilder().setTitle(title).setDescription(message).setColor(0x00FF00);
  }

  private static CustomEmbedBuilder failure(String title, String message) {
    return new CustomEmbedBuilder().setTitle(title).setDescription(message).setColor(0xFF0000);
  }
}
