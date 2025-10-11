package eu.greev.dcbot.ticketsystem.interactions.argumented;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.interactions.ArgumentedInteraction;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import eu.greev.dcbot.utils.CustomEmbedBuilder;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Slf4j
public class CloseTicketInteraction extends ArgumentedInteraction {
    private static final CustomEmbedBuilder CONFIRMATION = new CustomEmbedBuilder()
                .addField("Close Confirmation", "Do you really want to close this ticket?", true);

    public CloseTicketInteraction(@NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda) {
        super(config, ticketService, jda);
        addCommand("Close this ticket");
    }

    @Override
    public String getIdentifier() {
        return "close";
    }

    @Override
    public void handleButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if(arguments.length == 0) {
            replyCloseOptions(event);
        } else if(arguments.length == 1) {
            switch(arguments[0]) {
                case "instant" -> closeTicket(event, null);
                case "message" -> replySetCloseMessage(event);
            }
        }
    }

    @Override
    public void handleModalInteraction(@NotNull ModalInteractionEvent event) {
        closeTicket(event, Optional.ofNullable(event.getValue("message")).map(ModalMapping::getAsString).orElse(null));
        event.deferEdit().queue();
    }

    @Override
    public void handleSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        replyCloseOptions(event);
    }

    private void replyCloseOptions(@NotNull IReplyCallback reply) {
        replyEphemeral(CONFIRMATION)
                .addActionRow(Button.primary("close instant", "✔️ Close"))
                .addActionRow(Button.primary("close message", "✔️ Close with message"))
                .queue();
    }

    private void replySetCloseMessage(@NotNull GenericComponentInteractionCreateEvent event) {
        TextInput message = TextInput.create("message", "Message", TextInputStyle.SHORT)
                .setPlaceholder("What closing message do you want to leave?")
                .setRequired(true)
                .setMaxLength(100)
                .build();

        Modal modal = Modal.create("close", "Closing Message")
                .addActionRow(message)
                .build();

        event.replyModal(modal).queue();
    }

    private void closeTicket(@NotNull IReplyCallback event, @Nullable String message) {
        Ticket ticket = ticketService.getTicketByChannel(event.getChannel());
        if(ticket == null) return;
        ticketService.closeTicket(ticket, false, event.getUser(), message);
    }
}