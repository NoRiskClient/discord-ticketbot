package gg.norisk.ticketbot;

import gg.norisk.ticketbot.util.TranslationUtils;
import java.text.MessageFormat;
import java.util.*;
import lombok.Getter;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalInteraction;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

public enum TicketCategory {
  GENERAL("general", false, new ModalField("reason", TextInputStyle.PARAGRAPH, 700, true)),
  BUG(
      "bug",
      false,
      new ModalField("problem", TextInputStyle.PARAGRAPH, 750, true),
      new ModalField("version", TextInputStyle.SHORT, 20, false),
      new ModalField("logs", TextInputStyle.PARAGRAPH, 50, false)),
  CREATOR(
      "creator",
      true,
      new ModalField("name", TextInputStyle.SHORT, 16, true),
      new ModalField("links", TextInputStyle.PARAGRAPH, 200, true),
      new ModalField("information", TextInputStyle.PARAGRAPH, 700, false)),

  PAYMENT(
      "payment",
      true,
      new ModalField("problem", TextInputStyle.PARAGRAPH, 700, true),
      new ModalField("name", TextInputStyle.SHORT, 16, true)),

  REPORT(
      "report",
      true,
      new ModalField("user", TextInputStyle.SHORT, 16, true),
      new ModalField("reason", TextInputStyle.PARAGRAPH, 700, true),
      new ModalField("evidence", TextInputStyle.PARAGRAPH, 250, false)),
  SECURITY("security", true, new ModalField("vulnerability", TextInputStyle.PARAGRAPH, 800, true));

  @Getter private final String id;
  @Getter private final boolean sensitive;
  private final List<ModalField> modalFields;

  TicketCategory(String id, boolean sensitive, ModalField... modalFields) {
    this.id = id;
    this.sensitive = sensitive;
    this.modalFields = List.of(modalFields);
  }

  public Modal getModal(Locale locale) {
    Modal.Builder modalBuilder =
        Modal.create(
            id,
            MessageFormat.format(
                TranslationUtils.translate("modal.create_ticket.title", locale),
                TranslationUtils.translate("category.label." + id, locale)));

    for (ModalField field : modalFields) {
      TextInput input =
          TextInput.create(
                  field.id,
                  TranslationUtils.translate("category.label." + id + "." + field.id, locale),
                  field.style)
              .setPlaceholder(
                  TranslationUtils.translate("category.description." + id + "." + field.id, locale))
              .setRequired(field.required)
              .setMaxLength(field.maxLength)
              .build();
      modalBuilder.addActionRow(input);
    }

    return modalBuilder.build();
  }

  public Map<String, String> extractInfo(ModalInteraction interaction) {
    Map<String, String> info = new LinkedHashMap<>();
    for (ModalField field : modalFields) {
      String value =
          Optional.ofNullable(interaction.getValue(field.id))
              .map(ModalMapping::getAsString)
              .orElse("N/A");
      info.put(id, value);
    }
    return info;
  }

  private record ModalField(String id, TextInputStyle style, int maxLength, boolean required) {}
}
