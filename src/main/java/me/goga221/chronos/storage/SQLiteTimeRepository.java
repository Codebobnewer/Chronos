package me.goga221.chronos.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * SQLite-backed {@link TimeRepository}. Stores a single row holding the
 * calendar's current position.
 */
public final class SQLiteTimeRepository implements TimeRepository {

    private static final String TABLE_DDL = """
            CREATE TABLE IF NOT EXISTS chronos_time (
                id INTEGER PRIMARY KEY CHECK (id = 0),
                total_game_seconds INTEGER NOT NULL,
                paused INTEGER NOT NULL DEFAULT 0
            )
            """;
    private static final String SELECT_SQL = "SELECT total_game_seconds, paused FROM chronos_time WHERE id = 0";
    private static final String UPSERT_SQL = "INSERT OR REPLACE INTO chronos_time (id, total_game_seconds, paused) VALUES (0, ?, ?)";

    private final HikariDataSource dataSource;

    public SQLiteTimeRepository(final File databaseFile) {
        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        hikariConfig.setMaximumPoolSize(1);
        hikariConfig.setPoolName("chronos-sqlite");
        this.dataSource = new HikariDataSource(hikariConfig);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(TABLE_DDL);
        } catch (final SQLException exception) {
            dataSource.close();
            throw new IllegalStateException("Failed to initialize Chronos database", exception);
        }
    }

    @Override
    public Optional<PersistedGameTime> load() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(SELECT_SQL)) {
            if (!resultSet.next()) {
                return Optional.empty();
            }
            return Optional.of(new PersistedGameTime(resultSet.getLong("total_game_seconds"), resultSet.getInt("paused") != 0));
        } catch (final SQLException exception) {
            throw new IllegalStateException("Failed to load Chronos time state", exception);
        }
    }

    @Override
    public void save(final PersistedGameTime state) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPSERT_SQL)) {
            statement.setLong(1, state.totalGameSeconds());
            statement.setInt(2, state.paused() ? 1 : 0);
            statement.executeUpdate();
        } catch (final SQLException exception) {
            throw new IllegalStateException("Failed to save Chronos time state", exception);
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
