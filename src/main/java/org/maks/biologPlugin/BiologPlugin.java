package org.maks.biologPlugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.maks.biologPlugin.command.BiologistCommand;
import org.maks.biologPlugin.db.DatabaseManager;
import org.maks.biologPlugin.gui.BiologistAdminGUI;
import org.maks.biologPlugin.gui.BiologistGUIManager;
import org.maks.biologPlugin.listener.CooldownResetItemListener;
import org.maks.biologPlugin.listener.MobDropListener;
import org.maks.biologPlugin.quest.QuestDefinition;
import org.maks.biologPlugin.quest.QuestDefinitionManager;
import org.maks.biologPlugin.quest.QuestManager;
import org.maks.biologPlugin.command.BiologistAdminCommand;

import java.util.List;
import java.util.Map;

public final class BiologPlugin extends JavaPlugin {

    private DatabaseManager databaseManager;
    private QuestDefinitionManager questDefinitionManager;
    private QuestManager questManager;

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
        Map<String, QuestDefinition> questMap = questDefinitionManager.getQuestMap();
        BiologistGUIManager guiManager = new BiologistGUIManager(questManager, questDefinitionManager);
        BiologistAdminGUI adminGUI = new BiologistAdminGUI(questDefinitionManager);

        getServer().getPluginManager().registerEvents(guiManager, this);
        getServer().getPluginManager().registerEvents(adminGUI, this);
        getServer().getPluginManager().registerEvents(new MobDropListener(questManager, questMap), this);

        // cooldown item
        Material mat = Material.matchMaterial(config.getString("cooldown_item.id", "BOWL"));
        String display = ChatColor.translateAlternateColorCodes('&', config.getString("cooldown_item.display", ""));
        List<String> lore = config.getStringList("cooldown_item.lore");
        for (int i = 0; i < lore.size(); i++) {
            lore.set(i, ChatColor.translateAlternateColorCodes('&', lore.get(i)));
        }
        getServer().getPluginManager().registerEvents(new CooldownResetItemListener(questManager, mat, display, lore), this);

        if (getCommand("biolog") != null) {
            getCommand("biolog").setExecutor(new BiologistCommand(guiManager));
        }
        if (getCommand("biologadmin") != null) {
            getCommand("biologadmin").setExecutor(new BiologistAdminCommand(adminGUI, questDefinitionManager));
        }
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }
}
