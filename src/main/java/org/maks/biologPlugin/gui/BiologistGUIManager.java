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

import java.util.*;

public class BiologistGUIManager implements Listener {
    private final QuestManager questManager;
    private final QuestDefinitionManager questDefinitionManager;
    private final Random random = new Random();

    public BiologistGUIManager(QuestManager questManager, QuestDefinitionManager questDefinitionManager) {
        this.questManager = questManager;
        this.questDefinitionManager = questDefinitionManager;
    }

    public void open(Player player) {
        questManager.getData(player, data -> {
            if (data.getQuestId() == null) {
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
        lore.add(ChatColor.AQUA + "Mobs:");
        for (String mob : quest.getMobs().values()) {
            lore.add(ChatColor.GRAY + "- " + mob);
        }
        lore.add(ChatColor.GREEN + "Click to accept");
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
        Inventory inv = Bukkit.createInventory(player, 27, ChatColor.DARK_GREEN + "Biologist");
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
        lore.add(ChatColor.YELLOW + "Progress: " + data.getProgress() + "/" + quest.getAmount());
        lore.add(ChatColor.AQUA + "Mobs:");
        for (String mob : quest.getMobs().values()) {
            lore.add(ChatColor.GRAY + "- " + mob);
        }
        lore.add(ChatColor.GREEN + "Submit item in slot 22");
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        questItem.setItemMeta(meta);
        inv.setItem(13, questItem);

        inv.setItem(22, new ItemStack(Material.AIR));
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
        if (e.getRawSlot() != 22) {
            e.setCancelled(true);
            return;
        }
        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) {
            e.setCancelled(true);
            return;
        }
        e.setCancelled(true);
        e.setCurrentItem(null);
        questManager.getData(player, data -> {
            QuestDefinition quest = questDefinitionManager.getQuest(data.getQuestId());
            if (quest == null || !data.isAccepted()) {
                player.getInventory().addItem(item);
                return;
            }
            // cooldown
            if (!questManager.canSubmit(data)) {
                long remaining = 24 * 60 * 60 * 1000L - (System.currentTimeMillis() - data.getLastSubmission());
                long hours = remaining / (1000 * 60 * 60);
                player.sendMessage(ChatColor.RED + "You must wait " + hours + "h before submitting again.");
                player.getInventory().addItem(item);
                return;
            }
            // validation
            ItemMeta meta = item.getItemMeta();
            if (item.getType() != Material.PAPER || meta == null ||
                    !meta.hasDisplayName() || !meta.getDisplayName().equals(ChatColor.GOLD + quest.getItemName()) ||
                    meta.getLore() == null || meta.getLore().isEmpty() || !meta.getLore().get(0).equals(ChatColor.GRAY + quest.getItemLore()) ||
                    !meta.hasEnchant(Enchantment.DURABILITY) || meta.getEnchantLevel(Enchantment.DURABILITY) < 10) {
                player.sendMessage(ChatColor.RED + "This item is not suitable for the Biologist.");
                player.getInventory().addItem(item);
                return;
            }
            boolean success = random.nextDouble() <= quest.getChance();
            data.setLastSubmission(System.currentTimeMillis());
            if (success) {
                data.setProgress(data.getProgress() + 1);
                player.sendMessage(ChatColor.GREEN + "Item accepted!");
                if (data.getProgress() >= quest.getAmount()) {
                    if (quest.getRewards() != null) {
                        for (ItemStack reward : quest.getRewards()) {
                            player.getInventory().addItem(reward.clone());
                        }
                    }
                    QuestDefinition next = questDefinitionManager.getNextQuest(quest.getId());
                    if (next != null) {
                        data.setQuestId(next.getId());
                        data.setProgress(0);
                        data.setAccepted(false);
                        player.sendMessage(ChatColor.GOLD + "Quest completed! Next quest available.");
                        openAccept(player, data);
                    } else {
                        data.setQuestId(null);
                        data.setProgress(0);
                        data.setAccepted(false);
                        player.sendMessage(ChatColor.GOLD + "Quest completed! You finished all quests.");
                        player.closeInventory();
                    }
                }
            } else {
                player.sendMessage(ChatColor.RED + "Biologist rejected your item.");
            }
            questManager.saveData(data);
        });
    }
}
