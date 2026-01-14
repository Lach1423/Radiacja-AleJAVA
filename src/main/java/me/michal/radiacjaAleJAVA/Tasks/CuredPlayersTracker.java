package me.michal.radiacjaAleJAVA.Tasks;

import me.michal.radiacjaAleJAVA.RadiacjaAleJAVA;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import static me.michal.radiacjaAleJAVA.RadiacjaAleJAVA.*;

public class CuredPlayersTracker extends BukkitRunnable {

    RadiacjaAleJAVA plugin;
    FileConfiguration config;

    public CuredPlayersTracker(RadiacjaAleJAVA plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public void run() {
        for (Player player : curedPlayers.keySet()) {
            long timePassed = System.currentTimeMillis() - curedPlayers.get(player);
            long timeLeft = config.getLong("Duration") - timePassed;
            BossBar curedBar = curedBars.get(player);
            if (timeLeft > 0 && timeLeft < 600000) {
                float progressOfBar = (float) timeLeft / config.getLong("Duration");
                curedBar.progress(progressOfBar);
                curedBar.name(Component.text("Działanie płynu Lugola", NamedTextColor.GREEN));
            } else if (timeLeft <= 0) {
                curedPlayers.remove(player);
                plugin.removeCuredBar(player, curedBar);
                Bukkit.getServer().broadcast(Component.text("LELELugol uległ wygaśnięciu dla ",  NamedTextColor.RED)
                        .append(player.displayName().color(NamedTextColor.RED)));
            } else {
                curedBar.progress(1);
                int timeLeftInMinutes = (int) timeLeft / 60000;
                String minutyOdmienioned = (timeLeftInMinutes > 20 && (timeLeftInMinutes % 10 >= 2 && timeLeftInMinutes % 10 <= 4)) ? "minuty" : "minut";
                String msg = String.format("Lugol upływa za g%d %s", timeLeftInMinutes, minutyOdmienioned);
                curedBar.name(Component.text(msg, NamedTextColor.GREEN));
            }
        }
    }
}
