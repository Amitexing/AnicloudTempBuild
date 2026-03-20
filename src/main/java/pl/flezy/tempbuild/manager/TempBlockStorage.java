package pl.flezy.tempbuild.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import pl.flezy.tempbuild.TempBuild;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TempBlockStorage {
    private final TempBuild plugin;
    private Connection connection;

    public TempBlockStorage(TempBuild plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Cannot create plugin data folder for sqlite storage.");
                return;
            }

            File dbFile = new File(plugin.getDataFolder(), "tempblocks.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS temp_blocks (
                        world_uuid TEXT NOT NULL,
                        x INTEGER NOT NULL,
                        y INTEGER NOT NULL,
                        z INTEGER NOT NULL,
                        block_data TEXT NOT NULL,
                        place_time_ms INTEGER NOT NULL,
                        decay_duration_ms INTEGER NOT NULL,
                        PRIMARY KEY (world_uuid, x, y, z)
                    )
                """);
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to initialize sqlite storage: " + exception.getMessage());
        }
    }

    public void upsert(Location location, BlockData blockData, long placeTimeMs, long decayDurationMs) {
        if (connection == null || location.getWorld() == null) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO temp_blocks(world_uuid, x, y, z, block_data, place_time_ms, decay_duration_ms)
            VALUES(?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(world_uuid, x, y, z) DO UPDATE SET
                block_data = excluded.block_data,
                place_time_ms = excluded.place_time_ms,
                decay_duration_ms = excluded.decay_duration_ms
        """)) {
            statement.setString(1, location.getWorld().getUID().toString());
            statement.setInt(2, location.getBlockX());
            statement.setInt(3, location.getBlockY());
            statement.setInt(4, location.getBlockZ());
            statement.setString(5, blockData.getAsString());
            statement.setLong(6, placeTimeMs);
            statement.setLong(7, decayDurationMs);
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().warning("Failed to upsert temp block: " + exception.getMessage());
        }
    }

    public void delete(Location location) {
        if (connection == null || location.getWorld() == null) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement("""
            DELETE FROM temp_blocks WHERE world_uuid = ? AND x = ? AND y = ? AND z = ?
        """)) {
            statement.setString(1, location.getWorld().getUID().toString());
            statement.setInt(2, location.getBlockX());
            statement.setInt(3, location.getBlockY());
            statement.setInt(4, location.getBlockZ());
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().warning("Failed to delete temp block: " + exception.getMessage());
        }
    }

    public List<StoredTempBlock> loadAll() {
        List<StoredTempBlock> records = new ArrayList<>();
        if (connection == null) {
            return records;
        }

        try (PreparedStatement statement = connection.prepareStatement("""
            SELECT world_uuid, x, y, z, block_data, place_time_ms, decay_duration_ms FROM temp_blocks
        """); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                UUID worldUuid = UUID.fromString(resultSet.getString("world_uuid"));
                World world = Bukkit.getWorld(worldUuid);
                if (world == null) {
                    continue;
                }

                Location location = new Location(
                        world,
                        resultSet.getInt("x"),
                        resultSet.getInt("y"),
                        resultSet.getInt("z")
                );

                BlockData blockData;
                try {
                    blockData = Bukkit.createBlockData(resultSet.getString("block_data"));
                } catch (IllegalArgumentException exception) {
                    plugin.getLogger().warning("Skipping invalid persisted block data at " + location);
                    continue;
                }

                long placeTime = resultSet.getLong("place_time_ms");
                long decayDuration = resultSet.getLong("decay_duration_ms");

                records.add(new StoredTempBlock(location, blockData, placeTime, decayDuration));
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Failed to load temp blocks from sqlite: " + exception.getMessage());
        }

        return records;
    }

    public void close() {
        if (connection == null) {
            return;
        }

        try {
            connection.close();
        } catch (SQLException exception) {
            plugin.getLogger().warning("Failed to close sqlite connection: " + exception.getMessage());
        }
    }

    public record StoredTempBlock(Location location, BlockData blockData, long placeTimeMs, long decayDurationMs) {}
}
