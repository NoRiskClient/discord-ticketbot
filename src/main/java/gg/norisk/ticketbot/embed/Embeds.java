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

  public static final EmbedDefinition TICKET_CREATION_FAILED = failure("ticket.creation");

  public static final EmbedDefinition TICKET_CREATION_SUCCESS = success("ticket.creation");

  public static final EmbedDefinition BASE_MESSAGE =
      new EmbedDefinition(
          "message.base_message.title",
          "message.base_message.description",
          false,
          true,
          List.of(),
          null);

  public static final EmbedDefinition VERSION_INFO =
      new EmbedDefinition(
          "message.version_info.title",
          "message.version_info.description",
          false,
          true,
          List.of(),
          null);

  public static final EmbedDefinition INITIAL_MESSAGE =
      new EmbedDefinition(
          null,
          "message.ticket.initial.description",
          true,
          true,
          List.of(
              new EmbedDefinition.Field("message.ticket.initial.field.id", "$ID", true),
              new EmbedDefinition.Field("message.ticket.initial.field.category", "$CATEGORY", true),
              new EmbedDefinition.Field(
                  "message.ticket.initial.field.owner", "$USER_MENTION", true),
              new EmbedDefinition.Field("**▬▬▬▬▬**", "$DETAILS")),
          null);

  public static final EmbedDefinition TICKET_CLAIM =
      new EmbedDefinition(
          "message.ticket.claim.title",
          "message.ticket.claim.description",
          true,
          true,
          List.of(),
          null);

  public static final EmbedDefinition TICKET_CLAIM_FAILED =
      failure("message.ticket.claim.failed.title", "$ERROR");

  public static final EmbedDefinition TICKET_CLAIM_SUCCESS = success("ticket.claim");

  public static final EmbedDefinition TICKET_CLOSE_FAILED =
      failure("message.ticket.close.failed.title", "$ERROR");

  public static final EmbedDefinition TICKET_CLOSE_SUCCESS = success("ticket.close");

  private static EmbedDefinition success(@Nullable String id) {
    return success(
        id == null ? null : "message." + id + ".success.title",
        id == null ? null : "message." + id + ".success.description");
  }

  private static EmbedDefinition success(@Nullable String name, @Nullable String description) {
    return new EmbedDefinition(name, description, true, true, List.of(), 0x00FF00);
  }

  private static EmbedDefinition failure(@Nullable String id) {
    return failure(
        id == null ? null : "message." + id + ".failed.title",
        id == null ? null : "message." + id + ".failed.description");
  }

  private static EmbedDefinition failure(@Nullable String name, @Nullable String description) {
    return new EmbedDefinition(name, description, true, true, List.of(), 0xFF0000);
  }
}
