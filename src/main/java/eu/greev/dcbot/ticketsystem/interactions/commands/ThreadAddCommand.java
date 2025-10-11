package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.interactions.Interaction;
import eu.greev.dcbot.ticketsystem.interactions.InteractionMessages;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

public class ThreadAddCommand extends Interaction {
    public ThreadAddCommand(@NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda) {
        super(config, ticketService, jda);
        addCommand("add", "Add a staff member to the ticket thread", THREADS_GROUP, d -> d
                .addOption(OptionType.USER, "staff", "Staff member to add", true));
    }

    @Override
    public String getIdentifier() {
        return "thread add";
    }

    @Override
    public void handleSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if(event.getChannelType() != ChannelType.GUILD_PRIVATE_THREAD) {
            replyEphemeralAndQueue(InteractionMessages.THREAD_ADD_NOT_A_THREAD);
            return;
        }

        Member member = event.getOption("staff", OptionMapping::getAsMember);
        if(member == null) {
            replyEphemeralAndQueue(InteractionMessages.INTERACTION_INVALID_MEMBER);
            return;
        }

        if(missingPermissions(null, member)) {
            InteractionMessages.THREAD_ADD_NOT_STAFF.placeholder("$OTHER_MENTION", member.getAsMention());
            replyEphemeralAndQueue(InteractionMessages.THREAD_ADD_NOT_STAFF);
            return;
        }

        ThreadChannel thread = event.getGuildChannel().asThreadChannel();
        if (thread.getMembers().contains(member)) {
            InteractionMessages.THREAD_ADD_ALREADY_ADDED.placeholder("$OTHER_MENTION", member.getAsMention());
            replyEphemeralAndQueue(InteractionMessages.THREAD_ADD_ALREADY_ADDED);
            return;
        }

        thread.addThreadMember(member).queue();
        InteractionMessages.THREAD_ADD_ADDED.placeholder("$OTHER_MENTION", member.getAsMention());
        replyAndQueue(InteractionMessages.THREAD_ADD_ADDED);
    }
}
