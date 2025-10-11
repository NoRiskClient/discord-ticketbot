package eu.greev.dcbot.ticketsystem.service;

import eu.greev.dcbot.ticketsystem.categories.TicketCategory;
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
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.logging.log4j.util.Strings;
import org.jdbi.v3.core.Jdbi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public Optional<String> createNewTicket(Map<String, String> info, TicketCategory category, User owner) {
        Guild guild = jda.getGuildById(config.getServerId());
        int openTickets = 0;
        for (TextChannel textChannel : guild.getTextChannels()) {
            Ticket tckt = getTicketByChannelId(textChannel.getIdLong());
            if (tckt != null && tckt.getOwner().equals(owner)) {
                openTickets++;
            }
        }

        if (openTickets >= config.getMaxTicketsPerUser()) {
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

        ChannelAction<TextChannel> action = guild.createTextChannel(generateChannelName(ticket), jda.getCategoryById(config.getUnclaimedCategory()))
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

        TextChannel ticketChannel = action.complete();
        ThreadChannel thread = ticketChannel.createThreadChannel("Discussion-" + ticket.getId(), true).complete();

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
        return Optional.empty();
    }

    public void closeTicket(Ticket ticket, boolean wasAccident, User closer, @Nullable String message) {
        Transcript transcript = ticket.getTranscript();
        int ticketId = ticket.getId();
        ticket.setCloser(closer).setOpen(false).setCloseMessage(message);
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

        transcript.addLogMessage("[%s] closed the ticket%s".formatted(closer.getName(), message == null ? "." : " with following message: " + message), Instant.now().getEpochSecond(), ticketId);

        EmbedBuilder builder = new EmbedBuilder().setTitle("Ticket " + ticketId)
                .addField("Closed by", closer.getAsMention(), false);

        if (message != null && !message.isBlank()) {
            builder.addField("Message", message, true);
        }

        builder.addField("Text Transcript⠀⠀⠀⠀⠀⠀⠀⠀", "See attachment", false)
                .setColor(Color.decode(config.getColor()))
                .setFooter(config.getServerName(), config.getServerLogo());

        if (ticket.getOwner().getMutualGuilds().contains(jda.getGuildById(config.getServerId()))) {
            try {
                ticket.getOwner().openPrivateChannel()
                        .flatMap(channel -> channel.sendMessageEmbeds(builder.build()).setFiles(FileUpload.fromData(transcript.toFile(ticketId))))
                        .complete();
            } catch (ErrorResponseException e) {
                log.warn("Couldn't send [{}] their transcript since an error occurred:\nMeaning:{} | Message:{} | Response:{}", ticket.getOwner().getName(), e.getMeaning(), e.getMessage(), e.getErrorResponse());
            }
        }

        if (config.getLogChannel() != 0) {
            jda.getGuildById(config.getServerId()).getTextChannelById(config.getLogChannel()).sendMessageEmbeds(builder.build()).setFiles(FileUpload.fromData(transcript.toFile(ticketId))).queue();
        }

        saveTranscriptChanges(ticket.getTranscript().getRecentChanges());
        ticket.getTextChannel().delete().queue();
    }

    public boolean claim(Ticket ticket, User supporter) {
        if (supporter == ticket.getOwner()) return false;

        ticket.setSupporter(supporter);
        ticket.getTextChannel().getManager().setName(generateChannelName(ticket)).queue();

        ticket.getThreadChannel().addThreadMember(supporter).queue();

        if (config.getCategories().get(ticket.getCategory().getId()) != null) {
            ticket.getTextChannel().getManager().setParent(jda.getCategoryById(config.getCategories().get(ticket.getCategory().getId()))).delay(500, TimeUnit.MILLISECONDS).queue(
                    success -> jda.getGuildById(config.getServerId()).modifyTextChannelPositions(jda.getCategoryById(config.getCategories().get(ticket.getCategory().getId())))
                            .sortOrder(
                                    (o1, o2) -> {
                                        Ticket t1 = getTicketByChannelId(o1.getIdLong());
                                        Ticket t2 = getTicketByChannelId(o2.getIdLong());

                                        if (t1 == null || t2 == null) {
                                            return 0;
                                        } else {
                                            int result = Long.compare(t1.getSupporter().getIdLong(), t2.getSupporter().getIdLong());

                                            return  result != 0 ? result : Long.compare(t1.getId(), t2.getId());
                                        }
                                    }
                            ).queue(),
                    error -> {
                        if (error.getMessage().contains("CHANNEL_PARENT_MAX_CHANNELS")) {
                            EmbedBuilder embedBuilder = new EmbedBuilder()
                                    .setColor(Color.YELLOW)
                                    .setDescription("❗**The channel category for this ticket category is full! Please try to close some tickets.**");
                            ticket.getThreadChannel().sendMessageEmbeds(embedBuilder.build()).queue();
                        } else {
                            log.error("Couldn't move ticket channel to category!", error);
                        }
                    }
            );
        } else {
            EmbedBuilder error = new EmbedBuilder()
                    .setColor(Color.YELLOW)
                    .setDescription("❗**Category %s doesn't have a channel category assigned, please tell an Admin to add it to the config!**".formatted(ticket.getCategory().getId()));
            ticket.getTextChannel().sendMessageEmbeds(error.build()).queue();
        }

        if (config.getCategoryRoles().get(ticket.getCategory().getId()) != null) {
            for (Long id : config.getCategoryRoles().get(ticket.getCategory().getId())) {
                Role role = ticket.getTextChannel().getGuild().getRoleById(id);
                if (role != null) {
                    ticket.getTextChannel().upsertPermissionOverride(role).setAllowed(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY).queue();
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

    public void toggleWaiting(Ticket ticket, boolean waiting) {
        TextChannelManager manager = ticket.getTextChannel().getManager();
        ticket.setWaiting(waiting);
        manager.setName(generateChannelName(ticket)).queue();
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

    public @Nullable Ticket getTicketByChannel(@Nullable Channel channel) {
        return channel == null ? null : getTicketByChannelId(channel.getIdLong());
    }

    public boolean isTicketChannel(@NotNull Channel channel) {
        final Channel finalChannel = channel instanceof ThreadChannel thread ? thread.getParentChannel() : channel;
        return allCurrentTickets.stream().anyMatch(t -> t.getThreadChannel().equals(finalChannel));
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

    public String generateChannelName(Ticket ticket) {
        String category = ticket.getCategory().getId();
        int ticketId = ticket.getId();

        String name = "";

        if (ticket.isWaiting()) {
            name += WAITING_EMOTE + "-";
        }

        if (ticket.getSupporter() != null) {
            name += config.getClaimEmojis().getOrDefault(ticket.getSupporter().getIdLong(), "✓") + "-";
        }

        name += category + "-" + ticketId + "-" + ticket.getOwner().getName();

        return name;
    }
}