package eu.greev.dcbot.ticketsystem.service;

import me.ryzeon.transcripts.DiscordHtmlTranscripts;
import eu.greev.dcbot.Main;
import eu.greev.dcbot.ticketsystem.categories.ICategory;
import eu.greev.dcbot.ticketsystem.entities.Edit;
import eu.greev.dcbot.ticketsystem.entities.Message;
import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.entities.TranscriptEntity;
import eu.greev.dcbot.utils.Config;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.logging.log4j.util.Strings;
import org.jdbi.v3.core.Jdbi;

import java.awt.*;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TicketService {
    private final JDA jda;
    private final Config config;
    private final Jdbi jdbi;
    private final TicketData ticketData;
    private final Set<Ticket> allCurrentTickets = new HashSet<>();
    public static final String WAITING_EMOTE = "\uD83D\uDD50";

    public TicketData getTicketData() {
        return ticketData;
    }

    public TicketService(JDA jda, Config config, Jdbi jdbi, TicketData ticketData) {
        this.jda = jda;
        this.config = config;
        this.jdbi = jdbi;
        this.ticketData = ticketData;

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                getOpenCachedTickets().stream()
                        .map(Ticket::getTranscript)
                        .map(Transcript::getRecentChanges)
                        .filter(changes -> !changes.isEmpty())
                        .forEach(TicketService.this::saveTranscriptChanges);
            }
        }, 0, TimeUnit.MINUTES.toMillis(3));
    }

    public Optional<String> createNewTicket(Map<String, String> info, ICategory category, User owner) {
        Guild guild = jda.getGuildById(config.getServerId());
        int openTickets = 0;
        for (TextChannel textChannel : guild.getTextChannels()) {
            Ticket tckt = getTicketByChannelId(textChannel.getIdLong());
            if (tckt != null && tckt.getOwner().equals(owner)) {
                openTickets++;
            }
        }

        if (!config.isDevMode() && openTickets >= config.getMaxTicketsPerUser()) {
            return Optional.of("You have reached the maximum number of open tickets (" + config.getMaxTicketsPerUser() + "). Please close an existing ticket before opening a new one.");
        }

        Ticket ticket = Ticket.builder()
                .ticketData(ticketData)
                .transcript(new Transcript(new ArrayList<>()))
                .owner(owner)
                .isOpen(true)
                .category(category)
                .info(info)
                .build();

        // Create DB record and get generated ticket ID before creating channels
        int newId = ticketData.saveTicket(ticket);
        ticket = ticket.toBuilder().id(newId).build();

        ChannelAction<TextChannel> action = guild.createTextChannel(generateChannelName(ticket, false), getOrCreateChannelCategory(Main.UNCLAIMED_KEY, null))
                .addRolePermissionOverride(guild.getPublicRole().getIdLong(), null, List.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY))
                .addMemberPermissionOverride(owner.getIdLong(), List.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY), null);

        if (config.getCategoryRoles().get(ticket.getCategory().getId()) != null) {
            for (Long id : config.getCategoryRoles().get(ticket.getCategory().getId())) {
                Role role = guild.getRoleById(id);
                if (role != null) {
                    action.addRolePermissionOverride(role.getIdLong(), List.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY), null);
                }
            }
        } else {
            action.addRolePermissionOverride(config.getStaffId(), List.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY), null);
        }

        TextChannel ticketChannel;
        try {
            ticketChannel = action.complete();
        } catch (ErrorResponseException e) {
            if (e.getMessage().contains("INVALID_COMMUNITY_PROPERTY_NAME")) {
                ticketChannel = action.setName(generateChannelName(ticket, true)).complete();
            } else {
                return Optional.of("An error occurred while creating the ticket channel: " + e.getMessage());
            }
        }

        ThreadChannel threadChannel;

        try {
            threadChannel = ticketChannel.createThreadChannel("Discussion-" + ticket.getId(), true).complete();
        } catch (ErrorResponseException e) {
            if (e.getErrorCode() == 160006) {
                log.warn("Active thread limit reached, falling back to placeholder thread.");
                ThreadChannel fallback = guild.getThreadChannelById(config.getPlaceholderThreadId());
                if (fallback != null) {
                    threadChannel = fallback;
                } else {
                    log.error("Failed to create thread for ticket #{} and placeholder thread not found!", ticket.getId(), e);
                    threadChannel = null;
                }
            } else {
                log.error("Failed to create thead for ticket #{}", ticket.getId(), e);
                threadChannel = null;
            }
        }

        final ThreadChannel thread = threadChannel;

        EmbedBuilder builder = new EmbedBuilder().setColor(Color.decode(config.getColor()))
                .setDescription("Hello there, " + owner.getAsMention() + "! " + """
                        A member of staff will assist you shortly.
                        In the meantime, please describe your issue in as much detail as possible! :)
                        """)
                .addField("**Ticket ID**", "`%s`".formatted(String.valueOf(ticket.getId())), true)
                .addField("**Category**", ticket.getCategory().getLabel(), true)
                .addField("**Owner**", owner.getAsMention(), true)
                .setImage("https://cdn.norisk.gg/misc/nrc_ticket_banner.png")
                .setAuthor(owner.getName(), null, owner.getEffectiveAvatarUrl());


        StringBuilder details = new StringBuilder();

        for (Map.Entry<String, String> entry : info.entrySet()) {
            details.append("**").append(entry.getKey()).append("**\n").append(entry.getValue()).append("\n");
        }

        String detailsValue = details.toString();
        if (detailsValue.length() > 1024) {
            detailsValue = detailsValue.substring(0, 1021) + "...";
        }

        builder.addField("**▬▬▬▬▬**", detailsValue, false);

        ticketChannel.sendMessage(owner.getAsMention() + " has created a new ticket").complete();

        String msgId = ticketChannel.sendMessageEmbeds(builder.build())
                .setActionRow(Button.primary("claim", "Claim"),
                        Button.danger("close", "Close")).complete().getId();
        ticket.setTextChannel(ticketChannel)
                .setThreadChannel(thread)
                .setBaseMessage(msgId);
        allCurrentTickets.add(ticket);

        ticketChannel.pinMessageById(msgId).queue();

        EmbedBuilder builder1 = new EmbedBuilder().setColor(Color.decode(config.getColor()))
                .setFooter(config.getServerName(), config.getServerLogo())
                .setDescription("""
                        If you opened this ticket accidentally, you have now the opportunity to close it again for 1 minute! Just click `Nevermind!` below.
                        This message will delete itself after this minute.
                        """);

        Ticket finalTicket = ticket;
        ticketChannel.sendMessageEmbeds(builder1.build())
                .setActionRow(Button.danger("nevermind", "Nevermind!"))
                .queue(suc -> {
                    suc.delete().queueAfter(1, TimeUnit.MINUTES, msg -> {
                    }, err -> {
                    });
                    finalTicket.setTempMsgId(suc.getId());
                });

        if (thread != null) {
            config.getAddToTicketThread().forEach(id -> {
                Role role = guild.getRoleById(id);
                if (role != null) {
                    guild.findMembersWithRoles(role).onSuccess(list -> list.forEach(member -> thread.addThreadMember(member).queue()));
                    return;
                }
                Member member = guild.retrieveMemberById(id).complete();
                if (member != null) {
                    thread.addThreadMember(member).queue();
                }
            });
        }
        return Optional.empty();
    }

    public void closeTicket(Ticket ticket, boolean wasAccident, Member closer, String message) {
        closeTicket(ticket, wasAccident, closer, message, null);
    }

    public void closeTicket(Ticket ticket, boolean wasAccident, Member closer, String message, String existingTranscriptUrl) {
        Transcript transcript = ticket.getTranscript();
        int ticketId = ticket.getId();
        ticket.setCloser(closer.getUser()).setOpen(false).setCloseMessage(message).setClosedAt(Instant.now().getEpochSecond());
        if (wasAccident) {
            ticket.getTextChannel().delete().queue();
            jdbi.withHandle(handle -> handle.createUpdate("DELETE FROM tickets WHERE ticketID=?").bind(0, ticketId).execute());
            allCurrentTickets.remove(ticket);

            ticketData.getTranscriptData().deleteTranscript(ticket);
            return;
        }

        jdbi.withHandle(handle -> handle.createUpdate("UPDATE tickets SET closer=? WHERE ticketID=?")
                .bind(0, closer.getId())
                .bind(1, ticketId)
                .execute());

        transcript.addLogMessage("[%s] closed the ticket%s".formatted(closer.getUser().getName(), message == null ? "." : " with following message: " + message), Instant.now().getEpochSecond(), ticketId);

        // Use existing transcript URL if provided, otherwise generate new one
        String transcriptUrl = existingTranscriptUrl;
        if (transcriptUrl == null) {
            try {
                if (ticket.getTextChannel() != null && config.getLogChannel() != 0) {
                    // Fetch messages first to avoid NPE in library when handling message references
                    var messages = ticket.getTextChannel().getIterableHistory()
                            .takeAsync(1000)
                            .get();

                    if (messages != null && !messages.isEmpty()) {
                        FileUpload htmlTranscriptUpload = DiscordHtmlTranscripts.getInstance()
                                .createTranscript(ticket.getTextChannel(), "transcript-" + ticketId + ".html");

                        var logChannel = jda.getGuildById(config.getServerId()).getTextChannelById(config.getLogChannel());
                        if (logChannel != null) {
                            var uploadMessage = logChannel.sendFiles(htmlTranscriptUpload).complete();
                            if (!uploadMessage.getAttachments().isEmpty()) {
                                transcriptUrl = uploadMessage.getAttachments().getFirst().getUrl();
                            }
                        }
                    } else {
                        log.warn("No messages found in ticket #{} channel, skipping transcript generation", ticketId);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to generate/upload HTML transcript for ticket #{}: {}", ticketId, e.getMessage());
                // Continue without transcript - don't let this block ticket closure
            }
        }

        EmbedBuilder builder = new EmbedBuilder().setTitle("Ticket " + ticketId)
                .addField("Closed by", closer.getAsMention(), false);

        if (message != null && !message.isBlank()) {
            builder.addField("Message", message, true);
        }

        if (transcriptUrl != null) {
            builder.addField("📝 Transcript", "[Hier klicken](" + transcriptUrl + ")", false);
        }

        builder.setColor(Color.decode(config.getColor()))
                .setFooter(config.getServerName(), config.getServerLogo());

        if (ticket.getOwner().getMutualGuilds().contains(jda.getGuildById(config.getServerId()))) {
            try {
                ticket.getOwner().openPrivateChannel()
                        .flatMap(channel -> channel.sendMessageEmbeds(builder.build()))
                        .complete();
            } catch (ErrorResponseException e) {
                log.warn("Couldn't send [{}] their transcript since an error occurred:\nMeaning:{} | Message:{} | Response:{}", ticket.getOwner().getName(), e.getMeaning(), e.getMessage(), e.getErrorResponse());
            }
        }

        if (config.getLogChannel() != 0 && transcriptUrl != null) {
            jda.getGuildById(config.getServerId()).getTextChannelById(config.getLogChannel())
                    .sendMessageEmbeds(builder.build())
                    .queue();
        }

        saveTranscriptChanges(ticket.getTranscript().getRecentChanges());

        Category parentCategory = ticket.getTextChannel().getParentCategory();

        ticket.getTextChannel().delete().queue(v -> {
            fillUpOrDeleteCategoryIfPossible(parentCategory);
        });
    }

    public boolean claim(Ticket ticket, User supporter) {
        if (!config.isDevMode() && supporter == ticket.getOwner()) return false;

        ticket.setSupporter(supporter);

        try {
            ticket.getTextChannel().getManager().setName(generateChannelName(ticket, false)).complete();
        } catch (ErrorResponseException e) {
            if (e.getMessage().contains("INVALID_COMMUNITY_PROPERTY_NAME")) {
                ticket.getTextChannel().getManager().setName(generateChannelName(ticket, true)).complete();
            } else {
                log.error("Couldn't rename ticket channel for ticket {}!", ticket.getId(), e);
            }
        }

        if (ticket.getThreadChannel() != null) {
            ticket.getThreadChannel().addThreadMember(supporter).queue();
        }

        Category oldCategory = ticket.getTextChannel().getParentCategory();
        Category supporterCategory = getOrCreateChannelCategory(String.valueOf(supporter.getIdLong()), supporter.getName());
        ticket.getTextChannel().getManager().setParent(supporterCategory).delay(500, TimeUnit.MILLISECONDS).queue(
                success -> {
                    supporterCategory.modifyTextChannelPositions()
                            .sortOrder(Comparator.comparingLong(ISnowflake::getIdLong))
                            .queue();

                    fillUpOrDeleteCategoryIfPossible(oldCategory);
                },
                error -> log.error("Couldn't move ticket channel to supporter category!", error)
        );

        if (config.getCategoryRoles().get(ticket.getCategory().getId()) != null) {
            for (Long id : config.getCategoryRoles().get(ticket.getCategory().getId())) {
                Role role = ticket.getTextChannel().getGuild().getRoleById(id);
                if (role != null) {
                    ticket.getTextChannel().upsertPermissionOverride(role).setAllowed(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY).queue();
                } else {
                    log.warn("Couldn't find role {} for category {}", id, ticket.getCategory().getId());
                }
            }
        } else {
            ticket.getTextChannel().upsertPermissionOverride(jda.getRoleById(config.getStaffId())).setAllowed(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY).queue();
        }

        ticket.getTranscript().addLogMessage("[" + supporter.getName() + "] claimed the ticket.", Instant.now().getEpochSecond(), ticket.getId());
        ticket.getTextChannel().editMessageComponentsById(ticket.getBaseMessage())
                .setActionRow(Button.danger("close", "Close"))
                .queue();
        return true;
    }

    public void loadChannelCategories() {
        Guild guild = jda.getGuildById(config.getServerId());

        jdbi.useHandle(handle -> handle.createQuery("SELECT categoryID, key FROM channel_categories")
                .map((resultSet, index, ctx) -> {
                    String categoryId = resultSet.getString("categoryID");
                    String key = resultSet.getString("key");
                    log.info("Found channel category: {} {}", categoryId, key);

                    Category category = guild.getCategoryById(categoryId);
                    if (category != null) {
                        Main.CHANNEL_CATEGORIES.computeIfAbsent(key, k -> new ArrayList<>()).add(category);
                    } else {
                        handle.createUpdate("DELETE FROM channel_categories WHERE categoryID = ?")
                                .bind(0, categoryId)
                                .execute();
                    }

                    return null;
                })
                .list());
    }

    public Category getOrCreateChannelCategory(String key, String supporterName) {
        return Main.CHANNEL_CATEGORIES.computeIfAbsent(key, k -> new ArrayList<>())
                .stream()
                .filter(c -> c.getChannels().size() < 50)
                .findFirst()
                .orElseGet(() -> {
                    Guild guild = jda.getGuildById(config.getServerId());
                    Category newCategory;

                    if (Main.CHANNEL_CATEGORIES.get(key) == null) {
                        Main.CHANNEL_CATEGORIES.put(key, new ArrayList<>());
                        guild.createCategory(config.getClaimEmojis().getOrDefault(Long.parseLong(key), "✓") + " " + supporterName).complete();
                    }

                    if (supporterName != null) {
                        newCategory = guild.createCategory(config.getClaimEmojis().getOrDefault(Long.parseLong(key), "✓") + " " + supporterName).complete();
                    } else {
                        newCategory = guild.createCategory(Main.CHANNEL_CATEGORIES.get(key).getFirst().getName()).complete();
                    }

                    guild.modifyCategoryPositions()
                            .selectPosition(newCategory)
                            .moveBelow(Main.CHANNEL_CATEGORIES.get(key).isEmpty() ? Main.CHANNEL_CATEGORIES.get(Main.UNCLAIMED_KEY).getLast() : Main.CHANNEL_CATEGORIES.get(key).getLast())
                            .queue();

                    Main.CHANNEL_CATEGORIES.get(key).add(newCategory);

                    jdbi.withHandle(handle ->
                            handle.createUpdate("INSERT INTO channel_categories (categoryID, key) VALUES (?, ?)")
                                    .bind(0, newCategory.getId())
                                    .bind(1, key)
                                    .execute()
                    );

                    return newCategory;
                });
    }

    public void fillUpOrDeleteCategoryIfPossible(Category category) {
        if (category == null) return;

        List<Category> categories = Main.CHANNEL_CATEGORIES
                .values()
                .stream()
                .filter(list -> list.contains(category))
                .findFirst()
                .orElse(null);

        if (categories == null) return;

        if (category.getIdLong() != categories.getLast().getIdLong()) {
            ((TextChannelManager) categories.getLast().getChannels().getLast().getManager())
                    .setParent(category)
                    .queue(success -> fillUpOrDeleteCategoryIfPossible(categories.getLast()));
        }

        List<Category> unclaimed = Main.CHANNEL_CATEGORIES.get(Main.UNCLAIMED_KEY);
        List<Category> pendingRating = Main.CHANNEL_CATEGORIES.get(Main.PENDING_RATING_KEY);
        if (category.getChannels().isEmpty() && !(unclaimed.contains(category) && unclaimed.size() == 1) && !(pendingRating.contains(category) && pendingRating.size() == 1)) {
            Main.CHANNEL_CATEGORIES.values().forEach(list -> list.remove(category));
            jdbi.withHandle(handle ->
                    handle.createUpdate("DELETE FROM channel_categories WHERE categoryID = ?")
                            .bind(0, category.getId())
                            .execute()
            );
            category.delete().queue();
        }
    }

    public void toggleWaiting(Ticket ticket, boolean waiting) {
        TextChannelManager manager = ticket.getTextChannel().getManager();
        ticket.setWaiting(waiting);
        String channelName = generateChannelName(ticket, false);

        manager.setName(channelName).queue(
                success -> log.debug("Successfully renamed ticket #{} channel to {}", ticket.getId(), channelName),
                error -> {
                    if (error.getMessage().contains("INVALID_COMMUNITY_PROPERTY_NAME")) {
                        String fallbackName = generateChannelName(ticket, true);
                        manager.setName(fallbackName).queue(
                                s -> log.debug("Successfully renamed ticket #{} channel to {} (fallback)", ticket.getId(), fallbackName),
                                e -> log.error("Couldn't rename ticket channel for ticket {}!", ticket.getId(), e)
                        );
                    } else {
                        log.error("Couldn't rename ticket channel for ticket {}!", ticket.getId(), error);
                    }
                }
        );
    }

    public boolean addUser(Ticket ticket, User user) {
        Guild guild = ticket.getTextChannel().getGuild();
        PermissionOverride permissionOverride = ticket.getTextChannel().getPermissionOverride(guild.getMember(user));
        if ((permissionOverride != null && permissionOverride.getAllowed().contains(Permission.VIEW_CHANNEL))
                || guild.getMember(user).getPermissions().contains(Permission.ADMINISTRATOR)) {
            return false;
        }

        ticket.getTranscript().addLogMessage("[" + user.getName() + "] got added to the ticket.", Instant.now().getEpochSecond(), ticket.getId());

        ticket.getTextChannel().upsertPermissionOverride(guild.getMember(user)).setAllowed(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY, Permission.MESSAGE_SEND).queue();
        ticket.addInvolved(user.getId());
        return true;
    }

    public boolean removeUser(Ticket ticket, User user) {
        Guild guild = ticket.getTextChannel().getGuild();
        PermissionOverride permissionOverride = ticket.getTextChannel().getPermissionOverride(guild.getMember(user));
        if (permissionOverride == null || !permissionOverride.getAllowed().contains(Permission.VIEW_CHANNEL)) {
            return false;
        }
        ticket.getTranscript().addLogMessage("[" + user.getName() + "] got removed from the ticket.", Instant.now().getEpochSecond(), ticket.getId());
        ticket.getTextChannel().upsertPermissionOverride(guild.getMember(user)).setDenied(Permission.VIEW_CHANNEL).queue();
        ticket.removeInvolved(user.getId());
        return true;
    }

    public boolean setOwner(Ticket ticket, Member owner) {
        if (ticket.getTextChannel().getPermissionOverride(owner) == null
                || !ticket.getTextChannel().getPermissionOverride(owner).getAllowed().contains(Permission.VIEW_CHANNEL)) {
            return false;
        }

        ticket.getTranscript().addLogMessage("[" + owner.getUser().getName() + "] is the new ticket owner.", Instant.now().getEpochSecond(), ticket.getId());
        ticket.setOwner(owner.getUser());
        return true;
    }

    public Ticket getTicketByChannelId(long idLong) {
        Optional<Ticket> optionalTicket = allCurrentTickets.stream()
                .filter(ticket -> ticket.getTextChannel() != null)
                .filter(ticket -> ticket.getTextChannel().getIdLong() == idLong)
                .findAny();

        return optionalTicket.orElseGet(() -> {
            Ticket loadedTicket = ticketData.loadTicket(idLong);
            if (loadedTicket != null) {
                allCurrentTickets.add(loadedTicket);
            }
            return loadedTicket;
        });
    }

    public Ticket getTicketByTicketId(int ticketID) {
        Optional<Ticket> optionalTicket = allCurrentTickets.stream()
                .filter(ticket -> ticket.getId() == (ticketID))
                .findAny();

        return optionalTicket.orElseGet(() -> {
            Ticket loadedTicket = ticketData.loadTicket(ticketID);
            if (loadedTicket != null) {
                allCurrentTickets.add(loadedTicket);
            }
            return loadedTicket;
        });
    }

    public List<Ticket> getOpenCachedTickets() {
        return allCurrentTickets.stream().filter(Ticket::isOpen).toList();
    }

    public List<Integer> getTicketIdsByOwner(long owner) {
        return ticketData.getTicketIdsByUser(String.valueOf(owner));
    }

    public List<Ticket> getOpenTickets(User owner) {
        return ticketData.getOpenTicketsOfUser(owner.getId())
                .stream()
                .map(this::getTicketByTicketId)
                .toList();
    }

    private void saveTranscriptChanges(List<TranscriptEntity> changes) {
        TranscriptData transcriptData = ticketData.getTranscriptData();
        for (TranscriptEntity entity : changes) {
            if (entity instanceof Edit edit) {
                transcriptData.addEditToMessage(edit);
                continue;
            }
            Message message = (Message) entity;

            if (message.getId() == 0 && message.getAuthor().equals(Strings.EMPTY)) {
                transcriptData.addLogMessage(message);
                continue;
            }

            if (message.isDeleted()) {
                transcriptData.deleteMessage(message.getId());
            } else {
                transcriptData.addNewMessage(message);
            }
        }
        changes.clear();
    }

    public String generateChannelName(Ticket ticket, boolean excludeUsername) {
        String category = ticket.getCategory().getId();
        int ticketId = ticket.getId();

        String name = "";

        if (ticket.isWaiting()) {
            name += WAITING_EMOTE + "-";
        }

        if (ticket.getSupporter() != null) {
            name += config.getClaimEmojis().getOrDefault(ticket.getSupporter().getIdLong(), "✓") + "-";
        }

        name += category + "-" + ticketId;

        if (!excludeUsername) {
            name += "-" + ticket.getOwner().getName();
        }

        return name;
    }

    public void transfer(Ticket ticket, User supporter) {
        ticket.setSupporter(supporter);
        try {
            ticket.getTextChannel().getManager().setName(generateChannelName(ticket, false)).complete();
        } catch (ErrorResponseException e) {
            if (e.getMessage().contains("INVALID_COMMUNITY_PROPERTY_NAME")) {
                ticket.getTextChannel().getManager().setName(generateChannelName(ticket, true)).complete();
            } else {
                log.error("Couldn't rename ticket channel for ticket {}!", ticket.getId(), e);
            }
        }
        Category oldCategory = ticket.getTextChannel().getParentCategory();
        Category newCategory = getOrCreateChannelCategory(String.valueOf(supporter.getIdLong()), supporter.getName());

        ticket.getTextChannel().getManager().setParent(newCategory).delay(500, TimeUnit.MILLISECONDS).queue(
                success -> {
                    newCategory.modifyTextChannelPositions()
                            .sortOrder(Comparator.comparingLong(ISnowflake::getIdLong))
                            .queue();
                    fillUpOrDeleteCategoryIfPossible(oldCategory);
                },
                error -> log.error("Couldn't move ticket channel to supporter category!", error)
        );
    }
}