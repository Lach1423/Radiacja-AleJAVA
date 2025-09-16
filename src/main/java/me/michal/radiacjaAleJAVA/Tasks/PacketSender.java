package me.michal.radiacjaAleJAVA.Tasks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class PacketSender {
    Chunk chunk;
    FileConfiguration config;
    int r;
    double h;
    private static final ArrayList<WrappedBlockData> materialArray = new ArrayList<>();
    static {
        Material material = Material.WHITE_STAINED_GLASS; // or whatever
        for (int i = 0; i < 256; i++) {
            materialArray.add(WrappedBlockData.createData(material));
        }
    }
    public static final PacketContainer templatePacketXAxis  = new PacketContainer(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
    public static final PacketContainer templatePacketZAxis  = new PacketContainer(PacketType.Play.Server.MULTI_BLOCK_CHANGE);

    public static void updateLocationArrays(int r) {
        ArrayList<Short> locationArrayXAxis = new ArrayList<>();
        ArrayList<Short> locationArrayZAxis = new ArrayList<>();
        for (int he = 0; he < 16; he++) {
            for (int i = 0; i < 16; i++) {
                locationArrayXAxis.add(setShortLocation(r, he, i));//cords within a chunk
                locationArrayZAxis.add(setShortLocation(i, he, r));
            }
        }
        writeDataToPacket(locationArrayXAxis, templatePacketXAxis);
        writeDataToPacket(locationArrayZAxis, templatePacketZAxis);
    }

    public static void writeDataToPacket(ArrayList<Short> locationArray, PacketContainer packet) {
        WrappedBlockData[] blockData = materialArray.toArray(new WrappedBlockData[0]);
        short[] blockLocations = ArrayUtils.toPrimitive(locationArray.toArray(new Short[0]));

        packet.getBlockDataArrays().writeSafely(0, blockData);
        packet.getShortArrays().writeSafely(0, blockLocations);

        /*if (shouldUpdateXTemplate) {
            templatePacketXAxis.getBlockDataArrays().writeSafely(0, blockData);
            templatePacketXAxis.getShortArrays().writeSafely(0, blockLocations);
        } else {
            templatePacketZAxis.getBlockDataArrays().writeSafely(0, blockData);
            templatePacketZAxis.getShortArrays().writeSafely(0, blockLocations);
        }*/ //If upper version won't work
    }

    private static short setShortLocation(int x, int y, int z) {
        x = x & 0xF;
        y = y & 0xF;
        z = z & 0xF;
        return (short) (x << 8 | z << 4 | y);
    }

    public PacketSender(Chunk chunk, FileConfiguration config, int r) {
        this.chunk = chunk;
        this.config = config;
        this.r = r;
        this.h = config.getDouble("Radiation_Safe_Zone_Height");
    }

    /*public void sendPacketNorthSouth(Player p, int x, int y, int z) {
        PacketContainer packet  = new PacketContainer(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
        ArrayList<Short> locationArray = new ArrayList<>();

        packet.getSectionPositions().write(0, new BlockPosition(x, y, z));//Chunk coordinates

        for (int he = 0; he < 16; he++) {
            for (int i = 0; i < 16; i++) {
                locationArray.add(setShortLocation(i, he, r));//cords within a chunk
            }
        }

        sendPackage(p, writeDataToPacket(locationArray, packet));
    }*/

    public void writeChunkCoordinatesIntoPacket(Player p, PacketContainer packetContainer, int x, int y, int z) {
        PacketContainer packet  = packetContainer.deepClone();
        packet.getSectionPositions().write(0, new BlockPosition(x, y, z));//Chunk coordinates
        sendPackage(p, packet);
    }

    private void sendPackage(Player p, PacketContainer packet) {
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(p, packet);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
