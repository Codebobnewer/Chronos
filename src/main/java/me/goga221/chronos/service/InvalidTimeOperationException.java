package me.goga221.chronos.service;

import me.goga221.chronos.message.MessageKey;

/**
 * Thrown when a requested time operation fails validation, carrying the
 * {@link MessageKey} the command layer should report back to the sender.
 */
public final class InvalidTimeOperationException extends RuntimeException {

    private final MessageKey messageKey;

    public InvalidTimeOperationException(final MessageKey messageKey, final String reason) {
        super(reason);
        this.messageKey = messageKey;
    }

    /**
     * Returns the message key describing this failure to the player.
     */
    public MessageKey messageKey() {
        return messageKey;
    }
}
