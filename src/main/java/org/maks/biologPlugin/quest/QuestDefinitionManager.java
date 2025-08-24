package org.maks.biologPlugin.quest;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.maks.biologPlugin.db.DatabaseManager;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class QuestDefinitionManager {
    private final Map<String, QuestDefinition> quests = new HashMap<>();
    private final DatabaseManager databaseManager;
    private final Gson gson = new Gson();

    public QuestDefinitionManager(DatabaseManager databaseManager, FileConfiguration config) {
        this.databaseManager = databaseManager;
        loadQuests(config);
        loadRewards();
    }

    private void loadQuests(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("quests");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            ConfigurationSection qSec = section.getConfigurationSection(id);
            if (qSec == null) continue;
            String name = qSec.getString("name", id);
            String desc = qSec.getString("desc", "");
            Map<String, String> mobs = new HashMap<>();
            List<String> mobList = qSec.getStringList("mobs");
            for (String line : mobList) {
                String[] split = line.split(":");
                if (split.length == 2) {
                    mobs.put(split[0], split[1]);
                }
            }
            double chance = qSec.getDouble("chance");
            int amount = qSec.getInt("amount");
            String itemName = qSec.getString("item_name", "");
            String itemLore = qSec.getString("item_lore", "");
            QuestDefinition quest = new QuestDefinition(id, name, desc, mobs, chance, amount, itemName, itemLore);
            quests.put(id, quest);
        }
    }

    private void loadRewards() {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT quest, items FROM biologist_rewards")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String questId = rs.getString("quest");
                String json = rs.getString("items");
                QuestDefinition quest = quests.get(questId);
                if (quest != null) {
                    Type type = new TypeToken<List<ItemStack>>(){}.getType();
                    List<ItemStack> items = gson.fromJson(json, type);
                    quest.setRewards(items);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public QuestDefinition getQuest(String id) {
        return quests.get(id);
    }

    public Collection<QuestDefinition> getQuests() {
        return quests.values();
    }

    public Map<String, QuestDefinition> getQuestMap() {
        return quests;
    }

    public void saveRewards(QuestDefinition quest) {
        if (quest.getRewards() == null) return;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("REPLACE INTO biologist_rewards (quest, items) VALUES (?, ?)");
        ) {
            Type type = new TypeToken<List<ItemStack>>(){}.getType();
            String json = gson.toJson(quest.getRewards(), type);
            ps.setString(1, quest.getId());
            ps.setString(2, json);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
