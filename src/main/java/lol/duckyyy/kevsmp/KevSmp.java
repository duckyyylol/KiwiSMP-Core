package lol.duckyyy.kevsmp;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.objects.Lag;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import io.papermc.paper.event.player.PlayerBedFailEnterEvent;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import lol.duckyyy.kevsmp.commands.*;
import lol.duckyyy.kevsmp.events.AccountLinkListener;
import lol.duckyyy.kevsmp.events.PlayerConfigurationBooleanUpdateEvent;
import lol.duckyyy.kevsmp.managers.ChestLockManager;
import lol.duckyyy.kevsmp.tasks.AFKTimer;
import lol.duckyyy.kevsmp.tasks.Alerts;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.*;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import javax.naming.Name;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;


public final class KevSmp extends JavaPlugin implements Listener {

    public static String PLUGIN_NAME = "KiwiSMP";
    HashMap<UUID, Integer> ctaCooldowns = new HashMap<>();
    public static FileConfiguration config;
    public static FileConfiguration chestConfig;
    public static FileConfiguration dataStorage;
    public static Dialog unlinkConfirmDialog;
    public ChestLockManager CLM;
    public HashMap<UUID, Integer> linkChecks = new HashMap<>();
    public HashMap<UUID, Integer> levelChecks = new HashMap<>();
    public HashMap<UUID, Integer> afkers = new HashMap<>();
    int AFK_THRESHOLD_SECONDS = 60;
    public HashMap<WarningAlertTypes, Boolean> warningsShown = new HashMap<>();
    public int warningResets = 0;

    public HashMap<UUID, PlayerInventory> awaitingRespawn = new HashMap<>();
    public static HashMap<Location, Inventory> graves = new HashMap<>();


    AccountLinkListener accountLinkListener = new AccountLinkListener();
    public static LuckPerms luckpermsApi;

    public static TextComponent MESSAGE_PREFIX =
            Component.empty().append(Component.text("K").color(TextColor.color(0x5ed245)).append(Component.text("I").color(TextColor.color(0x64d84b))).append(Component.text("W").color(TextColor.color(0x6bdd51))).append(Component.text("I").color(TextColor.color(0x71e357))).decorate(TextDecoration.BOLD)).appendSpace().append(Component.text("»").color(NamedTextColor.DARK_GRAY)).appendSpace();

    public static void debugMessage(Player p, String content) {
        p.sendMessage(Component.text("[DEBUG]").color(NamedTextColor.GREEN).appendSpace().append(Component.empty()).append(Component.text(content)));
    }


    public static TextComponent componentWithPrefix(TextComponent component) {
        return MESSAGE_PREFIX.append(Component.empty()).append(component);
    }

    public enum WarningAlertTypes {
        TPS_BELOW, TPS_BELOW_SEVERE, TPS_ABOVE
    }

    public void sendAlertWarning(Player player, String content) {
        Component alertPrepend = Component.empty().append(MiniMessage.miniMessage().deserialize("<b><gold>!</gold></b>")).appendSpace();

        player.sendMessage(alertPrepend.append(MiniMessage.miniMessage().deserialize(content)).appendNewline().append(MiniMessage.miniMessage().deserialize("<gray><i>> Disable these alerts with " + "/modalerts")));
    }

    public void broadcastWarning(WarningAlertTypes alertType, int tps) throws InterruptedException {

        warningResets = warningResets + 1;
        if (!warningsShown.containsKey(alertType))
            warningsShown.put(alertType, false);
        if (warningResets >= 15) {
            warningsShown.put(alertType, false);
            warningResets = 0;
        }
        if (!warningsShown.get(alertType) && warningResets < 15) {

            Collection<? extends Player> allPlayers = Bukkit.getOnlinePlayers();
            List<Player> verbosePlayers = (List<Player>) allPlayers.stream().filter(p -> p.hasPermission("kiwismp.verbose") && dataStorage.getBoolean(p.getUniqueId() + ".show_mod_alerts")).toList();

            String warningContent;

            if (alertType.equals(WarningAlertTypes.TPS_ABOVE)) {
                warningContent = "<red>The game is ticking faster than usual.";
            } else if (alertType.equals(WarningAlertTypes.TPS_BELOW)) {
                warningContent = "<red>The game is ticking slower than usual. Some may experience light latency.";
            } else if (alertType.equals(WarningAlertTypes.TPS_BELOW_SEVERE)) {
                warningContent = "<red>The game is ticking too slow. The game is severely impacted. The server is overloaded. If this persists, please contact Ducky.";
            } else {
                warningContent = "<red>Failed to recognize alert type.";
            }

            verbosePlayers.forEach(p -> sendAlertWarning(p, warningContent));

            warningsShown.put(alertType, true);
            //
        }
    }

    public void performTPSCheck() {
        int tps = (int) Bukkit.getServer().getTPS()[0];

        if (tps == 20)
            return;

        if (tps > 20) {
            try {
                broadcastWarning(WarningAlertTypes.TPS_ABOVE, tps);
            } catch (InterruptedException e) {
                getLogger().severe("Failed to broadcast severe TPS spike alert");
                getLogger().severe(e.getMessage());
            }
        }

        if (tps < 19 && tps > 15) {
            try {
                broadcastWarning(WarningAlertTypes.TPS_BELOW, tps);
            } catch (InterruptedException e) {
                getLogger().severe("Failed to broadcast TPS drop alert");
                getLogger().severe(e.getMessage());
            }
        }
        if (tps < 15) {
            try {
                broadcastWarning(WarningAlertTypes.TPS_BELOW_SEVERE, tps);
            } catch (InterruptedException e) {
                getLogger().severe("Failed to broadcast severe TPS drop alert");
                getLogger().severe(e.getMessage());
            }
        }
    }

