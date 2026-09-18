package me.goga221.chronos.storage;

import java.util.Optional;

/**
 * Persists the calendar's current position across server restarts.
 * <p>
 * Implementations must only handle storage; all calendar business logic
 * belongs in the service layer.
 */
public interface TimeRepository {

    /**
     * Loads the last persisted state, if any has ever been saved.
     */
    Optional<PersistedGameTime> load();

    /**
     * Overwrites the persisted state.
     */
    void save(PersistedGameTime state);

    /**
     * Releases any resources held by this repository.
     */
    void close();
}
