package me.michal.radiacjaAleJAVA.Tasks;

import me.michal.radiacjaAleJAVA.RadiacjaAleJAVA;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import static me.michal.radiacjaAleJAVA.RadiacjaAleJAVA.*;

public class CuredPlayersTracker extends BukkitRunnable {

    RadiacjaAleJAVA plugin;

    public CuredPlayersTracker(RadiacjaAleJAVA plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : curedPlayers.keySet()) {
            long timePassed = System.currentTimeMillis() - curedPlayers.get(player);
            long timeLeft = duration - timePassed;
            BossBar curedBar = curedBars.get(player);
            if (timeLeft > 0 && timeLeft < 600000) {
                double progressOfBar = (double) timeLeft / duration;
                curedBar.setProgress(progressOfBar);
                curedBar.setTitle(ChatColor.GREEN + "Działanie płynu Lugola");
            } else if (timeLeft <= 0) {
                curedPlayers.remove(player);
                plugin.removeCuredBar(player, curedBar);
                Bukkit.broadcastMessage(ChatColor.RED + "LELELugol uległ wygaśnięciu dla " + ChatColor.GOLD + player.getName());
            } else {
                curedBar.setProgress(1);
                int timeLeftInMinutes = (int) timeLeft / 60000;
                String minut_Odmienioned = (timeLeftInMinutes > 20 && (timeLeftInMinutes % 10 >= 2 && timeLeftInMinutes % 10 <= 4)) ? "minuty" : "minut";
                curedBar.setTitle(ChatColor.GREEN + "Lugol upływa za " + timeLeftInMinutes + " " + minut_Odmienioned);
            }
        }
    }
}
