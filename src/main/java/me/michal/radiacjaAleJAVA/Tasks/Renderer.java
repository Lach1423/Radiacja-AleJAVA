package me.michal.radiacjaAleJAVA.Tasks;

import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class Renderer {
    Player player;
    int radius;
    int playerX;
    int playerY;
    int playerZ;
    int radiusChunk;
    int playerViewDistance;
    PacketSender packetSender;

    public Renderer(Player player, int radius, int playerViewDistance) {
        this.player = player;
        this.radius = radius;
        this.playerX = (int) player.getX();
        this.playerY = Math.floorDiv((int) player.getY(), 16);
        this.playerZ = (int) player.getZ();
        this.radiusChunk = Math.floorDiv(radius, 16);
        this.playerViewDistance = playerViewDistance * 16;
        this.packetSender = new PacketSender();
    }

    public void renderWall(PacketSender.AxisTemplate axis, boolean skip) {
        PacketContainer packet;
        List<PacketContainer> packets = new ArrayList<>();
        int[] coordinates = calculateCoordinates();
        int chunkX;
        int chunkY;
        int chunkZ;

        for (int h = playerY - 2; h <= playerY + 2; h++) {
            chunkY = h;
            switch (axis) {
                case X_AXIS -> {
                    chunkZ = (int) (radiusChunk * Math.signum(playerZ));
                    for (int i = coordinates[0]; i <= coordinates[1]; i++) {
                        chunkX = i;
                        if (!skip || i < -1 || i > 1) {
                            packet = packetSender.writeChunkCoordinatesIntoPacket(axis, chunkX, chunkY, chunkZ);
                            packets.add(packet);
                        }
                    }
                }
                case Z_AXIS -> {
                    chunkX = (int) (radiusChunk * Math.signum(playerX));
                    for (int i = coordinates[2]; i <= coordinates[3]; i++) {
                        chunkZ = i;
                        if (skip && i >= -1 && i <= 1) {
                            return;
                        }
                        packet = packetSender.writeChunkCoordinatesIntoPacket(PacketSender.AxisTemplate.Z_AXIS, chunkX, chunkY, chunkZ);
                        packets.add(packet);
                    }
                }
            }
            packetSender.sendPackages(player, packets);

            PacketSender.Corner corner = getCorner(coordinates, Math.floorDiv(radius, 16));
            player.sendMessage(String.valueOf(corner));
            if (corner != null) {
                packet = packetSender.writeCoordinatesToCorner(Math.floorDiv(radius, 16), corner, h);//render Corners
                player.sendMessage("Wysyłam packet: " + packet);
                packetSender.sendPackage(player, packet);
            }
        }
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

    public void renderHole(PacketSender.AxisTemplate axis, int playerDistanceToWall) {
        List<int[]> hole = getCircle(playerDistanceToWall);
        /*switch (playerDistanceToWall) {
            case 1 -> hole = getCircle(1);
            case 2 -> hole = getCircle(2);
            case 3 -> hole = getCircle(3);
            case 4 -> hole = getCircle(4);
            case 5 -> hole = getCircle(5);
            case 6 -> hole = getCircle(6);
            case 7 -> hole = getCircle(7);
            case 8 -> hole = getCircle(8);
        }*/
        for (int h = (int) (player.getY() + 8); h >= player.getY(); h--) {
            switch (axis) {
                case X_AXIS -> {
                    for (int i = playerX - 8; i < playerX + 8; i++) {
                        if (!hole.contains(new int[] {i, h})) {
                            Location loc = new Location(player.getWorld(), i, h, radius * Math.signum(playerZ));
                            player.sendBlockChange(loc, loc.getBlock().getBlockData());
                        }
                    }
                }
                case Z_AXIS -> {
                    for (int i = playerZ - 8; i < playerZ + 8; i++) {
                        if (!hole.contains(new int[] {i, h})) {
                            Location loc = new Location(player.getWorld(), radius * Math.signum(playerX), h, i);
                            player.sendBlockChange(loc, loc.getBlock().getBlockData());
                        }
                    }
                }
            }
        }
    }

    private List<int[]> getCircle(int radius) {
        List<int[]> points = new ArrayList<>();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                if (x * x + y * y <= radius * radius) {
                    points.add(new int[] {x, y});
                }
            }
        }
        return points;
    }
}
