package gg.norisk.ticketbot.interaction.selections;

import gg.norisk.ticketbot.Config;
import gg.norisk.ticketbot.TicketCategory;
import gg.norisk.ticketbot.TicketService;
import gg.norisk.ticketbot.interaction.ArgumentedInteraction;
import java.util.Arrays;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import org.jetbrains.annotations.NotNull;

public class CategorySelectionInteraction extends ArgumentedInteraction {
  public CategorySelectionInteraction(
      @NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda) {
    super(config, ticketService, jda);
    this.permissionsRequired = false;
    this.ticketChannelRequired = false;
  }

  @Override
  public String getIdentifier() {
    return "select-category";
  }

  @Override
  public void handleStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
    if (arguments.length == 1) {
      Arrays.stream(TicketCategory.values())
          .filter(c -> c.getId().equals(arguments[0]))
          .findFirst()
          .ifPresent(category -> event.replyModal(category.getModal()).queue());
    }
  }
}
