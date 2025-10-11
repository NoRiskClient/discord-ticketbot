package eu.greev.dcbot;

import eu.greev.dcbot.scheduler.HourlyScheduler;
import eu.greev.dcbot.ticketsystem.TicketListener;
import eu.greev.dcbot.ticketsystem.categories.TicketCategory;
import eu.greev.dcbot.ticketsystem.interactions.ArgumentedInteraction;
import eu.greev.dcbot.ticketsystem.interactions.CategorySelectionInteraction;
import eu.greev.dcbot.ticketsystem.interactions.Interaction;
import eu.greev.dcbot.ticketsystem.interactions.InteractionFactory;
import eu.greev.dcbot.ticketsystem.interactions.argumented.CloseTicketInteraction;
import eu.greev.dcbot.ticketsystem.interactions.buttons.TicketConfirmButtonInteraction;
import eu.greev.dcbot.ticketsystem.interactions.buttons.TicketNevermind;
import eu.greev.dcbot.ticketsystem.interactions.commands.*;
import eu.greev.dcbot.ticketsystem.interactions.impl.TicketClaimInteraction;
import eu.greev.dcbot.ticketsystem.interactions.impl.TranscriptInteraction;
import eu.greev.dcbot.ticketsystem.interactions.modals.TicketCreationModal;
import eu.greev.dcbot.ticketsystem.service.TicketData;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.apache.log4j.PropertyConfigurator;
import org.apache.logging.log4j.util.Strings;
import org.jdbi.v3.core.Jdbi;
import org.sqlite.SQLiteDataSource;

import java.io.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class Main {
    private static final Map<String, Interaction> INTERACTIONS = new HashMap<>();
    @Getter private static String getTicketCommandId;
    private static Jdbi jdbi;

    public static void main(String[] args) throws InterruptedException {
        PropertyConfigurator.configure(Main.class.getClassLoader().getResourceAsStream("log4j2.properties"));
        JDA jda = null;

        Config config = Config.load(new File("./Tickets/config.yml"));

        if (Strings.isEmpty(config.getToken())) {
            log.error("No valid token provided! Add your bot token into `./Tickets/config.yml` with the key `token`");
            System.exit(1);
        }

        try {
            jda = JDABuilder.create(config.getToken(),
                            List.of(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_PRESENCES))
                    .disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS, CacheFlag.SCHEDULED_EVENTS)
                    .setActivity(Activity.listening(" ticket commands."))
                    .setChunkingFilter(ChunkingFilter.ALL).setMemberCachePolicy(MemberCachePolicy.ALL)
                    .setStatus(OnlineStatus.ONLINE)
                    .build();
        } catch (InvalidTokenException e) {
            log.error("Bot could not be initialized", e);
            System.exit(1);
        }
        jda.awaitReady();

        initDatasource();

        TicketData ticketData = new TicketData(jda, jdbi);
        TicketService ticketService = new TicketService(jda, config, jdbi, ticketData);
        jda.addEventListener(new TicketListener(ticketService, config, jda));

        new HourlyScheduler(config, ticketService, ticketData, jda).start();

        for(TicketCategory category : TicketCategory.values()) registerCategoryInteractions(config, ticketService, jda, ticketData, category);

        registerInteractions(config, ticketService, jda,
                CategorySelectionInteraction::new,
                TicketClaimInteraction::new,
                CloseTicketInteraction::new,
                TicketConfirmButtonInteraction::new,
                TicketNevermind::new,
                TranscriptInteraction::new,
                SetupCommand::new,
                TicketInfoCommand::new,
                ListTicketsCommand::new,
                StatsCommand::new,
                AddMemberCommand::new,
                RemoveMemberCommand::new,
                TransferCommand::new,
                SetOwnerCommand::new,
                SetWaitingCommand::new,
                ThreadAddCommand::new,
                ThreadJoinCommand::new);

        SlashCommandData parent = Commands.slash("ticket", "Manage the ticket system");
        for(Map.Entry<SubcommandGroupData, List<SubcommandData>> entry : Interaction.COMMANDS.entrySet()) {
            if(entry.getKey() == null) parent.addSubcommands(entry.getValue());
            else parent.addSubcommandGroups(entry.getKey());
        }

        jda.updateCommands().addCommands(parent)
                .queue(s -> s.getFirst().getSubcommands().forEach(c -> {
                    if (c.getName().equals("get-tickets")) {
                        getTicketCommandId = c.getId();
                    }
                })
        );

        log.info("Started: {}", OffsetDateTime.now(ZoneId.systemDefault()));
    }

    private static void registerInteractions(Config config, TicketService ticketService, JDA jda, InteractionFactory... interactions) {
        for(InteractionFactory factory : interactions) {
            registerInteraction(factory.create(config, ticketService, jda));
        }
    }

    private static void registerInteraction(Interaction interaction) {
        registerInteraction(interaction.getIdentifier(), interaction);
    }

    private static void registerInteraction(String identifier, Interaction interaction) {
        INTERACTIONS.put(identifier, interaction);
    }

    private static void initDatasource() {
        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:./Tickets/tickets.db");
        jdbi = Jdbi.create(ds);

        String setup = "";
        try (InputStream in = Main.class.getClassLoader().getResourceAsStream("dbsetup.sql")) {
            setup = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            log.error("Could not read db setup file", e);
            System.exit(1);
        }
        Arrays.stream(setup.split(";")).toList().forEach(query -> jdbi.withHandle(h -> h.createUpdate(query).execute()));
    }

    private static void registerCategoryInteractions(Config config, TicketService ticketService, JDA jda, TicketData ticketData, TicketCategory category) {
        registerInteraction(new TicketCreationModal(config, ticketService, jda, category, ticketData));
    }

    public static void handleInteraction(String string, IReplyCallback event) {
        System.out.println("Interaction " + string);
        String id = string.split(" ")[0];
        Optional.ofNullable(INTERACTIONS.get(id)).ifPresent(i -> {
            if(i instanceof ArgumentedInteraction arg) arg.handleArgumented(string, event);
            else i.handle(event);
        });
    }
}