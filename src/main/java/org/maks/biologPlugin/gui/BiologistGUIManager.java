package org.maks.biologPlugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.maks.biologPlugin.quest.QuestDefinition;
import org.maks.biologPlugin.quest.QuestManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BiologistGUIManager implements Listener {
    private final QuestManager questManager;
    private final Map<String, QuestDefinition> quests;
    private final Random random = new Random();

    public BiologistGUIManager(QuestManager questManager, Map<String, QuestDefinition> quests) {
        this.questManager = questManager;
        this.quests = quests;
    }

    public void open(Player player) {
        QuestManager.PlayerData data = questManager.getData(player);
        QuestDefinition quest;
        if (data.getQuestId() == null) {
            quest = quests.values().stream().findFirst().orElse(null);
            if (quest == null) {
                player.sendMessage(ChatColor.RED + "No quests configured.");
                return;
            }
            data.setQuestId(quest.getId());
            questManager.saveData(data);
        } else {
            quest = quests.get(data.getQuestId());
        }
        Inventory inv = Bukkit.createInventory(player, 27, ChatColor.DARK_GREEN + "Biologist");
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        fm.setDisplayName(" ");
        filler.setItemMeta(fm);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        // Quest item in slot 13
        ItemStack questItem = new ItemStack(Material.BOOK);
        ItemMeta meta = questItem.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + quest.getName());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + quest.getDescription());
        lore.add(ChatColor.YELLOW + "Progress: " + data.getProgress() + "/" + quest.getAmount());
        lore.add(ChatColor.GREEN + "Submit item in slot 22");
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        questItem.setItemMeta(meta);
        inv.setItem(13, questItem);

        // Submission slot 22 empty for player to place item
        inv.setItem(22, new ItemStack(Material.AIR));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
        Inventory inv = e.getInventory();
        if (!e.getView().getTitle().equals(ChatColor.DARK_GREEN + "Biologist")) return;
        if (e.getRawSlot() < inv.getSize()) {
            if (e.getRawSlot() != 22) {
                e.setCancelled(true);
            } else {
                // Player attempts to submit item
                ItemStack item = e.getCurrentItem();
                if (item == null || item.getType() == Material.AIR) return;
                QuestManager.PlayerData data = questManager.getData(player);
                QuestDefinition quest = quests.get(data.getQuestId());
                if (quest == null) return;
                boolean success = random.nextDouble() <= quest.getChance();
                data.setLastSubmission(System.currentTimeMillis());
                if (success) {
                    data.setProgress(data.getProgress() + 1);
                    player.sendMessage(ChatColor.GREEN + "Item accepted!" );
                    if (data.getProgress() >= quest.getAmount()) {
                        if (quest.getRewards() != null) {
                            for (ItemStack reward : quest.getRewards()) {
                                player.getInventory().addItem(reward.clone());
                            }
                        }
                        data.setProgress(0);
                        player.sendMessage(ChatColor.GOLD + "Quest completed!");
                    }
                    questManager.saveData(data);
                    e.setCurrentItem(null);
                } else {
                    player.sendMessage(ChatColor.RED + "Biologist rejected your item.");
                    questManager.saveData(data);
                }
                e.setCancelled(true);
            }
        }
    }
}
