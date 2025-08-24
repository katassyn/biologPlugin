package org.maks.biologPlugin.listener;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.maks.biologPlugin.quest.QuestManager;

import java.util.List;

public class CooldownResetItemListener implements Listener {
    private final QuestManager questManager;
    private final Material material;
    private final String display;
    private final List<String> lore;

    public CooldownResetItemListener(QuestManager questManager, Material material, String display, List<String> lore) {
        this.questManager = questManager;
        this.material = material;
        this.display = display;
        this.lore = lore;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        ItemStack item = e.getItem();
        if (item == null || item.getType() != material) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !display.equals(meta.getDisplayName())) return;
        if (lore != null && !lore.equals(meta.getLore())) return;
        questManager.getData(e.getPlayer(), data -> {
            data.setLastSubmission(0);
            questManager.saveData(data);
            e.getPlayer().sendMessage(ChatColor.GREEN + "Cooldown reset.");
        });
        item.setAmount(item.getAmount() - 1);
        e.setCancelled(true);
    }
}
