# Ticket Category Groups Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the ticket-creation dropdown with a two-level button flow (super-category buttons → ephemeral category buttons → modal), add 4 new categories, and gate "Creator General" behind a configurable role.

**Architecture:** Hardcoded `TicketGroup` list in `Main.java`. A persistent base message shows super-category buttons (`group-<id>`). Clicking one replies ephemerally with that group's category buttons (`select-<id>`). Clicking a category button opens the existing modal. A shared `TicketMenu` helper builds the base message and the role-gate check.

**Tech Stack:** Java 17, JDA 5.6.1, Lombok, Gradle.

**Testing note:** The repo has no test framework (`src/test` is empty) and JDA interaction testing is impractical. Per the approved spec, verification is `./gradlew build` (compile) plus a manual Discord checklist (Task 10). Each task ends with a compile check and a commit so every commit builds.

> Windows: use `.\gradlew.bat build` (PowerShell) instead of `./gradlew build`.

---

## File Structure

**Create:**
- `src/main/java/eu/greev/dcbot/ticketsystem/categories/TicketGroup.java` — group model (id, label, categories)
- `src/main/java/eu/greev/dcbot/ticketsystem/categories/CreatorGeneral.java` — new category, role-gated
- `src/main/java/eu/greev/dcbot/ticketsystem/categories/Helper.java` — new category
- `src/main/java/eu/greev/dcbot/ticketsystem/categories/Designer.java` — new category
- `src/main/java/eu/greev/dcbot/ticketsystem/categories/Dev.java` — new category
- `src/main/java/eu/greev/dcbot/ticketsystem/TicketMenu.java` — base message builder + role-gate helper
- `src/main/java/eu/greev/dcbot/ticketsystem/interactions/GroupSelection.java` — super-category button handler

**Modify:**
- `src/main/java/eu/greev/dcbot/utils/Config.java` — add `creatorRole`
- `config.example.yml` — document `creatorRole`
- `src/main/java/eu/greev/dcbot/ticketsystem/categories/ICategory.java` — add `getRequiredRoleId`
- `src/main/java/eu/greev/dcbot/ticketsystem/categories/Bug.java` — label `Bug` → `Bug Report`
- `src/main/java/eu/greev/dcbot/Main.java` — `GROUPS` field, instantiate/register categories, register groups
- `src/main/java/eu/greev/dcbot/ticketsystem/interactions/CategorySelection.java` — button-based + gate re-check
- `src/main/java/eu/greev/dcbot/ticketsystem/interactions/commands/Setup.java` — use `TicketMenu`
- `src/main/java/eu/greev/dcbot/ticketsystem/TicketListener.java` — drop select handler, use `TicketMenu`, null-guard buttons

---

### Task 1: Add `creatorRole` config

**Files:**
- Modify: `src/main/java/eu/greev/dcbot/utils/Config.java`
- Modify: `config.example.yml`

- [ ] **Step 1: Add the field to `Config.java`**

In `Config.java`, find:

```java
    private long pendingRatingCategory = 0;
```

Add directly below it:

```java
    private long creatorRole = 0;
```

- [ ] **Step 2: Document it in `config.example.yml`**

In `config.example.yml`, find:

```yaml
# Category ID where tickets pending rating are moved (0 = disabled)
pendingRatingCategory: 0
```

Add directly below it (keep one blank line before the next block):

```yaml

# Role ID required to open the "Creator General" category (0 = disabled for everyone)
creatorRole: 0
```

