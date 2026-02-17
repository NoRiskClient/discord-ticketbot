package gg.norisk.ticketbot;

import gg.norisk.ticketbot.embed.EmbedDefinition;
import gg.norisk.ticketbot.entities.Ticket;
import gg.norisk.ticketbot.util.Result;
import gg.norisk.ticketbot.util.TranslationUtils;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

@AllArgsConstructor
public class TicketService {
  private final Config config;
  private final Database database;
  private final JDA jda;

  public Result<Ticket> createTicket(
      Map<String, String> info, TicketCategory category, User owner, Locale locale) {
    Ticket ticket =
        Ticket.builder()
            .id(0)
            .category(category)
            .owner(owner)
            .info(info)
            .createdAt(Instant.now())
            .build();

    int id = database.saveTicket(ticket);

    TextChannel channel =
        config
            .getGuild(jda)
            .createTextChannel(generateChannelName(ticket), config.getUnclaimedCategory(jda))
            .complete();

    ticket.setChannel(channel);

    StringBuilder details = new StringBuilder();

    for (Map.Entry<String, String> entry : info.entrySet()) {
      details
          .append("**")
          .append(
              TranslationUtils.translate(
                  "category." + category.getId() + "." + entry.getKey() + ".label", locale))
          .append("**\n")
          .append(entry.getValue())
          .append("\n");
    }

    List<EmbedDefinition.Field> fields =
        List.of(
            new EmbedDefinition.Field(
                "message.ticket.initial.field.id", "`" + ticket.getId() + "`", true),
            new EmbedDefinition.Field(
                "message.ticket.initial.field.category",
                "category." + category.getId() + ".label",
                true),
            new EmbedDefinition.Field(
                "message.ticket.initial.field.owner", owner.getAsMention(), true),
            new EmbedDefinition.Field("**▬▬▬▬▬**", details.toString(), false));

    channel
        .sendMessage(owner.getAsMention())
        .addEmbeds(
            new EmbedDefinition(
                    null, "message.ticket.initial.description", true, true, fields, null)
                .toBuilder(
                        config,
                        locale,
                        new HashMap<>(Map.of("USER_MENTION", owner.getAsMention())),
                        config.getGuild(jda),
                        owner)
                    .setImage("https://cdn.norisk.gg/misc/nrc_ticket_banner.png")
                    .build())
        .queue();

    return Result.success(ticket);
  }

  public boolean isTicketChannel(Channel channel) {
    return database.getTicketByChannelId(channel.getId()) != null;
  }

  public Ticket getTicketByChannelId(String channelId) {
    return database.getTicketByChannelId(channelId);
  }

  @Nullable
  @Contract("null -> null")
  public Ticket getTicketByChannel(@Nullable Channel channel) {
    if (channel == null) {
      return null;
    }

    return database.getTicketByChannelId(channel.getId());
  }

  private String generateChannelName(Ticket ticket) {
    return ticket.getCategory().getId() + "-" + ticket.getId() + "-" + ticket.getOwner().getName();
  }
}
