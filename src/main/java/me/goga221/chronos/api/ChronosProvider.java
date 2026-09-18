package me.goga221.chronos.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Entry point for other plugins to obtain the {@link ChronosAPI}.
 * <p>
 * Chronos registers its implementation with Bukkit's {@code ServicesManager}
 * on enable, so consumers only need a soft dependency ({@code softdepend} in
 * their {@code plugin.yml}) rather than a hard compile-time dependency.
 */
public final class ChronosProvider {

    private ChronosProvider() {
    }

    /**
     * Returns the active {@link ChronosAPI} instance.
     *
     * @throws IllegalStateException if Chronos is not installed or not enabled
     */
    public static ChronosAPI get() {
        final RegisteredServiceProvider<ChronosAPI> registration = Bukkit.getServicesManager().getRegistration(ChronosAPI.class);
        if (registration == null) {
            throw new IllegalStateException("Chronos API is not available. Is the Chronos plugin installed and enabled?");
        }
        return registration.getProvider();
    }
}
