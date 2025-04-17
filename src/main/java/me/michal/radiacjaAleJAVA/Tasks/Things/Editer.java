package me.michal.radiacjaAleJAVA.Tasks.Things;

import me.michal.radiacjaAleJAVA.RadiacjaAleJAVA;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class Editer {
    RadiacjaAleJAVA plugin;
    private static File file;

    public Editer(RadiacjaAleJAVA plugin) {
        this.plugin = plugin;
    }

    public void editStat(Player p, String[] a) {
        File world = p.getWorld().getWorldFolder();
        File stats = new File(world, "stats");

        Editer.file = new File(stats, p.getUniqueId() + ".json");

        String ketToEdit = a[1] + a[2].substring(0, a[2].lastIndexOf(":"));
        String newValue = a[2].substring(a[2].lastIndexOf(":") + 1);

        try {
            // Create ObjectMapper instance
            ObjectMapper mapper = new ObjectMapper();

            // Read JSON file into a Map
            Map<String, Object> jsonMap = mapper.readValue(file, Map.class);

            // Modify the key value : "minecraft:villager_spawn_egg":1
            jsonMap.put(ketToEdit, newValue);

            // Write the updated Map back to the JSON file
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, jsonMap);

            System.out.println("JSON file updated successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
