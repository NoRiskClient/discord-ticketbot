package gg.norisk.ticketbot.util;

import java.awt.*;
import java.util.Locale;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.Nullable;

public class EmbedUtils {
  public static EmbedBuilder resolve(
      EmbedBuilder builder, Locale locale, Map<String, String> placeholders) {
    MessageEmbed embed = builder.build();
    EmbedBuilder resolvedBuilder = new EmbedBuilder();

    resolvedBuilder.setTitle("**" + resolve(embed.getTitle(), locale, placeholders) + "**");
    resolvedBuilder.setDescription(resolve(embed.getDescription(), locale, placeholders));
    resolvedBuilder.setColor(
        embed.getColorRaw() == Role.DEFAULT_COLOR_RAW
            ? Color.decode(placeholders.get("CONFIG_COLOR")).getRGB()
            : embed.getColorRaw());

    if (embed.getAuthor() != null) {
      resolvedBuilder.setAuthor(
          resolve(embed.getAuthor().getName(), locale, placeholders),
          resolve(embed.getAuthor().getUrl(), locale, placeholders),
          resolve(embed.getAuthor().getIconUrl(), locale, placeholders));
    }

    for (MessageEmbed.Field field : embed.getFields()) {
      resolvedBuilder.addField(resolve(field, locale, placeholders));
    }

    if (embed.getFooter() != null) {
      resolvedBuilder.setFooter(
          resolve(embed.getFooter().getText(), locale, placeholders),
          resolve(embed.getFooter().getIconUrl(), locale, placeholders));
    }

    return resolvedBuilder;
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

  private static MessageEmbed.Field resolve(
      MessageEmbed.Field field, Locale locale, Map<String, String> placeholders) {
    return new MessageEmbed.Field(
        resolve(field.getName(), locale, placeholders),
        resolve(field.getValue(), locale, placeholders),
        field.isInline());
  }
}
