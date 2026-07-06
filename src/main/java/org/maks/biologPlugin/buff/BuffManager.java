package org.maks.biologPlugin.buff;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.maks.biologPlugin.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BuffManager implements Listener {
    private final DatabaseManager databaseManager;
    private final JavaPlugin plugin;
    private final FileConfiguration config;
    
    // UUID namespaces for different buff types
    private static final UUID FLAT_DAMAGE_UUID = UUID.fromString("12345678-1234-1234-1234-123456789001");
    private static final UUID FLAT_HP_UUID = UUID.fromString("12345678-1234-1234-1234-123456789002");
    private static final UUID MULTI_DAMAGE_UUID = UUID.fromString("12345678-1234-1234-1234-123456789003");
    private static final UUID MULTI_HP_UUID = UUID.fromString("12345678-1234-1234-1234-123456789004");

    public BuffManager(JavaPlugin plugin, DatabaseManager databaseManager, FileConfiguration config) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.config = config;
    }

    public static class PlayerBuffs {
        private double flatDamage = 0;
        private double flatHp = 0;
        private double multiDamage = 0;
        private double multiHp = 0;
        private Set<String> completedQuests = new HashSet<>();
        
        // Getters and setters
        public double getFlatDamage() { return flatDamage; }
        public void setFlatDamage(double flatDamage) { this.flatDamage = flatDamage; }
        public double getFlatHp() { return flatHp; }
        public void setFlatHp(double flatHp) { this.flatHp = flatHp; }
        public double getMultiDamage() { return multiDamage; }
        public void setMultiDamage(double multiDamage) { this.multiDamage = multiDamage; }
        public double getMultiHp() { return multiHp; }
        public void setMultiHp(double multiHp) { this.multiHp = multiHp; }
        public Set<String> getCompletedQuests() { return completedQuests; }
        public void setCompletedQuests(Set<String> completedQuests) { this.completedQuests = completedQuests; }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            PlayerBuffs buffs = loadPlayerBuffs(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> applyBuffsToPlayer(player, buffs));
        }, 20L); // 1 second delay to ensure player is fully loaded
    }

    public void addQuestBuff(Player player, String questId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerBuffs buffs = loadPlayerBuffs(player.getUniqueId());
            
            // Check if quest already completed
            if (buffs.getCompletedQuests().contains(questId)) {
                return;
            }
            
            // Add quest to completed list
            buffs.getCompletedQuests().add(questId);
            
            // Get buff values from config
            String path = "quest_buffs." + questId;
            double flatDmg = config.getDouble(path + ".flat_dmg", 0);
            double flatHp = config.getDouble(path + ".flat_hp", 0);
            double multiDmg = config.getDouble(path + ".multi_dmg", 0);
            double multiHp = config.getDouble(path + ".multi_hp", 0);
            
            // Add to current buffs
            buffs.setFlatDamage(buffs.getFlatDamage() + flatDmg);
            buffs.setFlatHp(buffs.getFlatHp() + flatHp);
            buffs.setMultiDamage(buffs.getMultiDamage() + multiDmg);
            buffs.setMultiHp(buffs.getMultiHp() + multiHp);
            
            // Save to database
            savePlayerBuffs(player.getUniqueId(), buffs);
            
            // Apply buffs to player
            Bukkit.getScheduler().runTask(plugin, () -> applyBuffsToPlayer(player, buffs));
            
            // Send detailed buff information to player
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(ChatColor.GOLD + "=== Quest Completion Rewards ===");
                if (flatDmg > 0) {
                    player.sendMessage(ChatColor.GREEN + "➤ +" + (int)flatDmg + " Attack Damage (Permanent)");
                }
                if (flatHp > 0) {
                    player.sendMessage(ChatColor.RED + "➤ +" + (int)flatHp + " Max Health (Permanent)");
                }
                if (multiDmg > 0) {
                    player.sendMessage(ChatColor.GREEN + "➤ +" + (int)(multiDmg * 100) + "% Damage Multiplier (Permanent)");
                }
                if (multiHp > 0) {
                    player.sendMessage(ChatColor.RED + "➤ +" + (int)(multiHp * 100) + "% Health Multiplier (Permanent)");
                }
                player.sendMessage(ChatColor.YELLOW + "These bonuses are permanent and will persist through death!");
            });
            
            plugin.getLogger().info("Applied quest buffs for " + player.getName() + " completing " + questId);
        });
    }

    private PlayerBuffs loadPlayerBuffs(UUID playerUuid) {
        PlayerBuffs buffs = new PlayerBuffs();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM biologist_buffs WHERE player = ?")) {
            
            ps.setString(1, playerUuid.toString());
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                buffs.setFlatDamage(rs.getDouble("flat_dmg"));
                buffs.setFlatHp(rs.getDouble("flat_hp"));
                buffs.setMultiDamage(rs.getDouble("multi_dmg"));
                buffs.setMultiHp(rs.getDouble("multi_hp"));
                
                String completedQuestsStr = rs.getString("completed_quests");
                if (completedQuestsStr != null && !completedQuestsStr.isEmpty()) {
                    buffs.setCompletedQuests(new HashSet<>(Arrays.asList(completedQuestsStr.split(","))));
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load player buffs: " + e.getMessage());
        }
        
        return buffs;
    }

    private void savePlayerBuffs(UUID playerUuid, PlayerBuffs buffs) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "REPLACE INTO biologist_buffs (player, flat_dmg, flat_hp, multi_dmg, multi_hp, completed_quests) VALUES (?, ?, ?, ?, ?, ?)")) {
            
            ps.setString(1, playerUuid.toString());
            ps.setDouble(2, buffs.getFlatDamage());
            ps.setDouble(3, buffs.getFlatHp());
            ps.setDouble(4, buffs.getMultiDamage());
            ps.setDouble(5, buffs.getMultiHp());
            ps.setString(6, String.join(",", buffs.getCompletedQuests()));
            
            ps.executeUpdate();
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save player buffs: " + e.getMessage());
        }
    }

    private void applyBuffsToPlayer(Player player, PlayerBuffs buffs) {
        // Remove old modifiers first
        removeAllBuffs(player);
        
        // Apply flat damage buff
        if (buffs.getFlatDamage() > 0) {
            AttributeInstance damageAttr = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (damageAttr != null) {
                AttributeModifier flatDmgMod = new AttributeModifier(
                    FLAT_DAMAGE_UUID, "BiologistFlatDamage", buffs.getFlatDamage(), 
                    AttributeModifier.Operation.ADD_NUMBER
                );
                damageAttr.addModifier(flatDmgMod);
            }
        }
        
        // Apply flat HP buff
        if (buffs.getFlatHp() > 0) {
            AttributeInstance hpAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (hpAttr != null) {
                AttributeModifier flatHpMod = new AttributeModifier(
                    FLAT_HP_UUID, "BiologistFlatHP", buffs.getFlatHp(), 
                    AttributeModifier.Operation.ADD_NUMBER
                );
                hpAttr.addModifier(flatHpMod);
                
                // Heal player to new max HP
                double newMaxHp = hpAttr.getValue();
                if (player.getHealth() < newMaxHp) {
                    player.setHealth(Math.min(newMaxHp, player.getHealth() + buffs.getFlatHp()));
                }
            }
        }
        
        // Apply multiplicative damage buff
        if (buffs.getMultiDamage() > 0) {
            AttributeInstance damageAttr = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (damageAttr != null) {
                AttributeModifier multiDmgMod = new AttributeModifier(
                    MULTI_DAMAGE_UUID, "BiologistMultiDamage", buffs.getMultiDamage(), 
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1
                );
                damageAttr.addModifier(multiDmgMod);
            }
        }
        
        // Apply multiplicative HP buff
        if (buffs.getMultiHp() > 0) {
            AttributeInstance hpAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (hpAttr != null) {
                AttributeModifier multiHpMod = new AttributeModifier(
                    MULTI_HP_UUID, "BiologistMultiHP", buffs.getMultiHp(), 
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1
                );
                hpAttr.addModifier(multiHpMod);
                
                // Heal player proportionally
                double newMaxHp = hpAttr.getValue();
                player.setHealth(Math.min(newMaxHp, player.getHealth() * (1.0 + buffs.getMultiHp())));
            }
        }
    }

    private void removeAllBuffs(Player player) {
        AttributeInstance damageAttr = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (damageAttr != null) {
            // Remove modifiers by finding them with the UUID
            damageAttr.getModifiers().stream()
                .filter(mod -> mod.getUniqueId().equals(FLAT_DAMAGE_UUID) || mod.getUniqueId().equals(MULTI_DAMAGE_UUID))
                .forEach(damageAttr::removeModifier);
        }
        
        AttributeInstance hpAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (hpAttr != null) {
            // Remove modifiers by finding them with the UUID
            hpAttr.getModifiers().stream()
                .filter(mod -> mod.getUniqueId().equals(FLAT_HP_UUID) || mod.getUniqueId().equals(MULTI_HP_UUID))
                .forEach(hpAttr::removeModifier);
        }
    }
}