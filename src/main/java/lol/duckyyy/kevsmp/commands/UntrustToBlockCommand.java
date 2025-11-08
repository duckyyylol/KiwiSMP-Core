package lol.duckyyy.kevsmp.commands;

import lol.duckyyy.kevsmp.KevSmp;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UntrustToBlockCommand implements CommandExecutor {
    private final KevSmp plugin;
    public UntrustToBlockCommand(KevSmp plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if(sender instanceof Player) {

            Player player = (Player) sender;

            Block lookingAt = player.getTargetBlockExact(5, FluidCollisionMode.NEVER);

            if(lookingAt == null || lookingAt.isEmpty() || lookingAt.getType().equals(Material.AIR)) {
                // not looking at a block OR not a lockable block
                player.sendMessage(KevSmp.componentWithPrefix(Component.text("Please type the command while looking at a lockable block.").color(NamedTextColor.RED)));
                return true;
            } else if (!this.plugin.CLM.getLockableBlocks().contains(lookingAt.getType())) {
                player.sendMessage(KevSmp.componentWithPrefix(Component.text(String.format("%s is not a lockable block.", lookingAt.getType().toString())).color(NamedTextColor.RED)));
                return true;
            } else if(!this.plugin.CLM.isLocked(lookingAt)) {
                // looking at unlocked block
                player.sendMessage(KevSmp.componentWithPrefix(Component.text(String.format("This %s is not locked.", lookingAt.getType().toString())).color(NamedTextColor.RED)));
                return true;
            }

            if(args.length <= 0) {
                // invalid command
                player.sendMessage(KevSmp.componentWithPrefix(Component.text("You must provide a player to distrust from this block.").color(NamedTextColor.RED)));
                return true;
            }

            String targetUsername = args[0];
            Player trustee = Bukkit.getPlayer(targetUsername);

            if(this.plugin.CLM.isLocked(lookingAt) && !this.plugin.CLM.ownsChest(lookingAt, player)) {
                // does not own locked block
                OfflinePlayer realOwner = Bukkit.getPlayer(this.plugin.CLM.getChestOwner(lookingAt));
                player.sendMessage(KevSmp.componentWithPrefix(Component.text(String.format("This %s belongs to %s, you can not distrust other players from it.", lookingAt.getType().toString(),
                        realOwner.getName())).color(NamedTextColor.RED)));
                return true;
            }

            if(trustee == null || !trustee.isOnline()) {
                player.sendMessage(KevSmp.componentWithPrefix(Component.text(String.format("%s is not currently online or could not be found.", targetUsername)).color(NamedTextColor.RED)));
                return true;
            }

            if(!this.plugin.CLM.isTrusted(lookingAt, trustee)) {
                player.sendMessage(KevSmp.componentWithPrefix(Component.text(String.format("%s is not currently trusted to this block.", targetUsername)).color(NamedTextColor.RED)));
                return true;
            }

            try {
                this.plugin.CLM.removeTrusted(lookingAt, trustee);
                player.sendMessage(KevSmp.componentWithPrefix(Component.text(String.format("Successfully removed trust for %s from this %s", trustee.getName(), lookingAt.getType().toString())).color(NamedTextColor.GREEN)));
                return true;
            } catch (RuntimeException e) {
                player.sendMessage(KevSmp.componentWithPrefix(Component.text(String.format("Failed to modify this %s", lookingAt.getType().toString())).color(NamedTextColor.RED)));
                return true;
            }

        } else return true;
    }
}
