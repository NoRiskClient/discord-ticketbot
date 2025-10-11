package eu.greev.dcbot.ticketsystem.interactions;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import eu.greev.dcbot.utils.CustomEmbedBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public abstract class Interaction {
    public static final Map<SubcommandGroupData, List<SubcommandData>> COMMANDS = new HashMap<>();

    protected static final SubcommandGroupData THREADS_GROUP = new SubcommandGroupData("thread", "Manages the ticket thread");
    protected static final SubcommandGroupData LIST_TICKETS_GROUP = new SubcommandGroupData("list-tickets", "Lists tickets with options to narrow down results");

    protected final @NotNull Config config;
    protected final @NotNull TicketService ticketService;
    protected final @NotNull JDA jda;

    protected boolean permissionsRequired = true;
    protected boolean ticketChannelRequired = true;
    protected boolean administratorRequired = false;

    @Getter protected @Nullable Ticket ticket;
    @Getter protected IReplyCallback reply;

    protected Interaction(@NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda) {
        this.config = config;
        this.ticketService = ticketService;
        this.jda = jda;
    }

    protected void addCommand(String description) {
        this.addCommand(description, null);
    }

    protected void addCommand(String description, @Nullable Function<SubcommandData, SubcommandData> options) {
        this.addCommand(this.getIdentifier(), description, null, options);
    }

    protected void addCommand(String name, String description, @Nullable SubcommandGroupData group, @Nullable Function<SubcommandData, SubcommandData> options) {
        SubcommandData command = new SubcommandData(name, description);
        if(options != null) command = options.apply(command);
        if(group != null) group.addSubcommands(command);
        COMMANDS.computeIfAbsent(group, k -> new ArrayList<>()).add(command);
    }

    public abstract String getIdentifier();

    public void handleButtonInteraction(@NotNull ButtonInteractionEvent event) {}
    public void handleModalInteraction(@NotNull ModalInteractionEvent event) {}
    public void handleStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {}
    public void handleSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {}

    private boolean serverNotPresent() {
        if(config.getServerName() != null) return false;
        replyEphemeralAndQueue(InteractionMessages.INTERACTION_INCORRECT_SETUP);
        return true;
    }

    private boolean missingPermissions(@NotNull IReplyCallback reply) {
        return missingPermissions(reply, reply.getMember());
    }

    protected boolean missingPermissions(@Nullable IReplyCallback reply, @Nullable Member member) {
        if(member == null) {
            log.error("Interaction {} has been ran without member", this.getIdentifier());
            return true;
        }

        if (!member.getRoles().contains(jda.getRoleById(config.getStaffId())) || (administratorRequired && !member.getPermissions().contains(Permission.ADMINISTRATOR))) {
            if(reply != null) replyEphemeralAndQueue(InteractionMessages.INTERACTION_MISSING_PERMISSIONS);
            return true;
        }

        return false;
    }

    private boolean isIncorrectChannel(IReplyCallback reply) {
        if(!Optional.ofNullable(reply.getChannel()).map(ticketService::isTicketChannel).orElse(false)) {
            replyEphemeralAndQueue(InteractionMessages.INTERACTION_WRONG_CHANNEL);
            return true;
        }

        return false;
    }

    public boolean conditionsNotFulfilled(IReplyCallback event) {
        return serverNotPresent() ||
                (permissionsRequired && missingPermissions(event)) ||
                (ticketChannelRequired && isIncorrectChannel(event));
    }

    public void handle(IReplyCallback event) {
        this.reply = event;
        if(conditionsNotFulfilled(event)) return;
        if(this.ticketChannelRequired) this.ticket = this.ticketService.getTicketByChannel(event.getChannel());
        switch (event) {
            case ButtonInteractionEvent e -> handleButtonInteraction(e);
            case ModalInteractionEvent e -> handleModalInteraction(e);
            case StringSelectInteractionEvent e -> handleStringSelectInteraction(e);
            case SlashCommandInteractionEvent e -> handleSlashCommandInteraction(e);
            default -> {}
        }
    }

    protected void replyEphemeralAndQueue(CustomEmbedBuilder builder) {
        replyEphemeral(builder).queue();
    }

    protected void replyAndQueue(CustomEmbedBuilder builder) {
        reply(builder).queue();
    }

    protected ReplyCallbackAction replyEphemeral(CustomEmbedBuilder builder) {
        return reply(builder).setEphemeral(true);
    }

    protected ReplyCallbackAction reply(CustomEmbedBuilder builder) {
        return reply.replyEmbeds(build(builder));
    }

    protected void deferEphemeralAndQueue(Supplier<CustomEmbedBuilder> builderSupplier) {
        reply.deferReply(true).queue();
        CustomEmbedBuilder builder = builderSupplier.get();
        reply.getHook().sendMessageEmbeds(build(builder)).setEphemeral(true).queue();
    }

    /**
     * Sets the default placeholder values and builds the CustomEmbedBuilder
     * @param builder The CustomEmbedBuilder to be built
     * @return The built MessageEmbed
     */
    private MessageEmbed build(CustomEmbedBuilder builder) {
        builder.placeholder("$SERVER_NAME", config.getServerName());
        builder.placeholder("$SERVER_LOGO", config.getServerLogo());
        builder.placeholder("$USER_NAME", reply.getUser().getName());
        builder.placeholder("$USER_AVATAR", reply.getUser().getEffectiveAvatarUrl());
        builder.placeholder("$MENTION", reply.getUser().getAsMention());
        builder.placeholder("$CONFIG_COLOR", config.getColor());
        return builder.build();
    }
}
