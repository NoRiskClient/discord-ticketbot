package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.categories.TicketCategory;
import eu.greev.dcbot.ticketsystem.interactions.ArgumentedInteraction;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ListTicketsCommand extends ArgumentedInteraction {
    public ListTicketsCommand(@NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda) {
        super(config, ticketService, jda);
        this.ticketChannelRequired = false;
        addCommand("all", "List all tickets", LIST_TICKETS_GROUP, d -> d);
        addCommand("by-owner", "List all tickets from a specified owner", LIST_TICKETS_GROUP, d -> d
                .addOption(OptionType.USER, "owner", "The owner of the tickets", true));
        addCommand("by-supporter", "List all tickets from a specified supporter", LIST_TICKETS_GROUP, d -> d
                .addOption(OptionType.USER, "supporter", "The supporter of the tickets", true));
    }

    @Override
    public String getIdentifier() {
        return "list-tickets";
    }

    @Override
    public void handleSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if(arguments.length != 1) return;
        List<Integer> tickets = switch(arguments[0]) {
            case "all" -> ticketService.
            case "by-owner" -> {}
            case "by-supporter" -> {}
            default -> new ArrayList<>();
        };
        /*User owner = event.getOption("by-owner", OptionMapping::getAsUser);
        User supporter = event.getOption("by-supporter", OptionMapping::getAsUser);

        User user = event.getOption("member").getAsUser();
        List<Integer> tickets = ticketService.getTicketIdsByOwner(user.getIdLong());
        EmbedBuilder builder = new EmbedBuilder()
                .setAuthor(user.getName(), null, user.getAvatarUrl())
                .setTitle("This user opened following tickets:")
                .setFooter(config.getServerName(), config.getServerLogo())
                .setColor(Color.decode(config.getColor()));

        if (tickets.isEmpty()) {
            builder.setColor(Color.RED).setTitle("This user never opened a ticket");
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
            return;
        }

        for (int i = 0; i < PAGE_SIZE; i++) {
            if (tickets.size() == i) break;
            Ticket ticket = ticketService.getTicketByTicketId(tickets.get(i));
            if (ticket == null) {
                replyEphemeralAndQueue(InteractionMessages.INTERACTION_GENERIC_FAILURE);
                return;
            }
            builder.addField(generateName(ticket.getCategory(), tickets.get(i)), "", true);
        }

        int maxPage = tickets.size() / PAGE_SIZE + (tickets.size() % PAGE_SIZE == 0 ? 0 : 1);
        PAGE_SCROLL_CACHE.removeIf(e -> e.getHandlerId() == event.getMember().getIdLong());
        ScrollEntity scrollEntity = new ScrollEntity(event.getMember().getIdLong(), user.getIdLong(), maxPage, Instant.now().toEpochMilli());

        event.replyEmbeds(builder.setDescription("Page 1/%d".formatted(maxPage)).build()).setActionRow(
                Button.primary("tickets-backward", Emoji.fromUnicode("◀️")),
                Button.primary("tickets-forward", Emoji.fromUnicode("▶️"))
        ).setEphemeral(true).queue(s -> PAGE_SCROLL_CACHE.add(scrollEntity));*/
    }

    public static String generateName(TicketCategory category, int ticketId) {
        return category.getLabel() + " # " + ticketId;
    }
}