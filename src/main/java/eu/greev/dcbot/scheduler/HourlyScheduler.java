package eu.greev.dcbot.scheduler;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketData;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;

import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@AllArgsConstructor
public class HourlyScheduler {
    private static final int REMIND_INTERVAL_HOURS = 24;
    private static final int AUTO_CLOSE_HOURS = 96;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Config config;
    private TicketService ticketService;
    private TicketData ticketData;
    private JDA jda;

    public void start() {
        scheduler.scheduleAtFixedRate(this::run, getInitialDelay(), 60 * 60, TimeUnit.SECONDS);
    }

    private int getInitialDelay() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextHour = now.plusHours(1).withMinute(0).withSecond(0).withNano(0);
        return (int) Duration.between(now, nextHour).getSeconds();
    }

    private void run() {
        log.info("Running hourly ticket check...");

        List<Integer> waitingTickets = ticketData.getOpenWaitingTicketsIds();

        for (Integer ticketId : waitingTickets) {
            log.debug("Checking ticket ID: {}", ticketId);
            Ticket ticket = ticketService.getTicketByTicketId(ticketId);

            boolean shouldRemind = ticket.isWaiting() && ticket.getWaitingSince() != null &&
                    ticket.getWaitingSince().plusHours((long) REMIND_INTERVAL_HOURS * (ticket.getRemindersSent() + 1)).isBefore(LocalDateTime.now());

            boolean shouldClose = ticket.isWaiting() && ticket.getWaitingSince() != null &&
                    ticket.getWaitingSince().plusHours(AUTO_CLOSE_HOURS).isBefore(LocalDateTime.now());

            log.debug("Should remind: {}, Should close: {}", shouldRemind, shouldClose);

            if (shouldClose) {
                ticketService.closeTicket(ticket, false, jda.getGuildById(config.getServerId()).getSelfMember());
                continue;
            }

            if (shouldRemind) {
                EmbedBuilder builder = new EmbedBuilder()
                        .setTitle(String.format("⏰ Reminder: Waiting for your response (%s/%s)", ticket.getRemindersSent() + 1, AUTO_CLOSE_HOURS / REMIND_INTERVAL_HOURS - 1))
                        .setColor(Color.decode(config.getColor()))
                        .appendDescription(String.format("**Our support team is waiting for an answer in <#%s>**", ticket.getTextChannel().getId()))
                        .appendDescription(String.format("\nIf you do not respond, the ticket will be automatically closed <t:%d:R>.", ticket.getWaitingSince().plusHours(AUTO_CLOSE_HOURS + 1).withMinute(0).toEpochSecond(ZonedDateTime.now().getOffset())))
                        .setFooter(config.getServerName(), config.getServerLogo());


                Member member = jda.getGuildById(config.getServerId()).getMember(ticket.getOwner());
                if (member != null) {
                    member.getUser().openPrivateChannel()
                            .flatMap(channel -> channel.sendMessageEmbeds(builder.build()))
                            .queue();

                    ticket.setRemindersSent(ticket.getRemindersSent() + 1);
                }
            }
        }
    }
}
