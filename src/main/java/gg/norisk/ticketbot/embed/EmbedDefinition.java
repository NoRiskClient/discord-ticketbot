package gg.norisk.ticketbot.embed;

import gg.norisk.ticketbot.Config;
import gg.norisk.ticketbot.util.TranslationUtils;
import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.Nullable;

@AllArgsConstructor
public class EmbedDefinition {
  @Getter private final String title;
  @Getter private final String description;
  @Getter private final boolean userAuthor;
  @Getter private final boolean serverFooter;
  @Getter private final List<Field> fields;
  @Nullable @Getter private final Integer color;

  public EmbedBuilder toBuilder(
      Config config,
      Locale locale,
      Map<String, String> placeholders,
      @Nullable Guild guild,
      @Nullable User user) {
    EmbedBuilder builder = new EmbedBuilder();
    builder.setTitle(resolve(title, locale, placeholders));
    builder.setDescription(resolve(description, locale, placeholders));

    if (color != null) {
      builder.setColor(color);
    } else {
      builder.setColor(Color.decode(config.getColor()));
    }

    if (serverFooter && guild != null) {
      builder.setFooter(guild.getName(), guild.getIconUrl());
    }

    if (userAuthor && user != null) {
      builder.setAuthor(user.getName(), null, user.getAvatarUrl());
    }

    for (Field field : fields) {
      builder.addField(
          resolve(field.name(), locale, placeholders),
          resolve(field.value(), locale, placeholders),
          field.inline());
    }

    return builder;
  }

  private static String resolve(
      @Nullable String string, Locale locale, Map<String, String> placeholders) {
    if (string == null) {
      return null;
    }

    string = TranslationUtils.translate(string, locale);

    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
      string = string.replace("$" + entry.getKey(), entry.getValue());
    }
    return string;
  }

  public record Field(String name, String value, boolean inline) {}
}
