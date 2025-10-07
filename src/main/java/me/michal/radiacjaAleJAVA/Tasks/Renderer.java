package me.michal.radiacjaAleJAVA.Tasks;

import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Renderer {
    Player player;
    World world;
    int radius;
    int playerX;
    int playerY;
    int playerZ;
    int radiusChunk;
    int playerViewDistance;
    PacketSender packetSender;
    private static final List<Set<Point>> circles = new ArrayList<>();
    static {
        for (int i = 0; i < 91; i++) {
            circles.add(getDonut(0, i)); // cache circle of radius i
        }
    }
    public enum MovementDirection {
        APPROACHING,    // player getting closer
        RECEDING,       // player getting away
        PARALLEL        // moving horizontally along the wall
    }

    public Renderer(Player player, int radius, int playerViewDistance) {
        this.player = player;
        this.world = player.getWorld();
        this.radius = radius;
        this.playerX = (int) player.getX();
        this.playerY = (int) player.getY();
        this.playerZ = (int) player.getZ();
        this.radiusChunk = Math.floorDiv(radius, 16);
        this.playerViewDistance = playerViewDistance * 16;
        this.packetSender = new PacketSender();
    }

    public void renderCircleXWall(MovementDirection direction ,int radius, boolean renderHole) {
        List<BlockState> blocks = new ArrayList<>();
        Set<Point> circle = null, donut = null, hole = null;

        int z = (int) (this.radius * Math.signum(playerZ));
        int baseY = playerY + 1; // head height

        switch (direction) {
            case APPROACHING -> circle = circles.get(radius);
            case RECEDING   -> donut = getDonut(radius + 1, radius + 7);
            case PARALLEL   -> {
                donut = getDonut(radius + 1, radius + 7);
                circle = circles.get(radius);
            }
        }

        if (renderHole) {
            hole = getDonut(0, radius - 80);
            for (Point point : hole) {
                int x = playerX + point.x;
                if (-this.radius < x && x < this.radius) continue;
                int y = baseY + point.y;
                Block block = world.getBlockAt(x, y, z);
                blocks.add(block.getState());
            }
        }

        if (donut != null) {
            for (Point point : donut) {
                int x = playerX + point.x;
                if (-this.radius < x && x < this.radius) continue;
                int y = baseY + point.y;
                Block block = world.getBlockAt(x, y, z);
                blocks.add(block.getState());
            }
        }

        if (circle != null) {
            if (hole != null) circle.removeAll(hole);
            for (Point point : circle) {
                int x = playerX + point.x;
                if (-this.radius < x && x < this.radius) continue;
                int y = baseY + point.y;
                Block block = world.getBlockAt(x, y, z);
                Material type = block.getType();
                if (type == Material.AIR || type == Material.WATER) {
                    BlockState state = block.getState();
                    state.setType(Material.WHITE_STAINED_GLASS);
                    blocks.add(state);
                }
            }
        }

        player.sendBlockChanges(blocks);
    }

    public void renderCircleZWall(MovementDirection direction, int radius, boolean renderHole) {
        List<BlockState> blocks = new ArrayList<>();
        Set<Point> circle = null, donut = null, hole = null;

        int x = (int) (this.radius * Math.signum(playerX));
        int baseY = playerY + 1; // head height

        switch (direction) {
            case APPROACHING -> circle = circles.get(radius);
            case RECEDING   -> donut = getDonut(radius + 1, radius + 7);
            case PARALLEL   -> {
                donut = getDonut(radius + 1, radius + 7);
                circle = circles.get(radius);
            }
        }

        if (renderHole) {
            hole = getDonut(0, radius - 80);
            for (Point point : hole) {
                int y = baseY + point.y;
                int z = playerZ + point.x;
                if (-this.radius < z && z < this.radius) continue;
                Block block = world.getBlockAt(x, y, z);
                blocks.add(block.getState());
            }
        }

        if (donut != null) {
            for (Point point : donut) {
                int y = baseY + point.y;
                int z = playerZ + point.x;
                if (-this.radius < z && z < this.radius) continue;
                BlockState state = world.getBlockAt(x, y, z).getState();
                blocks.add(state);
            }
        }

        if (circle != null) {
            if (hole != null) circle.removeAll(hole);
            for (Point point : circle) {
                int y = baseY + point.y;
                int z = playerZ + point.x;
                if (-this.radius < z && z < this.radius) continue;
                Block block = world.getBlockAt(x, y, z);
                Material type = block.getType();
                if (type == Material.AIR || type == Material.WATER) {
                    BlockState state = block.getState();
                    state.setType(Material.WHITE_STAINED_GLASS);
                    blocks.add(state);
                }
            }
        }

        player.sendBlockChanges(blocks);
    }

    public int[] calculateCoordinates() {
        int[] result = new int[4];

        result[0] = Math.floorDiv(Math.max(-radius + 16, playerX - playerViewDistance), 16); //min //+ 1 for corners
        result[1] = Math.floorDiv(Math.min(radius - 16, playerX + playerViewDistance), 16); //max //- 1 for corners
        result[2] = Math.floorDiv(Math.max(-radius + 16, playerZ - playerViewDistance), 16); //min //+ 1 for corners
        result[3] = Math.floorDiv(Math.min(radius - 16, playerZ + playerViewDistance), 16); //max //- 1 for corners

        return result;
    }

    private PacketSender.Corner getCorner(int[] c, int r) {
        if (c[0] == -r + 1 && c[2] == -r + 1) return PacketSender.Corner.NORTH_WEST;
        else if (c[0] == -r + 1 && c[2] == r - 1) return PacketSender.Corner.SOUTH_WEST;
        else if (c[1] == r - 1 && c[3] == r - 1) return PacketSender.Corner.SOUTH_EAST;
        else if (c[1] == r - 1 && c[3] == -r + 1) return PacketSender.Corner.SOUTH_WEST;
        else return null;
    }

    public void renderHole(PacketSender.AxisTemplate axis, int holeRadius) {
        List<int[]> hole = getCircle(holeRadius);
        int playerY = this.playerY + 1; //plus one for the hole center to be at player head
        switch (axis) {
            case X_AXIS -> {
                for (int[] point : hole) {
                    double x = playerX + point[0];
                    double y = playerY + point[1];
                    double z = this.radius * Math.signum(playerZ);
                    sendBlock(player, x, y, z);
                }
            }
            case Z_AXIS -> {
                for (int[] point : hole) {
                    double x = this.radius * Math.signum(playerX);
                    double y = playerY + point[1];
                    double z = playerZ + point[0];
                    sendBlock(player, x, y, z);
                }
            }
        }
    }

    public List<int[]> getCircle(int radius) {
        ArrayList<int[]> points = new ArrayList<>();
        for (int r = 0; r <= radius; r++) {
            for (int angle = 0; angle < 360; angle++) {
                double rad = Math.toRadians(angle);
                int x = (int) Math.round(r * Math.cos(rad));
                int y = (int) Math.round(r * Math.sin(rad));
                points.add(new int[]{x, y});
            }
        }
        return points;
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


    private void sendBlock(Player player, double x, double y, double z) {
        Location location = new Location(player.getWorld(), x, y, z);
        BlockData blockData = location.getBlock().getBlockData();
        player.sendBlockChange(location, blockData);
    }
}
