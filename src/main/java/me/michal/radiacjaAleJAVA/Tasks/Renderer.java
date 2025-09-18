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
        this.playerY = (int) player.getY();
        this.playerZ = (int) player.getZ();
        this.radiusChunk = Math.floorDiv(radius, 16);
        this.playerViewDistance = playerViewDistance;
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
            for (int i = Math.floorDiv(coordinates[0], 16); i <= Math.floorDiv(coordinates[1], 16); i++) {
                chunkX = i;
                PacketContainer packet = packetSender.writeChunkCoordinatesIntoPacket(player, PacketSender.templatePacketZAxis, chunkX, chunkY, chunkZ);
                packetSender.sendPackage(player, packet);
            }
            chunkX = (int) (radiusChunk * Math.signum(playerX));
            for (int i = Math.floorDiv(coordinates[2], 16); i <= Math.floorDiv(coordinates[3], 16); i++) {
                chunkZ = i;
                PacketContainer packet = packetSender.writeChunkCoordinatesIntoPacket(player, PacketSender.templatePacketXAxis, chunkX, chunkY, chunkZ);
                packetSender.sendPackage(player, packet);
            }
        }
    }

    public int[] calculateCoordinates() {
        int[] result = new int[4];

        result[0] = Math.max(-radius, playerX - playerViewDistance); //min
        result[1] = Math.min(radius, playerX + playerViewDistance); //max
        result[2] = Math.max(-radius, playerZ - playerViewDistance); //min
        result[3] = Math.min(radius, playerZ + playerViewDistance); //max

        return result;
    }

    public void renderHole() {}
}
