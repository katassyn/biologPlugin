package org.maks.biologPlugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import java.util.Collections;
import java.util.Random;
import org.maks.biologPlugin.command.BiologistCommand;
import org.maks.biologPlugin.db.DatabaseManager;
import org.maks.biologPlugin.gui.BiologistAdminGUI;
import org.maks.biologPlugin.gui.BiologistGUIManager;
import org.maks.biologPlugin.listener.CooldownResetItemListener;
import org.maks.biologPlugin.quest.QuestDefinition;
import org.maks.biologPlugin.quest.QuestDefinitionManager;
import org.maks.biologPlugin.quest.QuestManager;
import org.maks.biologPlugin.command.BiologistAdminCommand;
import org.maks.biologPlugin.buff.BuffManager;

import java.util.List;
import java.util.Map;

public final class BiologPlugin extends JavaPlugin implements Listener {

    private DatabaseManager databaseManager;
    private QuestDefinitionManager questDefinitionManager;
    private QuestManager questManager;
    private BuffManager buffManager;
    private final Random random = new Random();
    private boolean debugDrop;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        String host = config.getString("mysql.host", "localhost");
        int port = config.getInt("mysql.port", 3306);
        String database = config.getString("mysql.database", "biologist_db");
        String username = config.getString("mysql.username", "root");
        String password = config.getString("mysql.password", "");
        databaseManager = new DatabaseManager(host, port, database, username, password);

        questManager = new QuestManager(this, databaseManager);
        questDefinitionManager = new QuestDefinitionManager(this, databaseManager, config);
        buffManager = new BuffManager(this, databaseManager, config);
        
        Map<String, QuestDefinition> questMap = questDefinitionManager.getQuestMap();
        BiologistGUIManager guiManager = new BiologistGUIManager(questManager, questDefinitionManager, buffManager);

        BiologistAdminGUI adminGUI = new BiologistAdminGUI(questDefinitionManager);

        getServer().getPluginManager().registerEvents(guiManager, this);
        getServer().getPluginManager().registerEvents(adminGUI, this);
        getServer().getPluginManager().registerEvents(buffManager, this);
        debugDrop = config.getBoolean("debug_drop", false);
        getServer().getPluginManager().registerEvents(this, this);

        // Register cooldown reset listener (no config needed)
        getServer().getPluginManager().registerEvents(new CooldownResetItemListener(questManager), this);

        if (getCommand("biolog") != null) {
            getCommand("biolog").setExecutor(new BiologistCommand(guiManager));
        }
        if (getCommand("biologadmin") != null) {
            getCommand("biologadmin").setExecutor(new BiologistAdminCommand(adminGUI, questDefinitionManager));
        }
    }


    @EventHandler
    public void onMythicMobDeath(MythicMobDeathEvent event) {
        Entity killerEntity = event.getKiller();
        Player killer = null;

        // Check if the killer is a player
        if (killerEntity instanceof Player) {
            killer = (Player) killerEntity;
        } 
        
        // Get mob name using correct MythicMobs API
        final String mobName = event.getMobType().getInternalName();
        
        // Log if no valid killer was found
        if (killer == null) {
            if (debugDrop) {
                getLogger().info("[DEBUG] MythicMob " + mobName + " died but no valid killer (player) was found");
            }
            return;
        }
        
        final Player finalKiller = killer;
        if (debugDrop) {
            getLogger().info("MythicMobDeathEvent fired for mob: " + mobName);
        }

        questManager.getData(finalKiller, data -> {
            if (debugDrop) {
                getLogger().info("Player " + finalKiller.getName() + " has quest: " + data.getQuestId() + " (accepted: " + data.isAccepted() + ")");
            }
            QuestDefinition quest = questDefinitionManager.getQuest(data.getQuestId());
            if (quest == null || !data.isAccepted()) {
                if (debugDrop) {
                    getLogger().info("Quest not found or not accepted");
                }
                return;
            }
            if (debugDrop) {
                getLogger().info("Checking if mob type '" + mobName + "' is in quest mobs: " + quest.getMobs().keySet());
            }
            if (!quest.getMobs().containsKey(mobName)) {
                if (debugDrop) {
                    getLogger().info("Mob type not found in quest requirements");
                }
                return;
            }

            boolean drop = random.nextDouble() <= quest.getDropChance();
            if (debugDrop) {
                getLogger().info("Biologist drop roll for mob " + mobName + " killed by " + finalKiller.getName() +
                        (drop ? " succeeded" : " failed"));
            }
            if (!drop) return;

            ItemStack item = new ItemStack(quest.getItemMaterial());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + quest.getItemName());
            meta.setLore(Collections.singletonList(ChatColor.GRAY + quest.getItemLore()));
            meta.addEnchant(Enchantment.DURABILITY, 10, true);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
            event.getEntity().getWorld().dropItem(event.getEntity().getLocation(), item);
        });
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }
}
