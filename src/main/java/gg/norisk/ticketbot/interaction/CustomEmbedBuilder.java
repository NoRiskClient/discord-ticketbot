package gg.norisk.ticketbot.interaction;

import gg.norisk.ticketbot.util.TranslationUtils;
import java.util.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.Nullable;

public class CustomEmbedBuilder {
  private final Map<String, String> placeholders = new HashMap<>();
  private String title;
  private String description;
  private List<MessageEmbed.Field> fields = new ArrayList<>();
  private MessageEmbed.Footer footer;
  private int color;
  private User author;
  private Locale locale = Locale.ENGLISH;
  private Guild guild;

  public CustomEmbedBuilder setTitle(String title) {
    this.title = title;
    return this;
  }

  public CustomEmbedBuilder setDescription(String description) {
    this.description = description;
    return this;
  }

  public CustomEmbedBuilder setColor(int color) {
    this.color = color;
    return this;
  }

  public CustomEmbedBuilder setAuthor(User author) {
    this.author = author;
    return this;
  }

  public CustomEmbedBuilder addField(String name, String value, boolean inline) {
    this.fields.add(new MessageEmbed.Field(name, value, inline));
    return this;
  }

  public CustomEmbedBuilder setFooter(String text, String iconUrl) {
    this.footer = new MessageEmbed.Footer(text, iconUrl, null);
    return this;
  }

  public CustomEmbedBuilder setLocale(Locale locale) {
    this.locale = locale;
    return this;
  }

  public CustomEmbedBuilder setPlaceholder(String placeholder, String value) {
    this.placeholders.put(placeholder, value);
    return this;
  }

  public CustomEmbedBuilder setGuild(Guild guild) {
    this.guild = guild;
    return this;
  }

  public MessageEmbed build() {
    title = "**" + resolve(title) + "**";
    description = resolve(description);

    fields = fields.stream().map(this::resolve).toList();

    footer = resolve(footer);

    EmbedBuilder embedBuilder = new EmbedBuilder();
    embedBuilder.setTitle(title);
    embedBuilder.setDescription(description);
    embedBuilder.setColor(color);
    embedBuilder.setFooter()
    fields.forEach(embedBuilder::addField);

    if (guild != null) {
      embedBuilder.setFooter(guild.getName(), guild.getIconUrl());
    }
    if (author != null) {
      embedBuilder.setAuthor(author.getName(), null, author.getEffectiveAvatarUrl());
    }

    return embedBuilder.build();
  }

  private MessageEmbed.Field resolve(MessageEmbed.Field field) {
    return new MessageEmbed.Field(
        resolve(field.getName()), resolve(field.getValue()), field.isInline());
  }

  private String resolve(@Nullable String string) {
    if (string == null) {
      return null;
    }

    string = TranslationUtils.translate(string, locale);

    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
      string = string.replace("$" + entry.getKey(), entry.getValue());
    }
    return string;
  }

  private MessageEmbed.Footer resolve(MessageEmbed.Footer footer) {
    return new MessageEmbed.Footer(
        resolve(footer.getText()), resolve(footer.getIconUrl()), resolve(footer.getProxyIconUrl()));
  }
}
