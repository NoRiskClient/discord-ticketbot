package gg.norisk.ticketbot;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericSelectMenuInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class EventListener extends ListenerAdapter {
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
}
