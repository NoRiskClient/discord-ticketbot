package gg.norisk.ticketbot.interaction;

import gg.norisk.ticketbot.Config;
import gg.norisk.ticketbot.TicketService;
import gg.norisk.ticketbot.embed.EmbedBuildInfo;
import gg.norisk.ticketbot.embed.EmbedDefinition;
import gg.norisk.ticketbot.embed.Embeds;
import gg.norisk.ticketbot.entities.Ticket;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Slf4j
public abstract class Interaction {
  public static final Map<SubcommandGroupData, List<SubcommandData>> COMMANDS = new HashMap<>();

  protected static final SubcommandGroupData THREADS_GROUP =
      new SubcommandGroupData("thread", "Manages the ticket thread");
  protected static final SubcommandGroupData LIST_TICKETS_GROUP =
      new SubcommandGroupData("list-tickets", "Lists tickets with options to narrow down results");

  protected final @NotNull Config config;
  protected final @NotNull TicketService ticketService;
  protected final @NotNull JDA jda;

  protected boolean administratorRequired = false;
  protected boolean allowedAnywhere = false;
  protected boolean allowedInTicketThread = true;
  protected boolean allowedInPrivateTicketChannel = false;

  @Getter protected @Nullable Ticket ticket;
  @Getter protected IReplyCallback reply;

  public Interaction(
      @NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda) {
    this.config = config;
    this.ticketService = ticketService;
    this.jda = jda;
  }

  protected void addCommand(String description) {
    this.addCommand(description, null);
  }

  protected void addCommand(
      String description, @Nullable Function<SubcommandData, SubcommandData> options) {
    this.addCommand(this.getIdentifier(), description, null, options);
  }

  protected void addCommand(
      String name,
      String description,
      @Nullable SubcommandGroupData group,
      @Nullable Function<SubcommandData, SubcommandData> options) {
    SubcommandData command = new SubcommandData(name, description);

    if (options != null) {
      command = options.apply(command);
    }

    if (group != null) {
      group.addSubcommands(command);
    }

    COMMANDS.computeIfAbsent(group, k -> new ArrayList<>()).add(command);
  }

  public abstract String getIdentifier();

  public void handleButtonInteraction(@NotNull ButtonInteractionEvent event) {}

  public void handleModalInteraction(@NotNull ModalInteractionEvent event) {}

  public void handleStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {}

  public void handleSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {}

  public boolean conditionsFulfilled(IReplyCallback event) {
    Member member = event.getMember();

    if ((member == null || !member.getPermissions().contains(Permission.ADMINISTRATOR))
        && administratorRequired) {
      replyEphemeralAndQueue(
          new EmbedBuildInfo(
              Embeds.INTERACTION_MISSING_PERMISSIONS, reply.getUserLocale().toLocale(), null));

      return false;
    }

    if (allowedAnywhere) {
      return true;
    }

    if (allowedInPrivateTicketChannel
        && ticketService.isTicketChannel(event.getChannel())
        && event.getChannel().getType() == ChannelType.PRIVATE) {
      return true;
    }

    if (allowedInTicketThread
        && ticketService.isTicketChannel(event.getChannel())
        && event.getChannel().getType() == ChannelType.GUILD_PUBLIC_THREAD) {
      return true;
    }

    replyEphemeralAndQueue(
        new EmbedBuildInfo(
            Embeds.INTERACTION_WRONG_CHANNEL, event.getUserLocale().toLocale(), null));

    return false;
  }

  public void handle(IReplyCallback event) {
    this.reply = event;

    if (!conditionsFulfilled(event)) {
      return;
    }

    this.ticket = this.ticketService.getTicketByChannel(event.getChannel());

    switch (event) {
      case ButtonInteractionEvent e -> handleButtonInteraction(e);
      case ModalInteractionEvent e -> handleModalInteraction(e);
      case StringSelectInteractionEvent e -> handleStringSelectInteraction(e);
      case SlashCommandInteractionEvent e -> handleSlashCommandInteraction(e);
      default -> {}
    }
  }

  protected void replyEphemeralAndQueue(EmbedBuildInfo info) {
    replyEphemeral(info).queue();
  }

  protected void replyAndQueue(EmbedBuildInfo info) {
    reply(info).queue();
  }

  protected ReplyCallbackAction replyEphemeral(EmbedBuildInfo info) {
    return reply(info).setEphemeral(true);
  }

  protected ReplyCallbackAction reply(EmbedBuildInfo info) {
    return reply.replyEmbeds(build(info));
  }

  protected void deferEphemeralAndQueue(Supplier<EmbedBuildInfo> infoSupplier) {
    reply.deferReply(true).queue();
    EmbedBuildInfo info = infoSupplier.get();
    reply.getHook().sendMessageEmbeds(build(info)).setEphemeral(true).queue();
  }

  private MessageEmbed build(EmbedBuildInfo info) {
    EmbedDefinition definition = info.definition();
    Map<String, String> placeholders =
        info.placeholders() != null ? info.placeholders() : new HashMap<>();

    placeholders.put("USER_MENTION", reply.getUser().getAsMention());

    return definition.toBuilder(
            config, info.locale(), placeholders, config.getGuild(jda), reply.getUser())
        .build();
  }
}
