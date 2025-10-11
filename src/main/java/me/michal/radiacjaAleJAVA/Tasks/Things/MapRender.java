package me.michal.radiacjaAleJAVA.Tasks.Things;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;
import org.jetbrains.annotations.NotNull;

public class MapRender extends MapRenderer {
    String[] options = {
            "Gamemode", "Info", "Lightning", "Experience", "Refuse Death", "Accept Death", "Say as", "Set Name","Set Cooldown", "Create Region", "Remove Region", "Ender Chest"
    };
    boolean redrawNeeded;
    int currentOption;

    public MapRender(int currentOption) {
        redrawNeeded = true;
        this.currentOption = currentOption;
    }

    @Override
    public void render(@NotNull MapView mapView, @NotNull MapCanvas canvas, @NotNull Player player) {
        if (redrawNeeded) {
            int[] currents = getCurrents();
            canvas.drawText(5, 5, MinecraftFont.Font, options[currents[0]]);
            canvas.drawText(5, 15, MinecraftFont.Font, ">" + options[currents[1]]);
            canvas.drawText(5, 25, MinecraftFont.Font, options[currents[2]]);
            redrawNeeded = false;
        }
    }

    public int[] getCurrents() {
        int[] currents = new int[3];
        for (int i = 0; i < 3; i++) {
            currents[i] = (currentOption + i) % 12;
        }
        return currents;
    }
}
