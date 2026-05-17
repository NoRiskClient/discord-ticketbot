package eu.greev.dcbot.ticketsystem.categories;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.util.LinkedHashMap;
import java.util.Map;

public class Helper implements ICategory {
    @Override
    public String getId() {
        return "helper";
    }

    @Override
    public String getLabel() {
        return "Helper";
    }

    @Override
    public String getDescription() {
        return "Support our community and help players";
    }

    @Override
    public Modal getModal() {
        TextInput name = TextInput.create("name", "In-Game Name", TextInputStyle.SHORT)
                .setPlaceholder("Your in-game name")
                .setRequired(true)
                .setMaxLength(16)
                .build();

        TextInput age = TextInput.create("age", "Age", TextInputStyle.SHORT)
                .setPlaceholder("Your age")
                .setRequired(true)
                .setMaxLength(3)
                .build();

        TextInput availability = TextInput.create("availability", "Timezone/Availability", TextInputStyle.SHORT)
                .setPlaceholder("e.g. CET, evenings & weekends")
                .setRequired(true)
                .setMaxLength(50)
                .build();

        TextInput why = TextInput.create("why", "Why you?", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Why should we pick you as a Helper?")
                .setRequired(true)
                .setMaxLength(700)
                .build();

        return Modal.create(getId(), getModalTitle())
                .addActionRow(name)
                .addActionRow(age)
                .addActionRow(availability)
                .addActionRow(why)
                .build();
    }

    @Override
    public Map<String, String> getInfo(ModalInteractionEvent event) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("In-Game Name", event.getValue("name").getAsString());
        map.put("Age", event.getValue("age").getAsString());
        map.put("Timezone/Availability", event.getValue("availability").getAsString());
        map.put("Why you?", event.getValue("why").getAsString());
        return map;
    }

    @Override
    public boolean isSensitive() {
        return true;
    }
}
