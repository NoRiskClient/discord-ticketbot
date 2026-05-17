# Ticket Category Groups — Design Spec

Date: 2026-05-17
Status: Approved (brainstorming), pending implementation plan

## Summary

Replace the single `StringSelectMenu` ("ticket-create-topic") in the base
channel with a two-level button flow:

1. Persistent base message shows **super-category** buttons.
2. Clicking a super-category button replies **ephemerally** (visible only to the
   clicking user) with the **category** buttons of that group.
3. Clicking a category button opens the existing modal for that category.

The ephemeral sub-menu is required because the base message is shared — editing
its action row would change it for everyone.

Also: add new categories, regroup existing ones, gate one category behind a
configurable role.

## Final Menu Structure

| Super-category (button) | Category buttons |
|---|---|
| **General** | `General` · `Report` · `Payment` |
| **Problems** | `Bug Report` · `Crash Report` · `Security Report` |
| **Creator** | `Creator Application` · `Creator General`* |
| **Staff App.** | `Helper` · `Designer` · `Dev` |

\* `Creator General` is shown but **disabled** when the user lacks the
configured creator role (see Role Gate).

### Category changes

- `Bug`: label `"Bug"` → `"Bug Report"`. Id stays `bug`.
- `Payment`, `Security`: unchanged classes, only assigned to a group.
- New classes: `CreatorGeneral` (id `creator-general`), `Helper` (id `helper`),
  `Designer` (id `designer`), `Dev` (id `dev`).
- `General`, `Report`, `CrashReport`, `Creator`, `Payment`, `Security`: modal
  fields unchanged.

### New category modal fields

`maxLength` = max characters (Discord TextInput limit, ≤ 4000).

**Creator General** (id `creator-general`, label "Creator General"):
- `Message` | paragraph | required | 700

**Helper** (id `helper`, label "Helper"):
- `In-Game Name` | short | required | 16
- `Age` | short | required | 3
- `Timezone/Availability` | short | required | 50
- `Why you?` | paragraph | required | 700

**Designer** (id `designer`, label "Designer"):
- `In-Game Name` | short | required | 16
- `Portfolio Links` | paragraph | optional | 300
- `Tools/Software` | short | required | 50
- `Motivation` | paragraph | required | 700

**Dev** (id `dev`, label "Dev"):
- `In-Game Name` | short | required | 16
- `Languages/Tech` | short | required | 50
- `Experience` | paragraph | required | 700
- `Code samples / GitHub` | paragraph | optional | 300

`getInfo()` for each new category maps every field to a `LinkedHashMap` entry,
following the existing pattern (optional/empty → `"N/A"`).

## Architecture

Hardcoded grouping (chosen over config-driven — structure is fixed, fits the
existing hardcoded category-registration pattern, restart-safe).

### Components

**`TicketGroup`** (new class, `ticketsystem.categories` or new `groups` pkg)
- Fields: `String id`, `String label`, `List<ICategory> categories`.
- Static list `Main.GROUPS` defines the 4 groups, built next to category
  registration in `Main.java`.

**`ICategory`** (interface, extended)
- New: `default Long getRequiredRoleId(Config config) { return null; }`
- `CreatorGeneral` overrides → returns `config.getCreatorRole()`.
- Centralizes the role gate as data, not branching logic.

**New category classes**: `CreatorGeneral`, `Helper`, `Designer`, `Dev`
implement `ICategory` with the fields above. `Bug` label changed.

**`GroupSelection implements Interaction`** (new button handler)
- Handles `group-<id>` button clicks.
- Replies ephemerally (`event.reply(...).setEphemeral(true)`) with an
  `ActionRow` of the group's category buttons (custom id `select-<catId>`).
- Each button: `.withDisabled(requiredRoleId != null &&
  !memberHasRole(member, requiredRoleId))`.
