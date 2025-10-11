package eu.greev.dcbot.ticketsystem.interactions.buttons;

import eu.greev.dcbot.ticketsystem.interactions.Interaction;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

public class TicketConfirmButtonInteraction extends Interaction {
    private static final Modal MESSAGE_MODAL = Modal.create("ticket-confirm-message-modal", "Closing Message")
            .addActionRow(
                    TextInput.create("message", "Message", TextInputStyle.SHORT)
                            .setPlaceholder("What closing message do you want to leave?")
                            .setRequired(true)
                            .setMaxLength(100)
                            .build()
            )
            .build();

    public TicketConfirmButtonInteraction(@NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda) {
        super(config, ticketService, jda);
        this.permissionsRequired = false;
        this.ticketChannelRequired = false;
    }

    @Override
    public String getIdentifier() {
        return "ticket-confirm-message";
    }

    @Override
    public void handleButtonInteraction(@NotNull ButtonInteractionEvent event) {
        event.replyModal(MESSAGE_MODAL).queue();
    }
}
