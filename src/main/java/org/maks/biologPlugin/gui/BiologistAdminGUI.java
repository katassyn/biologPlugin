package org.maks.biologPlugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.maks.biologPlugin.quest.QuestDefinition;
import org.maks.biologPlugin.quest.QuestDefinitionManager;

import java.util.ArrayList;
import java.util.List;

public class BiologistAdminGUI implements Listener {
    private final QuestDefinitionManager questDefinitionManager;

    public BiologistAdminGUI(QuestDefinitionManager questDefinitionManager) {
        this.questDefinitionManager = questDefinitionManager;
    }

    public void open(Player player, QuestDefinition quest) {
        Inventory inv = Bukkit.createInventory(player, 27, ChatColor.DARK_AQUA + "Edit Rewards:" + quest.getId());
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        fm.setDisplayName(" ");
        filler.setItemMeta(fm);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
        // Slots 10-16 allow items
        if (quest.getRewards() != null) {
            int slot = 10;
            for (ItemStack item : quest.getRewards()) {
                inv.setItem(slot++, item.clone());
            }
        }
        inv.setItem(18, createButton(Material.LIME_WOOL, ChatColor.GREEN + "Save"));
        inv.setItem(26, createButton(Material.RED_WOOL, ChatColor.RED + "Cancel"));
        player.openInventory(inv);
    }

    private ItemStack createButton(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();
        if (!title.startsWith(ChatColor.DARK_AQUA + "Edit Rewards:")) return;
        e.setCancelled(true);
        Inventory inv = e.getInventory();
        if (e.getRawSlot() >= 10 && e.getRawSlot() <= 16) {
            e.setCancelled(false); // allow editing
            return;
        }
        QuestDefinition quest = questDefinitionManager.getQuest(title.substring((ChatColor.DARK_AQUA + "Edit Rewards:").length()));
        if (quest == null) return;
        if (e.getRawSlot() == 18) { // save
            List<ItemStack> items = new ArrayList<>();
            for (int slot = 10; slot <= 16; slot++) {
                ItemStack item = inv.getItem(slot);
                if (item != null && item.getType() != Material.AIR) {
                    items.add(item.clone());
                }
            }
            quest.setRewards(items);
            questDefinitionManager.saveRewards(quest);
            player.sendMessage(ChatColor.GREEN + "Rewards saved.");
            player.closeInventory();
        } else if (e.getRawSlot() == 26) {
            player.closeInventory();
        }
    }
}
