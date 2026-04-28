package eu.greev.dcbot.ticketsystem.interactions;

import eu.greev.dcbot.Main;
import eu.greev.dcbot.ticketsystem.categories.ICategory;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

@AllArgsConstructor
public class CategorySelection implements Interaction {
    private ICategory category;

    @Override
    public void execute(Event evt) {
        StringSelectInteractionEvent event = (StringSelectInteractionEvent) evt;
        event.replyModal(category.getModal()).queue();

        // Reset dropdown so user can select the same option again
        StringSelectMenu.Builder selectionBuilder = StringSelectMenu.create("ticket-create-topic")
                .setPlaceholder("Select your ticket topic");

        for (ICategory cat : Main.CATEGORIES) {
            selectionBuilder.addOption(cat.getLabel(), "select-" + cat.getId(), cat.getDescription());
        }

        event.getMessage().editMessageComponents(
                ActionRow.of(selectionBuilder.build())
        ).queue();
    }
}