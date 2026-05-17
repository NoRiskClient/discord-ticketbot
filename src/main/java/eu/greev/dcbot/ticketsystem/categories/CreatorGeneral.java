package eu.greev.dcbot.ticketsystem.categories;

import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.util.LinkedHashMap;
import java.util.Map;

public class CreatorGeneral implements ICategory {
    @Override
    public String getId() {
        return "creator-general";
    }

    @Override
    public String getLabel() {
        return "Creator General";
    }

    @Override
    public String getDescription() {
        return "General questions for creators";
    }

    @Override
    public Modal getModal() {
        TextInput message = TextInput.create("message", "Message", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Describe your request or question in detail.")
                .setRequired(true)
                .setMaxLength(700)
                .build();

        return Modal.create(getId(), getModalTitle())
                .addActionRow(message)
                .build();
    }

    @Override
    public Map<String, String> getInfo(ModalInteractionEvent event) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("Message", event.getValue("message").getAsString());
        return map;
    }

    @Override
    public Long getRequiredRoleId(Config config) {
        return config.getCreatorRole();
    }

    @Override
    public boolean isSensitive() {
        return true;
    }
}
