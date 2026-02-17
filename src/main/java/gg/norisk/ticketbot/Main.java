package gg.norisk.ticketbot;

import gg.norisk.ticketbot.embed.Embeds;
import gg.norisk.ticketbot.interaction.ArgumentedInteraction;
import gg.norisk.ticketbot.interaction.Interaction;
import gg.norisk.ticketbot.interaction.InteractionFactory;
import gg.norisk.ticketbot.interaction.commands.VersionCommand;
import gg.norisk.ticketbot.interaction.modals.TicketCreationModalInteraction;
import gg.norisk.ticketbot.interaction.selections.CategorySelectionInteraction;
import gg.norisk.ticketbot.util.TranslationUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.Nullable;

@Slf4j
public class Main {
  public static final String VERSION = "2.0.0-beta";
  private static final Map<String, Interaction> INTERACTIONS = new HashMap<>();
  public static @Nullable String BASE_MESSAGE_ID;

  public static void main(String[] args) throws IOException, InterruptedException {
    log.info("Starting discord-ticketbot v{}...", VERSION);

    log.debug("Loading configuration...");

    Config config = Config.load(Path.of("Tickets/config.yml"));

    config.validate();

    log.debug("Initializing JDA...");

    JDA jda;

    try {
      jda =
          JDABuilder.create(
                  config.getToken(),
                  List.of(
                      GatewayIntent.MESSAGE_CONTENT,
                      GatewayIntent.GUILD_MEMBERS,
                      GatewayIntent.GUILD_MESSAGES,
                      GatewayIntent.GUILD_PRESENCES))
              .disableCache(
                  CacheFlag.ACTIVITY,
                  CacheFlag.VOICE_STATE,
                  CacheFlag.EMOJI,
                  CacheFlag.STICKER,
                  CacheFlag.CLIENT_STATUS,
                  CacheFlag.ONLINE_STATUS,
                  CacheFlag.SCHEDULED_EVENTS)
              .setActivity(
                  Activity.of(
                      Activity.ActivityType.valueOf(config.getActivityType().toUpperCase()),
                      config.getActivityText()))
              .setChunkingFilter(ChunkingFilter.ALL)
              .setMemberCachePolicy(MemberCachePolicy.ALL)
              .setStatus(OnlineStatus.ONLINE)
              .build();
    } catch (InvalidTokenException e) {
      log.error("Provided bot token is invalid! Please check the token and try again.");
      System.exit(1);
      return;
    } catch (IllegalArgumentException e) {
      log.error("Invalid activity type! Please check your configuration.", e);
      System.exit(1);
      return;
    }

    jda.awaitReady();

    config.validateJdaContext(jda);

    log.debug("Initializing database...");

    Database database = new Database("jdbc:sqlite:./Tickets/tickets.db", jda);

    try {
      database.initialize();
    } catch (IOException e) {
      log.error("Failed to initialize the database!", e);
      System.exit(1);
      return;
    }

    log.debug("Updating base message...");

    updateBaseMessage(config, database, jda);

    TicketService ticketService = new TicketService(config, database, jda);

    log.debug("Registering interactions...");

    registerTicketCreationInteractions(config, ticketService, jda);
    registerInteractions(
        config, ticketService, jda, CategorySelectionInteraction::new, VersionCommand::new);

    log.debug("Registering commands...");

    SlashCommandData parent = Commands.slash("ticket", "Manage the ticket system");

    for (Map.Entry<SubcommandGroupData, List<SubcommandData>> entry :
        Interaction.COMMANDS.entrySet()) {
      if (entry.getKey() == null) {
        parent.addSubcommands(entry.getValue());
      } else {
        parent.addSubcommandGroups(entry.getKey());
      }
    }

    jda.updateCommands().addCommands(parent).queue();

    jda.addEventListener(new EventListener());

    log.info("Ticket bot successfully started!");
  }

  private static void registerInteraction(Interaction interaction) {
    INTERACTIONS.put(interaction.getIdentifier(), interaction);
  }

  private static void registerInteractions(
      Config config, TicketService ticketService, JDA jda, InteractionFactory... factories) {
    for (InteractionFactory factory : factories) {
      Interaction interaction = factory.create(config, ticketService, jda);
      registerInteraction(interaction);
    }
  }

  private static void registerTicketCreationInteractions(
      Config config, TicketService ticketService, JDA jda) {
    for (TicketCategory category : TicketCategory.values()) {
      Interaction interaction =
          new TicketCreationModalInteraction(config, ticketService, jda, category);
      registerInteraction(interaction);
    }
  }

  public static void handleInteraction(String s, IReplyCallback event) {
    log.debug("Received interaction with id: {}", s);

    String id = s.split(" ")[0];
    if (INTERACTIONS.containsKey(id)) {
      Interaction interaction = INTERACTIONS.get(id);
      if (interaction instanceof ArgumentedInteraction argumented) {
        argumented.handleArgumented(s, event);
      } else {
        interaction.handle(event);
      }
    } else {
      log.warn("No interaction found for id: {}", id);
    }
  }

  private static void updateBaseMessage(Config config, Database database, JDA jda) {
    TextChannel channel = Objects.requireNonNull(jda.getTextChannelById(config.getBaseChannelId()));
    String id = database.getMiscValue("base_message_id");

    if (id != null && !id.isBlank()) {
      channel
          .deleteMessageById(id)
          .queue(success -> log.debug("Deleted old base message with id {}", id), error -> {});
    }

    StringSelectMenu.Builder selectionBuilder =
        StringSelectMenu.create("select-category")
            .setPlaceholder(
                TranslationUtils.translate(
                    "selection.category.placeholder", channel.getGuild().getLocale().toLocale()));

    for (TicketCategory category : TicketCategory.values()) {
      selectionBuilder.addOption(
          TranslationUtils.translate(
              "category." + category.getId() + ".label", channel.getGuild().getLocale().toLocale()),
          category.getId(),
          TranslationUtils.translate(
              "category." + category.getId() + ".description",
              channel.getGuild().getLocale().toLocale()));
    }

    Message message =
        channel
            .sendMessageEmbeds(
                Embeds.BASE_MESSAGE.toBuilder(
                        config,
                        channel.getGuild().getLocale().toLocale(),
                        Map.of(),
                        channel.getGuild(),
                        null)
                    .build())
            .setActionRow(selectionBuilder.build())
            .complete();

    database.setMiscValue("base_message_id", message.getId());
    BASE_MESSAGE_ID = message.getId();
  }
}
