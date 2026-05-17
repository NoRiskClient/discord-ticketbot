package eu.greev.dcbot.ticketsystem.interactions;

import eu.greev.dcbot.ticketsystem.TicketMenu;
import eu.greev.dcbot.ticketsystem.categories.ICategory;
import eu.greev.dcbot.ticketsystem.categories.TicketGroup;
import eu.greev.dcbot.utils.Config;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.Color;
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
        StringBuilder description = new StringBuilder();
        for (ICategory category : group.getCategories()) {
            boolean enabled = TicketMenu.isEnabled(category, event.getMember(), config);
            Button button = Button.primary("select-" + category.getId(), category.getLabel());
            if (!enabled) {
                button = button.withDisabled(true);
            }
            buttons.add(button);

            description.append(enabled ? "🎫 " : "🔒 ")
                    .append("**").append(category.getLabel()).append("**\n")
                    .append("> ").append(category.getDescription()).append("\n\n");
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setColor(Color.decode(config.getColor()))
                .setAuthor(group.getLabel(), null, config.getServerLogo())
                .setFooter(config.getServerName(), config.getServerLogo());

        if (buttons.isEmpty()) {
            embed.setDescription("There are currently no categories available in this group.");
            event.replyEmbeds(embed.build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        embed.setDescription("Pick the option that fits best — a private ticket will be created for you.\n\n"
                + description.toString().stripTrailing());

        event.replyEmbeds(embed.build())
                .addActionRow(buttons)
                .setEphemeral(true)
                .queue();
    }
}
