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
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
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

    ThreadChannel channel =
        config
            .getUnclaimedForum(jda)
            .createForumPost(
                generateChannelName(ticket),
                new MessageCreateBuilder()
                    .addEmbeds(
                        Embeds.INITIAL_MESSAGE.toBuilder(
                                config,
                                config.getStaffLocale(),
                                new HashMap<>(
                                    Map.of(
                                        "ID",
                                        String.valueOf(id),
                                        "CATEGORY",
                                        TranslationUtils.translate(
                                            "category." + ticket.getCategory().getId() + ".label",
                                            config.getStaffLocale()),
                                        "USER_MENTION",
                                        owner.getAsMention(),
                                        "DETAILS",
                                        buildDetails(
                                            ticket.getCategory(),
                                            ticket.getInfo(),
                                            config.getStaffLocale()))),
                                config.getGuild(jda),
                                owner)
                            .setImage("https://cdn.norisk.gg/misc/nrc_ticket_banner.png")
                            .build())
                    .addActionRow(Button.primary("claim", "Claim"), Button.danger("close", "Close"))
                    .build())
            .setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_WEEK)
            .complete()
            .getThreadChannel();

    ticket.setChannel(channel);
    database.saveTicket(ticket);

    return Result.success(ticket);
  }

  public Result<Void> claimTicket(
      @NotNull Ticket ticket, @NotNull User supporter, @Nullable IReplyCallback event) {
    if (ticket.getOwner().getIdLong() == supporter.getIdLong()) {
      return Result.failure("supporter_is_owner");
    }

    if (ticket.getSupporter() == null) {
      ticket.setClaimedAt(Instant.now());
    }

    ticket.setSupporter(supporter);

    if (event != null) {
      event
          .replyEmbeds(
              Embeds.TICKET_CLAIM_SUCCESS.toBuilder(
                      config, config.getStaffLocale(), Map.of(), config.getGuild(jda), supporter)
                  .build())
          .setEphemeral(true)
          .complete();
    }

    Objects.requireNonNull(ticket.getChannel()).delete().queue();

    ThreadChannel channel =
        config
            .getForum(jda, ticket.getCategory())
            .createForumPost(
                generateChannelName(ticket),
                new MessageCreateBuilder()
                    .addEmbeds(
                        Embeds.INITIAL_MESSAGE.toBuilder(
                                config,
                                config.getStaffLocale(),
                                new HashMap<>(
                                    Map.of(
                                        "ID",
                                        String.valueOf(ticket.getId()),
                                        "CATEGORY",
                                        TranslationUtils.translate(
                                            "category." + ticket.getCategory().getId() + ".label",
                                            config.getStaffLocale()),
                                        "USER_MENTION",
                                        ticket.getOwner().getAsMention(),
                                        "DETAILS",
                                        buildDetails(
                                            ticket.getCategory(),
                                            ticket.getInfo(),
                                            config.getStaffLocale()))),
                                config.getGuild(jda),
                                ticket.getOwner())
                            .setImage("https://cdn.norisk.gg/misc/nrc_ticket_banner.png")
                            .build())
                    .addActionRow(Button.danger("close", "Close"))
                    .build())
            .setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_WEEK)
            .complete()
            .getThreadChannel();

    ticket.setChannel(channel);
    database.saveTicket(ticket);

    channel.addThreadMember(supporter).queue();

    ticket
        .getOwner()
        .openPrivateChannel()
        .complete()
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
      @NotNull Ticket ticket,
      @NotNull Member closer,
      @Nullable String reason,
      @Nullable IReplyCallback event) {
    if (!closer.getRoles().contains(jda.getRoleById(config.getStaffId()))
        && ticket.getSupporter() != null) {
      return Result.failure("only_supporter_can_close");
    }

    ticket.setClosedAt(Instant.now());
    ticket.setCloser(closer.getUser());

    if (event != null) {
      event
          .replyEmbeds(
              Embeds.TICKET_CLOSE_SUCCESS.toBuilder(
                      config,
                      config.getStaffLocale(),
                      new HashMap<>(Map.of("REASON", reason == null ? "N/A" : reason)),
                      config.getGuild(jda),
                      closer.getUser())
                  .build())
          .setEphemeral(true)
          .complete();
    }

    Objects.requireNonNull(ticket.getChannel())
        .sendMessageEmbeds(
            Embeds.TICKET_CLOSE.toBuilder(
                    config,
                    config.getStaffLocale(),
                    new HashMap<>(
                        Map.of(
                            "CLOSER",
                            closer.getAsMention(),
                            "REASON",
                            reason == null ? "N/A" : reason)),
                    config.getGuild(jda),
                    closer.getUser())
                .build())
        .complete();

    Objects.requireNonNull(ticket.getChannel()).getManager().setArchived(true).queue();

    return Result.success(null);
  }

  @Contract("null -> false")
  public boolean isTicketChannel(@Nullable Channel channel) {
    if (channel == null) {
      return false;
    }

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

  public String buildDetails(TicketCategory category, Map<String, String> info, Locale locale) {
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

    return details.toString();
  }
}
