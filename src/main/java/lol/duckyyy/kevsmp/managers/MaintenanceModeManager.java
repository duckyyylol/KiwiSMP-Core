package lol.duckyyy.kevsmp.managers;

import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import lol.duckyyy.kevsmp.KevSmp;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.BlockInventoryHolder;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class MaintenanceModeManager implements Listener {
    private final KevSmp plugin;
    private boolean isMaintenanceModeEnabled;
    private final FileConfiguration config;
    private final File dataFile;
    private final Component maintenanceKickMessage =
            Component.empty().append(MiniMessage.miniMessage().deserialize("<b><white>Maintenance Mode is Enabled")).appendNewline().appendNewline().append(MiniMessage.miniMessage().deserialize(
                    "<green>We are currently performing server maintenance. Please check back soon!")).appendNewline().appendNewline().appendNewline().append(MiniMessage.miniMessage().deserialize(
                            "<gray><i>Please stay tuned for updates regarding this maintenance period on the Kiwi Squad Discord server."));
    List<UUID> maintenanceWhitelist;


    public MaintenanceModeManager(KevSmp plugin) {
        this.plugin = plugin;
        File dataFile = new File(plugin.getDataFolder(), "maintenance.yml");
        this.dataFile = dataFile;
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
                this.isMaintenanceModeEnabled = false;
            } catch (IOException e) {
                System.out.println("Failed to load server maintenance configuration");
            }
        } else {
            this.isMaintenanceModeEnabled = YamlConfiguration.loadConfiguration(dataFile).getBoolean("enabled");
        }

        this.config = YamlConfiguration.loadConfiguration(dataFile);

        this.maintenanceWhitelist = new ArrayList<UUID>();
    }

    public boolean isMaintenanceModeEnabled() {
        return this.isMaintenanceModeEnabled;
    }

    public List<UUID> getMaintenanceWhitelist() {
        return new ArrayList<>(((List<String>) Objects.requireNonNull(this.config.get("whitelist"))).stream().map(UUID::fromString).toList());

    }

    public boolean isWhitelisted(UUID uuid) {
        List<UUID> whitelisted = new ArrayList<>(((List<String>) Objects.requireNonNull(this.config.get("whitelist"))).stream().map(UUID::fromString).toList());

        return whitelisted.contains(uuid);
    }

    public FileConfiguration getConfig() {
        return this.config;
    }

    public boolean toggleMaintenance() {
        this.config.set("enabled", !this.isMaintenanceModeEnabled());

        try {
            this.config.save(this.dataFile);
            this.isMaintenanceModeEnabled = !this.isMaintenanceModeEnabled();

            return !this.isMaintenanceModeEnabled();
        } catch (IOException e) {
            return false;
        }
    }

    public boolean addWhitelist(UUID uuid) {
        List<String> configStringList = (List<String>) this.config.get("whitelist");
        if (configStringList == null)
            configStringList = new ArrayList<>();
        List<UUID> whitelisted = new ArrayList<>(configStringList.stream().map(UUID::fromString).toList());
        whitelisted.add(uuid);

        this.config.set("whitelist", whitelisted.stream().map(UUID::toString).toList());

        try {
            this.config.save(this.dataFile);
            return true;
        } catch (IOException e) {
            return false;
        }

    }

    public boolean removeWhitelist(UUID uuid) {
        List<String> configStringList = (List<String>) this.config.get("whitelist");
        if (configStringList == null)
            configStringList = new ArrayList<>();
        List<UUID> whitelisted = new ArrayList<>(configStringList.stream().map(UUID::fromString).toList());
        whitelisted.remove(uuid);

        this.config.set("whitelist", whitelisted.stream().map(UUID::toString).toList());

        try {
            this.config.save(this.dataFile);
            return true;
        } catch (IOException e) {
            return false;
        }

    }

    public void kickAllPlayer() {
        Bukkit.getOnlinePlayers().stream().forEach(p -> {
            try {
                if (!this.isWhitelisted(p.getUniqueId())) {
                    p.kick(this.maintenanceKickMessage);
                } else {
                    p.sendMessage(KevSmp.componentWithPrefix(Component.empty().append(MiniMessage.miniMessage().deserialize("You are whitelisted during maintenance mode, so you were not kicked."))));
                }
            } catch (RuntimeException e) {
                this.plugin.getLogger().warning("Failed to kick player when enabling maintenance mode (x1)");
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent ev) {
        Player player = ev.getPlayer();
        if (this.isMaintenanceModeEnabled()) {
            if (!this.isWhitelisted(player.getUniqueId())) {
                if (!player.isConnected())
                    return;

                player.kick(this.maintenanceKickMessage);
            }
        }
    }


}
