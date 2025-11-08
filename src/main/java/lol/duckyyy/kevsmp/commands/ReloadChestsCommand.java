package lol.duckyyy.kevsmp.commands;

import lol.duckyyy.kevsmp.KevSmp;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadChestsCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        KevSmp.reloadChestsInWorld(KevSmp.chestConfig);

        return true;
    }
}
