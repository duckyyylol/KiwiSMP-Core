package lol.duckyyy.kevsmp.commands;

import lol.duckyyy.kevsmp.KevSmp;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ToggleMaintenanceCommand implements CommandExecutor {
    private final KevSmp plugin;

    public ToggleMaintenanceCommand(KevSmp plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if ((sender instanceof Player) && !((Player) sender).hasPermission("kiwismp.*")) {
            // no permissions
            return true;
        }

        if (args.length == 0) {
            if (this.plugin.MaintenanceManager.isMaintenanceModeEnabled()) {
                // disable
                boolean newMode = this.plugin.MaintenanceManager.toggleMaintenance();
                sender.sendMessage(String.format("Toggled maintenance mode %s successfully.", !newMode ? "ON" : "OFF"));

                return true;
            }

            try {
                boolean newMode = this.plugin.MaintenanceManager.toggleMaintenance();
                sender.sendMessage(String.format("Toggled maintenance mode %s successfully.", !newMode ? "ON" : "OFF"));
                this.plugin.MaintenanceManager.kickAllPlayer();

            } catch (RuntimeException e) {
                sender.sendMessage("Failed to toggle maintenance mode.");
            }
        } else if(args.length == 1 && args[0].equalsIgnoreCase("warning")){
            Title maintenanceEnablingTitle = Title.title(MiniMessage.miniMessage().deserialize("<gradient:#6ADA50:#52D94B><b>Maintenance Mode"), MiniMessage.miniMessage().deserialize("<gray>You"
                    + " will be kicked from " + "the server shortly."));

            Bukkit.getOnlinePlayers().forEach(p -> {
                p.sendMessage(Component.empty().append(maintenanceEnablingTitle.title()).appendNewline().append(maintenanceEnablingTitle.subtitle()).appendSpace().append(MiniMessage.miniMessage().deserialize("<gray>Please keep an eye on the Kiwi Squad Discord for updates regarding this maintenance period.")));
            });

            Bukkit.getServer().getOnlinePlayers().forEach(p -> p.showTitle(maintenanceEnablingTitle));

            return true;
        } else if (args[0].equalsIgnoreCase("whitelist")) {
            if (args.length == 1 || !java.util.List.of("add", "remove").contains(args[1].toLowerCase())) {
                sender.sendMessage(KevSmp.componentWithPrefix(Component.empty().append(MiniMessage.miniMessage().deserialize("<red>Correct Usage: <gray>/maintenance whitelist [add/remove] username"))));
                return true;
            }

            if (args.length == 2) {
                sender.sendMessage(KevSmp.componentWithPrefix(Component.empty().append(MiniMessage.miniMessage().deserialize("<red>Please provide a player to add to the maintenance whitelist"))));
                return true;
            }

            if (args[1].equalsIgnoreCase("add")) {

                String targetUsername = args[2];
                Player player = Bukkit.getPlayer(targetUsername);
                if (player == null) {
                    sender.sendMessage(KevSmp.componentWithPrefix(Component.empty().append(MiniMessage.miniMessage().deserialize(String.format("<red>Could not add %s to the whitelist",
                            targetUsername)))));
                    return true;
                }

                boolean added = this.plugin.MaintenanceManager.addWhitelist(player.getUniqueId());
                if (!added) {
                    sender.sendMessage(KevSmp.componentWithPrefix(Component.empty().append(MiniMessage.miniMessage().deserialize(String.format("<red>Could not add %s to the whitelist",
                            targetUsername)))));
                } else {
                    sender.sendMessage(KevSmp.componentWithPrefix(Component.empty().append(MiniMessage.miniMessage().deserialize(String.format("Successfully added %s to the whitelist",
                            targetUsername)))));
                }
                return true;

            } else if(args[1].equalsIgnoreCase("remove")) {
                String targetUsername = args[2];
                Player player = Bukkit.getPlayer(targetUsername);
                if (player == null) {
                    sender.sendMessage(KevSmp.componentWithPrefix(Component.empty().append(MiniMessage.miniMessage().deserialize(String.format("<red>Could not remove %s from the whitelist",
                            targetUsername)))));
                    return true;
                }

                boolean removed = this.plugin.MaintenanceManager.removeWhitelist(player.getUniqueId());
                if (!removed) {
                    sender.sendMessage(KevSmp.componentWithPrefix(Component.empty().append(MiniMessage.miniMessage().deserialize(String.format("<red>Could not remove %s from the whitelist",
                            targetUsername)))));
                } else {
                    sender.sendMessage(KevSmp.componentWithPrefix(Component.empty().append(MiniMessage.miniMessage().deserialize(String.format("Successfully removed %s from the whitelist",
                            targetUsername)))));
                }
                return true;
            } else {
                sender.sendMessage(KevSmp.componentWithPrefix(Component.empty().append(MiniMessage.miniMessage().deserialize("<red>Command failed. Ensure you are using it correctly."))));
                return true;
            }
        }

        return true;
    }
}
