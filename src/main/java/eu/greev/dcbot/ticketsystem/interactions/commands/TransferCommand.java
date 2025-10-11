package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.interactions.Interaction;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Instant;

public class TransferCommand extends Interaction {
    public TransferCommand(@NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda) {
        super(config, ticketService, jda);
        addCommand("Sets the new supporter",
                d -> d.addOption(OptionType.USER, "staff", "The staff member who should be the supporter", true));
    }

    @Override
    public String getIdentifier() {
        return "transfer";
    }

    @Override
    public void handleSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        Ticket ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());
        EmbedBuilder error = new EmbedBuilder()
                .setColor(Color.RED)
                .setFooter(config.getServerName(), config.getServerLogo());

        if (ticket.getSupporter() == null) {
            event.replyEmbeds(error.setDescription("You can not transfer a ticket which wasn't claimed!").build())
                    .setEphemeral(true)
                    .queue();
            return;
        }
         if (!ticket.getSupporter().equals(event.getUser()) && !event.getMember().getPermissions().contains(Permission.ADMINISTRATOR)) {
             event.replyEmbeds(error.setDescription("You can not transfer this ticket since you don't handle it or you don't have enough permissions!").build())
                     .setEphemeral(true)
                     .queue();
             return;
         }

        Member sup = event.getOption("staff").getAsMember();
        if (sup.getRoles().contains(jda.getRoleById(config.getStaffId())) || !sup.getUser().equals(ticket.getSupporter())) {
            ticket.setSupporter(sup.getUser());
            ticket.getTextChannel().getManager().setName(ticketService.generateChannelName(ticket)).queue();
            EmbedBuilder builder = new EmbedBuilder().setFooter(config.getServerName(), config.getServerLogo())
                    .setColor(Color.decode(config.getColor()))
                    .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                    .addField("✅ **New supporter**", sup.getAsMention() + " is the new supporter", false);

            ticket.getTranscript().addLogMessage("Ticket got transferred to [" + sup.getUser().getName() + "].", Instant.now().getEpochSecond(), ticket.getId());
            event.replyEmbeds(builder.build()).queue();
            return;
        }

        event.replyEmbeds(new EmbedBuilder().setFooter(config.getServerName(), config.getServerLogo())
                .setColor(Color.RED)
                .addField("❌ **Setting new supporter failed**", "This member is either already the supporter or not a staff member", false).build()).setEphemeral(true).queue();
    }
}