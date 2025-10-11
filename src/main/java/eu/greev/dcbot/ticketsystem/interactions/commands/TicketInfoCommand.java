package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.interactions.Interaction;
import eu.greev.dcbot.ticketsystem.interactions.InteractionMessages;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import eu.greev.dcbot.utils.CustomEmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public class TicketInfoCommand extends Interaction {
    public TicketInfoCommand(@NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda) {
        super(config, ticketService, jda);
        addCommand("Returns info about a ticket",
                d -> d.addOption(OptionType.INTEGER, "ticket-id", "The id of the ticket", true));
    }

    @Override
    public String getIdentifier() {
        return "info";
    }

    @Override
    public void handleSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        this.ticket = Optional.ofNullable(event.getOption("ticket-id", OptionMapping::getAsInt)).map(ticketService::getTicketByTicketId).orElse(null);
        if (ticket == null) {
            replyEphemeralAndQueue(InteractionMessages.TICKET_INFO_INVALID_ID);
            return;
        } else if (ticket.getTextChannel() != null && event.getGuild().getGuildChannelById(ticket.getTextChannel().getIdLong()) != null) {
            replyEphemeralAndQueue(InteractionMessages.TICKET_INFO_STILL_OPEN);
            return;
        }

        CustomEmbedBuilder info = new CustomEmbedBuilder(InteractionMessages.TICKET_INFO_INFO);
        info.placeholder("$TICKET_ID", String.valueOf(this.ticket.getId()));

        if (ticket.getOwner() != null)
            info.addField("Owner", ticket.getOwner().getAsMention(), false);

        if (ticket.getCloser() != null)
            info.addField("Closer", ticket.getCloser().getAsMention(), false);

        if (ticket.getSupporter() != null)
            info.addField("Supporter", ticket.getSupporter().getAsMention(), false);

        for (Map.Entry<String, String> entry : ticket.getInfo().entrySet()) {
            info.addField(entry.getKey(), entry.getValue(), false);
        }

        if (!ticket.getInvolved().isEmpty())
            info.addField("Involved", ticket.getInvolved().toString(), false);

        replyEphemeral(info)
                .setActionRow(Button.secondary("transcript", "Get transcript"))
                .queue();
    }
}