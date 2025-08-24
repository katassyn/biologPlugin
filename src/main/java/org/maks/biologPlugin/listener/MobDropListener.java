package org.maks.biologPlugin.listener;

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.maks.biologPlugin.quest.QuestDefinition;
import org.maks.biologPlugin.quest.QuestManager;

public class MobDropListener implements Listener {
    private final QuestManager questManager;
    private final java.util.Map<String, QuestDefinition> quests;

    public MobDropListener(QuestManager questManager, java.util.Map<String, QuestDefinition> quests) {
        this.questManager = questManager;
        this.quests = quests;
    }

    @EventHandler
    public void onMythicDeath(MythicMobDeathEvent e) {
        Player killer = e.getKiller();
        if (killer == null) return;
        questManager.getData(killer, data -> {
            QuestDefinition quest = quests.get(data.getQuestId());
            if (quest == null || !data.isAccepted()) return;
            if (!quest.getMobs().containsKey(e.getMobType())) return;
            ItemStack item = new ItemStack(quest.getItemMaterial());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + quest.getItemName());
            meta.setLore(java.util.Collections.singletonList(ChatColor.GRAY + quest.getItemLore()));
            meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 10, true);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
            e.getEntity().getWorld().dropItem(e.getEntity().getLocation(), item);
        });

    }
}