- [ ] **Step 3: Compile**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/java/eu/greev/dcbot/utils/Config.java config.example.yml
git commit -m "feat: add creatorRole config option"
```

---

### Task 2: Add role-gate hook to `ICategory`

**Files:**
- Modify: `src/main/java/eu/greev/dcbot/ticketsystem/categories/ICategory.java`

- [ ] **Step 1: Add import and default method**

Replace the entire file with:

```java
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
     */
    default Long getRequiredRoleId(Config config) {
        return null;
    }
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/java/eu/greev/dcbot/ticketsystem/categories/ICategory.java
git commit -m "feat: add getRequiredRoleId hook to ICategory"
```

---

### Task 3: New category classes + `Bug` label

**Files:**
- Create: `src/main/java/eu/greev/dcbot/ticketsystem/categories/CreatorGeneral.java`
- Create: `src/main/java/eu/greev/dcbot/ticketsystem/categories/Helper.java`
- Create: `src/main/java/eu/greev/dcbot/ticketsystem/categories/Designer.java`
- Create: `src/main/java/eu/greev/dcbot/ticketsystem/categories/Dev.java`
- Modify: `src/main/java/eu/greev/dcbot/ticketsystem/categories/Bug.java`

- [ ] **Step 1: Create `CreatorGeneral.java`**

```java
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
```

- [ ] **Step 2: Create `Helper.java`**

```java
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
        return "Apply to become a Helper";
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
```

- [ ] **Step 3: Create `Designer.java`**

```java
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
        return "Apply to become a Designer";
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
```

- [ ] **Step 4: Create `Dev.java`**

```java
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
        return "Dev";
    }

    @Override
    public String getDescription() {
        return "Apply to become a Developer";
    }

    @Override
    public Modal getModal() {
        TextInput name = TextInput.create("name", "In-Game Name", TextInputStyle.SHORT)
                .setPlaceholder("Your in-game name")
                .setRequired(true)
                .setMaxLength(16)
                .build();

        TextInput tech = TextInput.create("tech", "Languages/Tech", TextInputStyle.SHORT)
                .setPlaceholder("e.g. Java, Kotlin, TypeScript")
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
        map.put("Languages/Tech", event.getValue("tech").getAsString());
        map.put("Experience", event.getValue("experience").getAsString());
        map.put("Code samples / GitHub", event.getValue("samples") != null && !event.getValue("samples").getAsString().isEmpty()
                ? event.getValue("samples").getAsString() : "N/A");
        return map;
    }

    @Override
    public boolean isSensitive() {
        return true;
    }
}
```

- [ ] **Step 5: Change `Bug` label**

In `Bug.java`, find:

```java
    @Override
    public String getLabel() {
        return "Bug";
    }
```

Replace with:

```java
    @Override
    public String getLabel() {
        return "Bug Report";
    }
```

- [ ] **Step 6: Compile**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add src/main/java/eu/greev/dcbot/ticketsystem/categories/CreatorGeneral.java src/main/java/eu/greev/dcbot/ticketsystem/categories/Helper.java src/main/java/eu/greev/dcbot/ticketsystem/categories/Designer.java src/main/java/eu/greev/dcbot/ticketsystem/categories/Dev.java src/main/java/eu/greev/dcbot/ticketsystem/categories/Bug.java
git commit -m "feat: add Creator General, Helper, Designer, Dev categories"
```

---

### Task 4: `TicketGroup` model + `Main.GROUPS` field

**Files:**
- Create: `src/main/java/eu/greev/dcbot/ticketsystem/categories/TicketGroup.java`
- Modify: `src/main/java/eu/greev/dcbot/Main.java:55`

- [ ] **Step 1: Create `TicketGroup.java`**

```java
package eu.greev.dcbot.ticketsystem.categories;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class TicketGroup {
    private final String id;
    private final String label;
    private final List<ICategory> categories;
}
```

- [ ] **Step 2: Add empty `GROUPS` field to `Main.java`**

In `Main.java`, find:

```java
    public static final List<ICategory> CATEGORIES = new ArrayList<>();
```

Add directly below it:

```java
    public static final List<TicketGroup> GROUPS = new ArrayList<>();
```

(`Main.java` already imports `eu.greev.dcbot.ticketsystem.categories.*` and `java.util.*`, so no new imports are needed.)

