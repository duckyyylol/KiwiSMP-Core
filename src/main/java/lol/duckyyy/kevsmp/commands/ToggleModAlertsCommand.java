package lol.duckyyy.kevsmp.commands;

import lol.duckyyy.kevsmp.KevSmp;
import lol.duckyyy.kevsmp.events.PlayerConfigurationBooleanUpdateEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public class ToggleModAlertsCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if(sender instanceof Player) {
            boolean curValue = KevSmp.dataStorage.getBoolean(((Player) sender).getUniqueId() + ".show_mod_alerts");

        Event ev = new PlayerConfigurationBooleanUpdateEvent(((Player) sender).getUniqueId() + ".show_mod_alerts", curValue);
        ev.callEvent();

            ((Player) sender).sendMessage(KevSmp.componentWithPrefix(Component.textOfChildren(MiniMessage.miniMessage().deserialize("Toggled mod alerts <b>" + (!curValue ? "on" : "off")))));

        return true;

        } else return false;
    }
}
