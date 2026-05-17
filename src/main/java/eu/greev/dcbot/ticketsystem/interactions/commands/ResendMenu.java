package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.TicketMenu;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.Color;
import java.util.concurrent.ExecutionException;

@Slf4j
public class ResendMenu extends AbstractCommand {

    public ResendMenu(Config config, TicketService ticketService, EmbedBuilder missingPerm, JDA jda) {
        super(config, ticketService, missingPerm, jda);
    }

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;

        if (!hasStaffPermission(event.getMember())) {
            event.replyEmbeds(missingPerm.setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl()).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        TextChannel baseChannel = event.getGuild().getTextChannelById(config.getBaseChannel());
        if (baseChannel == null) {
            event.replyEmbeds(new EmbedBuilder()
                            .setColor(Color.RED)
                            .setDescription("❌ Base channel not found. Run `/ticket setup` first.")
                            .build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.deferReply(true).queue();

        try {
            baseChannel.getIterableHistory()
                    .takeAsync(1000)
                    .get()
                    .forEach(m -> m.delete().queue());
        } catch (InterruptedException | ExecutionException e) {
            log.error("Could not delete messages", e);
        }

        baseChannel.sendMessage(TicketMenu.buildBaseMessage(config)).queue();

        event.getHook().sendMessageEmbeds(new EmbedBuilder()
                        .setColor(Color.decode(config.getColor()))
                        .setFooter(config.getServerName(), config.getServerLogo())
                        .addField("✅ **Menu reposted**", "Ticket menu refreshed in " + baseChannel.getAsMention(), false)
                        .build())
                .setEphemeral(true)
                .queue();
    }
}
