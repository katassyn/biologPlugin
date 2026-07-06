package org.maks.biologPlugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.maks.biologPlugin.quest.QuestDefinition;
import org.maks.biologPlugin.quest.QuestDefinitionManager;
import org.maks.biologPlugin.quest.QuestManager;
import org.maks.biologPlugin.buff.BuffManager;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class BiologistGUIManager implements Listener {
    private final QuestManager questManager;
    private final QuestDefinitionManager questDefinitionManager;
    private final BuffManager buffManager;
    private final FileConfiguration config;
    private final Random random = new Random();

    public BiologistGUIManager(QuestManager questManager, QuestDefinitionManager questDefinitionManager, BuffManager buffManager, FileConfiguration config) {
        this.questManager = questManager;
        this.questDefinitionManager = questDefinitionManager;
        this.buffManager = buffManager;
        this.config = config;
    }

    public void open(Player player) {
        questManager.getData(player, data -> {
            if (data.getQuestId() == null) {
                // Check if player has completed all quests (questId is null and they have progress)
                if (data.getProgress() > 0 || data.getLastSubmission() > 0) {
                    player.sendMessage(ChatColor.GOLD + "Congratulations! You have completed all available quests from the Biologist.");
                    player.sendMessage(ChatColor.YELLOW + "Check back later for new quests!");
                    return;
                }
                
                // New player - assign first quest
                QuestDefinition first = questDefinitionManager.getFirstQuest();
                if (first == null) {
                    player.sendMessage(ChatColor.RED + "No quests configured.");
                    return;
                }
                data.setQuestId(first.getId());
                data.setProgress(0);
                data.setAccepted(false);
                questManager.saveData(data);
            }
            if (data.isAccepted()) {
                openProgress(player, data);
            } else {
                openAccept(player, data);
            }
        });
    }

    private void openAccept(Player player, QuestManager.PlayerData data) {
        QuestDefinition quest = questDefinitionManager.getQuest(data.getQuestId());
        if (quest == null) {
            player.sendMessage(ChatColor.RED + "Quest not found.");
            return;
        }
        Inventory inv = Bukkit.createInventory(player, 27, ChatColor.DARK_GREEN + "Biologist - Accept");
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        fm.setDisplayName(" ");
        filler.setItemMeta(fm);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        ItemStack questItem = new ItemStack(Material.BOOK);
        ItemMeta meta = questItem.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + quest.getName());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + quest.getDescription());
        lore.add(ChatColor.AQUA + "Quest Targets:");
        for (String mob : quest.getMobs().values()) {
            lore.add(ChatColor.GRAY + "- " + mob);
        }
        lore.add(ChatColor.YELLOW + "Required: " + quest.getAmount());
        lore.add("");
        lore.add(ChatColor.GOLD + "Mission Completion Bonuses:");
        
        // Get buff information from config
        String buffPath = "quest_buffs." + quest.getId();
        double flatDmg = config.getDouble(buffPath + ".flat_dmg", 0);
        double flatHp = config.getDouble(buffPath + ".flat_hp", 0);
        double multiDmg = config.getDouble(buffPath + ".multi_dmg", 0);
        double multiHp = config.getDouble(buffPath + ".multi_hp", 0);
        
        if (flatDmg > 0) {
            lore.add(ChatColor.GREEN + "➤ +" + (int)flatDmg + " Attack Damage (Permanent)");
        }
        if (flatHp > 0) {
            lore.add(ChatColor.RED + "➤ +" + (int)flatHp + " Max Health (Permanent)");
        }
        if (multiDmg > 0) {
            lore.add(ChatColor.GREEN + "➤ +" + (int)(multiDmg * 100) + "% Damage Multiplier (Permanent)");
        }
        if (multiHp > 0) {
            lore.add(ChatColor.RED + "➤ +" + (int)(multiHp * 100) + "% Health Multiplier (Permanent)");
        }
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        questItem.setItemMeta(meta);
        inv.setItem(13, questItem);

        ItemStack accept = new ItemStack(Material.LIME_WOOL);
        ItemMeta am = accept.getItemMeta();
        am.setDisplayName(ChatColor.GREEN + "Accept");
        accept.setItemMeta(am);
        inv.setItem(22, accept);

        player.openInventory(inv);
    }

    private void openProgress(Player player, QuestManager.PlayerData data) {
        QuestDefinition quest = questDefinitionManager.getQuest(data.getQuestId());
        if (quest == null) {
            player.sendMessage(ChatColor.RED + "Quest not found.");
            return;
        }
        Inventory inv = Bukkit.createInventory(player, 54, ChatColor.DARK_GREEN + "Biologist");
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        fm.setDisplayName(" ");
        filler.setItemMeta(fm);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        ItemStack descItem = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta descMeta = descItem.getItemMeta();
        descMeta.setDisplayName(ChatColor.GOLD + quest.getName());
        descMeta.setLore(Arrays.asList(
                ChatColor.GRAY + quest.getDescription(),
                ChatColor.YELLOW + "Required: " + quest.getAmount()
        ));
        descItem.setItemMeta(descMeta);
        inv.setItem(11, descItem);

        ItemStack progressItem = new ItemStack(Material.COMPASS);
        ItemMeta progMeta = progressItem.getItemMeta();
        progMeta.setDisplayName(ChatColor.AQUA + "Progress");
        progMeta.setLore(Collections.singletonList(
                ChatColor.YELLOW + "" + data.getProgress() + "/" + quest.getAmount()
        ));
        progressItem.setItemMeta(progMeta);
        inv.setItem(13, progressItem);

        ItemStack targetsItem = new ItemStack(Material.PAPER);
        ItemMeta targetsItemMeta = targetsItem.getItemMeta();
        targetsItemMeta.setDisplayName(ChatColor.GOLD + "Targets:");
        List<String> targetLore = new ArrayList<>();
        for (String mob : quest.getMobs().values()) {
            targetLore.add(ChatColor.GRAY + "- " + mob);
        }
        targetsItemMeta.setLore(targetLore);
        targetsItemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        targetsItem.setItemMeta(targetsItemMeta);
        inv.setItem(15, targetsItem);

        // Cooldown display in left bottom corner
        long remaining = questManager.canSubmit(data) ? 0 : 24 * 60 * 60 * 1000L - (System.currentTimeMillis() - data.getLastSubmission());
        ItemStack cooldownItem;
        ItemMeta cooldownMeta;
        
        if (remaining > 0) {
            // Show red wool with remaining time
            cooldownItem = new ItemStack(Material.RED_WOOL);
            cooldownMeta = cooldownItem.getItemMeta();
            long hours = remaining / (1000 * 60 * 60);
            long minutes = (remaining % (1000 * 60 * 60)) / (1000 * 60);
            cooldownMeta.setDisplayName(ChatColor.RED + "Cooldown");
            cooldownMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Wait " + hours + "h " + minutes + "m"));
        } else {
            // Show green wool when can submit
            cooldownItem = new ItemStack(Material.GREEN_WOOL);
            cooldownMeta = cooldownItem.getItemMeta();
            cooldownMeta.setDisplayName(ChatColor.GREEN + "Ready");
            cooldownMeta.setLore(Collections.singletonList(ChatColor.GRAY + "You can submit items"));
        }
        
        cooldownItem.setItemMeta(cooldownMeta);
        inv.setItem(45, cooldownItem);

        int totalSegments = 7; // Changed from 8 to 7 to make room for cooldown
        int filled = (int) Math.floor(((double) data.getProgress() / quest.getAmount()) * totalSegments);
        for (int i = 0; i < totalSegments; i++) {
            ItemStack segment = new ItemStack(i < filled ? Material.YELLOW_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta sm = segment.getItemMeta();
            sm.setDisplayName(" ");
            segment.setItemMeta(sm);
            inv.setItem(46 + i, segment); // Start from slot 46 instead of 45
        }

        // Add "Quest Rewards >>>" header
        ItemStack rewardsHeader = new ItemStack(Material.GOLD_INGOT);
        ItemMeta headerMeta = rewardsHeader.getItemMeta();
        headerMeta.setDisplayName(ChatColor.GOLD + "Quest Rewards >>>");
        List<String> headerLore = new ArrayList<>();
        headerLore.add(ChatColor.YELLOW + "All rewards for quest completion!");
        headerMeta.setLore(headerLore);
        rewardsHeader.setItemMeta(headerMeta);
        inv.setItem(27, rewardsHeader);
        
        // Add mission completion bonuses info
        ItemStack bonusesInfo = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta bonusesMeta = bonusesInfo.getItemMeta();
        bonusesMeta.setDisplayName(ChatColor.GOLD + "Attribute Bonuses");
        List<String> bonusLore = new ArrayList<>();
        
        // Get buff information from config
        String buffPath = "quest_buffs." + quest.getId();
        double flatDmg = config.getDouble(buffPath + ".flat_dmg", 0);
        double flatHp = config.getDouble(buffPath + ".flat_hp", 0);
        double multiDmg = config.getDouble(buffPath + ".multi_dmg", 0);
        double multiHp = config.getDouble(buffPath + ".multi_hp", 0);
        
        bonusLore.add(ChatColor.YELLOW + " Permanent bonuses:");
        if (flatDmg > 0) {
            bonusLore.add(ChatColor.GREEN + "➤ +" + (int)flatDmg + " Attack Damage");
        }
        if (flatHp > 0) {
            bonusLore.add(ChatColor.RED + "➤ +" + (int)flatHp + " Max Health");
        }
        if (multiDmg > 0) {
            bonusLore.add(ChatColor.GREEN + "➤ +" + (int)(multiDmg * 100) + "% Damage Multiplier");
        }
        if (multiHp > 0) {
            bonusLore.add(ChatColor.RED + "➤ +" + (int)(multiHp * 100) + "% Health Multiplier");
        }
        
        if (flatDmg == 0 && flatHp == 0 && multiDmg == 0 && multiHp == 0) {
            bonusLore.add(ChatColor.GRAY + "No attribute bonuses for this quest");
        } else {
            bonusLore.add("");
            bonusLore.add(ChatColor.YELLOW + "Persist through death and logout!");
        }
        
        bonusesMeta.setLore(bonusLore);
        bonusesMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        bonusesInfo.setItemMeta(bonusesMeta);
        inv.setItem(28, bonusesInfo);
        
        // Add item rewards
        if (quest.getRewards() != null && !quest.getRewards().isEmpty()) {
            int rewardSlot = 29;
            for (ItemStack reward : quest.getRewards()) {
                if (rewardSlot <= 35) {
                    ItemStack guaranteedReward = reward.clone();
//                    // Add lore to show it's guaranteed
//                    ItemMeta rewardMeta = guaranteedReward.getItemMeta();
//                    if (rewardMeta != null) {
//                        List<String> rewardLore = rewardMeta.getLore();
//                        if (rewardLore == null) rewardLore = new ArrayList<>();
//                        rewardLore.add("");
//                        rewardLore.add(ChatColor.GOLD + "GUARANTEED reward!");
//                        rewardMeta.setLore(rewardLore);
//                        guaranteedReward.setItemMeta(rewardMeta);
//                    }
                    inv.setItem(rewardSlot++, guaranteedReward);
                }
            }
        }

        ItemStack submit = new ItemStack(Material.LIME_WOOL);
        ItemMeta sm = submit.getItemMeta();
        sm.setDisplayName(ChatColor.GREEN + "Submit Item");
        submit.setItemMeta(sm);
        inv.setItem(53, submit);

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        String title = e.getView().getTitle();
        if (title.equals(ChatColor.DARK_GREEN + "Biologist - Accept")) {
            e.setCancelled(true);
            if (e.getRawSlot() == 22) {
                questManager.getData(player, data -> {
                    data.setAccepted(true);
                    questManager.saveData(data);
                    openProgress(player, data);
                });
            }
            return;
        }
        if (!title.equals(ChatColor.DARK_GREEN + "Biologist")) return;
        e.setCancelled(true);
        if (e.getRawSlot() != 53) return;
        questManager.getData(player, data -> {
            QuestDefinition quest = questDefinitionManager.getQuest(data.getQuestId());
            if (quest == null || !data.isAccepted()) {
                return;
            }
            // cooldown
            if (!questManager.canSubmit(data)) {
                long remaining = 24 * 60 * 60 * 1000L - (System.currentTimeMillis() - data.getLastSubmission());
                long hours = remaining / (1000 * 60 * 60);
                player.sendMessage(ChatColor.RED + "You must wait " + hours + "h before submitting again.");
                return;
            }
            // build required item
            ItemStack required = new ItemStack(quest.getItemMaterial());
            ItemMeta meta = required.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + quest.getItemName());
            meta.setLore(Collections.singletonList(ChatColor.GRAY + quest.getItemLore()));
            meta.addEnchant(Enchantment.DURABILITY, 10, true);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
            meta.setUnbreakable(true);
            required.setItemMeta(meta);

            if (!player.getInventory().containsAtLeast(required, 1)) {
                player.sendMessage(ChatColor.RED + "You don't have the required item for the Biologist.");
                return;
            }

            player.getInventory().removeItem(required);

            boolean success = random.nextDouble() <= quest.getChance();
            data.setLastSubmission(System.currentTimeMillis());
            if (success) {
                data.setProgress(data.getProgress() + 1);
                player.sendMessage(ChatColor.GREEN + "Item accepted!");
                if (data.getProgress() >= quest.getAmount()) {
                    // Give item rewards
                    if (quest.getRewards() != null) {
                        for (ItemStack reward : quest.getRewards()) {
                            player.getInventory().addItem(reward.clone());
                        }
                    }
                    
                    // Apply quest completion buffs
                    buffManager.addQuestBuff(player, quest.getId());
                    
                    // Send chat notification to all online players
                    String completionMessage = ChatColor.YELLOW + "Player " + ChatColor.GOLD + player.getName() + 
                            ChatColor.YELLOW + " finished Biologist quest " + ChatColor.GOLD + quest.getId() + 
                            ChatColor.YELLOW + ": \"" + ChatColor.WHITE + quest.getName() + ChatColor.YELLOW + "\"";
                    Bukkit.broadcastMessage(completionMessage);
                    
                    QuestDefinition next = questDefinitionManager.getNextQuest(quest.getId());
                    if (next != null) {
                        data.setQuestId(next.getId());
                        data.setProgress(0);
                        data.setAccepted(false);
                        player.sendMessage(ChatColor.GOLD + "Quest completed! Next quest available.");
                        openAccept(player, data);
                        questManager.saveData(data);
                        return;
                    } else {
                        data.setQuestId(null);
                        data.setProgress(0);
                        data.setAccepted(false);
                        player.sendMessage(ChatColor.GOLD + "Quest completed! You finished all quests.");
                        player.closeInventory();
                        questManager.saveData(data);
                        return;
                    }
                }
            } else {
                player.sendMessage(ChatColor.RED + "Biologist rejected your item.");
            }
            questManager.saveData(data);
            openProgress(player, data);
        });
    }
}
