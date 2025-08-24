package org.maks.biologPlugin.quest;

import org.bukkit.entity.Player;
import org.maks.biologPlugin.db.DatabaseManager;

import java.sql.*;
import java.time.Instant;
import java.util.UUID;

public class QuestManager {
    private final DatabaseManager databaseManager;

    public QuestManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        createTables();
    }

    private void createTables() {
        try (Connection conn = databaseManager.getConnection();
             Statement st = conn.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS biologist_players (uuid VARCHAR(36) PRIMARY KEY, quest VARCHAR(64), progress INT, last_submission BIGINT)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS biologist_rewards (quest VARCHAR(64) PRIMARY KEY, items TEXT)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public PlayerData getData(Player player) {
        UUID uuid = player.getUniqueId();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT quest, progress, last_submission FROM biologist_players WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String quest = rs.getString("quest");
                int progress = rs.getInt("progress");
                long last = rs.getLong("last_submission");
                return new PlayerData(uuid, quest, progress, last);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new PlayerData(uuid, null, 0, 0);
    }

    public void saveData(PlayerData data) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("REPLACE INTO biologist_players (uuid, quest, progress, last_submission) VALUES (?, ?, ?, ?)");
        ) {
            ps.setString(1, data.getUuid().toString());
            ps.setString(2, data.getQuestId());
            ps.setInt(3, data.getProgress());
            ps.setLong(4, data.getLastSubmission());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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

        public PlayerData(UUID uuid, String questId, int progress, long lastSubmission) {
            this.uuid = uuid;
            this.questId = questId;
            this.progress = progress;
            this.lastSubmission = lastSubmission;
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
    }
}