- Role check helper: `requiredRoleId == 0` OR member lacks role OR
  `member == null` → disabled. (`creatorRole = 0` ⇒ disabled for everyone —
  conservative default, user-confirmed.)

**`CategorySelection`** (refactored)
- Now handles `ButtonInteractionEvent` instead of `StringSelectInteractionEvent`.
- `event.replyModal(category.getModal())`.
- Old dropdown-reset logic removed (no dropdown anymore).
- Server-side gate re-check: if `getRequiredRoleId` set and member lacks role
  → ephemeral error instead of opening modal (defense against forged requests).

**`Config`** (extended)
- New field: `private long creatorRole;` (Lombok `@Getter/@Setter`).
- Documented in `config.example.yml`.

**Base message builder** (new shared helper)
- New class `eu.greev.dcbot.ticketsystem.TicketMenu` with static method
  `MessageCreateData buildBaseMessage(Config config)` returning the base embed
  + super-category button `ActionRow`.
- Used by both `Setup.execute` and `TicketListener.onGuildUpdateIcon`
  (currently duplicated select-menu construction).

**`TicketListener`** (modified)
- `onStringSelectInteraction`: removed (no select menu remains).
- `onButtonInteraction`: routes `group-*` and `select-*` via the existing
  `Main.INTERACTIONS` map. Add null-guard: unknown/expired custom id →
  ephemeral "unknown or expired" message instead of NPE.

**`Main.java`** (modified)
- Register `group-<id>` → `GroupSelection` for each group.
- `select-<catId>` registration stays (via `registerCategory`).
- Register the 4 new categories.
- Build `Main.GROUPS`.

## Data Flow

1. Base channel holds one persistent message: embed + ActionRow
   `[General] [Problems] [Creator] [Staff App.]` (custom ids
   `group-general`, `group-problems`, `group-creator`, `group-staffapp`).
2. User clicks a super-category button → `GroupSelection.execute` →
   ephemeral reply with that group's category buttons.
3. User clicks a category button → `CategorySelection.execute` →
   `replyModal`.
4. Modal submit → `TicketModal` (unchanged) → ticket created.

### Restart safety

Custom ids are static. `INTERACTIONS` is rebuilt on startup. The persistent
base message stays valid; ephemeral sub-menu buttons also resolve because
their ids are deterministic.

## Edge Cases

- **Disabled Creator General**: Discord blocks the click client-side; plus a
  server-side re-check in `CategorySelection`.
- **`member == null`**: treated as "no role" → disabled.
- **`creatorRole == 0`** (unset / role deleted): Creator General disabled for
  everyone (conservative, user-confirmed).
- **Discord limits**: 4 super-category buttons (≤ 5 per row OK); largest group
  3 categories (≤ 5 OK); largest modal 4 inputs (≤ 5 OK).
- **Old dropdown messages**: `Setup` deletes channel history; same path in
  `onGuildUpdateIcon`. No migration needed.
- **Unknown custom id** after a code change: null-guard in
  `onButtonInteraction` → ephemeral notice, no crash.

## Config Migration

`creatorRole` is a new key, default `0`. Snakeyaml loads missing keys as the
field default (`0`), so existing `config.yml` files keep working. `0` means
Creator General is disabled for everyone until an admin sets a role id.

## Testing

No test framework exists in the repo (`src/test` empty); JDA interaction
testing is impractical. Verification:

- `./gradlew build` — compilation passes.
- Manual Discord checks:
  - Base message shows 4 super-category buttons.
  - Clicking a super-category shows category buttons **only to that user**
    (ephemeral).
  - `Creator General` disabled without creator role, enabled with it.
  - Server-side gate blocks forged category clicks.
  - After bot restart, base message buttons still work.
  - New categories open correct modals; submitted tickets created correctly.

## Out of Scope

- Config-driven grouping (rejected — fixed structure, YAGNI).
- Changes to ticket creation / `TicketModal` internals.
- Changes to existing categories' modal fields (other than `Bug` label).
