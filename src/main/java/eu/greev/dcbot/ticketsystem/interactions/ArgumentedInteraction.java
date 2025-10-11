package eu.greev.dcbot.ticketsystem.interactions;

import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import org.jetbrains.annotations.NotNull;

public abstract class ArgumentedInteraction extends Interaction {
    protected String[] arguments = new String[]{};

    protected ArgumentedInteraction(@NotNull Config config, @NotNull TicketService ticketService, @NotNull JDA jda) {
        super(config, ticketService, jda);
    }

    @Override
    public void handle(IReplyCallback event) {
        super.handle(event);
    }

    public void handleArgumented(String id, IReplyCallback event) {
        String[] arguments = id.split(" ");
        this.arguments = new String[arguments.length-1];
        System.arraycopy(arguments, 1, this.arguments, 0, this.arguments.length);
        handle(event);
    }
}
