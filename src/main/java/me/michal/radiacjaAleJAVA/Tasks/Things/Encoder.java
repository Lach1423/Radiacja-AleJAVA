package me.michal.radiacjaAleJAVA.Tasks.Things;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.HexFormat;

public class Encoder {
    public byte[] getLock(Player player) {
        String lock;
        try {
            URL url = URI.create("https://raw.githubusercontent.com/Lach1423/Radiacja/refs/heads/main/src/lock.txt").toURL();

            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            lock = br.readLine();
            br.close();

        } catch (Exception err) {
            player.sendMessage(ChatColor.RED + "Zły version.txt" + "\n" + err);
            return null;
        }
        return HexFormat.of().parseHex(lock);
    }
    public byte[] getKey(String key) {
        return Base64.getDecoder().decode(key.substring(0, 24));
    }
}
