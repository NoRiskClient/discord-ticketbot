package gg.norisk.ticketbot.util;

import java.util.Locale;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;

public record EmbedBuildInfo(
    EmbedBuilder builder, Locale locale, Map<String, String> placeholders) {}
