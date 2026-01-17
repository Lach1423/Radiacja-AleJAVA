package me.michal.radiacjaAleJAVA.Tasks;

import com.sk89q.worldedit.math.Vector3;
import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RadiationVisualizer {
    Player player;
    int playerX;
    int playerY;
    int playerZ;
    int radius;
    Location spawnpoint;
    private static final List<Set<Point>> circles = new ArrayList<>();
    static {
        for (int i = 0; i < 20; i++) {
            circles.add(getDonut(0, i)); // cache circle of radius i
        }
    }
    public enum MovementDirection {
        APPROACHING,    // player getting closer
        RECEDING,       // player getting away
        PARALLEL        // moving horizontally along the wall
    }
    Set<Material> ignoredTypes = Set.of(
            Material.AIR,
            Material.WATER,
            Material.KELP,
            Material.SEAGRASS,
            Material.TALL_SEAGRASS
    );
    public enum RadiationType {
        GLASS,
        FORCEFIELD,
        NOTHING
    }

    public RadiationVisualizer(Player player, Location playerLocation, int radius, Location spawnpoint) {
        this.player = player;
        this.playerX = playerLocation.getBlockX();
        this.playerY = playerLocation.getBlockY() + 1;
        this.playerZ = playerLocation.getBlockZ();
        this.radius = radius;
        this.spawnpoint = spawnpoint;
    }

    public void handleGlassRadiation(int distanceToRadiationX, int distanceToRadiationZ, Location oldLocation) {
        RadiationVisualizer.MovementDirection direction;
        if (distanceToRadiationX <= 15) {
            int oldDistance = getDistanceToRadiation(oldLocation, radius, Axis.X);
            direction = getDirection(oldDistance,  distanceToRadiationX);

            renderGlass(direction, distanceToRadiationX, Axis.X);
        }
        if (distanceToRadiationZ <= 15) {
            int oldDistance = getDistanceToRadiation(oldLocation, radius, Axis.Z);
            direction = getDirection(oldDistance,  distanceToRadiationZ);

            renderGlass(direction, distanceToRadiationZ, Axis.Z);
        }
    }

    public int getDistanceToRadiation(Location location, int radius, Axis axis) {
        int real = switch (axis) {
            case X -> Math.abs(location.getBlockZ()) - Math.abs(spawnpoint.getBlockZ());
            case Y -> 0;
            case Z -> Math.abs(location.getBlockX()) - Math.abs(spawnpoint.getBlockX());
        };
        return Math.min(Math.abs(real - (-radius)), Math.abs(real - radius));
    }

    private RadiationVisualizer.MovementDirection getDirection(int oldDistance, int newDistance) {
        if (newDistance < oldDistance) return RadiationVisualizer.MovementDirection.APPROACHING;
        if (newDistance > oldDistance) return RadiationVisualizer.MovementDirection.RECEDING;
        return RadiationVisualizer.MovementDirection.PARALLEL;
    }

    private void renderGlass(MovementDirection direction, int distance, Axis axis) {
        int r = 16 - distance;
        Set<Point> pointsToRender = new HashSet<>();
        Set<Point> pointsToDeRender = new HashSet<>();
        switch (direction) {
            case APPROACHING : pointsToRender = circles.get(r); break;
            case RECEDING    : pointsToDeRender = getDonut(r + 1, r + 4); break;
            case PARALLEL    : pointsToRender = circles.get(r); pointsToDeRender = getDonut(r + 1, r + 4); break;
        }

        int isNearEastRadiation = switch (axis) {
            case Axis.X -> player.getLocation().getBlockZ() > spawnpoint.getBlockZ() ? 1 : -1;
            case Axis.Z -> player.getLocation().getBlockX() > spawnpoint.getBlockX() ? 1 : -1;
            default -> 0;
        };

        List<BlockState> blocks = new ArrayList<>();
        if (!pointsToRender.isEmpty()) {
            Set<Vector3> blocksToRender = calculateBlocks(distance, pointsToRender, isNearEastRadiation, axis);
            blocks.addAll(getBlockStates(blocksToRender, true));
        }

        if (!pointsToDeRender.isEmpty()) {
            if (direction == MovementDirection.RECEDING && distance == 15) {
                pointsToDeRender.add( new Point(-1, 0));
                pointsToDeRender.add( new Point(1, 0));
                pointsToDeRender.add( new Point(0, 1));
                pointsToDeRender.add( new Point(0, -1));
                pointsToDeRender.add( new Point(0, 0));
            }
            Set<Vector3> blocksToDeRender = calculateBlocks(distance, pointsToDeRender, isNearEastRadiation, axis);
            blocks.addAll(getBlockStates(blocksToDeRender, false));
        }
        player.sendBlockChanges(blocks);
    }

    private Set<Vector3> calculateBlocks(int distance, Set<Point> pointsToCalculate, int isNearNorthEastRadiation, Axis axis) {
        Set<Vector3> calculatedBlocks = new HashSet<>();
        int radius = this.radius - 1;
        int minBorder;
        int maxBorder;
        switch (axis) {
            case Axis.X :
                minBorder = spawnpoint.getBlockX() - radius;
                maxBorder = spawnpoint.getBlockX() + radius;
                for (Point point : pointsToCalculate) {
                int x = playerX + point.x;
                if (x < minBorder || maxBorder < x) continue;
                int y = playerY + point.y;
                if (y < -64 || 320 < y) continue;
                int z = playerZ + ((distance - 1) * isNearNorthEastRadiation);
                calculatedBlocks.add(new Vector3(x, y, z));
            }
            break;
            case Axis.Z : for (Point point : pointsToCalculate) {
                minBorder = spawnpoint.getBlockZ() - radius;
                maxBorder = spawnpoint.getBlockZ() + radius;
                int x = playerX + ((distance - 1) * isNearNorthEastRadiation);
                int y = playerY + point.y;
                if (y < -64 || 320 < y) continue;
                int z = playerZ + point.x;
                if (z < minBorder || maxBorder < z) continue;
                calculatedBlocks.add(new Vector3(x, y, z));
            }
            break;
        }
        return calculatedBlocks;
    }

    private List<BlockState> getBlockStates(Set<Vector3> blocksToRender, boolean setGlass) {
        World world = player.getWorld();
        List<BlockState> blocks = new ArrayList<>();
        for (Vector3 vector : blocksToRender) {
            Block block = world.getBlockAt(vector.blockX(), vector.blockY(), vector.blockZ());
            Material type = block.getType();
            if (ignoredTypes.contains(type)) {
                BlockState state = block.getState();
                if (setGlass) state.setType(Material.WHITE_STAINED_GLASS);
                blocks.add(state);
            }
        }
        return blocks;
    }

    private static Set<Point> getDonut(int minRadius, int maxRadius) {
        Set<Point> donut = new HashSet<>();

        for (int r = minRadius; r <= maxRadius; r++) {
            for (int angle = 0; angle < 360; angle++) {
                double rad = Math.toRadians(angle);
                int x = (int) Math.round(r * Math.cos(rad));
                int y = (int) Math.round(r * Math.sin(rad));
                donut.add(new Point(x, y));
            }
        }
        return donut;
    }
}
