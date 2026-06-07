package eu.greev.dcbot.scheduler;

import eu.greev.dcbot.ticketsystem.service.TicketData;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;

import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@AllArgsConstructor
public class DailyScheduler {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Config config;
    private JDA jda;
    private TicketData ticketData;
    private TicketService ticketService;

    public void start() {
        scheduler.scheduleAtFixedRate(this::run, getInitialDelay(), 24 * 60 * 60, TimeUnit.SECONDS);
    }

    private int getInitialDelay() {
        LocalDateTime now = LocalDateTime.now();
        // Minute set to 30 to avoid collision with HourlyScheduler
        LocalDateTime next3Pm = now.plusDays(1).withHour(15).withMinute(30).withSecond(0).withNano(0);
        return (int) Duration.between(now, next3Pm).getSeconds();
    }

    private void run() {
        log.info("Running daily category consolidation...");
        ticketService.consolidateCategoriesAndCleanup();

        if (config.getLogChannel() != 0) {
            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle("Tickets waiting for supporter response")
                    .setFooter(config.getServerName(), config.getServerLogo())
                    .setColor(Color.decode(config.getColor()));

            StringBuilder description = new StringBuilder();
            StringBuilder mentions = new StringBuilder();

            for (Map.Entry<String, Map<String, String>> entry : ticketData.longestSinceLastSupporterMessage(80).entrySet()) {
                description.append("<@").append(entry.getKey()).append(">:\n");
                mentions.append("<@").append(entry.getKey()).append(">").append(" ");

                for (Map.Entry<String, String> ticketEntry : entry.getValue().entrySet()) {
                    description.append(" - <#").append(ticketEntry.getKey()).append(">: ").append("<t:").append(ticketEntry.getValue()).append(":R>").append("\n");
                }
            }

            embedBuilder.setDescription(description.toString());

            var channel = jda.getTextChannelById(config.getSupporterReminderChannel() == 0 ? config.getLogChannel() : config.getSupporterReminderChannel());
            if (channel != null) {
                channel.sendMessage(mentions.toString())
                        .addEmbeds(embedBuilder.build())
                        .queue();
            }
        }
    }
}
