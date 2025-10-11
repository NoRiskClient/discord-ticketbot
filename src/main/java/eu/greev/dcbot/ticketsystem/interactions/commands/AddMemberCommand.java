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

public class AddMemberCommand extends Interaction {
    public AddMemberCommand(@NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda) {
        super(config, ticketService, jda);
        addCommand("Add a User to this ticket",
                d -> d.addOption(OptionType.USER, "member", "The user adding to the current ticket", true));
    }

    @Override
    public String getIdentifier() {
        return "add";
    }

    @Override
    public void handleSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        Member member = event.getOption("member", OptionMapping::getAsMember);
        if(member == null) {
            replyEphemeralAndQueue(InteractionMessages.INTERACTION_INVALID_MEMBER);
            return;
        }

        if(!missingPermissions(null, member)) {
            InteractionMessages.ADD_MEMBER_STAFF.placeholder("$OTHER_MENTION", member.getAsMention());
            replyEphemeralAndQueue(InteractionMessages.ADD_MEMBER_STAFF);
            return;
        }

        if(this.ticket == null) return;
        if (ticketService.addUser(ticket, member.getUser())) {
            InteractionMessages.ADD_MEMBER_ADDED.placeholder("$OTHER_MENTION", member.getAsMention());
            replyAndQueue(InteractionMessages.ADD_MEMBER_ADDED);
        } else {
            InteractionMessages.ADD_MEMBER_ALREADY_ADDED.placeholder("$OTHER_MENTION", member.getAsMention());
            replyEphemeralAndQueue(InteractionMessages.ADD_MEMBER_ALREADY_ADDED);
        }
    }
}