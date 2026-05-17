package eu.greev.dcbot.ticketsystem.categories;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.util.LinkedHashMap;
import java.util.Map;

public class Designer implements ICategory {
    @Override
    public String getId() {
        return "designer";
    }

    @Override
    public String getLabel() {
        return "Designer";
    }

    @Override
    public String getDescription() {
        return "Shape our visuals — banners, builds & creative work";
    }

    @Override
    public Modal getModal() {
        TextInput name = TextInput.create("name", "In-Game Name", TextInputStyle.SHORT)
                .setPlaceholder("Your in-game name")
                .setRequired(true)
                .setMaxLength(16)
                .build();

        TextInput portfolio = TextInput.create("portfolio", "Portfolio Links", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Links to your work (optional)")
                .setRequired(false)
                .setMaxLength(300)
                .build();

        TextInput tools = TextInput.create("tools", "Tools/Software", TextInputStyle.SHORT)
                .setPlaceholder("e.g. Photoshop, Blender, Figma")
                .setRequired(true)
                .setMaxLength(50)
                .build();

        TextInput motivation = TextInput.create("motivation", "Motivation", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Why do you want to be a Designer?")
                .setRequired(true)
                .setMaxLength(700)
                .build();

        return Modal.create(getId(), getModalTitle())
                .addActionRow(name)
                .addActionRow(portfolio)
                .addActionRow(tools)
                .addActionRow(motivation)
                .build();
    }

    @Override
    public Map<String, String> getInfo(ModalInteractionEvent event) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("In-Game Name", event.getValue("name").getAsString());
        map.put("Portfolio Links", event.getValue("portfolio") != null && !event.getValue("portfolio").getAsString().isEmpty()
                ? event.getValue("portfolio").getAsString() : "N/A");
        map.put("Tools/Software", event.getValue("tools").getAsString());
        map.put("Motivation", event.getValue("motivation").getAsString());
        return map;
    }

    @Override
    public boolean isSensitive() {
        return true;
    }
}
