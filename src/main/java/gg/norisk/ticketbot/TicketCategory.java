package gg.norisk.ticketbot;

import java.util.*;
import lombok.Getter;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalInteraction;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

public enum TicketCategory {
  GENERAL(
      "general",
      "General",
      "General questions regarding the client",
      false,
      new ModalField(
          "reason",
          "Reason",
          "Please describe your issue or question in detail.",
          TextInputStyle.PARAGRAPH,
          700,
          true)),
  BUG(
      "bug",
      "Bug",
      "Errors/Bugs in the launcher, app or game",
      false,
      new ModalField(
          "problem",
          "Problem",
          "Please provide as much information about your problem as possible.",
          TextInputStyle.PARAGRAPH,
          750,
          true),
      new ModalField(
          "version",
          "Version",
          "Please provide the version of the launcher/app/game.",
          TextInputStyle.SHORT,
          20,
          false),
      new ModalField(
          "logs",
          "Logs",
          "Provide logs/errors. Use mclo.gs link or upload file after ticket creation if too long.",
          TextInputStyle.PARAGRAPH,
          50,
          false)),
  CREATOR(
      "creator",
      "Creator Application",
      "Apply for a creator code",
      true,
      new ModalField(
          "name",
          "What is your in-game name?",
          "Your in-game name",
          TextInputStyle.SHORT,
          16,
          true),
      new ModalField(
          "links",
          "Provide links to your content (YouTube, etc.)",
          "Links to your content",
          TextInputStyle.PARAGRAPH,
          200,
          true),
      new ModalField(
          "information",
          "Tell us more about yourself",
          "Additional information",
          TextInputStyle.PARAGRAPH,
          700,
          false)),

  PAYMENT(
      "payment",
      "Payment",
      "Payment failed or items not received",
      true,
      new ModalField(
          "problem",
          "Problem",
          "Describe your payment issue in detail.",
          TextInputStyle.PARAGRAPH,
          700,
          true),
      new ModalField("name", "In-Game Name", "Your in-game name", TextInputStyle.SHORT, 16, true)),

  REPORT(
      "report",
      "Report",
      "Here you can report a user",
      true,
      new ModalField("user", "User", "Who do you want to report?", TextInputStyle.SHORT, 16, true),
      new ModalField(
          "reason",
          "Reason",
          "Why do you want to report this user?",
          TextInputStyle.PARAGRAPH,
          700,
          true),
      new ModalField(
          "evidence",
          "Evidence",
          "Provide any evidence (screenshots, videos, etc.) to support your report.",
          TextInputStyle.PARAGRAPH,
          250,
          false)),
  SECURITY(
      "security",
      "Security Report",
      "Here you can report security vulnerabilities",
      true,
      new ModalField(
          "vulnerability",
          "Vulnerability",
          "Describe the security vulnerability you have discovered.",
          TextInputStyle.PARAGRAPH,
          800,
          true));

  @Getter private final String id;
  @Getter private final String label;
  @Getter private final String description;
  @Getter private final Modal modal;
  @Getter private final boolean sensitive;
  private final List<ModalField> modalFields;

  TicketCategory(
      String id, String label, String description, boolean sensitive, ModalField... modalFields) {
    this.id = id;
    this.label = label;
    this.description = description;
    this.sensitive = sensitive;
    this.modalFields = List.of(modalFields);

    Modal.Builder modalBuilder = Modal.create(id, "Ticket: " + label);

    for (ModalField field : modalFields) {
      TextInput input =
          TextInput.create(field.key, field.display, field.style)
              .setPlaceholder(field.description)
              .setRequired(field.required)
              .setMaxLength(field.maxLength)
              .build();
      modalBuilder.addActionRow(input);
    }

    this.modal = modalBuilder.build();
  }

  public Map<String, String> extractInfo(ModalInteraction interaction) {
    Map<String, String> info = new LinkedHashMap<>();
    for (ModalField field : modalFields) {
      String value =
          Optional.ofNullable(interaction.getValue(field.key))
              .map(ModalMapping::getAsString)
              .orElse("N/A");
      info.put(field.display, value);
    }
    return info;
  }

  private record ModalField(
      String key,
      String display,
      String description,
      TextInputStyle style,
      int maxLength,
      boolean required) {}
}
