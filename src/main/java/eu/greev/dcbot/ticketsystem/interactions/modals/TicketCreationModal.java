package eu.greev.dcbot.ticketsystem.interactions.modals;

import eu.greev.dcbot.ticketsystem.categories.TicketCategory;
import eu.greev.dcbot.ticketsystem.interactions.Interaction;
import eu.greev.dcbot.ticketsystem.interactions.InteractionMessages;
import eu.greev.dcbot.ticketsystem.service.TicketData;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TicketCreationModal extends Interaction {
    private final TicketCategory category;
    private final TicketData ticketData;
    private final List<String> discordFormattingChars = Arrays.asList("\\", "*", "~", "|", "_", "`");

    public TicketCreationModal(@NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda, @NotNull TicketCategory category, @NotNull TicketData ticketData) {
        super(config, ticketService, jda);
        this.category = category;
        this.ticketData = ticketData;
        this.ticketChannelRequired = false;
    }

    @Override
    public String getIdentifier() {
        return category.getId();
    }

    @Override
    public void handleModalInteraction(@NotNull ModalInteractionEvent event) {
        deferEphemeralAndQueue(() -> {
            Map<String, String> info = category.getInfoGetter().apply(event);

            info.replaceAll((k, v) -> escapeFormatting(v));

            Optional<String> error = ticketService.createNewTicket(info, category, event.getUser());

            if (error.isEmpty()) {
                //TODO: das sollte unbedingt NICHT das last ticket sein sondern eine return value von create new ticket???
                this.ticket = ticketService.getTicketByTicketId(ticketData.getLastTicketId());
                InteractionMessages.TICKET_CREATION_CREATED.placeholder("$CHANNEL", this.ticket.getTextChannel().getAsMention());
                return InteractionMessages.TICKET_CREATION_CREATED;
            } else {
                InteractionMessages.TICKET_CREATION_FAILED.placeholder("%ERROR", error.get());
                return InteractionMessages.TICKET_CREATION_FAILED;
            }
        });
    }

    private String escapeFormatting(String text) {
        for (String formatString : this.discordFormattingChars) {
            text = text.replace(formatString, "\\" + formatString);
        }
        return text;
    }
}
