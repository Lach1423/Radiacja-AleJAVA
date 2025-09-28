package me.michal.radiacjaAleJAVA.Tasks;

import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
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
        this.playerY = (int) player.getY();
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
        int playerY = Math.floorDiv(this.playerY, 16);

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

    public void renderHole(PacketSender.AxisTemplate axis, int radius) {
        List<int[]> hole = getCircle(radius);
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

    private List<int[]> getCircle(int radius) {
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

    private void sendBlock(Player player, double x, double y, double z) {
        Location location = new Location(player.getWorld(), x, y, z);
        BlockData blockData = location.getBlock().getBlockData();
        player.sendBlockChange(location, blockData);
    }
}
