package eu.greev.dcbot.scheduler;

import eu.greev.dcbot.utils.Config;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodically re-reads ./Tickets/config.yml and pushes the hot-reloadable
 * {@code applicationsOpen} map onto the shared live {@link Config} instance,
 * so opening/closing applications takes effect without a bot restart.
 */
@Slf4j
public class ConfigReloadScheduler {
    private static final int INTERVAL_SECONDS = 300; // every 5 minutes
    private static final String CONFIG_PATH = "./Tickets/config.yml";

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Config config;

    public ConfigReloadScheduler(Config config) {
        this.config = config;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::run, INTERVAL_SECONDS, INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void run() {
        try (FileInputStream in = new FileInputStream(CONFIG_PATH)) {
            Yaml yaml = new Yaml(new Constructor(Config.class));
            Config reloaded = yaml.load(in);
            if (reloaded == null || reloaded.getApplicationsOpen() == null) {
                return;
            }
            config.setApplicationsOpen(reloaded.getApplicationsOpen());
            log.debug("Reloaded applicationsOpen from config: {}", reloaded.getApplicationsOpen());
        } catch (Exception e) {
            log.error("Could not reload config.yml, keeping previous applicationsOpen", e);
        }
    }
}
