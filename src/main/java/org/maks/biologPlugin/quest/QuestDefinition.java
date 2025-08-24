package org.maks.biologPlugin.quest;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class QuestDefinition {
    private final String id;
    private final String name;
    private final String description;
    private final Map<String, String> mobs; // mythicId -> display name
    private final double chance;
    private final int amount;
    private final Material itemMaterial;
    private final String itemName;
    private final String itemLore;

    private List<ItemStack> rewards;

    public QuestDefinition(String id, String name, String description, Map<String, String> mobs,
                           double chance, int amount, Material itemMaterial, String itemName, String itemLore) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.mobs = mobs;
        this.chance = chance;
        this.amount = amount;
        this.itemMaterial = itemMaterial;
        this.itemName = itemName;
        this.itemLore = itemLore;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, String> getMobs() {
        return mobs;
    }

    public double getChance() {
        return chance;
    }

    public int getAmount() {
        return amount;
    }

    public Material getItemMaterial() { return itemMaterial; }

    public String getItemName() { return itemName; }

    public String getItemLore() { return itemLore; }

    public List<ItemStack> getRewards() {
        return rewards;
    }

    public void setRewards(List<ItemStack> rewards) {
        this.rewards = rewards;
    }
}
