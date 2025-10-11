package eu.greev.dcbot.ticketsystem.interactions;

import eu.greev.dcbot.ticketsystem.categories.TicketCategory;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class CategorySelectionInteraction extends ArgumentedInteraction {
    public CategorySelectionInteraction(@NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda) {
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
        if(arguments.length == 1) {
            TicketCategory category = Arrays.stream(TicketCategory.values())
                    .filter(c -> c.getId().equals(arguments[0])).findFirst().orElse(null);
            if(category != null) event.replyModal(category.getModal()).queue();
        }
    }
}