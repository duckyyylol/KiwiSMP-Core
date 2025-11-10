package lol.duckyyy.kevsmp.commands;

import lol.duckyyy.kevsmp.KevSmp;
import lol.duckyyy.kevsmp.managers.MaintenanceModeManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AnnounceCommand implements CommandExecutor {
    private final KevSmp plugin;
    public AnnounceCommand(KevSmp plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if(sender instanceof Player) {
            Player player = (Player) sender;
            if(!this.plugin.groupHasPermission(this.plugin.getHighestGroup(this.plugin.getUser(player.getUniqueId())), "kiwismp.announce")) {
                return true;
            }

            String announcement = String.join(" ", args);
            if(announcement.isEmpty()) return true;
            Component translated =
                    Component.empty().append(MiniMessage.miniMessage().deserialize("<dark_gray><b><st>――――――――</st></b> <gray>\uE000 Announcement <dark_gray><b><st>――――――――</st>")).appendNewline().append(MiniMessage.miniMessage().deserialize(announcement));

            KevSmp.debugMessage(player, announcement);
            KevSmp.debugMessage(player, translated.toString());

//            Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(translated));

            Bukkit.broadcast(translated, "kiwismp.default");
        } else {
            // console
            String announcement = String.join(" ", args);
            Component translated = Component.empty().append(MiniMessage.miniMessage().deserialize(announcement));

            Bukkit.getServer().getOnlinePlayers().forEach(p -> p.sendMessage(translated));
        }
        return true;
    }
}
