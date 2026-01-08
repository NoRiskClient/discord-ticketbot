package gg.norisk.ticketbot.interaction;

import gg.norisk.ticketbot.Config;
import gg.norisk.ticketbot.Database;
import gg.norisk.ticketbot.TicketManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import org.jetbrains.annotations.NotNull;

public abstract class ArgumentedInteraction extends Interaction {
  protected String[] arguments = new String[] {};

  protected ArgumentedInteraction(
      @NotNull Config config,
      @NotNull TicketManager ticketManager,
      @NotNull Database database,
      @NotNull JDA jda) {
    super(config, ticketManager, database, jda);
  }

  @Override
  public void handle(IReplyCallback event) {
    super.handle(event);
  }

  public void handleArgumented(String id, IReplyCallback event) {
    String[] arguments = id.split(" ");
    this.arguments = new String[arguments.length - 1];
    System.arraycopy(arguments, 1, this.arguments, 0, this.arguments.length);
    handle(event);
  }
}
