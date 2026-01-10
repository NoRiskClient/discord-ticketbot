package gg.norisk.ticketbot;

import gg.norisk.ticketbot.interaction.ArgumentedInteraction;
import gg.norisk.ticketbot.interaction.Interaction;
import gg.norisk.ticketbot.interaction.InteractionFactory;
import gg.norisk.ticketbot.interaction.modal.TicketCreationModalInteraction;
import gg.norisk.ticketbot.interaction.selections.CategorySelectionInteraction;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

@Slf4j
public class Main {
  private static final Map<String, Interaction> INTERACTIONS = new HashMap<>();

  public static void main(String[] args) throws IOException, InterruptedException {
    log.info("Starting ticket bot...");

    log.debug("Loading configuration...");

    Config config = Config.load(Path.of("Tickets/config.yml"));

    if (config.getToken().isBlank()) {
      log.error(
          "No bot token provided! Please provide a valid token in your configuration and restart the bot.");
      System.exit(1);
    }

    if (config.getGuildId().isBlank()) {
      log.error(
          "No guild ID provided! Please provide a valid guild ID in your configuration and restart the bot.");
      System.exit(1);
    }

    if (config.getStaffId().isBlank()) {
      log.error(
          "No staff role ID provided! Please provide a valid staff role ID in your configuration and restart the bot.");
      System.exit(1);
    }

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

    log.debug("Initializing database...");

    Database database = new Database("jdbc:sqlite:./Tickets/tickets.db", jda);

    try {
      database.initialize();
    } catch (IOException e) {
      log.error("Failed to initialize the database!", e);
      System.exit(1);
      return;
    }

    TicketService ticketService = new TicketService(database);

    log.debug("Registering interactions...");

    registerTicketCreationInteractions(config, ticketService, jda);
    registerInteractions(config, ticketService, jda, CategorySelectionInteraction::new);

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
        argumented.handleArgumented(id, event);
      } else {
        interaction.handle(event);
      }
    }
  }
}
