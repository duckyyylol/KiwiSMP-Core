package lol.duckyyy.kevsmp.commands;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import lol.duckyyy.kevsmp.KevSmp;
import net.kyori.adventure.key.Key;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UnlinkCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if(!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");

            return true;
        }

        Player player = (Player) sender;
        player.showDialog(KevSmp.unlinkConfirmDialog);

        return true;
    }
}
