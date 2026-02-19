package gg.norisk.ticketbot;

import gg.norisk.ticketbot.embed.Embeds;
import gg.norisk.ticketbot.entities.Ticket;
import gg.norisk.ticketbot.util.Result;
import gg.norisk.ticketbot.util.TranslationUtils;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
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
            .locale(locale)
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
    database.saveTicket(ticket);

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

    channel
        .sendMessage(owner.getAsMention())
        .addEmbeds(
            Embeds.INITIAL_MESSAGE.toBuilder(
                    config,
                    locale,
                    new HashMap<>(
                        Map.of(
                            "ID",
                            String.valueOf(id),
                            "CATEGORY",
                            ticket.getCategory().getId(),
                            "USER_MENTION",
                            owner.getAsMention(),
                            "DETAILS",
                            details.toString())),
                    config.getGuild(jda),
                    owner)
                .setImage("https://cdn.norisk.gg/misc/nrc_ticket_banner.png")
                .build())
        .setActionRow(Button.primary("claim", "Claim"), Button.danger("close", "Close"))
        .queue();

    return Result.success(ticket);
  }

  public Result<Void> claimTicket(@NotNull Ticket ticket, @NotNull User supporter) {
    if (ticket.getOwner().getIdLong() == supporter.getIdLong()) {
      return Result.failure("supporter_is_owner");
    }

    if (ticket.getSupporter() == null) {
      ticket.setClaimedAt(Instant.now());
    }

    ticket.setSupporter(supporter);
    database.saveTicket(ticket);

    Objects.requireNonNull(ticket.getChannel())
        .sendMessageEmbeds(
            Embeds.TICKET_CLAIM.toBuilder(
                    config,
                    ticket.getLocale(),
                    new HashMap<>(Map.of("SUPPORTER_MENTION", supporter.getAsMention())),
                    config.getGuild(jda),
                    supporter)
                .build())
        .queue();

    return Result.success(null);
  }

  public Result<Void> closeTicket(
      @NotNull Ticket ticket, @NotNull Member closer, @Nullable String reason) {
    if (!closer.getRoles().contains(jda.getRoleById(config.getStaffId()))
        && ticket.getSupporter() != null) {
      return Result.failure("only_supporter_can_close");
    }

    ticket.setClosedAt(Instant.now());
    ticket.setCloser(closer.getUser());

    Objects.requireNonNull(ticket.getChannel()).delete().queue();

    return Result.success(null);
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
