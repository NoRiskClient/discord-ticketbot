package eu.greev.dcbot.scheduler;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketData;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.utils.TimeUtil;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@AllArgsConstructor
public class HourlyScheduler {
    private static final int REMIND_INTERVAL_HOURS = 24;
    private static final int REMIND_SUPPORTER_HOURS = 24;
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

        List<Integer> ticketsIds = ticketData.getOpenTicketsIds();
        int userReminders = 0;
        int autoClosures = 0;
        int supporterReminders = 0;
        int ratingReminders = 0;
        int ratingAutoClosures = 0;

        for (Integer ticketId : ticketsIds) {
            log.debug("Checking ticket ID: {}", ticketId);
            Ticket ticket = ticketService.getTicketByTicketId(ticketId);

            if (ticket == null || ticket.getTextChannel() == null) {
                log.warn("Ticket ID {} not found or channel missing, marking as closed...", ticketId);
                ticketData.markStaleTicketAsClosed(ticketId);
                continue;
            }

            boolean shouldRemind = ticket.isWaiting() && ticket.getWaitingSince() != null &&
                    ticket.getWaitingSince()
                            .plus((long) REMIND_INTERVAL_HOURS * (ticket.getRemindersSent() + 1), ChronoUnit.HOURS)
                            .isBefore(Instant.now());

            boolean shouldClose = ticket.isWaiting() && ticket.getWaitingSince() != null &&
                    ticket.getWaitingSince()
                            .plus(AUTO_CLOSE_HOURS, ChronoUnit.HOURS)
                            .isBefore(Instant.now());

            boolean shouldRemindSupporter = !ticket.isWaiting() &&
                    ticket.getSupporter() != null &&
                    ticket.getTextChannel() != null &&
                    !ticket.getTextChannel().getLatestMessageId().equals("0") &&
                    TimeUtil.getTimeCreated(ticket.getTextChannel().getLatestMessageIdLong()).plusHours((long) REMIND_SUPPORTER_HOURS * (ticket.getSupporterRemindersSent() + 1))
                            .isBefore(Instant.now().atZone(ZoneId.of("UTC")).toOffsetDateTime());

            if (shouldClose) {
                ticketService.closeTicket(ticket, false, jda.getGuildById(config.getServerId()).getSelfMember(), "Automatic close due to inactivity");
                autoClosures++;
            } else if (shouldRemind) {
                EmbedBuilder builder = new EmbedBuilder()
                        .setTitle(String.format("⏰ Reminder: Waiting for your response (%s/%s)", ticket.getRemindersSent() + 1, AUTO_CLOSE_HOURS / REMIND_INTERVAL_HOURS - 1))
                        .setColor(Color.decode(config.getColor()))
                        .appendDescription("**Our support team is waiting for you to respond in %s**".formatted(ticket.getTextChannel()))
                        .appendDescription(String.format("\nIf you do not respond, the ticket will be automatically closed <t:%d:R>.",
                                ticket.getWaitingSince()
                                        .plus(Duration.ofHours(AUTO_CLOSE_HOURS + 1))
                                        .atZone(ZoneId.of("UTC"))
                                        .withMinute(0)
                                        .toInstant()
                                        .toEpochMilli() / 1000)
                        )
                        .setFooter(config.getServerName(), config.getServerLogo());

                EmbedBuilder threadMessageBuilder = new EmbedBuilder()
                        .setTitle("Reminder Sent")
                        .setDescription("The reminder was sent to %s (%s/%s)".formatted(ticket.getOwner().getAsMention(), ticket.getRemindersSent() + 1, AUTO_CLOSE_HOURS / REMIND_INTERVAL_HOURS - 1))
                        .setColor(Color.decode(config.getColor()))
                        .setFooter(config.getServerName(), config.getServerLogo());

                try {
                    ticket.getOwner().openPrivateChannel()
                            .flatMap(channel -> channel.sendMessageEmbeds(builder.build()))
                            .complete();
                } catch (ErrorResponseException e) {
                    ticket.getTextChannel()
                            .sendMessage(ticket.getOwner().getAsMention())
                            .setEmbeds(builder.build())
                            .queue();
                }

                ticket.getThreadChannel().sendMessageEmbeds(threadMessageBuilder.build()).queue();

                ticket.setRemindersSent(ticket.getRemindersSent() + 1);
                userReminders++;
            } else if (shouldRemindSupporter) {
                ticket.getThreadChannel().sendMessage(ticket.getSupporter().getAsMention()).queue();

                ticket.setSupporterRemindersSent(ticket.getSupporterRemindersSent() + 1);
                supporterReminders++;
            }

            // Rating reminder logic
            if (ticket.isPendingRating() && ticket.getPendingRatingSince() != null) {
                int maxReminders = config.getRatingMaxReminders();
                int intervalHours = config.getRatingReminderIntervalHours();

                boolean shouldAutoCloseRating = ticket.getRatingRemindersSent() >= maxReminders;
                boolean shouldRemindRating = !shouldAutoCloseRating &&
                        ticket.getPendingRatingSince()
                                .plus((long) intervalHours * (ticket.getRatingRemindersSent() + 1), ChronoUnit.HOURS)
                                .isBefore(Instant.now());

                if (shouldAutoCloseRating) {
                    ticket.setPendingRating(false);
                    ticketService.closeTicket(ticket, false, jda.getGuildById(config.getServerId()).getSelfMember(), "Closed without rating (no response)");
                    ratingAutoClosures++;
                } else if (shouldRemindRating) {
                    EmbedBuilder reminderEmbed = new EmbedBuilder()
                            .setColor(Color.ORANGE)
                            .setTitle(String.format("⏰ Rating Reminder (%d/%d)", ticket.getRatingRemindersSent() + 1, maxReminders))
                            .setDescription(ticket.getOwner().getAsMention() + ", please rate your support experience!\nThe ticket will be closed automatically if you don't respond.")
                            .setFooter(config.getServerName(), config.getServerLogo());

                    ticket.getTextChannel().sendMessage(ticket.getOwner().getAsMention())
                            .setEmbeds(reminderEmbed.build())
                            .queue();

                    ticket.setRatingRemindersSent(ticket.getRatingRemindersSent() + 1);
                    ratingReminders++;
                }
            }

            log.debug("Should remind: {}, Should close: {}, Should remind supporter: {}", shouldRemind, shouldClose, shouldRemindSupporter);

            try {
                TimeUnit.SECONDS.sleep(10L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        sendProcessingSummary(userReminders, supporterReminders, autoClosures, ratingReminders, ratingAutoClosures, ticketsIds.size());
    }

    private void sendProcessingSummary(int userReminders, int supporterReminders, int autoClosures, int ratingReminders, int ratingAutoClosures, int totalTickets) {
        if (config.getLogChannel() != 0 && (userReminders != 0 || supporterReminders != 0 || autoClosures != 0 || ratingReminders != 0 || ratingAutoClosures != 0)) {
            EmbedBuilder summaryBuilder = new EmbedBuilder()
                    .setTitle("📊 Hourly Ticket Processing Summary")
                    .setColor(Color.decode(config.getColor()))
                    .addField("Total Tickets Processed", "`" + totalTickets + "`", false)
                    .addField("User Reminders Sent", "`" + userReminders + "`", false)
                    .addField("Supporter Reminders Sent", "`" + supporterReminders + "`", false)
                    .addField("Auto-Closures", "`" + autoClosures + "`", false)
                    .addField("Rating Reminders Sent", "`" + ratingReminders + "`", false)
                    .addField("Rating Auto-Closures", "`" + ratingAutoClosures + "`", false)
                    .setFooter(config.getServerName(), config.getServerLogo());

            try {
                jda.getTextChannelById(config.getLogChannel())
                        .sendMessageEmbeds(summaryBuilder.build())
                        .queue(
                                success -> log.info("Processing summary sent to log channel"),
                                failure -> log.error("Failed to send processing summary to log channel: {}", failure.getMessage())
                        );
            } catch (Exception e) {
                log.error("Error sending processing summary: {}", e.getMessage());
            }
        }
    }
}
