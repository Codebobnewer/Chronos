package me.goga221.chronos.config;

/**
 * Thrown when {@code config.yml} cannot be parsed into a valid {@link ChronosConfig}.
 */
public final class ConfigurationException extends RuntimeException {

    public ConfigurationException(final String message) {
        super(message);
    }

    public ConfigurationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
