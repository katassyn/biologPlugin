package org.maks.biologPlugin.quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.maks.biologPlugin.db.DatabaseManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Base64;

public class QuestDefinitionManager {
    private final LinkedHashMap<String, QuestDefinition> quests = new LinkedHashMap<>();
    private final DatabaseManager databaseManager;
    private final Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();
    private final JavaPlugin plugin;

    public QuestDefinitionManager(JavaPlugin plugin, DatabaseManager databaseManager, FileConfiguration config) {
        this.databaseManager = databaseManager;
        this.plugin = plugin;
        loadQuests(config);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::loadRewards);

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
            double dropChance = qSec.getDouble("drop_chance", 1.0);
            int amount = qSec.getInt("amount");
            Material itemMat = Material.matchMaterial(qSec.getString("item_id", "PAPER"));
            String itemName = qSec.getString("item_name", "");
            String itemLore = qSec.getString("item_lore", "");
            QuestDefinition quest = new QuestDefinition(id, name, desc, mobs, chance, dropChance, amount, itemMat, itemName, itemLore);
            quests.put(id, quest);
        }
    }

    private void loadRewards() {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT quest, items FROM biologist_rewards")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String questId = rs.getString("quest");
                String serializedData = rs.getString("items");
                QuestDefinition quest = quests.get(questId);
                if (quest != null && serializedData != null && !serializedData.isEmpty()) {
                    List<ItemStack> items = deserializeItems(serializedData);
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

    // Serialize ItemStack list to Base64 string
    private String serializeItems(List<ItemStack> items) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            
            dataOutput.writeInt(items.size());
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to serialize rewards: " + e.getMessage());
            return "";
        }
    }

    // Deserialize Base64 string to ItemStack list
    private List<ItemStack> deserializeItems(String data) {
        if (data == null || data.isEmpty()) return new ArrayList<>();
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            
            int size = dataInput.readInt();
            List<ItemStack> items = new ArrayList<>(size);
            
            for (int i = 0; i < size; i++) {
                items.add((ItemStack) dataInput.readObject());
            }
            
            return items;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to deserialize rewards: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public QuestDefinition getFirstQuest() {
        return quests.values().stream().findFirst().orElse(null);
    }

    public QuestDefinition getNextQuest(String currentId) {
        Iterator<String> it = quests.keySet().iterator();
        while (it.hasNext()) {
            String id = it.next();
            if (id.equals(currentId)) {
                return it.hasNext() ? quests.get(it.next()) : null;
            }
        }
        return null;
    }

    public void saveRewards(QuestDefinition quest) {
        if (quest.getRewards() == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("REPLACE INTO biologist_rewards (quest, items) VALUES (?, ?)");
            ) {
                String serializedData = serializeItems(quest.getRewards());
                ps.setString(1, quest.getId());
                ps.setString(2, serializedData);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
