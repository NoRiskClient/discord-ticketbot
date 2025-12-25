package gg.norisk.ticketbot;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

@Slf4j
public class Main {
  public static void main(String[] args) throws IOException, InterruptedException {
    log.info("Starting TicketBot...");

    Config config = Config.load(Path.of("Tickets/config.yml"));

    if (config.getToken().isBlank()) {
      log.error(
          "No bot token provided! Please provide a valid token in your configuration and restart the bot.");
      System.exit(1);
    }

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

    Database database = new Database("jdbc:sqlite:./Tickets/tickets.db");

    try {
      database.initialize();
    } catch (IOException e) {
      log.error("Failed to initialize the database!", e);
      System.exit(1);
      return;
    }
  }
}
