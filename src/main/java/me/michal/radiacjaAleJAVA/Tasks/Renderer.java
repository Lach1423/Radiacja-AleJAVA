package me.michal.radiacjaAleJAVA.Tasks;

import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.entity.Player;

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

    public void renderWall() {
        int[] coordinates = calculateCoordinates();
        int chunkX;
        int chunkY;
        int chunkZ;

        for (int h = playerY - 2; h <= playerY + 2; h++) {
            chunkY = h;
            chunkZ = (int) (radiusChunk * Math.signum(playerZ));
            for (int i = coordinates[0]; i <= coordinates[1]; i++) {
                chunkX = i;
                PacketContainer packet = packetSender.writeChunkCoordinatesIntoPacket(PacketSender.AxisTemplate.X_AXIS, chunkX, chunkY, chunkZ);
                packetSender.sendPackage(player, packet);
            }

            chunkX = (int) (radiusChunk * Math.signum(playerX));
            for (int i = coordinates[2]; i <= coordinates[3]; i++) {
                chunkZ = i;
                PacketContainer packet = packetSender.writeChunkCoordinatesIntoPacket(PacketSender.AxisTemplate.Z_AXIS, chunkX, chunkY, chunkZ);
                packetSender.sendPackage(player, packet);
            }
            //render Corners
        }
    }

    public int[] calculateCoordinates() {
        int[] result = new int[4];

        result[0] = Math.floorDiv(Math.max(-radius, playerX - playerViewDistance), 16); //min
        result[1] = Math.floorDiv(Math.min(radius, playerX + playerViewDistance), 16); //max
        result[2] = Math.floorDiv(Math.max(-radius, playerZ - playerViewDistance), 16); //min
        result[3] = Math.floorDiv(Math.min(radius, playerZ + playerViewDistance), 16); //max

        return result;
    }

    public void renderHole() {}
}
