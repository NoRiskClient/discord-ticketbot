package eu.greev.dcbot.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Getter
@Setter
public class Config {
    private long serverId;
    private long staffId;
    private long unclaimedCategory;
    private long baseChannel;
    private long logChannel = 0;
    private @Nullable String serverLogo;
    private @Nullable String serverName;
    private @Nullable String color;
    private @Nullable String token;
    private int maxTicketsPerUser = 3;
    private List<Long> addToTicketThread = new ArrayList<>();
    private Map<Long, String> claimEmojis = new HashMap<>();
    private Map<String, Long> categories = new HashMap<>();
    private Map<String, List<Long>> categoryRoles = new HashMap<>();

    public void save(File file) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);
        try(FileWriter writer = new FileWriter(file)) {
            if(!file.exists()) Files.createDirectories(file.toPath());
            yaml.dump(this, writer);
        } catch (IOException e) {
            log.error("Unable to save config file.", e);
        }
    }

    public static Config load(File file) {
        Constructor constructor = new Constructor(Config.class);
        Yaml yaml = new Yaml(constructor);
        try(FileInputStream in = new FileInputStream(file)) {
            return yaml.load(in);
        } catch (FileNotFoundException e) {
            log.info("Config file does not exist, using defaults.");
        } catch (IOException e) {
            log.error("Unable to load config file.", e);
        }

        return new Config();
    }
}