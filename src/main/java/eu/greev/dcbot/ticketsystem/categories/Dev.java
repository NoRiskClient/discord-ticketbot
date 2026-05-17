package eu.greev.dcbot.ticketsystem.categories;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.util.LinkedHashMap;
import java.util.Map;

public class Dev implements ICategory {
    @Override
    public String getId() {
        return "dev";
    }

    @Override
    public String getLabel() {
        return "Developer";
    }

    @Override
    public String getDescription() {
        return "Build features and shape the project with your code";
    }

    @Override
    public Modal getModal() {
        TextInput name = TextInput.create("name", "In-Game Name", TextInputStyle.SHORT)
                .setPlaceholder("Your in-game name")
                .setRequired(true)
                .setMaxLength(16)
                .build();

        TextInput tech = TextInput.create("tech", "Tech Stack", TextInputStyle.SHORT)
                .setPlaceholder("e.g. Kotlin, Fabric, Ktor, MongoDB")
                .setRequired(true)
                .setMaxLength(50)
                .build();

        TextInput experience = TextInput.create("experience", "Experience", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Describe your development experience.")
                .setRequired(true)
                .setMaxLength(700)
                .build();

        TextInput samples = TextInput.create("samples", "Code samples / GitHub", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Links to code samples or GitHub (optional)")
                .setRequired(false)
                .setMaxLength(300)
                .build();

        return Modal.create(getId(), getModalTitle())
                .addActionRow(name)
                .addActionRow(tech)
                .addActionRow(experience)
                .addActionRow(samples)
                .build();
    }

    @Override
    public Map<String, String> getInfo(ModalInteractionEvent event) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("In-Game Name", event.getValue("name").getAsString());
        map.put("Tech Stack", event.getValue("tech").getAsString());
        map.put("Experience", event.getValue("experience").getAsString());
        map.put("Code samples / GitHub", event.getValue("samples") != null && !event.getValue("samples").getAsString().isEmpty()
                ? event.getValue("samples").getAsString() : "N/A");
        return map;
    }

    @Override
    public boolean isSensitive() {
        return true;
    }

    @Override
    public boolean isApplication() {
        return true;
    }
}
