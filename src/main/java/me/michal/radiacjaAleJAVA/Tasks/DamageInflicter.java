package me.michal.radiacjaAleJAVA.Tasks;

import me.michal.radiacjaAleJAVA.RadiacjaAleJAVA;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import static me.michal.radiacjaAleJAVA.RadiacjaAleJAVA.*;

public class DamageInflicter extends BukkitRunnable {

    RadiacjaAleJAVA plugin;

    public DamageInflicter(RadiacjaAleJAVA plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : affectedPlayers) {
            if (!curedPlayers.containsKey(player)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 5, 10));
            }
        }
    }
}
