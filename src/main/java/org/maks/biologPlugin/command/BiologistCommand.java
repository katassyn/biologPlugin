package org.maks.biologPlugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.maks.biologPlugin.gui.BiologistGUIManager;

public class BiologistCommand implements CommandExecutor {
    private final BiologistGUIManager guiManager;

    public BiologistCommand(BiologistGUIManager guiManager) {
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        guiManager.open(player);
        return true;
    }
}
