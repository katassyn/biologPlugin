package org.maks.biologPlugin.quest;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.maks.biologPlugin.db.DatabaseManager;

import java.sql.*;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

public class QuestManager {
    private final DatabaseManager databaseManager;
    private final JavaPlugin plugin;

    public QuestManager(JavaPlugin plugin, DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.plugin = plugin;
        createTables();
    }

    private void createTables() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection();
                 Statement st = conn.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS biologist_players (uuid VARCHAR(36) PRIMARY KEY, quest VARCHAR(64), progress INT, last_submission BIGINT, accepted TINYINT(1))");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS biologist_rewards (quest VARCHAR(64) PRIMARY KEY, items TEXT)");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private PlayerData loadData(UUID uuid) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT quest, progress, last_submission, accepted FROM biologist_players WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String quest = rs.getString("quest");
                int progress = rs.getInt("progress");
                long last = rs.getLong("last_submission");
                boolean accepted = rs.getBoolean("accepted");
                return new PlayerData(uuid, quest, progress, last, accepted);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new PlayerData(uuid, null, 0, 0, false);
    }

    public void getData(Player player, Consumer<PlayerData> callback) {
        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = loadData(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(data));
        });
    }

    public void saveData(PlayerData data) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("REPLACE INTO biologist_players (uuid, quest, progress, last_submission, accepted) VALUES (?, ?, ?, ?, ?)");
            ) {
                ps.setString(1, data.getUuid().toString());
                ps.setString(2, data.getQuestId());
                ps.setInt(3, data.getProgress());
                ps.setLong(4, data.getLastSubmission());
                ps.setBoolean(5, data.isAccepted());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public boolean canSubmit(PlayerData data) {
        long now = Instant.now().toEpochMilli();
        return now - data.getLastSubmission() >= 24 * 60 * 60 * 1000L;
    }

    public static class PlayerData {
        private final UUID uuid;
        private String questId;
        private int progress;
        private long lastSubmission;
        private boolean accepted;

        public PlayerData(UUID uuid, String questId, int progress, long lastSubmission, boolean accepted) {
            this.uuid = uuid;
            this.questId = questId;
            this.progress = progress;
            this.lastSubmission = lastSubmission;
            this.accepted = accepted;
        }

        public UUID getUuid() {
            return uuid;
        }

        public String getQuestId() {
            return questId;
        }

        public void setQuestId(String questId) {
            this.questId = questId;
        }

        public int getProgress() {
            return progress;
        }

        public void setProgress(int progress) {
            this.progress = progress;
        }

        public long getLastSubmission() {
            return lastSubmission;
        }

        public void setLastSubmission(long lastSubmission) {
            this.lastSubmission = lastSubmission;
        }

        public boolean isAccepted() {
            return accepted;
        }

        public void setAccepted(boolean accepted) {
            this.accepted = accepted;
        }
    }
}
