package eu.greev.dcbot.ticketsystem.interactions.modals;

import eu.greev.dcbot.ticketsystem.categories.ICategory;
import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.interactions.Interaction;
import eu.greev.dcbot.ticketsystem.service.TicketData;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
public class TicketModal implements Interaction {
    private final ICategory category;
    private final Config config;
    private final TicketService ticketService;
    private final TicketData ticketData;
    private final List<String> discordFormattingChars = Arrays.asList("\\", "*", "~", "|", "_", "`");

    @Override
    public void execute(Event evt) {
        ModalInteractionEvent event = (ModalInteractionEvent) evt;
        event.deferReply(true).queue();
        if (config.getServerName() == null) {
            EmbedBuilder error = new EmbedBuilder()
                    .setColor(Color.RED)
                    .setDescription("❌ **Ticketsystem wasn't setup, please tell an Admin to use </ticket setup:0>!**");
            event.getHook().sendMessageEmbeds(error.build()).setEphemeral(true).queue();
            return;
        }
        EmbedBuilder builder = new EmbedBuilder().setColor(Color.RED)
                .setFooter(config.getServerName(), config.getServerLogo());

        Map<String, String> info = category.getInfo(event);

        info.replaceAll((k, v) -> escapeFormatting(v));

        // Validation for CrashReport: check mclo.gs link
        if (category.getId().equals("crashreport")) {
            String logsUrl = event.getValue("mclogs").getAsString();
            if (!logsUrl.contains("mclo.gs")) {
                EmbedBuilder errorEmbed = new EmbedBuilder()
                        .setColor(Color.RED)
                        .setTitle("Invalid Link")
                        .setDescription("""
                                Please provide a valid **mclo.gs** link!\n\n
                                "**How to create one:**\n
                                "1. Go to [mclo.gs](https://mclo.gs)\n
                                "2. Upload your log file\n
                                "3. Copy the link""")
                        .setFooter(config.getServerName(), config.getServerLogo());
                event.getHook().sendMessageEmbeds(errorEmbed.build()).setEphemeral(true).queue();
                return;
            }
        }

        Optional<String> error = ticketService.createNewTicket(info, category, event.getUser());

        if (error.isPresent()) {
            builder.addField("❌ **Creating ticket failed**", error.get(), false);
            event.getHook().sendMessageEmbeds(builder.build()).setEphemeral(true).queue();
            return;
        }

        Ticket ticket = ticketService.getTicketByTicketId(ticketData.getLastTicketId());
        builder.setAuthor(event.getMember().getEffectiveName(), null, event.getMember().getEffectiveAvatarUrl())
                .setColor(Color.decode(config.getColor()))
                .addField("✅ **Ticket created**", "Successfully created a ticket for you " + ticket.getTextChannel().getAsMention(), false);
        event.getHook().sendMessageEmbeds(builder.build()).setEphemeral(true).queue();

        ticket.getTranscript().addInfoMessage("Category", category.getLabel(), ticket.getId());
        for(Map.Entry<String, String> entry : info.entrySet()) {
            ticket.getTranscript().addInfoMessage(entry.getKey().replace(" ", "-"), entry.getValue(), ticket.getId());
        }
    }

    private String escapeFormatting(String text) {
        for (String formatString : this.discordFormattingChars) {
            text = text.replace(formatString, "\\" + formatString);
        }
        return text;
    }
}
