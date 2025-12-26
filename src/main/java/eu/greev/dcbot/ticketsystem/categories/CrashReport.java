package eu.greev.dcbot.ticketsystem.categories;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.util.LinkedHashMap;
import java.util.Map;

public class CrashReport implements ICategory {
    @Override
    public String getId() {
        return "crashreport";
    }

    @Override
    public String getLabel() {
        return "Crash Report";
    }

    @Override
    public String getDescription() {
        return "Game crashes - requires mc.logs link";
    }

    @Override
    public Modal getModal() {
        TextInput logsInput = TextInput.create("mclogs", "mc.logs Link", TextInputStyle.SHORT)
                .setPlaceholder("https://mclo.gs/xxxxxxx")
                .setMaxLength(100)
                .setRequired(true)
                .build();

        TextInput descInput = TextInput.create("description", "Description", TextInputStyle.PARAGRAPH)
                .setPlaceholder("What happened? When does the game crash?")
                .setMaxLength(500)
                .setRequired(true)
                .build();

        return Modal.create(getId(), getModalTitle())
                .addActionRow(logsInput)
                .addActionRow(descInput)
                .build();
    }

    @Override
    public Map<String, String> getInfo(ModalInteractionEvent event) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("mc.logs", event.getValue("mclogs").getAsString());
        map.put("Description", event.getValue("description").getAsString());
        return map;
    }
}
