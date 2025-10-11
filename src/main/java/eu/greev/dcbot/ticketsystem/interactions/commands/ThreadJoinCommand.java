package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.interactions.Interaction;
import eu.greev.dcbot.ticketsystem.interactions.InteractionMessages;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ThreadJoinCommand extends Interaction {
    public ThreadJoinCommand(@NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda) {
        super(config, ticketService, jda);
        addCommand("join", "Join the ticket thread", THREADS_GROUP, d -> d);
    }

    @Override
    public String getIdentifier() {
        return "thread join";
    }

    @Override
    public void handleSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        Ticket ticket = ticketService.getTicketByChannelId(event.getGuildChannel().asTextChannel().getIdLong());
        ThreadChannel thread = ticket.getThreadChannel();

        if (thread.getMembers().contains(event.getMember())) {
            replyEphemeralAndQueue(InteractionMessages.THREAD_JOIN_ALREADY_JOINED);
            return;
        }

        thread.addThreadMember(Objects.requireNonNull(event.getMember())).queue();

        replyEphemeralAndQueue(InteractionMessages.THREAD_JOIN_JOINED);
    }
}