    public <K, V> K getKey(Map<K, V> map, V value) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }


    boolean isUserLinked(UUID uuid) {
        return isUserLinked(uuid, true);
    }

    public boolean placeDeathChest(Player player, PlayerInventory inventory, Location location) {
        Location loc = new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        if (inventory.isEmpty())
            return true;
        //        InventoryView graveInventory = MenuType.GENERIC_9X6.builder().title(Component.text(player.getName())).build(player);
        Inventory inv = Bukkit.createInventory(null, 54);

        BlockData chestData = getServer().createBlockData(Material.CHEST);
        Block chest = loc.getBlock();
        chest.setBlockData(chestData);


        List<ItemStack> itemsList = new ArrayList<>();

        itemsList.addAll(Arrays.stream(inventory.getStorageContents()).toList());
        itemsList.addAll(Arrays.stream(inventory.getArmorContents()).toList());
        itemsList.add(inventory.getItemInOffHand());

        ItemStack[] items = itemsList.toArray(new ItemStack[0]);

        for (int i = 0; i < items.length; i++) {
            inv.setItem(i, items[i]);
        }

        try {
            loc.getWorld().setBlockData(location, chest.getBlockData());

            writeChest(chestConfig, loc, inv, player);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isUserLinked(UUID uuid, boolean sendCta) {
        if (!DiscordSRV.isReady) {
            if (!ctaCooldowns.containsKey(uuid)) {
                ctaCooldowns.put(uuid, Bukkit.getCurrentTick());

                if (Bukkit.getCurrentTick() >= ctaCooldowns.get(uuid)) {
                    Player player = Bukkit.getPlayer(uuid);
                    // send cta (try again)
                    ctaCooldowns.put(uuid, Bukkit.getCurrentTick() + 40);
                }
            }

            return false;
        }

        boolean linked = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(uuid) != null;

        if (!linked && sendCta) {
            if (!ctaCooldowns.containsKey(uuid))
                ctaCooldowns.put(uuid, Bukkit.getCurrentTick());
            if (Bukkit.getCurrentTick() >= ctaCooldowns.get(uuid)) {
                Player player = Bukkit.getPlayer(uuid);
                String linkCode = "";
                if (DiscordSRV.getPlugin().getAccountLinkManager().getLinkingCodes().containsValue(uuid)) {
                    linkCode = getKey(DiscordSRV.getPlugin().getAccountLinkManager().getLinkingCodes(), uuid);
                } else {
                    linkCode = DiscordSRV.getPlugin().getAccountLinkManager().generateCode(uuid);
                }

                assert player != null;
                player.sendMessage(Component.empty().append(Component.text("――――――――――――――――――――――――――――").decorate(TextDecoration.BOLD, TextDecoration.STRIKETHROUGH).color(NamedTextColor.DARK_GRAY)).appendNewline().append(Component.text("Linking is " + "Required to Play").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD)).appendNewline().appendNewline().append(Component.text("DM " + "@Kiwibot#8867 on Discord with your link code:").color(NamedTextColor.GREEN).appendSpace().append(Component.text(linkCode).color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD))).appendNewline().appendNewline().appendNewline().append(Component.text("!").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)).appendSpace().appendSpace().append(Component.text("Ensure your message contains only the 4-digit code, and nothing else.")).appendNewline().append(Component.text("――――――――――――――――――――――――――――").decorate(TextDecoration.BOLD, TextDecoration.STRIKETHROUGH).color(NamedTextColor.DARK_GRAY)));

                ctaCooldowns.put(uuid, Bukkit.getCurrentTick() + 100);
            }
        }

        return linked;
    }

    public String locationToString(Location location) {
        return String.format("%s,%s,%s,%s", location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public FileConfiguration loadChestConfig() {
        File chestFile = new File(this.getDataFolder(), "chests.yml");
        if (!chestFile.exists()) {
            try {
                chestFile.createNewFile();
            } catch (IOException e) {
                System.out.println("Failed to load chest configuration");
            }

            return YamlConfiguration.loadConfiguration(chestFile);

        } else {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(chestFile);
            for (String locationId : cfg.getKeys(false)) {
                if (!locationId.equals("owners")) {
                    String[] locParts = locationId.split(",");
                    World world = Bukkit.getWorld(locParts[0]);
                    int x = Integer.parseInt(locParts[1]);
                    int y = Integer.parseInt(locParts[2]);
                    int z = Integer.parseInt(locParts[3]);

                    Location location = new Location(world, x, y, z);

                    List<ItemStack> contents = (List<ItemStack>) cfg.get(locationId);

                    assert contents != null;
                    Inventory inv = Bukkit.createInventory(null, 54);
                    inv.setContents(contents.toArray(new ItemStack[0]));

                    graves.put(location, inv);
                }

            }
            return cfg;
        }
    }

    public FileConfiguration loadDataStorageFile() {
        File dataFile = new File(this.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                System.out.println("Failed to load data configuration");
            }

            return YamlConfiguration.loadConfiguration(dataFile);

        } else {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);

            return cfg;
        }
    }

    public void writeStringToDataStorage(FileConfiguration cfg, String path, String value) {
        File dataFile = new File(this.getDataFolder(), "data.yml");

        cfg.set(path, value);
        try {
            cfg.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean writeToggleBooleanInDataStorage(FileConfiguration cfg, String path) {
        File dataFile = new File(this.getDataFolder(), "data.yml");

        boolean toggled = !cfg.getBoolean(path);

        cfg.set(path, toggled);
        try {
            cfg.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return toggled;
    }


    public void writeChest(FileConfiguration cfg, Location convert_location, Inventory inventory, Player owner) {
        File chestFile = new File(this.getDataFolder(), "chests.yml");
        Location loc = new Location(convert_location.getWorld(), convert_location.getBlockX(), convert_location.getBlockY(), convert_location.getBlockZ());
        String locationId = locationToString(loc);
        ItemStack[] inventoryItems = inventory.getContents();

        cfg.set("owners." + locationId, owner.getUniqueId().toString());
        cfg.set(locationId, inventoryItems);


        try {
            cfg.save(chestFile);
            this.getLogger().info("Saving Death Chest " + locationId);
            loadGravesMap();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void reloadChestsInWorld(FileConfiguration cfg) {
        //        HashMap<String, List<ItemStack>> chests = getAllChests(cfg);

        for (String locationId : cfg.getKeys(false)) {
            if (!locationId.equals("owners")) {
                String[] locParts = locationId.split(",");
                World world = Bukkit.getWorld(locParts[0]);
                int x = Integer.parseInt(locParts[1]);
                int y = Integer.parseInt(locParts[2]);
                int z = Integer.parseInt(locParts[3]);

                Location location = new Location(world, x, y, z);
                Block block = location.getBlock();

                List<ItemStack> contents = (List<ItemStack>) cfg.get(locationId);
                contents = contents.stream().filter(c -> !Objects.equals(c.toString(), "null")).toList();

                //                assert contents != null;
                Inventory inv = Bukkit.createInventory(null, 54);
                inv.setContents(contents.toArray(new ItemStack[0]));

                graves.put(location, inv);
                if (block != null && block.getLocation().getChunk().isLoaded()) {
                    block.setType(Material.CHEST);
                } else {
                    assert block != null;
                    System.out.println("Skipped chest in unloaded chunk");
                }
                Bukkit.broadcast(Component.text(String.format("Loaded Chest at %s with %s items", locationId, contents.size())));
            }
        }

    }

    //    public HashMap<String, List<ItemStack>> getAllChests(FileConfiguration cfg) {
    //        HashMap<String, List<ItemStack>> toReturn = new HashMap<>();
    //
    //        for(String locationId : cfg.getKeys(false)) {
    //            if(!locationId.equals("owners")) {
    //
    //                toReturn.put(locationId, (List<ItemStack>) cfg.getList(locationId));
    //            }
    //        }
    //
    //        return toReturn;
    //    }

    public ItemStack[] getChestContents(FileConfiguration config, Location convert_location) {
        Location loc = new Location(convert_location.getWorld(), convert_location.getBlockX(), convert_location.getBlockY(), convert_location.getBlockZ());
        String locationId = locationToString(loc);

        return null;
    }

    public HashMap<Location, Inventory> loadGravesMap() {
        FileConfiguration cfg = this.loadChestConfig();
        for (String locationId : cfg.getKeys(false)) {
            if (!locationId.equals("owners")) {
                String[] locParts = locationId.split(",");
                World world = Bukkit.getWorld(locParts[0]);
                int x = Integer.parseInt(locParts[1]);
                int y = Integer.parseInt(locParts[2]);
                int z = Integer.parseInt(locParts[3]);

                Location location = new Location(world, x, y, z);

                List<ItemStack> contents = (List<ItemStack>) cfg.get(locationId);

                if (contents != null) {
                    Inventory inv = Bukkit.createInventory(null, 54);
                    inv.setContents(contents.toArray(new ItemStack[0]));

                    graves.put(location, inv);
                } else
                    graves.remove(location);

            }

        }

        return graves;
    }


    @Override
    public void onEnable() {

        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        DiscordSRV.api.subscribe(accountLinkListener);




        config = getConfig();
        chestConfig = loadChestConfig();
        dataStorage = loadDataStorageFile();
        CLM = new ChestLockManager(this);
        Bukkit.getPluginManager().registerEvents(CLM, this);

        DialogAction.StaticAction confirmAction = DialogAction.staticAction(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "discord unlink"));

        DialogAction.CustomClickAction cancelAction = DialogAction.customClick(Key.key("kiwismp:unlink/cancel"), null);

        ActionButton confirmButton = ActionButton.builder(Component.text("Unlink").color(NamedTextColor.RED)).tooltip(Component.text("Click to unlink your account")).action(confirmAction).build();

        ActionButton cancelButton = ActionButton.builder(Component.text("Cancel")).tooltip(Component.text("Close this dialog and continue playing")).action(cancelAction).build();

        unlinkConfirmDialog =
                Dialog.create(d -> d.empty().base(DialogBase.builder(Component.text("Confirm Unlinking")).canCloseWithEscape(false).body(List.of(DialogBody.plainMessage(Component.empty().append(Component.text("If you unlink your account, you will: ").color(NamedTextColor.RED)).appendNewline().appendNewline().append(Component.text(KevSmp.config.getBoolean("access_control.link_required_to_join") ? "1. Be kicked from the server" : "1. Be teleported to world spawn").appendNewline().appendNewline().append(Component.text(KevSmp.config.getBoolean("access_control.link_required_to_join") ? "2. Be required to link your account before rejoining" : "2. Be unable to move until you link a new account"))).appendNewline().appendNewline().append(Component.text("3. Lose any Discord-based perks, such as Twitch subscriber rank.")).appendNewline().appendNewline().appendNewline().append(Component.text("You will " + "NOT:").decorate(TextDecoration.BOLD).color(NamedTextColor.GREEN)).appendNewline().appendNewline().append(Component.text("1. " + "Lose any game progress, like advancements.").appendNewline().appendNewline().append(Component.text("2. Lose any items. Your inventory will remain exactly the same."))).appendNewline().appendNewline().appendNewline().appendNewline().appendNewline().appendNewline().appendNewline().appendNewline().appendNewline().append(Component.text("Unlink Discord account?")).appendNewline().appendNewline().append(Component.text("You can link another Discord account at any time.").color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))))).build()).type(DialogType.confirmation(confirmButton, cancelButton)));
        //        loadGravesMap();
        //        reloadChestsInWorld(chestConfig);


        RegisteredServiceProvider<LuckPerms> luckPermsRegisteredServiceProvider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (luckPermsRegisteredServiceProvider != null) {
            luckpermsApi = luckPermsRegisteredServiceProvider.getProvider();
        }

        Bukkit.getPluginCommand("sync").setExecutor(new SyncDiscordCommand());
        Bukkit.getPluginCommand("reloadchests").setExecutor(new ReloadChestsCommand());
        Bukkit.getPluginCommand("unlink").setExecutor(new UnlinkCommand());
        Bukkit.getPluginCommand("modstats").setExecutor(new ToggleModStatsCommand());
        Bukkit.getPluginCommand("modalerts").setExecutor(new ToggleModAlertsCommand());
        Bukkit.getPluginCommand("trust").setExecutor(new TrustToBlockCommand(this));
        Bukkit.getPluginCommand("untrust").setExecutor(new UntrustToBlockCommand(this));

        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new AFKTimer(this), 20, 20);
        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Alerts(this), 20, 20);

    }


    public boolean isAfk(Player player) {
        if (!afkers.containsKey(player.getUniqueId())) {
            afkers.put(player.getUniqueId(), 1);
            return false;
        }
        return afkers.get(player.getUniqueId()) > AFK_THRESHOLD_SECONDS;
    }

    public void resetAfkTime(Player player) {
        if (isAfk(player)) {
            player.sendMessage(componentWithPrefix(Component.textOfChildren(MiniMessage.miniMessage().deserialize(String.format("<yellow>Welcome back from your eternal slumber (AFK), " + "<gold>%s" + "<yellow>!", player.getName())))));
            Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(componentWithPrefix(Component.textOfChildren(MiniMessage.miniMessage().deserialize(String.format("<gray>%s <dark_gray>is no longer " + "AFK", player.getName()))))));
        }
        afkers.put(player.getUniqueId(), 0);
    }

    public void updateAfkTime(Player player) {
        if (!isAfk(player)) {
            afkers.put(player.getUniqueId(), afkers.get(player.getUniqueId()) + 1);
        }

        if (isAfk(player)) {
            if (afkers.get(player.getUniqueId()) > 999) {
                return;
            } else {
                afkers.put(player.getUniqueId(), 1000);
                player.sendMessage(componentWithPrefix(Component.textOfChildren(MiniMessage.miniMessage().deserialize("<yellow>You've been marked AFK. We await your return!"))));
                Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(componentWithPrefix(Component.textOfChildren(MiniMessage.miniMessage().deserialize(String.format("<gray>%s <dark_gray>is now " + "AFK", player.getName()))))));
            }

        }
    }

    public @Nullable Group getGroup(String name) {
        try {
            return luckpermsApi.getGroupManager().getGroup(name);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public @Nullable net.luckperms.api.model.user.User getUser(UUID uuid) {
        try {
            return luckpermsApi.getUserManager().getUser(uuid);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public boolean userHasPermission(net.luckperms.api.model.user.User user, String node) {
        try {
            Node permNode = Node.builder(node).build();
            net.luckperms.api.model.user.User permUser = getUser(user.getUniqueId());
            if (permUser == null)
                return false;

            return permUser.getNodes().contains(permNode);
        } catch (RuntimeException e) {
            return false;
        }
    }

    public Group getHighestGroup(net.luckperms.api.model.user.User user) {
        return this.getGroup(user.getPrimaryGroup());
    }

    public boolean groupHasPermission(Group group, String node) {
        try {
            Node permNode = Node.builder(node).build();
            Group permGroup = getGroup(group.getName());
            if (permGroup == null)
                return false;
            return permGroup.getNodes().contains(permNode);
        } catch (RuntimeException e) {
            return false;
        }
    }

    public void addPermissionToGroup(Group group, String node) {
        try {
            Node permNode = Node.builder(node).build();
            Group permGroup = getGroup(group.getName());
            if (permGroup == null)
                return;

            luckpermsApi.getGroupManager().modifyGroup(group.getName(), g -> {
                g.data().add(permNode);
            });

        } catch (RuntimeException e) {
            return;
        }
    }

    public void removePermissionFromGroup(Group group, String node) {
        try {
            Node permNode = Node.builder(node).build();
            Group permGroup = getGroup(group.getName());
            if (permGroup == null)
                return;

            luckpermsApi.getGroupManager().modifyGroup(group.getName(), g -> {
                g.data().remove(permNode);
            });

        } catch (RuntimeException e) {
            return;
        }
    }

    public void addPermissionToUser(net.luckperms.api.model.user.User user, String node) {
        try {
            Node permNode = Node.builder(node).build();
            net.luckperms.api.model.user.User permUser = getUser(user.getUniqueId());
            if (permUser == null)
                return;

            luckpermsApi.getUserManager().modifyUser(user.getUniqueId(), u -> {
                u.data().add(permNode);
            });

        } catch (RuntimeException e) {
            return;
        }
    }

    public void removePermissionFromUser(net.luckperms.api.model.user.User user, String node) {
        try {
            Node permNode = Node.builder(node).build();
            net.luckperms.api.model.user.User permUser = getUser(user.getUniqueId());
            if (permUser == null)
                return;

            luckpermsApi.getUserManager().modifyUser(user.getUniqueId(), u -> {
                u.data().remove(permNode);
            });

        } catch (RuntimeException e) {
            return;
        }
    }


    @Override
    public void onDisable() {
        // Plugin shutdown logic
        DiscordSRV.api.unsubscribe(accountLinkListener);
    }

    @EventHandler
    public void onPlayerConfigurationBooleanUpdate(PlayerConfigurationBooleanUpdateEvent ev) {
        //        boolean newValue = ev.getNewValue();
        String path = ev.getPath();

        writeToggleBooleanInDataStorage(dataStorage, path);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent ev) {
        loadGravesMap();
        CLM.loadLockedChestMap();
    }

    @EventHandler
    void handleUnlinkDialog(PlayerCustomClickEvent ev) {
        if (ev.getIdentifier().equals(Key.key("kiwismp:unlink/cancel"))) {
            DialogResponseView view = ev.getDialogResponseView();
            if (view == null) {
            }

        }
    }

    //    @EventHandler
    //    public void disableJoinMessage(PlayerJoinEvent ev) {
    //        ev.joinMessage(Component.empty());
    //    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent ev) {

        if (!KevSmp.luckpermsApi.getUserManager().getUser(ev.getPlayer().getUniqueId()).getNodes().contains(Node.builder("kiwismp.allowed").build())) {
            ev.joinMessage(Component.empty());
            return;
        }

        try {

            Player player = ev.getPlayer();


            List<String> welcomeEmojis = config.getStringList("lang.messages.welcomes");
            Random random = new Random();
            String welcomeMessage = welcomeEmojis.get(random.nextInt(0, welcomeEmojis.size()));

            if (!player.hasPlayedBefore()) {
                Component firstJoinMessage = MiniMessage.miniMessage().deserialize("<b><gradient:#FCD05C:#E5D098>joined the serve</gradient><gradient:#E5D098:#FCD05C>r for the first " + "time" +
                        "!</gradient></b>");
                Component fancyUsername = MiniMessage.miniMessage().deserialize(String.format("<b><gradient:#FFE6A5:#FCD05C>%s</gradient></b>", player.getName()));
                ev.joinMessage(Component.empty().append(Component.text(String.join(" ", welcomeEmojis))).appendSpace().append(fancyUsername).appendSpace().append(firstJoinMessage).appendSpace().append(Component.text(String.join(" ", welcomeEmojis))));
            } else {
                ev.joinMessage(componentWithPrefix(Component.empty().append(Component.text(welcomeMessage).appendSpace().append(Component.text("Welcome back,").color(NamedTextColor.YELLOW)).appendSpace().append(Component.text(player.getName()).color(NamedTextColor.GOLD)).append(Component.text("!").color(NamedTextColor.YELLOW)))));
            }
        } catch (RuntimeException e) {
            return;
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent ev) {
        Player player = ev.getPlayer();
        ev.quitMessage(componentWithPrefix(Component.empty().append(Component.text("Seeya later,").color(NamedTextColor.RED)).appendSpace().append(Component.text(player.getName()).color(TextColor.color(0xe53232))).append(Component.text("!").color(NamedTextColor.RED))));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent ev) {
        Player player = ev.getPlayer();
        Location deathLocation = ev.getPlayer().getLocation();
        Location chestLocation = deathLocation.add(0, 1, 0);
        //        Location firstTestLocation = deathLocation.add(0, 1, 0);

        //        if(firstTestLocation.getBlock().equals(BlockType.AIR)) {
        //            // is air, place chest
        //            chestLocation = deathLocation.add(0, 1, 0);
        //        } else {
        //            // not air
        //            for (int i = 0; i < 5; i++) {
        //                if(chestLocation.equals(firstTestLocation)) {
        //                    Location testLocationX = deathLocation.add(i, 0, 0); // x checks
        //                    if(testLocationX.getBlock().equals(BlockType.AIR)) {
        //                        chestLocation = testLocationX;
        //                    } else {
        //                        Location testLocationZ = deathLocation.add(0, 0, i); // z checks
        //                        if(testLocationZ.getBlock().equals(BlockType.AIR)) {
        //                            chestLocation = testLocationZ;
        //                        }
        //                    }
        //                }
        //
        //            }
        //
        //            if(chestLocation.equals(firstTestLocation) && !firstTestLocation.getBlock().equals(BlockType.AIR)) {
        //                awaitingRespawn.put(player.getUniqueId(), player.getInventory());
        //            }
        //        }

        //        if(!awaitingRespawn.containsKey(player.getUniqueId())) {
        boolean placed = placeDeathChest(player, player.getInventory(), chestLocation);
        ev.getDrops().clear();
        if (placed) {

            player.sendMessage(componentWithPrefix(Component.empty().append(Component.text("Uh oh, you died! The contents of your inventory have been placed in a chest.").color(NamedTextColor.GREEN)).appendSpace().append(Component.text("(coordinates: ").color(NamedTextColor.GRAY)).append(Component.text(String.format("X %s,", Math.round(chestLocation.x()))).color(NamedTextColor.DARK_AQUA).appendSpace()).append(Component.text(String.format("Y %s,", Math.round(chestLocation.y()))).color(NamedTextColor.AQUA).appendSpace()).append(Component.text(String.format("Z %s", Math.round(chestLocation.z()))).color(NamedTextColor.BLUE).appendSpace()).append(Component.text(")").color(NamedTextColor.GRAY))));
            loadGravesMap();
        } else {
            player.sendMessage(componentWithPrefix(Component.empty().append(Component.text("Something went wrong when placing your death chest.").color(NamedTextColor.RED))).appendSpace().appendSpace().append(Component.text("(coordinates: ").color(NamedTextColor.GRAY)).append(Component.text(String.format("X %s,", Math.round(chestLocation.x()))).color(NamedTextColor.DARK_AQUA).appendSpace()).append(Component.text(String.format("Y %s,", Math.round(chestLocation.y()))).color(NamedTextColor.AQUA).appendSpace()).append(Component.text(String.format("Z %s", Math.round(chestLocation.z()))).color(NamedTextColor.BLUE).appendSpace()).append(Component.text(")").color(NamedTextColor.GRAY)));
        }
    }


    @EventHandler
    public void onTick(ServerTickEndEvent ev) {
        Bukkit.getOnlinePlayers().forEach((Player player) -> {
            boolean linked = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId()) != null;

            if (!linkChecks.containsKey(player.getUniqueId())) {
                linkChecks.put(player.getUniqueId(), 0);
            } else {
                linkChecks.put(player.getUniqueId(), linkChecks.get(player.getUniqueId()) + 1);
            }


            if (!linked && linkChecks.get(player.getUniqueId()) >= 100) {
                linkChecks.put(player.getUniqueId(), 0);
                if (config.getBoolean("access_control.link_required_to_join")) {

                    String linkCode = "";
                    if (DiscordSRV.getPlugin().getAccountLinkManager().getLinkingCodes().containsValue(player.getUniqueId())) {
                        linkCode = getKey(DiscordSRV.getPlugin().getAccountLinkManager().getLinkingCodes(), player.getUniqueId());
                    } else {
                        linkCode = DiscordSRV.getPlugin().getAccountLinkManager().generateCode(player.getUniqueId());
                    }

                    player.kick(Component.empty().append(Component.text("Linking is " + "Required to Play").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD)).appendNewline().appendNewline().append(Component.text("DM " + "@Kiwibot#8867 on Discord with your link code:").color(NamedTextColor.GREEN).appendSpace().append(Component.text(linkCode).color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD))).appendNewline().appendNewline().appendNewline().append(Component.text("!").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)).appendSpace().appendSpace().append(Component.text("Ensure your message contains only the 4-digit code, and nothing else.")));
                }
                return;
            }

            String linkedUserId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());

            if (linkedUserId == null)
                return;

            User linkedUser = DiscordSRV.getPlugin().getJda().getUserById(linkedUserId);
            if (linkedUser == null)
                return;

            // AFK DETECTION
            if (player.isSwimming() && player.isFlying()) {
            }
            // AFK DETECTION

            Guild guild = linkedUser.getJDA().getGuildById(Objects.requireNonNull(KevSmp.config.getString("discord.guild_id")));
            if (guild == null)
                return;
            Member guildMember = guild.getMemberById(linkedUser.getId());
            if (guildMember == null)
                return;
            Role subscriberRole = guild.getRoleById(Objects.requireNonNull(KevSmp.config.getString("discord.roles.subscriber_role_id")));
            Role moderatorRole = guild.getRoleById(Objects.requireNonNull(KevSmp.config.getString("discord.roles.moderator_role_id")));
            Role vipRole = guild.getRoleById(Objects.requireNonNull(KevSmp.config.getString("discord.roles.vip_role_id")));
            Role requiredLevelRole = guild.getRoleById(Objects.requireNonNull(KevSmp.config.getString("discord.roles.required_level_role_id")));

            // LEVEL REQUIREMENT


            //            if ((guildMember.getRoles().contains(requiredLevelRole) && !KevSmp.luckpermsApi.getUserManager().getUser(player.getUniqueId()).getNodes().contains(Node.builder("kiwismp
            //            .allowed").build())) || KevSmp.luckpermsApi.getUserManager().getUser(player.getUniqueId()).getNodes().contains(Node.builder("kiwismp.level_immune").build())) {
            //                KevSmp.luckpermsApi.getUserManager().modifyUser(player.getUniqueId(), u -> {
            //                    u.data().add(Node.builder("kiwismp.allowed").build());
            //                });
            //            } else if ((!guildMember.getRoles().contains(requiredLevelRole) && KevSmp.luckpermsApi.getUserManager().getUser(player.getUniqueId()).getNodes().contains(Node.builder
            //            ("kiwismp.allowed").build())) && !KevSmp.luckpermsApi.getUserManager().getUser(player.getUniqueId()).getNodes().contains(Node.builder("kiwismp.level_immune").build())) {
            //                if(!KevSmp.luckpermsApi.getUserManager().getUser(player.getUniqueId()).getNodes().contains(Node.builder("kiwismp.level_immune").build())) {
            //                KevSmp.luckpermsApi.getUserManager().modifyUser(player.getUniqueId(), u -> {
            //                    u.data().remove(Node.builder("kiwismp.allowed").build());
            //                });
            //                }
            //            } else if (!guildMember.getRoles().contains(requiredLevelRole) && !KevSmp.luckpermsApi.getUserManager().getUser(player.getUniqueId()).getNodes().contains(Node.builder
            //            ("kiwismp.allowed").build())) {
            //                if(!levelChecks.containsKey(player.getUniqueId())) {
            //                    levelChecks.put(player.getUniqueId(), 0);
            //                } else {
            //                if(!highestWeightGroup.getNodes().contains(Node.builder("kiwismp.level_immune").build())) {
            //                    debugMessage(player, "ADDING");
            //                    debugMessage(player, highestWeightGroup.getName());
            //                    levelChecks.put(player.getUniqueId(), levelChecks.get(player.getUniqueId()) + 1);
            //                }
            //
            //                debugMessage(player, String.format("%s", levelChecks.get(player.getUniqueId())));
            //                }

            Group highestWeightGroup = luckpermsApi.getGroupManager().getGroup(luckpermsApi.getUserManager().getUser(player.getUniqueId()).getPrimaryGroup());


            if (!groupHasPermission(highestWeightGroup, "kiwismp.level_immune")) {

                if (!groupHasPermission(highestWeightGroup, "kiwismp.allowed")) {

                    if (!userHasPermission(getUser(player.getUniqueId()), "kiwismp.level_immune")) {

                        if(!guildMember.getRoles().contains(requiredLevelRole)) {
                            removePermissionFromUser(getUser(player.getUniqueId()), "kiwismp.allowed");
                        }

                        if (!userHasPermission(getUser(player.getUniqueId()), "kiwismp.allowed") && !guildMember.getRoles().contains(requiredLevelRole)) {

                            if (!levelChecks.containsKey(player.getUniqueId())) {
                                levelChecks.put(player.getUniqueId(), 0);
                            } else {
                                levelChecks.put(player.getUniqueId(), levelChecks.get(player.getUniqueId()) + 1);
                            }

                            if (levelChecks.get(player.getUniqueId()) > 100) {
                                Component requiredLevelKickMessage = Component.empty().append(MiniMessage.miniMessage().deserialize("<white><b>Chat Level Required")).appendNewline().appendNewline().append(MiniMessage.miniMessage().deserialize(String.format("<green>You must be at least <b>%s</b> level (Level %s) to play!", getConfig().getString("discord.roles.required_level_role_name"), getConfig().getString("discord.roles.required_level_num")))).appendNewline().appendNewline().append(MiniMessage.miniMessage().deserialize("<b><gold>!</b> <reset>If you've linked the incorrect Discord account, or you're receiving this message in error, please contact Ducky."));

                                player.kick(requiredLevelKickMessage);

                                levelChecks.put(player.getUniqueId(), 0);
                            }
                        } else if (!userHasPermission(getUser(player.getUniqueId()), "kiwismp.allowed") && guildMember.getRoles().contains(requiredLevelRole)) {
                            addPermissionToUser(getUser(player.getUniqueId()), "kiwismp.allowed");
                        }
                    }

                }
            }
                // LEVEL REQUIREMENT

                if (guildMember.getId().equals(config.getString("discord.users.ducky")) && !KevSmp.luckpermsApi.getUserManager().getUser(player.getUniqueId()).getNodes().contains(Node.builder(
                        "group" + ".developer").build())) {
                    KevSmp.luckpermsApi.getUserManager().modifyUser(player.getUniqueId(), u -> {
                        u.data().add(Node.builder("group.developer").build());
                    });
                    player.sendMessage(KevSmp.componentWithPrefix(Component.text("Developer status added")));
                }

                if (!guildMember.getId().equals(config.getString("discord.users.ducky")) && KevSmp.luckpermsApi.getUserManager().getUser(player.getUniqueId()).getNodes().contains(Node.builder(
                        "group" + ".developer").build())) {
                    KevSmp.luckpermsApi.getUserManager().modifyUser(player.getUniqueId(), u -> {
                        u.data().remove(Node.builder("group.developer").build());
                    });
                    player.sendMessage(KevSmp.componentWithPrefix(Component.text("Developer status removed")));
                }

                if (guildMember.getId().equals(config.getString("discord.users.kevwie")) && !KevSmp.luckpermsApi.getUserManager().getUser(player.getUniqueId()).getNodes().contains(Node.builder(
                        "group" + ".kevwie").build())) {
                    KevSmp.luckpermsApi.getUserManager().modifyUser(player.getUniqueId(), u -> {
                        u.data().add(Node.builder("group.kevwie").build());
                    });
                    player.sendMessage(KevSmp.componentWithPrefix(Component.text("Kevwie status added")));
                }

                if (!guildMember.getId().equals(config.getString("discord.users.kevwie")) && KevSmp.luckpermsApi.getUserManager().getUser(player.getUniqueId()).getNodes().contains(Node.builder(
                        "group" + ".kevwie").build())) {
                    KevSmp.luckpermsApi.getUserManager().modifyUser(player.getUniqueId(), u -> {
                        u.data().remove(Node.builder("group.kevwie").build());
                    });
                    player.sendMessage(KevSmp.componentWithPrefix(Component.text("Kevwie status remove")));
                }


                if (guildMember.getRoles().contains(moderatorRole) && !KevSmp.luckpermsApi.getUserManager().getUser(player.getUniqueId()).getNodes().contains(Node.builder("group.moderator").build())) {
                    KevSmp.luckpermsApi.getUserManager().modifyUser(player.getUniqueId(), u -> {
                        u.data().add(Node.builder("group.moderator").build());
                    });
                    player.sendMessage(KevSmp.componentWithPrefix(Component.text("Moderator status added")));
                } else if (!guildMember.getRoles().contains(moderatorRole) && KevSmp.luckpermsApi.getUserManager().getUser(player.getUniqueId()).getNodes().contains(Node.builder("group.moderator").build())) {
                    KevSmp.luckpermsApi.getUserManager().modifyUser(player.getUniqueId(), u -> {
                        u.data().remove(Node.builder("group.moderator").build());
                    });
                    player.sendMessage(KevSmp.componentWithPrefix(Component.text("Moderator status removed")));
                }

                if (guildMember.getRoles().contains(vipRole) && !KevSmp.luckpermsApi.getUserManager().getUser(player.getUniqueId()).getNodes().contains(Node.builder("group.vip").build())) {
                    KevSmp.luckpermsApi.getUserManager().modifyUser(player.getUniqueId(), u -> {
                        u.data().add(Node.builder("group.vip").build());
                    });
                    player.sendMessage(KevSmp.componentWithPrefix(Component.text("VIP status added")));
                } else if (!guildMember.getRoles().contains(vipRole) && KevSmp.luckpermsApi.getUserManager().getUser(player.getUniqueId()).getNodes().contains(Node.builder("group.vip").build())) {
                    KevSmp.luckpermsApi.getUserManager().modifyUser(player.getUniqueId(), u -> {
                        u.data().remove(Node.builder("group.vip").build());
                    });
                    player.sendMessage(KevSmp.componentWithPrefix(Component.text("VIP status removed")));
                }

                String status = "default";

                if (guildMember.getRoles().contains(subscriberRole) && KevSmp.luckpermsApi.getUserManager().getUser(player.getUniqueId()).getNodes().contains(Node.builder("group.subscriber").build())) {
                    // no action required
                } else if (!guildMember.getRoles().contains(subscriberRole) && KevSmp.luckpermsApi.getUserManager().getUser(player.getUniqueId()).getNodes().contains(Node.builder("group.subscriber").build())) {
                    // remove from group (status lost)
                    KevSmp.luckpermsApi.getUserManager().modifyUser(player.getUniqueId(), u -> {
                        u.data().remove(Node.builder("group.subscriber").build());
                        u.data().add(Node.builder("group.default").build());
                    });
                    player.sendMessage(KevSmp.componentWithPrefix(Component.text(String.format("Your status has changed. Now %s status.", status))));
                } else if (guildMember.getRoles().contains(subscriberRole) && !KevSmp.luckpermsApi.getUserManager().getUser(player.getUniqueId()).getNodes().contains(Node.builder("group.subscriber").build())) {
                    // add to group (status gained)
                    KevSmp.luckpermsApi.getUserManager().modifyUser(player.getUniqueId(), u -> {
                        u.data().add(Node.builder("group.subscriber").build());
                    });
                    status = "subscriber";
                    player.sendMessage(KevSmp.componentWithPrefix(Component.text(String.format("Your status has changed. Now %s status.", status))));

                } else if (!guildMember.getRoles().contains(subscriberRole) && !KevSmp.luckpermsApi.getUserManager().getUser(player.getUniqueId()).getNodes().contains(Node.builder("group.subscriber"
                ).build())) {
                } else {
                }

                Component tabHeader = Component.empty().appendNewline().append(MiniMessage.miniMessage().deserialize("<b><gradient:#9AF054:#40AE43>ᴋɪᴡɪ ѕᴍᴘ</gradient></b>")).appendNewline();

                if (luckpermsApi.getUserManager().getUser(player.getUniqueId()).getNodes().contains(Node.builder("group.subscriber").build())) {
                    tabHeader = tabHeader.appendNewline().append(MiniMessage.miniMessage().deserialize("<gradient:#53B049:#86af48>Thank you fo</gradient><gradient:#86af48:#53B049" + ">r Subscribing"
                            + "!</gradient>")).appendNewline();
                }

                int onlinePlayers = Bukkit.getOnlinePlayers().size();
                tabHeader = tabHeader.appendNewline().append(MiniMessage.miniMessage().deserialize(String.format("<gray><i>(%s)", onlinePlayers == 1 ? "it's empty in here..." :
                        String.format("%s " + "players " + "online", Bukkit.getOnlinePlayers().size())))).appendNewline();

                player.sendPlayerListHeader(tabHeader);

                if (dataStorage.getBoolean(player.getUniqueId() + ".show_mod_stats")) {
                    if (player.hasPermission("kiwismp.mod")) {
                        double tps = Math.round(Bukkit.getServer().getTPS()[0]);
                        Component modStatsFooter = Component.empty().appendNewline().append(Component.text("⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯").decorate(TextDecoration.BOLD,
                                TextDecoration.STRIKETHROUGH).color(NamedTextColor.DARK_GRAY)).appendNewline().append(MiniMessage.miniMessage().deserialize("<b><color:#40AE43>Mod Stats</b>")).appendNewline().appendNewline().append(MiniMessage.miniMessage().deserialize(String.format("%s<%s>%s <b>TPS</b>", tps > 20 ? "<b><yellow>ѕᴘʀɪɴᴛɪɴɢ</b> " : "", tps > 18 ? "aqua" : tps > 16 ? "gold" : "red", tps))).appendSpace().append(Component.text("|").color(NamedTextColor.DARK_GRAY)).appendSpace().append(MiniMessage.miniMessage().deserialize(String.format("<gray>%s<dark_gray>/<gray>%s <b>AFK</b>", afkers.values().stream().filter(a -> a > AFK_THRESHOLD_SECONDS).toArray().length, onlinePlayers))).appendNewline().appendNewline().appendNewline().appendNewline();
                        int warnings = 0;

                        if (tps > 20)
                            warnings = warnings + 1;
                        if (tps < 16)
                            warnings = warnings + 1;


                        if (warnings > 0) {
                            modStatsFooter = modStatsFooter.append(MiniMessage.miniMessage().deserialize(String.format("<gold><b>Warnings: <yellow>%s", warnings)));
                        } else {
                            modStatsFooter = modStatsFooter.append(MiniMessage.miniMessage().deserialize("<gray><i>To disable this, run /modstats"));
                        }

                        if (tps > 20) {
                            modStatsFooter = modStatsFooter.appendNewline().appendNewline().append(MiniMessage.miniMessage().deserialize("<b><gold>!</b> <red>The game is ticking faster than usual"));
                        } else if (tps < 16) {
                            modStatsFooter = modStatsFooter.appendNewline().appendNewline().append(MiniMessage.miniMessage().deserialize("<b><gold>!</b> <red>The game is ticking too slow. The " +
                                    "server " + "may " + "be overloaded."));

                        }

                        player.sendPlayerListFooter(modStatsFooter);
                    }

                } else {
                    player.sendPlayerListFooter(Component.empty().appendNewline());
                }
            });
        }



        @EventHandler public void onMove (PlayerMoveEvent ev){

            //        Location worldSpawn = new Location(Bukkit.getServer().getWorld(Objects.requireNonNull(getConfig().getString("spawn.level"))), getConfig().getDouble("spawn.x"), getConfig()
            //        .getDouble("spawn" +
            //                ".y"), getConfig().getDouble("spawn" + ".z"));
            Location worldSpawn = Bukkit.getWorld(Objects.requireNonNull(getConfig().getString("spawn.level"))).getSpawnLocation();
            Location startingPos = ev.getFrom();
            boolean linked = isUserLinked(ev.getPlayer().getUniqueId());

            if (!linked) {
                if (!startingPos.equals(worldSpawn)) {
                    ev.getPlayer().teleport(worldSpawn);
                    startingPos = worldSpawn;
                } else {
                    ev.getPlayer().teleport(startingPos);
                }

            } else {
                resetAfkTime(ev.getPlayer());
            }
        }

        @EventHandler public void onBlockBreak (BlockBreakEvent ev){
            ev.setCancelled(!isUserLinked(ev.getPlayer().getUniqueId()));
        }

        @EventHandler public void onBlockPlace (BlockPlaceEvent ev){
            ev.setCancelled(!isUserLinked(ev.getPlayer().getUniqueId()));
        }

        @EventHandler public void onItemPickup (PlayerAttemptPickupItemEvent ev){
            ev.setCancelled(!isUserLinked(ev.getPlayer().getUniqueId(), false));
        }

        @EventHandler public void onItemDrop (PlayerDropItemEvent ev){
            ev.setCancelled(!isUserLinked(ev.getPlayer().getUniqueId()));
        }

        @EventHandler public void onBedEnter (PlayerBedEnterEvent ev){
            ev.setCancelled(!isUserLinked(ev.getPlayer().getUniqueId()));
        }

        @EventHandler public void onBedFailEnter (PlayerBedFailEnterEvent ev){
            ev.setCancelled(!isUserLinked(ev.getPlayer().getUniqueId(), false));
        }

        @EventHandler public void onBlockExplosion (BlockExplodeEvent ev){
            ev.blockList().removeIf(block -> block.getType().equals(Material.CHEST) && graves.containsKey(block.getLocation()));
        }

        @EventHandler public void onEntityExplode (EntityExplodeEvent ev){
            ev.blockList().removeIf(block -> block.getType().equals(Material.CHEST) && graves.containsKey(block.getLocation()));
        }

        @EventHandler public void onInteract (PlayerInteractEvent ev){
            if (!isUserLinked(ev.getPlayer().getUniqueId())) {
                ev.setCancelled(true);

                return;
            } else {
                resetAfkTime(ev.getPlayer());
            }

            if (ev.getClickedBlock() != null && ev.getClickedBlock().getType().equals(Material.CHEST)) {
                Block clickedChest = ev.getClickedBlock();
                Location chestLocation = clickedChest.getLocation();


                if (graves.containsKey(chestLocation)) {
                    Inventory graveInventory = graves.get(chestLocation);
                    String locationId = locationToString(chestLocation);
                    List<String> owners = chestConfig.getStringList("owners");


                    String stringUuid = chestConfig.getString("owners." + locationId);
                    UUID ownerUuid = UUID.fromString(stringUuid);

                    if (ev.getPlayer().getUniqueId().equals(ownerUuid)) {
                        Player player = Bukkit.getPlayer(ownerUuid);
                        if (!player.isOnline())
                            return;

                        ev.getPlayer().openInventory(graveInventory);
                        ev.setCancelled(true);
                    } else {
                        ev.setCancelled(true);
                        ev.getPlayer().sendMessage(componentWithPrefix(Component.text("This chest does not belong to you").color(NamedTextColor.RED)));
                    }
                } else
                    return;
            }
        }

        //    @EventHandler
        //    public void onInventoryClick(InventoryClickEvent ev) {
        //
        //    }

        @EventHandler public void onInventoryClose (InventoryCloseEvent ev){

            Player player = (Player) ev.getPlayer();
            Inventory targetInventory = ev.getInventory();

            if (graves.containsValue(targetInventory)) {
                if (targetInventory.isEmpty()) {
                    Location loc = getKey(graves, targetInventory);
                    String locationId = locationToString(loc);
                    chestConfig.set(locationId, null);
                    chestConfig.set("owners." + locationId, null);
                    File file = new File(getDataFolder(), "chests.yml");
                    Block block = loc.getBlock();
                    try {
                        chestConfig.save(file);

                    } catch (IOException e) {
                        block.setType(Material.AIR);
                        loadGravesMap();

                    }
                    block.setType(Material.AIR);
                    loadGravesMap();
                    graves.remove(loc);

                } else {
                    Location loc = getKey(graves, targetInventory);
                    String[] locParts = locationToString(loc).split(",");
                    World world = Bukkit.getWorld(locParts[0]);
                    int x = Integer.parseInt(locParts[1]);
                    int y = Integer.parseInt(locParts[2]);
                    int z = Integer.parseInt(locParts[3]);

                    Location location = new Location(world, x, y, z);
                    writeChest(chestConfig, location, targetInventory, player);
                }
            } else {
            }
        }


        @EventHandler public void onPlayerDamage (EntityDamageEvent ev){
            if (ev.getEntity() instanceof Player) {
                ev.setCancelled(!isUserLinked(ev.getEntity().getUniqueId()));
            }
        }

        @EventHandler public void onEntityDamage (EntityDamageByEntityEvent ev){
            if (ev.getDamager() instanceof Player) {
                ev.setCancelled(!isUserLinked(ev.getDamager().getUniqueId()));
            }
        }

        @EventHandler public void onEntityInteract (PlayerInteractEntityEvent ev){
            ev.setCancelled(!isUserLinked(ev.getPlayer().getUniqueId()));
        }

    }

