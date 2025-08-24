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

import org.bukkit.enchantments.Enchantment;

import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

public class MobDropListener implements Listener {
    private final QuestManager questManager;
    private final Map<String, QuestDefinition> quests;
    private final Random random = new Random();
    private final boolean debug;
    private final Logger logger;

    public MobDropListener(QuestManager questManager, Map<String, QuestDefinition> quests, boolean debug, Logger logger) {
        this.questManager = questManager;
        this.quests = quests;
        this.debug = debug;
        this.logger = logger;
    }

    @EventHandler
    public void onMythicDeath(MythicMobDeathEvent e) {
        Player killer = e.getKiller();
        if (killer == null) return;
        questManager.getData(killer, data -> {
            QuestDefinition quest = quests.get(data.getQuestId());
            if (quest == null || !data.isAccepted()) return;
            if (!quest.getMobs().containsKey(e.getMobType())) return;

            boolean drop = random.nextDouble() <= quest.getDropChance();
            if (debug) {
                logger.info("Biologist drop roll for mob " + e.getMobType() + " killed by " + killer.getName() +
                        (drop ? " succeeded" : " failed"));
            }
            if (!drop) return;

            ItemStack item = new ItemStack(quest.getItemMaterial());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + quest.getItemName());
            meta.setLore(Collections.singletonList(ChatColor.GRAY + quest.getItemLore()));
            meta.addEnchant(Enchantment.DURABILITY, 10, true);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
            e.getEntity().getWorld().dropItem(e.getEntity().getLocation(), item);
        });

    }
}
