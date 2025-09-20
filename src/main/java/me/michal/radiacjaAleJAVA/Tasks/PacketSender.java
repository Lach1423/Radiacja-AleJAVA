package me.michal.radiacjaAleJAVA.Tasks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class PacketSender {
    private static final ArrayList<WrappedBlockData> materialArray = new ArrayList<>();
    static {
        Material material = Material.WHITE_STAINED_GLASS; // or whatever
        for (int i = 0; i < 256; i++) {
            materialArray.add(WrappedBlockData.createData(material));
        }
    }
    public static final PacketContainer packetTemplateXAxis = new PacketContainer(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
    public static final PacketContainer packetTemplateZAxis = new PacketContainer(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
    public static final PacketContainer packetTemplateCorner  = new PacketContainer(PacketType.Play.Server.MULTI_BLOCK_CHANGE);

    public static void updateLocationArrays(int radius) {
        ArrayList<Short> locationArrayXAxis = new ArrayList<>();
        ArrayList<Short> locationArrayZAxis = new ArrayList<>();
        //ArrayList<Short> locationArrayCorner = new ArrayList<>();
        int radiusBlockInChunk = radius % 16;

        for (int he = 0; he < 16; he++) {
            /*for (int i = 0; i < radiusBlockInChunk; i++) {
                locationArrayCorner.add(setShortLocation(radius, he, i));
                locationArrayCorner.add(setShortLocation(i, he, radius));
            }*/
            for (int i = 0; i < 16; i++) {
                locationArrayXAxis.add(setShortLocation(i, he, radius));//cords within a chunk
                locationArrayZAxis.add(setShortLocation(radius, he, i));
            }
            //locationArrayCorner.removeLast();
        }

        writeArraysToPacket(locationArrayXAxis, true/*packetTemplateXAxis*/);
        writeArraysToPacket(locationArrayZAxis, false/*packetTemplateZAxis*/);
        //writeDataToPacket(locationArrayCorner, packetTemplateCorner);
    }

    public static void writeArraysToPacket(ArrayList<Short> locationArray, boolean shouldUpdateXTemplate/*PacketContainer packet*/) {
        WrappedBlockData[] blockData = materialArray.toArray(new WrappedBlockData[0]);
        short[] blockLocations = ArrayUtils.toPrimitive(locationArray.toArray(new Short[0]));

        /*packet.getBlockDataArrays().writeSafely(0, blockData);
        packet.getShortArrays().writeSafely(0, blockLocations);*/

        if (shouldUpdateXTemplate) {
            packetTemplateXAxis.getBlockDataArrays().writeSafely(0, blockData);
            packetTemplateXAxis.getShortArrays().writeSafely(0, blockLocations);
        } else {
            packetTemplateZAxis.getBlockDataArrays().writeSafely(0, blockData);
            packetTemplateZAxis.getShortArrays().writeSafely(0, blockLocations);
        } //If upper version won't work
    }

    private static short setShortLocation(int x, int y, int z) {
        x = x & 0xF;
        y = y & 0xF;
        z = z & 0xF;
        return (short) (x << 8 | z << 4 | y);
    }

    public PacketSender() {
    }

    public PacketContainer writeChunkCoordinatesIntoPacket(boolean shouldUseXAxis/*PacketContainer packetContainer*/, int x, int y, int z) {
        PacketContainer packet; /*= packetContainer.deepClone();*/
        if (shouldUseXAxis) {
            packet = packetTemplateXAxis.deepClone();
        } else {
            packet = packetTemplateZAxis.deepClone();
        }
        packet.getSectionPositions().write(0, new BlockPosition(x, y, z));//Chunk coordinates
        return packet;
    }

    public void sendPackage(Player p, PacketContainer packet) {
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(p, packet);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
