package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.interactions.Interaction;
import eu.greev.dcbot.ticketsystem.interactions.InteractionMessages;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

public class RemoveMemberCommand extends Interaction {
    public RemoveMemberCommand(@NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda) {
        super(config, ticketService, jda);
        addCommand("Remove a User from this ticket",
                d -> d.addOption(OptionType.USER, "member", "The user removing from the current ticket", true));
    }

    @Override
    public String getIdentifier() {
        return "remove";
    }

    @Override
    public void handleSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        Member member = event.getOption("member", OptionMapping::getAsMember);
        if(member == null) {
            replyEphemeralAndQueue(InteractionMessages.INTERACTION_INVALID_MEMBER);
            return;
        }

        if(!missingPermissions(null, member)) {
            InteractionMessages.REMOVE_MEMBER_STAFF.placeholder("$OTHER_MENTION", member.getAsMention());
            replyEphemeralAndQueue(InteractionMessages.REMOVE_MEMBER_STAFF);
        }

        if(this.ticket == null) return;
        if (ticketService.removeUser(ticket, member.getUser())) {
            InteractionMessages.REMOVE_MEMBER_REMOVED.placeholder("$OTHER_MENTION", member.getAsMention());
            replyAndQueue(InteractionMessages.REMOVE_MEMBER_REMOVED);
        } else {
            InteractionMessages.REMOVE_MEMBER_NOT_IN_TICKET.placeholder("$OTHER_MENTION", member.getAsMention());
            replyEphemeralAndQueue(InteractionMessages.REMOVE_MEMBER_NOT_IN_TICKET);
        }
    }
}