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

        MessageCreateBuilder messageBuilder = new MessageCreateBuilder()
                .addEmbeds(builder.build());
        if (!groupButtons.isEmpty()) {
            messageBuilder.addActionRow(groupButtons);
        }
        return messageBuilder.build();
    }

    /**
     * @return true if the category may be opened by this member.
     *         {@code getRequiredRoleId == null} → always enabled.
     *         {@code == 0} → disabled for everyone (gate configured but no role set).
     *         otherwise → enabled only if the member has that role.
     */
    public static boolean isEnabled(ICategory category, Member member, Config config) {
        if (category.isApplication()
                && !Boolean.TRUE.equals(config.getApplicationsOpen().get(category.getId()))) {
            return false;
        }
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
        return member.getRoles().stream().mapToLong(Role::getIdLong).anyMatch(id -> id == required);
    }
}
