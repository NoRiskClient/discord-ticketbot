package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.util.Map;

public class SetClaimEmoji extends AbstractCommand {
    public SetClaimEmoji(Config config, TicketService ticketService, EmbedBuilder missingPerm, JDA jda) {
        super(config, ticketService, missingPerm, jda);
    }

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;
        if (!hasStaffPermission(event.getMember())) {
            event.replyEmbeds(missingPerm.setFooter(config.getServerName(), config.getServerLogo()).build()).setEphemeral(true).queue();
            return;
        }

        String emoji = event.getOption("emoji").getAsString();

        if (!isValidEmojiOnly(emoji)) {
            event.replyEmbeds(new EmbedBuilder()
                    .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                    .setTitle("❌ **Please provide a valid emoji**")
                    .setColor(Color.RED)
                    .setFooter(config.getServerName(), config.getServerLogo())
                    .build()
            ).setEphemeral(true).queue();
            return;
        }

        Map<Long, String> emojis = config.getClaimEmojis();

        if (emojis.containsValue(emoji) && !emoji.equals(emojis.get(event.getUser().getIdLong()))) {
            event.replyEmbeds(new EmbedBuilder()
                    .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                    .setTitle("❌ **This emoji is already in use by another staff member**")
                    .setColor(Color.RED)
                    .setFooter(config.getServerName(), config.getServerLogo())
                    .build()
            ).setEphemeral(true).queue();
            return;
        }

        emojis.put(event.getUser().getIdLong(), emoji);
        config.setClaimEmojis(emojis);
        config.dumpConfig("./Tickets/config.yml");

        event.replyEmbeds(new EmbedBuilder()
                .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                .setTitle("✅ **Successfully set your claim emoji to %s**".formatted(emoji))
                .setColor(Color.decode(config.getColor()))
                .setFooter(config.getServerName(), config.getServerLogo())
                .build()
        ).setEphemeral(true).queue();
    }

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
