package eu.greev.dcbot.scheduler;

import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.jdbi.v3.core.Jdbi;

@Slf4j
@AllArgsConstructor
public class DailyScheduler {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private TicketService ticketService;

    public void start() {
        scheduler.scheduleAtFixedRate(this::run, getInitialDelay(), 24 * 60 * 60, TimeUnit.SECONDS);
    }

    private int getInitialDelay() {
        LocalDateTime now = LocalDateTime.now();
        // Minute set to 30 to avoid collision with HourlyScheduler
        LocalDateTime nextMidnight = now.plusDays(1).withHour(0).withMinute(30).withSecond(0).withNano(0);
        return (int) Duration.between(now, nextMidnight).getSeconds();
    }

    private void run() {
        log.info("Running daily category consolidation...");
        ticketService.consolidateCategoriesAndCleanup();
    }
}
