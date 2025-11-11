package eu.greev.dcbot.scheduler;

import eu.greev.dcbot.Main;
import eu.greev.dcbot.ticketsystem.categories.ICategory;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.jdbi.v3.core.Jdbi;

@Slf4j
@AllArgsConstructor
public class DailyScheduler {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Config config;
    private TicketService ticketService;
    private JDA jda;
    private Jdbi jdbi;

    public void start() {
        scheduler.scheduleAtFixedRate(this::run, getInitialDelay(), 24 * 60 * 60, TimeUnit.SECONDS);
    }

    private int getInitialDelay() {
        LocalDateTime now = LocalDateTime.now();
        // Minute set to 30 to avoid collision with HourlyScheduler
        LocalDateTime nextMidnight = now.plusDays(1).withHour(0).withMinute(30).withSecond(0).withNano(0);
        return (int) Duration.between(now, nextMidnight).getSeconds();
    }

    private void run() {
        log.info("Running daily category consolidation...");
        consolidateCategoriesAndCleanup();
    }

    private void consolidateCategoriesAndCleanup() {
        for (ICategory category : Main.CATEGORIES) {
            Category mainCategory = jda.getCategoryById(config.getCategories().get(category.getId()));
            if (mainCategory == null) {
                continue;
            }

            List<Category> overflowCategories = new ArrayList<>(Main.OVERFLOW_CHANNEL_CATEGORIES.get(category));
            overflowCategories.addFirst(mainCategory);

            consolidateChannels(overflowCategories, mainCategory, category);
        }

        Category mainUnclaimedCategory = jda.getCategoryById(config.getUnclaimedCategory());
        if (mainUnclaimedCategory != null) {
            List<Category> unclaimedOverflow = new ArrayList<>(Main.OVERFLOW_UNCLAIMED_CHANNEL_CATEGORIES);
            unclaimedOverflow.addFirst(mainUnclaimedCategory);

            consolidateChannels(unclaimedOverflow, mainUnclaimedCategory, null);
        }
    }

    private void consolidateChannels(List<Category> categories, Category mainCategory, ICategory ticketCategory) {
        if (mainCategory == null) {
            return;
        }

        List<TextChannel> allChannels = categories.stream()
                .flatMap(c -> c.getTextChannels().stream())
                .toList();

        int channelsCount = allChannels.size();
        int categoriesNeeded = Math.max(1, (channelsCount + 49) / 50);

        List<Category> categoriesToKeep = categories.stream()
                .limit(categoriesNeeded)
                .toList();

        int channelIndex = 0;
        for (Category targetCategory : categoriesToKeep) {
            int channelsForThisCategory = Math.min(50, allChannels.size() - channelIndex);

            for (int i = 0; i < channelsForThisCategory; i++) {
                TextChannel channel = allChannels.get(channelIndex++);
                if (!channel.getParentCategory().equals(targetCategory)) {
                    channel.getManager().setParent(targetCategory).queue();
                }
            }
        }

        categoriesToKeep.forEach(c ->
                c.modifyTextChannelPositions()
                        .sortOrder(ticketService.getChannelComparator())
                        .queue()
        );

        List<Category> categoriesToDelete = categories.stream()
                .skip(categoriesNeeded)
                .filter(c -> !c.equals(mainCategory))
                .toList();

        for (Category category : categoriesToDelete) {
            if (ticketCategory != null) {
                Main.OVERFLOW_CHANNEL_CATEGORIES.get(ticketCategory).remove(category);
            } else {
                Main.OVERFLOW_UNCLAIMED_CHANNEL_CATEGORIES.remove(category);
            }

            jdbi.useHandle(handle ->
                    handle.createUpdate("DELETE FROM overflow_categories WHERE categoryID = ?")
                            .bind(0, category.getId())
                            .execute()
            );

            category.delete().queue();
        }
    }
}
