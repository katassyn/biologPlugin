package io.lumine.mythic.bukkit.events;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Minimal stub for compilation. Real implementation provided by MythicMobs plugin.
 */
public class MythicMobDeathEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    public String getMobType() { return null; }

    public LivingEntity getEntity() { return null; }

    public Player getKiller() { return null; }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
