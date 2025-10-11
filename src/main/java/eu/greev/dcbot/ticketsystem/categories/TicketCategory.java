package eu.greev.dcbot.ticketsystem.categories;

import lombok.Getter;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalInteraction;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public enum TicketCategory {
    BUG("bug", "Bug", "Errors/Bugs in the launcher, app or game", (id, title) -> {
        TextInput problemInput = TextInput.create("problem", "Problem", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Please provide as much information about your problem as possible.")
                .setMaxLength(750)
                .setRequired(true)
                .build();

        TextInput versionInput = TextInput.create("version", "Version", TextInputStyle.SHORT)
                .setPlaceholder("Please provide the version of the launcher/app/game.")
                .setMaxLength(20)
                .setRequired(false)
                .build();

        TextInput logsInput = TextInput.create("logs", "Logs", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Provide logs/errors. Use mclo.gs link or upload file after ticket creation if too long.")
                .setMaxLength(50)
                .setRequired(false)
                .build();

        return Modal.create(id, title)
                .addActionRow(problemInput)
                .addActionRow(versionInput)
                .addActionRow(logsInput)
                .build();
    }, "Problem", "problem", "Version", "version", "Logs", "logs"), //TODO: Version und Logs sind hier optional, kp was da passiert

    CREATOR("creator", "Creator Application", "Apply for a creator code", (id, title) -> {
        TextInput nameInput = TextInput.create("name", "What is your in-game name?", TextInputStyle.SHORT)
                .setPlaceholder("Your in-game name")
                .setRequired(true)
                .setMaxLength(16)
                .build();

        TextInput linksInput = TextInput.create("links", "Provide links to your content (YouTube, etc.)", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Links to your content")
                .setRequired(true)
                .setMaxLength(200)
                .build();

        TextInput informationInput = TextInput.create("information", "Tell us more about yourself", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Additional information")
                .setRequired(false)
                .setMaxLength(700)
                .build();

        return Modal.create(id, title)
                .addActionRow(nameInput)
                .addActionRow(linksInput)
                .addActionRow(informationInput)
                .build();
    }, "In-Game Name", "name", "Content Links", "links", "Additional Information", "information"), //TODO: information ist hier optional kp was da passiert

    GENERAL("general", "General", "General questions regarding the client", (id, title) -> {
        TextInput reason = TextInput.create("reason", "Reason", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Please describe your issue or question in detail.")
                .setRequired(true)
                .setMaxLength(700)
                .build();

        return Modal.create(id, title)
                .addActionRow(reason)
                .build();
    }, "Reason", "reason"),

    PAYMENT("payment", "Payment", "Payment failed or items not received", (id, title) -> {
        TextInput nameInput = TextInput.create("name", "In-Game Name", TextInputStyle.SHORT)
                .setPlaceholder("Your in-game name")
                .setRequired(true)
                .setMaxLength(16)
                .build();

        TextInput problemInput = TextInput.create("problem", "Problem", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Describe your payment issue in detail.")
                .setRequired(true)
                .setMaxLength(700)
                .build();

        return Modal.create(id, title)
                .addActionRow(nameInput)
                .addActionRow(problemInput)
                .build();
    }, "In-Game Name", "name", "Problem", "problem"),

    REPORT("report", "Report", "Here you can report a user", (id, title) -> {
        TextInput user = TextInput.create("user", "User", TextInputStyle.SHORT)
                .setPlaceholder("Who do you want to report?")
                .setRequired(true)
                .setMaxLength(16)
                .build();

        TextInput reason = TextInput.create("reason", "Reason", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Why do you want to report this user?")
                .setRequired(true)
                .setMaxLength(700)
                .build();

        TextInput evidence = TextInput.create("evidence", "Evidence", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Provide any evidence (links, screenshots, etc.) to support your report.")
                .setRequired(true)
                .setMaxLength(250)
                .build();

        return Modal.create(id, title)
                .addActionRow(user)
                .addActionRow(reason)
                .addActionRow(evidence)
                .build();
    }, "User", "user", "Reason", "reason", "Evidence", "evidence"),

    SECURITY("security", "Security Report", "Here you can report security vulnerabilities", (id, title) -> {
        TextInput vulnerability = TextInput.create("vulnerability", "Vulnerability", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Describe the security vulnerability you have discovered.")
                .setRequired(true)
                .setMaxLength(800)
                .build();

        return Modal.create(id, title)
                .addActionRow(vulnerability)
                .build();
    }, "Vulnerability", "vulnerability");

    @Getter private final String id;
    @Getter private final String label;
    @Getter private final String description;
    @Getter private final Modal modal;
    @Getter private final Function<ModalInteraction, Map<String, String>> infoGetter;

    TicketCategory(String id, String label, String description, BiFunction<String, String, Modal> modal, String... ticketDataFields) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.modal = modal.apply(id, "Ticket: " + label);

        if(ticketDataFields.length % 2 != 0) {
            throw new IllegalArgumentException("Every Ticket Data Field needs a Key and a Display value.");
        }

        for(int i = 0; i < ticketDataFields.length; i += 2) {
            if(!ticketDataFields[i+1].matches("[a-z]+")) {
                throw new IllegalArgumentException("Ticket Data Field Key has an invalid value: " + ticketDataFields[i+1]);
            }
        }

        this.infoGetter = e -> {
            HashMap<String, String> map = new LinkedHashMap<>();
            for(int i = 0; i<ticketDataFields.length; i+=2) {
                String display = ticketDataFields[i];
                String key = ticketDataFields[i+1];
                String value = Optional.ofNullable(e.getValue(key)).map(ModalMapping::getAsString).orElse("N/A");
                map.put(display, value);
            }
            return map;
        };
    }
}
