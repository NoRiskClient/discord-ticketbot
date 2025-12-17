package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.interactions.Interaction;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

public abstract class AbstractCommand implements Interaction {
    final TicketService ticketService;
    final EmbedBuilder missingPerm;
    final Config config;
    final JDA jda;

    protected AbstractCommand(Config config, TicketService ticketService, EmbedBuilder missingPerm, JDA jda) {
        this.ticketService = ticketService;
        this.missingPerm = missingPerm;
        this.config = config;
        this.jda = jda;
    }

    protected boolean hasStaffPermission(Member member) {
        if (config.isDevMode()) return true;
        boolean isStaff = member.getRoles().contains(jda.getRoleById(config.getStaffId()));
        boolean isAdmin = member.hasPermission(Permission.ADMINISTRATOR);
        return isStaff || isAdmin;
    }
}