- [ ] **Step 3: Compile**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/java/eu/greev/dcbot/ticketsystem/categories/TicketGroup.java src/main/java/eu/greev/dcbot/Main.java
git commit -m "feat: add TicketGroup model and GROUPS registry"
```

---

### Task 5: `TicketMenu` helper (base message + role gate)

**Files:**
- Create: `src/main/java/eu/greev/dcbot/ticketsystem/TicketMenu.java`

- [ ] **Step 1: Create `TicketMenu.java`**

```java
package eu.greev.dcbot.ticketsystem;

import eu.greev.dcbot.Main;
import eu.greev.dcbot.ticketsystem.categories.ICategory;
import eu.greev.dcbot.ticketsystem.categories.TicketGroup;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class TicketMenu {
    private TicketMenu() {
    }

    public static MessageCreateData buildBaseMessage(Config config) {
        EmbedBuilder builder = new EmbedBuilder()
                .setFooter(config.getServerName(), config.getServerLogo())
                .setColor(Color.decode(config.getColor()))
                .addField(new MessageEmbed.Field("**Support request**", """
                        You have questions or a problem?
                        Just click one of the buttons below.
                        We will try to handle your ticket as soon as possible.
                        """, false));

        List<Button> groupButtons = new ArrayList<>();
        for (TicketGroup group : Main.GROUPS) {
            groupButtons.add(Button.primary("group-" + group.getId(), group.getLabel()));
        }

        return new MessageCreateBuilder()
                .addEmbeds(builder.build())
                .addActionRow(groupButtons)
                .build();
    }

    /**
     * @return true if the category may be opened by this member.
     *         {@code getRequiredRoleId == null} → always enabled.
     *         {@code == 0} → disabled for everyone (gate configured but no role set).
     *         otherwise → enabled only if the member has that role.
     */
    public static boolean isEnabled(ICategory category, Member member, Config config) {
        Long required = category.getRequiredRoleId(config);
        if (required == null) {
            return true;
        }
        if (required == 0L) {
            return false;
        }
        if (member == null) {
            return false;
        }
        return member.getRoles().stream().map(Role::getIdLong).anyMatch(id -> id == required);
    }
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/java/eu/greev/dcbot/ticketsystem/TicketMenu.java
git commit -m "feat: add TicketMenu base-message builder and role gate"
```

---

### Task 6: `GroupSelection` button handler

**Files:**
- Create: `src/main/java/eu/greev/dcbot/ticketsystem/interactions/GroupSelection.java`

- [ ] **Step 1: Create `GroupSelection.java`**

```java
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
```

- [ ] **Step 2: Compile**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/java/eu/greev/dcbot/ticketsystem/interactions/GroupSelection.java
git commit -m "feat: add GroupSelection ephemeral sub-menu handler"
```

---

### Task 7: Refactor `CategorySelection` + wire `Main.java`

This task changes the `CategorySelection` constructor signature, so the `Main.java` call site must change in the same commit to keep the build green.

**Files:**
- Modify: `src/main/java/eu/greev/dcbot/ticketsystem/interactions/CategorySelection.java`
- Modify: `src/main/java/eu/greev/dcbot/Main.java:110-116,282-287`

- [ ] **Step 1: Rewrite `CategorySelection.java`**

Replace the entire file with:

```java
package eu.greev.dcbot.ticketsystem.interactions;

import eu.greev.dcbot.ticketsystem.TicketMenu;
import eu.greev.dcbot.ticketsystem.categories.ICategory;
import eu.greev.dcbot.utils.Config;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

@AllArgsConstructor
public class CategorySelection implements Interaction {
    private final ICategory category;
    private final Config config;

    @Override
    public void execute(Event evt) {
        ButtonInteractionEvent event = (ButtonInteractionEvent) evt;

        if (!TicketMenu.isEnabled(category, event.getMember(), config)) {
            event.reply("You don't have permission to open this category.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.replyModal(category.getModal()).queue();
    }
}
```

- [ ] **Step 2: Replace the category-registration block in `Main.java`**

In `Main.java`, find:

```java
        registerCategory(new General(), config, ticketService, ticketData);
        registerCategory(new Report(), config, ticketService, ticketData);
        registerCategory(new Creator(), config, ticketService, ticketData);
        registerCategory(new Bug(), config, ticketService, ticketData);
        registerCategory(new CrashReport(), config, ticketService, ticketData);
        registerCategory(new Payment(), config, ticketService, ticketData);
        registerCategory(new Security(), config, ticketService, ticketData);
```

Replace with:

```java
        General general = new General();
        Report report = new Report();
        Payment payment = new Payment();
        Bug bug = new Bug();
        CrashReport crashReport = new CrashReport();
        Security security = new Security();
        Creator creator = new Creator();
        CreatorGeneral creatorGeneral = new CreatorGeneral();
        Helper helper = new Helper();
        Designer designer = new Designer();
        Dev dev = new Dev();

        registerCategory(general, config, ticketService, ticketData);
        registerCategory(report, config, ticketService, ticketData);
        registerCategory(payment, config, ticketService, ticketData);
        registerCategory(bug, config, ticketService, ticketData);
        registerCategory(crashReport, config, ticketService, ticketData);
        registerCategory(security, config, ticketService, ticketData);
        registerCategory(creator, config, ticketService, ticketData);
        registerCategory(creatorGeneral, config, ticketService, ticketData);
        registerCategory(helper, config, ticketService, ticketData);
        registerCategory(designer, config, ticketService, ticketData);
        registerCategory(dev, config, ticketService, ticketData);

        GROUPS.add(new TicketGroup("general", "General", List.of(general, report, payment)));
        GROUPS.add(new TicketGroup("problems", "Problems", List.of(bug, crashReport, security)));
        GROUPS.add(new TicketGroup("creator", "Creator", List.of(creator, creatorGeneral)));
        GROUPS.add(new TicketGroup("staffapp", "Staff App.", List.of(helper, designer, dev)));

        for (TicketGroup group : GROUPS) {
            registerInteraction("group-" + group.getId(), new GroupSelection(group, config));
        }
```

- [ ] **Step 3: Update `registerCategory` to pass `config` into `CategorySelection`**

In `Main.java`, find:

```java
    private static void registerCategory(ICategory category, Config config, TicketService ticketService, TicketData ticketData) {
        registerInteraction("select-" + category.getId(), new CategorySelection(category));
```

Replace with:

```java
    private static void registerCategory(ICategory category, Config config, TicketService ticketService, TicketData ticketData) {
        registerInteraction("select-" + category.getId(), new CategorySelection(category, config));
```

(`Main.java` already wildcard-imports `eu.greev.dcbot.ticketsystem.categories.*`, `eu.greev.dcbot.ticketsystem.interactions.*`, and `java.util.*`, so `CreatorGeneral`, `Helper`, `Designer`, `Dev`, `TicketGroup`, `GroupSelection`, and `List` are all already available.)

- [ ] **Step 4: Compile**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add src/main/java/eu/greev/dcbot/ticketsystem/interactions/CategorySelection.java src/main/java/eu/greev/dcbot/Main.java
git commit -m "feat: button-based CategorySelection with role gate, register groups"
```

---

### Task 8: `Setup` uses `TicketMenu`

**Files:**
- Modify: `src/main/java/eu/greev/dcbot/ticketsystem/interactions/commands/Setup.java`

- [ ] **Step 1: Replace the support-request embed + select-menu block**

In `Setup.java`, find:

```java
        EmbedBuilder builder = new EmbedBuilder().setFooter(config.getServerName(), config.getServerLogo())
                .setColor(color)
                .addField(new MessageEmbed.Field("**Support request**", """
                        You have questions or a problem?
                        Just click the one of the buttons below.
                        We will try to handle your ticket as soon as possible.
                        """, false));

        StringSelectMenu.Builder selectionBuilder = StringSelectMenu.create("ticket-create-topic")
                .setPlaceholder("Select your ticket topic");

        for (ICategory category : Main.CATEGORIES) {
            selectionBuilder.addOption(category.getLabel(), "select-" + category.getId(), category.getDescription());
        }

        baseChannel.sendMessageEmbeds(builder.build())
                .setActionRow(selectionBuilder.build())
                .queue();
```

Replace with:

```java
        baseChannel.sendMessage(TicketMenu.buildBaseMessage(config)).queue();
```

- [ ] **Step 2: Fix imports**

In `Setup.java`, remove these import lines:

```java
import eu.greev.dcbot.Main;
import eu.greev.dcbot.ticketsystem.categories.ICategory;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
```

Add this import (alongside the other `eu.greev.dcbot` imports):

```java
import eu.greev.dcbot.ticketsystem.TicketMenu;
```

(`EmbedBuilder` and `java.awt.*` stay — still used by the success embed `builder1` and the `color` parsing.)

- [ ] **Step 3: Compile**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/java/eu/greev/dcbot/ticketsystem/interactions/commands/Setup.java
git commit -m "refactor: Setup uses shared TicketMenu base message"
```

---

### Task 9: `TicketListener` — drop select handler, use `TicketMenu`, null-guard buttons

**Files:**
- Modify: `src/main/java/eu/greev/dcbot/ticketsystem/TicketListener.java`

- [ ] **Step 1: Add a null-guard in `onButtonInteraction`**

In `TicketListener.java`, find (end of `onButtonInteraction`):

```java
        Main.INTERACTIONS.get(buttonId).execute(event);
    }
```

Replace with:

```java
        Interaction interaction = Main.INTERACTIONS.get(buttonId);
        if (interaction == null) {
            event.reply("This button is unknown or expired. Please use the menu again.")
                    .setEphemeral(true)
                    .queue();
            return;
        }
        interaction.execute(event);
    }
```

- [ ] **Step 2: Delete the `onStringSelectInteraction` method**

In `TicketListener.java`, find and delete this entire method:

```java
    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (event.getSelectMenu().getId() == null || !event.getSelectMenu().getId().equals("ticket-create-topic"))
            return;
        Main.INTERACTIONS.get(event.getSelectedOptions().get(0).getValue()).execute(event);
    }
```

- [ ] **Step 3: Replace the menu rebuild in `onGuildUpdateIcon`**

In `TicketListener.java`, find:

```java
            EmbedBuilder builder = new EmbedBuilder().setFooter(config.getServerName(), config.getServerLogo())
                    .setColor(Color.decode(config.getColor()))
                    .addField(new MessageEmbed.Field("**Support request**", """
                            You have questions or a problem?
                            Just click the one of the buttons below.
                            We will try to handle your ticket as soon as possible.
                            """, false));

            StringSelectMenu.Builder selectionBuilder = StringSelectMenu.create("ticket-create-topic")
                    .setPlaceholder("Select your ticket topic");

            for (ICategory category : Main.CATEGORIES) {
                selectionBuilder.addOption(category.getLabel(), "select-" + category.getId(), category.getDescription());
            }

            event.getGuild().getTextChannelById(config.getBaseChannel()).sendMessageEmbeds(builder.build())
                    .setActionRow(selectionBuilder.build())
                    .complete();
```

Replace with:

```java
            event.getGuild().getTextChannelById(config.getBaseChannel())
                    .sendMessage(TicketMenu.buildBaseMessage(config))
                    .complete();
```

- [ ] **Step 4: Fix imports**

In `TicketListener.java`, remove these import lines:

```java
import eu.greev.dcbot.ticketsystem.categories.ICategory;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
```

Add these imports (alongside the other `eu.greev.dcbot` imports):

```java
import eu.greev.dcbot.ticketsystem.TicketMenu;
import eu.greev.dcbot.ticketsystem.interactions.Interaction;
```

(`MessageEmbed` import stays — it is still used by `onChannelDelete`/embeds elsewhere via `MessageEmbed.Field`? Verify: if the compiler reports `MessageEmbed` as the only removed usage and it is now unused, leaving the import is harmless in Java and does not fail the build. Do not remove it unless you have confirmed no other reference. `EmbedBuilder` and `java.awt.*` stay — still used by member-join/leave embeds.)

- [ ] **Step 5: Compile**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add src/main/java/eu/greev/dcbot/ticketsystem/TicketListener.java
git commit -m "refactor: drop select handler, use TicketMenu, null-guard buttons"
```

---

### Task 10: Full build + manual verification

**Files:** none (verification only)

- [ ] **Step 1: Clean build**

Run: `./gradlew clean build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Manual Discord checklist**

Run the bot against a test guild and verify:

- [ ] `/ticket setup ...` posts ONE message with 4 buttons: `General`, `Problems`, `Creator`, `Staff App.`
- [ ] Clicking `General` shows an ephemeral message (only you see it) with `General`, `Report`, `Payment`
- [ ] Clicking `Problems` shows `Bug Report`, `Crash Report`, `Security Report`
- [ ] Clicking `Creator` shows `Creator Application` enabled and `Creator General` **disabled** (no creator role / `creatorRole: 0`)
- [ ] Set `creatorRole` in `config.yml` to a role you have, restart, re-open `Creator` → `Creator General` now **enabled**; opening it shows the `Message` modal and creates a ticket
- [ ] Clicking `Staff App.` shows `Helper`, `Designer`, `Dev`; each opens its modal with the correct fields and creates a ticket
- [ ] `Designer` ticket created with empty `Portfolio Links` shows `N/A`; `Dev` with empty `Code samples / GitHub` shows `N/A`
- [ ] Restart the bot; the existing base message's buttons still work (group click → ephemeral sub-menu)
- [ ] A second user clicking a group button gets their own ephemeral menu (the first user's menu is unaffected)

- [ ] **Step 3: Commit (if any verification fixes were needed)**

```bash
git add -A
git commit -m "fix: manual verification adjustments for ticket category groups"
```

If no fixes were needed, skip this commit.

---

## Self-Review

**Spec coverage:**
- Two-level button flow → Tasks 5, 6, 7, 8, 9 ✓
- Final menu structure / groups → Task 7 (`GROUPS`) ✓
- Payment→General, Security→Problems → Task 7 ✓
- `Bug` label → Task 3 ✓
- New categories + per-category modal fields → Task 3 ✓
- `creatorRole` config + migration default 0 → Task 1 ✓
- `getRequiredRoleId` hook → Task 2; override → Task 3 (`CreatorGeneral`) ✓
- Disabled gating + `creatorRole==0` disabled-for-all + `member==null` → Task 5 (`isEnabled`) ✓
- Server-side gate re-check → Task 7 (`CategorySelection`) ✓
- Shared base-message builder (DRY) → Task 5, used in Tasks 8, 9 ✓
- Remove `onStringSelectInteraction` → Task 9 ✓
- Null-guard unknown custom id → Task 9 ✓
- Restart safety (static ids) → verified Task 10 ✓
- Testing = build + manual → Task 10 ✓

**Placeholder scan:** No TBD/TODO; all code blocks complete. The only conditional instruction (Task 9 Step 4 `MessageEmbed` import) is explicit about the decision rule and safe-by-default (unused imports do not fail a Java build).

**Type consistency:** `getRequiredRoleId(Config)` defined Task 2, overridden Task 3, consumed Task 5. `TicketMenu.isEnabled(ICategory, Member, Config)` defined Task 5, called Tasks 6 & 7. `TicketMenu.buildBaseMessage(Config)` defined Task 5, called Tasks 8 & 9. `TicketGroup(String,String,List<ICategory>)` + `getId()/getLabel()/getCategories()` (Lombok `@Getter`) defined Task 4, used Tasks 5, 6, 7. `CategorySelection(ICategory, Config)` and `GroupSelection(TicketGroup, Config)` constructors match their `Main.java` call sites in Task 7. Custom-id scheme `group-<id>` / `select-<id>` consistent across Tasks 5, 6, 7 and the existing `registerCategory`.
