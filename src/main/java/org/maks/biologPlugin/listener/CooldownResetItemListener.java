package org.maks.biologPlugin.listener;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.maks.biologPlugin.quest.QuestManager;

/**
 * Listener that handles usage of the Biologist Potion to reset cooldown.
 * Scans for any item with display name "Biologist Potion" (colors stripped).
 */
public class CooldownResetItemListener implements Listener {
    private final QuestManager questManager;

    public CooldownResetItemListener(QuestManager questManager) {
        this.questManager = questManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        ItemStack item = e.getItem();
        if (item == null) return;
        
        // Check display name with stripped colors
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        String itemDisplayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        
        if (!"Biologist Potion".equals(itemDisplayName)) return;

        questManager.getData(e.getPlayer(), data -> {
            // Check if player has active cooldown
            if (questManager.canSubmit(data)) {
                e.getPlayer().sendMessage(ChatColor.RED + "You don't have active cooldown to reset.");
                return;
            }
            
            data.setLastSubmission(0);
            questManager.saveData(data);
            e.getPlayer().sendMessage(ChatColor.GREEN + "Cooldown reset.");
            
            // Only consume item if cooldown was actually reset
            item.setAmount(item.getAmount() - 1);
        });

        e.setCancelled(true);
    }
}
