package lol.duckyyy.kevsmp.managers;

import lol.duckyyy.kevsmp.KevSmp;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;


public class ChestLockManager implements Listener {
    private final KevSmp plugin;
    private HashMap<Location, List<UUID>> lockedChests;
    private final FileConfiguration config;
    private final File dataFile;
    List<Material> lockableBlocks;

    public ChestLockManager(KevSmp plugin) {
        this.plugin = plugin;
        this.lockedChests = new HashMap<>();
        File dataFile = new File(plugin.getDataFolder(), "lockedChests.yml");
        this.dataFile = dataFile;
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                System.out.println("Failed to load locked chests configuration");
            }
        }

        this.config = YamlConfiguration.loadConfiguration(dataFile);
        List<Material> lockableBlocks = new ArrayList<>();

        // Common Containers
        lockableBlocks.add(Material.CHEST);
        lockableBlocks.add(Material.COPPER_CHEST);
        lockableBlocks.add(Material.TRAPPED_CHEST);
        lockableBlocks.add(Material.BARREL);
        lockableBlocks.add(Material.FURNACE);
        lockableBlocks.add(Material.SMOKER);
        lockableBlocks.add(Material.BLAST_FURNACE);

        // Redstone Component Containers
        lockableBlocks.add(Material.HOPPER);
        lockableBlocks.add(Material.CRAFTER);
        lockableBlocks.add(Material.CHISELED_BOOKSHELF);
        lockableBlocks.add(Material.JUKEBOX);

        // Add Shelf Variants
        Arrays.stream(Material.values()).filter(s -> s.name().toLowerCase().contains("shelf")).forEach(lockableBlocks::add);

        this.lockableBlocks = lockableBlocks;
    }

    public HashMap<Location, List<UUID>> getLockedChests() {
        return this.lockedChests;
    }

    public List<Material> getLockableBlocks() {
        return this.lockableBlocks;
    }

    public FileConfiguration getConfig() {
        return this.config;
    }

    public String locationToString(Location location) {
        return String.format("%s,%s,%s,%s", location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public List<Block> getAllChestBlocks() {
        List<Block> toReturn = new ArrayList<>(List.of());

        System.out.println("KEYS");
        System.out.println(this.config.getKeys(false));
        for (String locationId : this.config.getKeys(false)) {
            String[] parsed = locationId.split(",");
            System.out.println("PARSED");
            System.out.println(String.join(", ", parsed));
            World world = Bukkit.getServer().getWorld(parsed[0]);
            int x = Integer.parseInt(parsed[1]);
            int y = Integer.parseInt(parsed[2]);
            int z = Integer.parseInt(parsed[3]);

            this.plugin.getLogger().info(world.getName());
            this.plugin.getLogger().info(String.format("%s", x));
            this.plugin.getLogger().info(String.format("%s", y));
            this.plugin.getLogger().info(String.format("%s", z));

            Location loc = new Location(world, x, y, z);
            this.plugin.getLogger().info(loc.getWorld().getName());
            this.plugin.getLogger().info(String.format("%s", loc.getBlockX()));
            this.plugin.getLogger().info(String.format("%s", loc.getBlockY()));
            this.plugin.getLogger().info(String.format("%s", loc.getBlockZ()));
            Block toAdd = loc.getBlock();

            if (loc.getChunk().isLoaded() && !toAdd.isEmpty())
                toReturn.add(toAdd);
        }

        this.plugin.getLogger().info(String.format("%s",toReturn.size()));

        return toReturn;
    }

    public List<UUID> getChestTrustees(Block block) {
        List<UUID> toReturn = new ArrayList<>();

        this.config.getStringList((this.locationToString(block.getLocation())  + ".trusted")).stream().map(UUID::fromString).forEach(toReturn::add);

        return toReturn;
    }

    public UUID getChestOwner(Block block) {
        return UUID.fromString(Objects.requireNonNull(this.config.getString(this.locationToString(block.getLocation()) + ".owner")));
    }

    public HashMap<Location, List<UUID>> loadLockedChestMap() {
        HashMap<Location, List<UUID>> toReturn = new HashMap<>();
        List<Block> blocks = this.getAllChestBlocks();
        blocks.forEach((Block block) -> {
            if(block == null) return;
            List<UUID> trustees = this.getChestTrustees(block);
            UUID owner = this.getChestOwner(block);
            if(owner != null) trustees.add(owner);

            toReturn.put(block.getLocation(), trustees);
        });

        this.lockedChests = toReturn;

        return this.lockedChests;
    }

    public boolean ownsChest(Block chest, Player allegedOwner) {
        return this.ownsChest(chest, allegedOwner, false);
    }

    public boolean ownsChest(Block chest, Player allegedOwner, boolean adminOverride) {
        if (adminOverride && this.plugin.userHasPermission(this.plugin.getUser(allegedOwner.getUniqueId()), "kiwismp.admin"))
            return true;

        return this.getChestOwner(chest).equals(allegedOwner.getUniqueId());

    }

    public boolean isTrusted(Block chest, Player allegedTrustee) {
        return this.getChestTrustees(chest).contains(allegedTrustee.getUniqueId());
    }

    public boolean isLocked(Block chest) {
        return this.lockedChests.containsKey(chest.getLocation());
    }

    public boolean lockChest(Block chest, Player owner) {
        this.config.set(this.locationToString(chest.getLocation()) + ".owner", owner.getUniqueId().toString());
        this.config.set(this.locationToString(chest.getLocation()) + ".trusted", List.of(owner.getUniqueId().toString()));

        try {
            this.config.save(this.dataFile);
            this.lockedChests = this.loadLockedChestMap();

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean unlockChest(Block chest) {
        this.config.set(this.locationToString(chest.getLocation()), null);

        try {
            this.config.save(this.dataFile);
            this.lockedChests = this.loadLockedChestMap();
            return true;
        } catch (IOException e) {
            return false;
        }
    }


    public void addTrusted(Block chest, Player trustee) {
        List<UUID> trustees = new ArrayList<>(((List<String>) Objects.requireNonNull(this.config.get(this.locationToString(chest.getLocation()) + ".trusted"))).stream().map(UUID::fromString).toList());
        trustees.add(trustee.getUniqueId());

        this.config.set(this.locationToString(chest.getLocation()) + ".trusted", trustees.stream().map(UUID::toString).toList());

        try {
            this.config.save(this.dataFile);
            this.lockedChests = this.loadLockedChestMap();
            return;
        } catch (IOException e) {
            return;
        }
    }

    public void removeTrusted(Block chest, Player trustee) {
        List<UUID> trustees = new ArrayList<>(((List<String>) Objects.requireNonNull(this.config.get(this.locationToString(chest.getLocation()) + ".trusted"))).stream().map(UUID::fromString).toList());
        trustees.remove(trustee.getUniqueId());

        this.config.set(this.locationToString(chest.getLocation()) + ".trusted", trustees.stream().map(UUID::toString).toList());

        try {
            this.config.save(this.dataFile);
            this.lockedChests = this.loadLockedChestMap();
            return;
        } catch (IOException e) {
            return;
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent ev) {
        try {


            if (this.isLocked(ev.getBlock()) && !this.ownsChest(ev.getBlock(), ev.getPlayer())) {
                ev.setCancelled(true);
                OfflinePlayer blockOwner = Bukkit.getOfflinePlayer(this.getChestOwner(ev.getBlock()));
                ev.getPlayer().sendMessage(KevSmp.componentWithPrefix(Component.text(String.format("This block is locked by %s, you can not break it.", blockOwner.getName())).color(NamedTextColor.RED)));
                return;
            } else {
                if (this.ownsChest(ev.getBlock(), ev.getPlayer())) {
                    this.unlockChest(ev.getBlock());
                    ev.getPlayer().sendMessage(KevSmp.componentWithPrefix(Component.text(String.format("You broke your locked %s", ev.getBlock().getType().toString()))));
                }
            }
        } catch (RuntimeException e) {
            return;
        }
    }

    @EventHandler
    public void onExplosion(BlockExplodeEvent ev) {
        ev.blockList().removeIf(this::isLocked);
    }

    @EventHandler
    public void onEntityExplosion(EntityExplodeEvent ev) {
        ev.blockList().removeIf(this::isLocked);
    }

    @EventHandler
    public void onInventoryInteract(InventoryClickEvent ev) {
        List<InventoryType> containerTypes = new ArrayList<>();

        // Common Containers
        containerTypes.add(InventoryType.CHEST);
        containerTypes.add(InventoryType.BARREL);
        containerTypes.add(InventoryType.FURNACE);
        containerTypes.add(InventoryType.SMOKER);
        containerTypes.add(InventoryType.BLAST_FURNACE);

        // Redstone Component Containers
        containerTypes.add(InventoryType.HOPPER);
        containerTypes.add(InventoryType.CRAFTER);
        containerTypes.add(InventoryType.SHELF);
        containerTypes.add(InventoryType.CHISELED_BOOKSHELF);
        containerTypes.add(InventoryType.JUKEBOX);



        if(containerTypes.contains(ev.getView().getTopInventory().getType())) {
            org.bukkit.inventory.BlockInventoryHolder holder = (BlockInventoryHolder) ev.getView().getTopInventory().getHolder();
            if(holder == null) return;
            Block block = holder.getBlock();

            ev.setCancelled(this.isLocked(block) && (!this.isTrusted(block, (Player) ev.getWhoClicked())));
            if(ev.isCancelled()) ev.getWhoClicked().closeInventory();
        }
    }

    @EventHandler
    public void onInventoryMove(InventoryMoveItemEvent ev) {
        if(!(ev.getSource().getHolder() instanceof HumanEntity)) return;
        List<InventoryType> containerTypes = new ArrayList<>();

        // Common Containers
        containerTypes.add(InventoryType.CHEST);
        containerTypes.add(InventoryType.BARREL);
        containerTypes.add(InventoryType.FURNACE);
        containerTypes.add(InventoryType.SMOKER);
        containerTypes.add(InventoryType.BLAST_FURNACE);

        // Redstone Component Containers
        containerTypes.add(InventoryType.HOPPER);
        containerTypes.add(InventoryType.CRAFTER);
        containerTypes.add(InventoryType.SHELF);
        containerTypes.add(InventoryType.CHISELED_BOOKSHELF);
        containerTypes.add(InventoryType.JUKEBOX);



        if(containerTypes.contains(ev.getDestination().getType())) {
            org.bukkit.inventory.BlockInventoryHolder holder = (BlockInventoryHolder) ev.getDestination().getHolder();
            if(holder == null) return;
            Block block = holder.getBlock();
            Player whoClicked = (Player) ev.getSource().getHolder();

            ev.setCancelled(this.isLocked(block) && (!this.isTrusted(block, whoClicked)));
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent ev) {
        Action act = ev.getAction();
        Block clickedBlock = ev.getClickedBlock();
        if (clickedBlock == null)
            return;

        if(!this.lockableBlocks.contains(clickedBlock.getType())) return;

        if (act.isRightClick()) {
            if (ev.getItem() == null || !ev.getItem().getType().equals(Material.IRON_NUGGET)) {
                // attempt to open chest
                if(!this.isLocked(clickedBlock)) return;
                if(!this.ownsChest(clickedBlock, ev.getPlayer()) && !this.isTrusted(clickedBlock, ev.getPlayer())) {
                    // does not own chest
//                    KevSmp.debugMessage(ev.getPlayer(), "Not your chest! (opening)");
                    OfflinePlayer blockOwner = Bukkit.getOfflinePlayer(this.getChestOwner(clickedBlock));
                    ev.getPlayer().sendMessage(KevSmp.componentWithPrefix(Component.text(String.format("This block is locked by %s, you can not use it.", blockOwner.getName())).color(NamedTextColor.RED)));

                    ev.setCancelled(true);
                    return;
                } else {
                    // owns chest or trusted, attempt to open
                    return;
                }
            } else if (!ev.getItem().isEmpty() && ev.getItem().getType().equals(Material.IRON_NUGGET) && ev.getPlayer().isSneaking()) {
                if(KevSmp.graves.containsKey(clickedBlock.getLocation())) {
                    ev.getPlayer().sendMessage(KevSmp.componentWithPrefix(Component.text("You can not lock death chests.").color(NamedTextColor.RED)));

                    return;
                }
                // trying to modify
                if(this.isLocked(clickedBlock) && !this.ownsChest(clickedBlock, ev.getPlayer())) {
                    // does not own locked chest
                    OfflinePlayer blockOwner = Bukkit.getOfflinePlayer(this.getChestOwner(clickedBlock));
                    ev.getPlayer().sendMessage(KevSmp.componentWithPrefix(Component.text(String.format("This block is locked by %s, you can not modify it.", blockOwner.getName())).color(NamedTextColor.RED)));

                    ev.setCancelled(true);
                    return;
                } else {
                    // owns chest/is not locked
                    if(this.isLocked(clickedBlock)) {
                        // unlock
                        boolean unlocked = this.unlockChest(clickedBlock);
                        if(!unlocked) {
                            ev.getPlayer().sendMessage(KevSmp.componentWithPrefix(Component.text("Failed to unlock this block").color(NamedTextColor.RED)));

                        } else {
                            ev.getPlayer().sendMessage(KevSmp.componentWithPrefix(Component.text("Successfully unlocked this block")));
                            return;
                        }

                    } else if(!this.isLocked(clickedBlock)) {
                        // lock
                        if(this.plugin.userHasPermission(this.plugin.getUser(ev.getPlayer().getUniqueId()), "kiwismp.deny_lock")) {
                            ev.getPlayer().sendMessage(KevSmp.componentWithPrefix(Component.text("Failed to lock this block. You don't have the proper permission.").color(NamedTextColor.RED)));

                            return;
                        }
                        boolean locked = this.lockChest(clickedBlock, ev.getPlayer());
                        if(!locked) {
                            ev.getPlayer().sendMessage(KevSmp.componentWithPrefix(Component.text("Failed to lock this block").color(NamedTextColor.RED)));

                        } else {
                            ev.getPlayer().sendMessage(KevSmp.componentWithPrefix(Component.text("Successfully locked this block")));
                        }
                    }
                }

            }

        } else
            return;
    }


}
