package eu.greev.dcbot.ticketsystem.categories;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class TicketGroup {
    private final String id;
    private final String label;
    private final List<ICategory> categories;
}
