package org.maks.biologPlugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.maks.biologPlugin.gui.BiologistAdminGUI;
import org.maks.biologPlugin.quest.QuestDefinition;
import org.maks.biologPlugin.quest.QuestDefinitionManager;

public class BiologistAdminCommand implements CommandExecutor {
    private final BiologistAdminGUI adminGUI;
    private final QuestDefinitionManager questDefinitionManager;

    public BiologistAdminCommand(BiologistAdminGUI adminGUI, QuestDefinitionManager questDefinitionManager) {
        this.adminGUI = adminGUI;
        this.questDefinitionManager = questDefinitionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage("Usage: /biologadmin <questId>");
            return true;
        }
        QuestDefinition quest = questDefinitionManager.getQuest(args[0]);
        if (quest == null) {
            sender.sendMessage("Quest not found.");
            return true;
        }
        adminGUI.open(player, quest);
        return true;
    }
}
