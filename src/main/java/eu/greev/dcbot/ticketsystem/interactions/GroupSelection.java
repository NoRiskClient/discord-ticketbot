package eu.greev.dcbot.ticketsystem.interactions;

import eu.greev.dcbot.ticketsystem.TicketMenu;
import eu.greev.dcbot.ticketsystem.categories.ICategory;
import eu.greev.dcbot.ticketsystem.categories.TicketGroup;
import eu.greev.dcbot.utils.Config;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class GroupSelection implements Interaction {
    private final TicketGroup group;
    private final Config config;

    @Override
    public void execute(Event evt) {
        ButtonInteractionEvent event = (ButtonInteractionEvent) evt;

        List<Button> buttons = new ArrayList<>();
        for (ICategory category : group.getCategories()) {
            Button button = Button.primary("select-" + category.getId(), category.getLabel());
            if (!TicketMenu.isEnabled(category, event.getMember(), config)) {
                button = button.withDisabled(true);
            }
            buttons.add(button);
        }

        event.reply("**" + group.getLabel() + "** — choose a category:")
                .addActionRow(buttons)
                .setEphemeral(true)
                .queue();
    }
}
