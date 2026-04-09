package gg.norisk.ticketbot;

import java.util.Locale;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericSelectMenuInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class EventListener extends ListenerAdapter {
  private TicketService ticketService;

  @Override
  public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
    Main.handleInteraction(event.getComponentId(), event);
  }

  @Override
  public void onModalInteraction(@NotNull ModalInteractionEvent event) {
    Main.handleInteraction(event.getModalId(), event);
  }

  @Override
  public void onGenericSelectMenuInteraction(@NotNull GenericSelectMenuInteractionEvent event) {
    Main.handleInteraction(event.getComponentId() + " " + event.getValues().getFirst(), event);
  }

  @Override
  public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
    Main.handleInteraction(
        (event.getSubcommandGroup() == null ? "" : event.getSubcommandGroup() + " ")
            + event.getSubcommandName(),
        event);
  }

  @Override
  public void onMessageContextInteraction(@NotNull MessageContextInteractionEvent event) {
    Main.handleInteraction(event.getName().toLowerCase(Locale.ROOT), event);
  }

  @Override
  public void onMessageReceived(@NotNull MessageReceivedEvent event) {
    ticketService.handleMessageReceived(event);
  }
}
