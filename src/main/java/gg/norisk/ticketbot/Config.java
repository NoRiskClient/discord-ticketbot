package gg.norisk.ticketbot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

@Slf4j
@Getter
@Setter
public class Config {
  private String token = "";
  private String activityType = "LISTENING";
  private String activityText = " ticket commands.";
  private String staffId = "";
  private String guildId = "";
  private String color = "#008cff";
  private String unclaimedCategoryId = "";

  public static Config load(@NotNull Path path) throws IOException {
    Files.createDirectories(path.getParent());

    if (!Files.exists(path)) {
      Config defaultConfig = new Config();
      defaultConfig.save(path);
      log.info("Default config created at: {}", Path.of(".").relativize(path));
      return defaultConfig;
    }

    Yaml yaml = new Yaml(new Constructor(Config.class));

    try {
      return yaml.load(Files.newInputStream(path));
    } catch (YAMLException e) {
      log.error("Error while parsing configuration:\n{}", e.getMessage());
      System.exit(1);
      return null;
    }
  }

  public void save(@NotNull Path path) {
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    options.setPrettyFlow(true);
    Yaml yaml = new Yaml(options);
    try {
      yaml.dump(this, Files.newBufferedWriter(path));
    } catch (Exception e) {
      log.error("Failed saving config to file: {}", path.toAbsolutePath(), e);
    }
  }

  public void validate() {
    Map<String, String> requiredFields = new LinkedHashMap<>();
    requiredFields.put("token", this.token);
    requiredFields.put("staffId", this.staffId);
    requiredFields.put("guildId", this.guildId);
    requiredFields.put("unclaimedCategoryId", this.unclaimedCategoryId);

    for (Map.Entry<String, String> entry : requiredFields.entrySet()) {
      if (entry.getValue() == null || entry.getValue().isBlank()) {
        log.error(
            "No {} provided! Please provide a {} in your configuration and restart the bot.",
            entry.getKey(),
            entry.getKey());
        System.exit(1);
      }
    }
  }

  public void validateJdaContext(JDA jda) {
    if (jda.getGuildById(this.guildId) == null) {
      log.error(
          "The guild ID provided in the configuration is invalid! Please check your configuration.");
      System.exit(1);
    }

    if (jda.getRoleById(this.staffId) == null) {
      log.error(
          "The staff role ID provided in the configuration is invalid! Please check your configuration.");
      System.exit(1);
    }

    if (jda.getCategoryById(this.unclaimedCategoryId) == null) {
      log.error(
          "The unclaimed category ID provided in the configuration is invalid! Please check your configuration.");
      System.exit(1);
    }
  }

  public Guild getGuild(JDA jda) {
    return Objects.requireNonNull(jda.getGuildById(this.guildId));
  }

  public Category getUnclaimedCategory(JDA jda) {
    return Objects.requireNonNull(jda.getCategoryById(this.unclaimedCategoryId));
  }
}
