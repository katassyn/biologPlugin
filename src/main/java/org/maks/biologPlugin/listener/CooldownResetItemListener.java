package org.maks.biologPlugin.listener;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.maks.biologPlugin.quest.QuestManager;

/**
 * Listener that handles usage of the special cooldown reset item. The item is
 * fully defined by its {@link ItemStack} including enchantments and item
 * flags, so that only properly configured items are accepted.
 */
public class CooldownResetItemListener implements Listener {
    private final QuestManager questManager;
    private final ItemStack cooldownItem;

    public CooldownResetItemListener(QuestManager questManager, ItemStack cooldownItem) {
        this.questManager = questManager;
        this.cooldownItem = cooldownItem;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        ItemStack item = e.getItem();
        if (item == null || !item.isSimilar(cooldownItem)) return;

        questManager.getData(e.getPlayer(), data -> {
            data.setLastSubmission(0);
            questManager.saveData(data);
            e.getPlayer().sendMessage(ChatColor.GREEN + "Cooldown reset.");
        });

        item.setAmount(item.getAmount() - 1);
        e.setCancelled(true);
    }
}
