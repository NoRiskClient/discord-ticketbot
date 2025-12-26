package eu.greev.dcbot.ticketsystem.entities;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Rating {
    private final int id;
    private final int ticketId;
    private final String ownerId;
    private final String supporterId;
    private final int rating;
    private final String message;
    private final long createdAt;
}
