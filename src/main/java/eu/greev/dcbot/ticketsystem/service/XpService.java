package eu.greev.dcbot.ticketsystem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.utils.Config;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Slf4j
public class XpService {
    private final Config config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final SupporterSettingsData supporterSettingsData;

    public XpService(Config config, SupporterSettingsData supporterSettingsData) {
        this.config = config;
        this.supporterSettingsData = supporterSettingsData;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Award XP to a helper for resolving a ticket.
     * This method collects ticket data and sends it to the API asynchronously.
     * No need to wait since we send the data directly (no channel fetch needed on backend).
     * The backend API will send the XP notification in Discord.
     *
     * @param ticket        The ticket being closed
     * @param rating        The star rating given by the ticket owner (1-5), can be null if skipped
     * @param transcriptUrl URL to the transcript in the log channel (for linking in notifications)
     */
    public void awardTicketXp(Ticket ticket, Integer rating, String transcriptUrl) {
        if (config.getXpApiUrl() == null || config.getXpApiUrl().isBlank()) {
            log.debug("[XP] XP API not configured, skipping");
            return;
        }

        if (ticket == null) {
            log.debug("[XP] No ticket provided, skipping XP award");
            return;
        }

        if (ticket.getSupporter() == null) {
            log.debug("[XP] No supporter for ticket #{}, skipping XP award", ticket.getId());
            return;
        }

        String supporterId = ticket.getSupporter().getId();
        if (supporterId == null || supporterId.isBlank() || !supporterId.matches("\\d{17,20}")) {
            log.warn("[XP] Invalid supporter ID '{}' for ticket #{}, skipping XP award", supporterId, ticket.getId());
            return;
        }

        if (ticket.getTextChannel() == null) {
            log.debug("[XP] No text channel for ticket #{}, skipping XP award", ticket.getId());
            return;
        }

        try {
            // Collect ticket data
            String ticketId = String.valueOf(ticket.getId());
            String category = ticket.getCategory() != null ? ticket.getCategory().getLabel() : "Unknown";
            String ownerId = ticket.getOwner() != null ? ticket.getOwner().getId() : null;
            // supporterId already validated above
            String channelId = ticket.getTextChannel().getId();

            // Get initial reason from info map
            String initialReason = null;
            if (ticket.getInfo() != null) {
                // The info map contains form fields, try to get reason/details
                initialReason = ticket.getInfo().getOrDefault("reason",
                        ticket.getInfo().getOrDefault("details",
                                ticket.getInfo().getOrDefault("info", null)));
            }

            // Collect messages from channel
            List<Map<String, String>> messages = collectMessages(ticket);
            if (messages.isEmpty()) {
                log.warn("[XP] No messages found in ticket #{}, skipping XP award", ticket.getId());
                return;
            }

            // Collect helper IDs (users who sent messages, excluding owner and bots)
            Set<String> helperIdSet = new HashSet<>();
            helperIdSet.add(supporterId); // Always include the claimer
            for (Map<String, String> msg : messages) {
                if ("helper".equals(msg.get("author"))) {
                    // We don't have the ID in the message map, but the claimer is covered
                }
            }
            // Add involved users (these are helpers who were added to the ticket)
            if (ticket.getInvolved() != null) {
                for (String involved : ticket.getInvolved()) {
                    if (!involved.equals(ownerId)) {
                        helperIdSet.add(involved);
                    }
                }
            }
            List<String> helperIds = new ArrayList<>(helperIdSet);

            // Check privacy setting for supporter
            boolean hideStats = supporterSettingsData.isHideStats(supporterId);

            // Build request body with all data
            Map<String, Object> body = new HashMap<>();
            body.put("channelId", channelId);
            body.put("supporterId", supporterId);
            body.put("ticketId", ticketId);
            body.put("category", category);
            body.put("ownerId", ownerId);
            body.put("initialReason", initialReason);
            body.put("messages", messages);
            body.put("helperIds", helperIds);
            body.put("hideStats", hideStats);
            if (rating != null) {
                body.put("rating", rating);
            }
            if (transcriptUrl != null && !transcriptUrl.isBlank()) {
                body.put("transcriptUrl", transcriptUrl);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getXpApiUrl() + "/tickets/award-xp"))
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", config.getXpApiKey())
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            // ASYNC call - no need to wait since backend has all data it needs
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            log.info("[XP] Successfully awarded XP for ticket #{}", ticket.getId());
                        } else {
                            log.warn("[XP] Failed to award XP for ticket #{}: {} - {}",
                                    ticket.getId(), response.statusCode(), response.body());
                        }
                    })
                    .exceptionally(e -> {
                        log.error("[XP] Error calling XP API for ticket #{}: {}", ticket.getId(), e.getMessage());
                        return null;
                    });

            log.info("[XP] Sent XP request for ticket #{} (async)", ticket.getId());

        } catch (Exception e) {
            log.error("[XP] Error preparing XP request for ticket #{}: {}", ticket.getId(), e.getMessage());
        }
    }

    /**
     * Award XP to a helper for resolving a ticket (without transcript URL).
     * This is a convenience overload for cases where no transcript is available.
     *
     * @param ticket  The ticket being closed
     * @param rating  The star rating given by the ticket owner (1-5), can be null if skipped
     */
    public void awardTicketXp(Ticket ticket, Integer rating) {
        awardTicketXp(ticket, rating, null);
    }

    /**
     * Collect messages from the ticket channel and convert to API format.
     */
    private List<Map<String, String>> collectMessages(Ticket ticket) {
        List<Map<String, String>> result = new ArrayList<>();

        try {
            String ownerId = ticket.getOwner() != null ? ticket.getOwner().getId() : null;
            String baseMessageId = ticket.getBaseMessage();

            // Fetch last 100 messages from channel
            List<Message> rawMessages = ticket.getTextChannel().getHistory()
                    .retrievePast(100)
                    .complete();

            // Reverse to get chronological order and process
            Collections.reverse(rawMessages);

            for (Message msg : rawMessages) {
                // Skip the base message (pinned ticket info)
                if (msg.getId().equals(baseMessageId)) {
                    continue;
                }

                // Skip bot messages (except system messages in embeds)
                if (msg.getAuthor().isBot()) {
                    continue;
                }

                // Skip empty messages
                String content = msg.getContentDisplay();
                if (content == null || content.isBlank()) {
                    continue;
                }

                // Determine author role
                String authorRole;
                if (ownerId != null && msg.getAuthor().getId().equals(ownerId)) {
                    authorRole = "owner";
                } else {
                    authorRole = "helper";
                }

                Map<String, String> msgMap = new HashMap<>();
                msgMap.put("author", authorRole);
                msgMap.put("content", content);
                result.add(msgMap);
            }

        } catch (Exception e) {
            log.error("[XP] Error collecting messages for ticket #{}: {}", ticket.getId(), e.getMessage());
        }

        return result;
    }

    /**
     * @deprecated Use {@link #awardTicketXp(Ticket, Integer)} instead.
     * This old method is kept for backwards compatibility but should not be used.
     */
    @Deprecated(since = "2025-12-29")
    public boolean awardTicketXp(String channelId, String supporterId, Integer rating) {
        log.warn("[XP] Using deprecated awardTicketXp method - please update to use Ticket object");
        // This old method can't work properly anymore since we need full ticket data
        // Just log a warning and return false
        return false;
    }
}
