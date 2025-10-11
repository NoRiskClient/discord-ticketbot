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

public class SetOwnerCommand extends Interaction {
    public SetOwnerCommand(@NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda) {
        super(config, ticketService, jda);
        addCommand("Set the new owner of the ticket",
                d -> d.addOption(OptionType.USER, "member", "The new owner"));
    }

    @Override
    public String getIdentifier() {
        return "set-owner";
    }

    @Override
    public void handleSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        Member member = event.getOption("member", OptionMapping::getAsMember);
        if(member == null) {
            replyEphemeralAndQueue(InteractionMessages.INTERACTION_INVALID_MEMBER);
            return;
        }

        if(ticket == null) return;
        if (!member.getUser().equals(ticket.getOwner())) {
            if (ticketService.setOwner(ticket, member)) {
                InteractionMessages.SET_OWNER_UPDATED.placeholder("$OTHER_MENTION", member.getAsMention());
                replyAndQueue(InteractionMessages.SET_OWNER_UPDATED);
            } else {
                InteractionMessages.SET_OWNER_NO_ACCESS.placeholder("$OTHER_MENTION", member.getAsMention());
                replyEphemeralAndQueue(InteractionMessages.SET_OWNER_NO_ACCESS);
            }
        } else {
            InteractionMessages.SET_OWNER_ALREADY_OWNER.placeholder("$OTHER_MENTION", member.getAsMention());
            replyEphemeralAndQueue(InteractionMessages.SET_OWNER_ALREADY_OWNER);
        }
    }
}