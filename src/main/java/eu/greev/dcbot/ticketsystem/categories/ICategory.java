package eu.greev.dcbot.ticketsystem.categories;

import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.util.Map;

public interface ICategory {
    String getId();
    String getLabel();
    String getDescription();
    Modal getModal();
    Map<String, String> getInfo(ModalInteractionEvent event);

    default String getModalTitle() {
        return "Ticket: " + getLabel();
    }

    default boolean isSensitive() {
        return false;
    }

    /**
     * Role id required to open this category.
     * {@code null} = no gate. A non-null value (including 0) means gated;
     * 0 is treated as "disabled for everyone" by {@code TicketMenu.isEnabled}.
     *
     * @param config server configuration; overriding implementations may resolve a configured role id from it
     */
    default Long getRequiredRoleId(Config config) {
        return null;
    }
}
