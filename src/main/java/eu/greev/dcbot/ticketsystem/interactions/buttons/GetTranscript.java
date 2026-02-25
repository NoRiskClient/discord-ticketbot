package eu.greev.dcbot.ticketsystem.interactions.buttons;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.*;

@AllArgsConstructor
public class GetTranscript extends AbstractButton {
    private final Config config;
    private final TicketService ticketService;

    @Override
    public void execute(Event evt) {
        ButtonInteractionEvent event = (ButtonInteractionEvent) evt;
        if (config.getServerName() == null) {
            EmbedBuilder error = new EmbedBuilder()
                    .setColor(Color.RED)
                    .setDescription("❌ **Ticketsystem wasn't setup, please tell an Admin to use </ticket setup:0>!**");
            event.replyEmbeds(error.build()).setEphemeral(true).queue();
            return;
        }
        int ticketID = Integer.parseInt(event.getMessage().getEmbeds().getFirst().getTitle().replace("Ticket #", ""));
        Ticket ticket = ticketService.getTicketByTicketId(ticketID);

        if (ticket == null) {
            EmbedBuilder error = new EmbedBuilder()
                    .setColor(Color.RED)
                    .setDescription("❌ **Something went wrong, please report this to the Bot creator!**");
            event.replyEmbeds(error.build()).setEphemeral(true).queue();
            return;
        }

        boolean isSensitive = ticket.getCategory().isSensitive();
        boolean isTicketOwner = event.getMember() != null && ticket.getOwner().getId().equals(event.getMember().getUser().getId()); // getMember can be null when user left the server
        boolean isPrivilegedSupporter = ticketService.isUserPrivilegedSupporter(event.getMember());
        if (isSensitive && !(isTicketOwner || isPrivilegedSupporter)) {
            EmbedBuilder error = new EmbedBuilder()
                    .setColor(Color.RED)
                    .setDescription("❌ **This ticket category is marked as sensitive, transcript cannot be viewed by you!**");
            event.replyEmbeds(error.build()).setEphemeral(true).queue();
            return;
        }

        event.getUser().openPrivateChannel()
                .flatMap(channel -> channel.sendFiles(FileUpload.fromData(ticket.getTranscript().toFile(ticketID))))
                .queue();

        EmbedBuilder builder = new EmbedBuilder()
                .setFooter(config.getServerName(), config.getServerLogo())
                .setAuthor(event.getMember() != null ? event.getMember().getEffectiveName() : "Unknown", null, event.getMember().getEffectiveAvatarUrl())
                .setColor(Color.decode(config.getColor()))
                .setDescription("Sent transcript of Ticket #" + ticketID + " via DM");
        event.replyEmbeds(builder.build()).setEphemeral(true).queue();
    }
}