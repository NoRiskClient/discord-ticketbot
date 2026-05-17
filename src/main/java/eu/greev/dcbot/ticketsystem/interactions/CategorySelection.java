package eu.greev.dcbot.ticketsystem.interactions;

import eu.greev.dcbot.ticketsystem.TicketMenu;
import eu.greev.dcbot.ticketsystem.categories.ICategory;
import eu.greev.dcbot.utils.Config;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

@AllArgsConstructor
public class CategorySelection implements Interaction {
    private final ICategory category;
    private final Config config;

    @Override
    public void execute(Event evt) {
        ButtonInteractionEvent event = (ButtonInteractionEvent) evt;

        if (!TicketMenu.isEnabled(category, event.getMember(), config)) {
            event.reply("You don't have permission to open this category.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.replyModal(category.getModal()).queue();
    }
}
