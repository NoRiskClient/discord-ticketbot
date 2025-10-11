package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.interactions.Interaction;
import eu.greev.dcbot.ticketsystem.interactions.InteractionMessages;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class SetClaimEmojiCommand extends Interaction {
    public SetClaimEmojiCommand(@NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda) {
        super(config, ticketService, jda);
        this.ticketChannelRequired = false;
        addCommand("Set your personal claim emoji", d -> d
                .addOption(OptionType.STRING, "emoji", "The emoji you want to set", true));
    }

    @Override
    public String getIdentifier() {
        return "set-claim-emoji";
    }

    @Override
    public void handleSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String emoji = event.getOption("emoji", OptionMapping::getAsString);

        if (!isValidEmojiOnly(emoji)) {
            replyEphemeralAndQueue(InteractionMessages.SET_CLAIM_EMOJI_INVALID);
            return;
        }

        Map<Long, String> emojis = config.getClaimEmojis();

        if (emojis.containsValue(emoji) && !emoji.equals(emojis.get(event.getUser().getIdLong()))) {
            replyEphemeralAndQueue(InteractionMessages.SET_CLAIM_EMOJI_USED);
            return;
        }

        emojis.put(event.getUser().getIdLong(), emoji);
        config.setClaimEmojis(emojis);

        InteractionMessages.SET_CLAIM_EMOJI_SUCCESS.placeholder("$EMOJI", emoji);
        replyEphemeralAndQueue(InteractionMessages.SET_CLAIM_EMOJI_SUCCESS);
    }

    @Contract(value = "null -> false", pure = true)
    private boolean isValidEmojiOnly(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        // Remove any whitespace and check if empty
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return false;
        }

        // Count emoji codepoints
        long emojiCount = trimmed.codePoints()
                .filter(codePoint ->
                        Character.getType(codePoint) == Character.OTHER_SYMBOL ||
                                Character.getType(codePoint) == Character.MODIFIER_SYMBOL ||
                                (codePoint >= 0x1F600 && codePoint <= 0x1F64F) || // Emoticons
                                (codePoint >= 0x1F300 && codePoint <= 0x1F5FF) || // Misc Symbols
                                (codePoint >= 0x1F680 && codePoint <= 0x1F6FF) || // Transport
                                (codePoint >= 0x2600 && codePoint <= 0x26FF) ||   // Misc symbols
                                (codePoint >= 0x2700 && codePoint <= 0x27BF)      // Dingbats
                ).count();

        // Check if input contains exactly one emoji and no other characters
        return emojiCount == 1 && emojiCount == trimmed.codePoints().count();
    }

}
