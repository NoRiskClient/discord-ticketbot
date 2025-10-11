package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.categories.TicketCategory;
import eu.greev.dcbot.ticketsystem.interactions.Interaction;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

@Slf4j
public class SetupCommand extends Interaction {
    public SetupCommand(@NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda) {
        super(config, ticketService, jda);
        this.ticketChannelRequired = false;
        this.administratorRequired = true;
        addCommand("Setup the System", d -> d
                .addOption(OptionType.CHANNEL, "base-channel", "The channel where the ticket select menu should be", true)
                .addOption(OptionType.CHANNEL, "unclaimed-category", "The category where the tickets should create", true)
                .addOption(OptionType.ROLE, "staff", "The role which is the team role", true)
                .addOption(OptionType.STRING, "color", "The color of the ticket embeds (HEX-Code)", false));
    }

    @Override
    public String getIdentifier() {
        return "setup";
    }

    @Override
    public void handleSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        String serverName = event.getGuild().getName();
        String serverLogo = event.getGuild().getIconUrl();
        long serverId = event.getGuild().getIdLong();
        long staffId = event.getOption("staff").getAsRole().getIdLong();

        EmbedBuilder error = new EmbedBuilder()
                .setColor(Color.RED)
                .setFooter(serverName, serverLogo);

        if (!(event.getOption("base-channel").getAsChannel() instanceof TextChannel)) {
            event.replyEmbeds(error.addField("❌ **Ticket setup failed**", "Option 'channel' has to be a valid text channel", false)
                    .build())
                    .setEphemeral(true)
                    .queue();
            return;
        } else if (!(event.getOption("unclaimed-category").getAsChannel() instanceof Category)) {
            event.replyEmbeds(error.addField("❌ **Ticket setup failed**", "Option 'category' has to be a valid category", false)
                            .build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        //#3fe245
        Color color = new Color(63, 226, 69, 255);
        OptionMapping clr = event.getOption("color");
        if (clr == null) {
            config.setColor("#3fe245");
        } else {
            try {
                color = Color.decode(clr.getAsString());
            } catch (NumberFormatException e) {
                event.replyEmbeds(error.addField("❌ **Ticket setup failed**", "Option 'color' has to be a hex code", false)
                        .build())
                        .setEphemeral(true)
                        .queue();
                return;
            }
            config.setColor(clr.getAsString());
        }

        TextChannel baseChannel = event.getOption("base-channel").getAsChannel().asTextChannel();
        long supportCategory = event.getOption("unclaimed-category").getAsChannel().getIdLong();

        config.setServerName(serverName);
        config.setServerLogo(serverLogo);
        config.setServerId(serverId);
        config.setUnclaimedCategory(supportCategory);
        config.setBaseChannel(baseChannel.getIdLong());
        config.setStaffId(staffId);
        config.setAddToTicketThread(new ArrayList<>());

        try {
            event.getGuild().getTextChannelById(config.getBaseChannel()).getIterableHistory()
                    .takeAsync(1000)
                    .get()
                    .forEach(m -> m.delete().queue());
        } catch (InterruptedException | ExecutionException e) {
            log.error("Could not delete messages", e);
        }

        EmbedBuilder builder = new EmbedBuilder().setFooter(config.getServerName(), config.getServerLogo())
                .setColor(color)
                .addField(new MessageEmbed.Field("**Support request**", """
                        You have questions or a problem?
                        Just click the one of the buttons below.
                        We will try to handle your ticket as soon as possible.
                        """, false));

        StringSelectMenu.Builder selectionBuilder = StringSelectMenu.create("ticket-create-topic")
                .setPlaceholder("Select your ticket topic");

        for (TicketCategory category : TicketCategory.values()) {
            selectionBuilder.addOption(category.getLabel(), "select-category " + category.getId(), category.getDescription());
        }

        baseChannel.sendMessageEmbeds(builder.build())
                .setActionRow(selectionBuilder.build())
                .queue();

        EmbedBuilder builder1 = new EmbedBuilder().setFooter(serverName, serverLogo)
                .setColor(color)
                .setAuthor(member.getEffectiveName(), null, event.getMember().getEffectiveAvatarUrl())
                .addField("✅ **Ticket created**", "Successfully setup ticketsystem.\nDon't forget to add the ticket category ids to the config! " + baseChannel.getAsMention(), false);

        event.replyEmbeds(builder1.build()).setEphemeral(true).queue();
    }
}