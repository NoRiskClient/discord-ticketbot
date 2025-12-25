package gg.norisk.ticketbot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

@Slf4j
@Getter
@Setter
public class Config {
  private String token = "";
  private String activityType = "LISTENING";
  private String activityText = " ticket commands.";

  public static Config load(@NotNull Path path) throws IOException {
    Files.createDirectories(path.getParent());

    if (!Files.exists(path)) {
      Config defaultConfig = new Config();
      defaultConfig.save(path);
      log.info("Default config created at: {}", Path.of(".").relativize(path));
      return defaultConfig;
    }

    Yaml yaml = new Yaml(new Constructor(Config.class));
    return yaml.load(Files.newInputStream(path));
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
}
