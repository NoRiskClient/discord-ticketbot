package eu.greev.dcbot.ticketsystem.interactions;

import eu.greev.dcbot.utils.CustomEmbedBuilder;

import java.awt.*;

//psjahn entm

public class InteractionMessages {
    public static final CustomEmbedBuilder ADD_MEMBER_STAFF = failure("Adding Member Failed", "$OTHER_MENTION is a staff member, they are already in the ticket.");
    public static final CustomEmbedBuilder ADD_MEMBER_ALREADY_ADDED = failure("Adding Member Failed", "$OTHER_MENTION is already in the ticket.");
    public static final CustomEmbedBuilder ADD_MEMBER_ADDED = success("Member Added", "$OTHER_MENTION has been added to the ticket").userAuthor();

    public static final CustomEmbedBuilder TICKET_CLAIM_CLAIMED = success("Ticket Claimed", "Your ticket will be handled by $MENTION").userAuthor().serverFooter();
    public static final CustomEmbedBuilder TICKET_CLAIM_FAILED = failure("Failed Claiming", "You cannot claim this ticket!").serverFooter();

    public static final CustomEmbedBuilder INTERACTION_GENERIC_FAILURE = failure("", "**Something went wrong, please report this to the Bot creator!**").serverFooter();
    public static final CustomEmbedBuilder INTERACTION_INCORRECT_SETUP = failure("Incorrect Setup", "Ticketsystem wasn't setup, please tell an Admin to use </ticket setup:0>!").serverFooter();
    public static final CustomEmbedBuilder INTERACTION_MISSING_PERMISSIONS = failure("Missing Permission", "You are not permitted to use this command!").serverFooter();
    public static final CustomEmbedBuilder INTERACTION_WRONG_CHANNEL = failure("Wrong Channel", "You have to use this command in a ticket!").serverFooter().userAuthor();
    public static final CustomEmbedBuilder INTERACTION_INVALID_MEMBER = failure("Invalid Member", "The given member is invalid!").serverFooter();

    public static final CustomEmbedBuilder REMOVE_MEMBER_STAFF = failure("Removing Member Failed", "$OTHER_MENTION is a staff member, you can't remove them from this ticket.").serverFooter();
    public static final CustomEmbedBuilder REMOVE_MEMBER_REMOVED = success("Member Removed", "$OTHER_MENTION has been removed from the ticket.").userAuthor().userAuthor();
    public static final CustomEmbedBuilder REMOVE_MEMBER_NOT_IN_TICKET = failure("Removing Member Failed", "$OTHER_MENTION isn't part of this ticket.").serverFooter();

    public static final CustomEmbedBuilder SET_CLAIM_EMOJI_INVALID = failure("Can't set emoji", "The Emoji you provided is invalid.").serverFooter();
    public static final CustomEmbedBuilder SET_CLAIM_EMOJI_USED = failure("Can't set emoji", "This emoji is already in use by another staff member.").serverFooter();
    public static final CustomEmbedBuilder SET_CLAIM_EMOJI_SUCCESS = success("Emoji Updated", "Successfully set your claim emoji to $EMOJI!").userAuthor().serverFooter();

    public static final CustomEmbedBuilder SET_OWNER_UPDATED = success("Updated Owner", "$OTHER_MENTION is now the owner of this ticket!");
    public static final CustomEmbedBuilder SET_OWNER_NO_ACCESS = success("Couldn't Update Owner", "$OTHER_MENTION doesn't have access to the channel; Add them to the ticket first.");
    public static final CustomEmbedBuilder SET_OWNER_ALREADY_OWNER = success("Couldn't Update Owner", "$OTHER_MENTION is already the owner of this ticket.");

    public static final CustomEmbedBuilder SET_WAITING_WAITING = new CustomEmbedBuilder().setDescription("Waiting for response.").userAuthor();
    public static final CustomEmbedBuilder SET_WAITING_ALREADY_WAITING = failure("Couldn't Update Waiting Mode", "This ticket is already in waiting mode!");

    public static final CustomEmbedBuilder THREAD_ADD_NOT_A_THREAD = failure("Couldn't Add Staff", "You have to use this command in a ticket thread!");
    public static final CustomEmbedBuilder THREAD_ADD_NOT_STAFF = failure("Couldn't Add Staff", "$OTHER_MENTION isn't a staff!");
    public static final CustomEmbedBuilder THREAD_ADD_ALREADY_ADDED = failure("Couldn't Add Staff", "$OTHER_MENTION is already in the ticket thread!");
    public static final CustomEmbedBuilder THREAD_ADD_ADDED = success("Added Staff", "$OTHER_MENTION has been added to the ticket thread!");

    public static final CustomEmbedBuilder THREAD_JOIN_ALREADY_JOINED = failure("Couldn't Join Ticket Thread", "You already are part of the ticket thread!");
    public static final CustomEmbedBuilder THREAD_JOIN_JOINED = success("Joined Ticket Thread", "You've been added to the ticket thread!");

    public static final CustomEmbedBuilder TICKET_INFO_INFO = new CustomEmbedBuilder()
            .setTitle("Ticket $TICKET_ID")
            .userAuthor();
    public static final CustomEmbedBuilder TICKET_INFO_INVALID_ID = failure("Couldn't Retrieve Info", "The supplied Ticket ID is invalid.");
    public static final CustomEmbedBuilder TICKET_INFO_STILL_OPEN = failure("Couldn't Retrieve Info", "This Ticket is still open.");

    public static final CustomEmbedBuilder TICKET_CREATION_CREATED = success("Ticket Created", "Successfully created a ticket for you: $CHANNEL").userAuthor();
    public static final CustomEmbedBuilder TICKET_CREATION_FAILED = failure("Creating Ticket Failed", "%ERROR");



    private static CustomEmbedBuilder failure(String title, String message) {
        return new CustomEmbedBuilder().setColor(Color.RED)
                .addField("❌ **%s**".formatted(title), message, false);
    }

    private static CustomEmbedBuilder success(String title, String message) {
        return new CustomEmbedBuilder()
                .addField("✅ **%s**".formatted(title), message, false);
    }
}
