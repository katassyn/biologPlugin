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
        boolean debugDrop = config.getBoolean("debug_drop", false);
        getServer().getPluginManager().registerEvents(new MobDropListener(questManager, questMap, debugDrop, getLogger()), this);

        // Build cooldown item from config
        ItemStack cooldownItem = buildCooldownItem(config);
        getServer().getPluginManager().registerEvents(new CooldownResetItemListener(questManager, cooldownItem), this);

        if (getCommand("biolog") != null) {
            getCommand("biolog").setExecutor(new BiologistCommand(guiManager));
        }
        if (getCommand("biologadmin") != null) {
            getCommand("biologadmin").setExecutor(new BiologistAdminCommand(adminGUI, questDefinitionManager));
        }
    }

    private ItemStack buildCooldownItem(FileConfiguration config) {
        Material mat = Material.matchMaterial(config.getString("cooldown_item.id", "BOWL"));
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', config.getString("cooldown_item.display", "")));
        List<String> lore = config.getStringList("cooldown_item.lore");
        for (int i = 0; i < lore.size(); i++) {
            lore.set(i, ChatColor.translateAlternateColorCodes('&', lore.get(i)));
        }
        meta.setLore(lore);

        String enchant = config.getString("cooldown_item.enchant", "");
        if (!enchant.isEmpty()) {
            String[] parts = enchant.split(":");
            if (parts.length == 2) {
                Enchantment e = Enchantment.getByName(parts[0]);
                try {
                    int level = Integer.parseInt(parts[1]);
                    if (e != null) {
                        meta.addEnchant(e, level, true);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        ConfigurationSection opts = config.getConfigurationSection("cooldown_item.options");
        if (opts != null) {
            if (opts.getBoolean("HideFlags", false)) {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE,
                        ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_PLACED_ON);
            }
            if (opts.getBoolean("HideAttributes", false)) {
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            }
            if (opts.getBoolean("Unbreakable", false)) {
                meta.setUnbreakable(true);
            }
        }

        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }
}
