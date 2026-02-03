package gg.norisk.ticketbot.embed;

import java.util.Locale;
import java.util.Map;

public record EmbedBuildInfo(
    EmbedDefinition definition, Locale locale, Map<String, String> placeholders) {}
