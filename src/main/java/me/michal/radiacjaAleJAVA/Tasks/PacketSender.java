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
import java.util.stream.IntStream;

public class PacketSender {
    Chunk chunk;
    FileConfiguration config;
    double r;
    double h;

    public PacketSender(Chunk chunk, FileConfiguration config) {
        this.chunk = chunk;
        this.config = config;
        this.r = config.getDouble("Radiation_Safe_Zone_Size");
        this.h = config.getDouble("Radiation_Safe_Zone_Height");
    }

    public void sendPacket(Player p) {
        if (chunk.getX() >= 0) {
            for (int he = -1; he < 2; he++) {
                for (int i = -1; i < 2; i++) {
                    sendPacketEast(p, (int) (p.getY()/16) + he, chunk.getZ() + i);
                }
            }
        } else {
            sendPacketWest(p, r);
        }

        if (chunk.getZ() >= 0) {
            sendPacketSouth(p, r);
        } else {
            sendPacketNorth(p, r);
        }
    }

    public void sendPacketEast(Player p, int y, int z) {
        PacketContainer packet  = new PacketContainer(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
        ArrayList<WrappedBlockData> blockArray = new ArrayList<>();
        ArrayList<Short> locationArray = new ArrayList<>();

        packet.getSectionPositions().write(0, new BlockPosition((int) Math.ceil(r/16), y, z));//Chunk coordinates

        blockArray = addGlassToArray(blockArray);

        for (int he = 0; he < 16; he++) {
            for (int i = 0; i < 16; i++) {
                locationArray.add(setShortLocation(0, he, i));//cords within a chunk
            }
        }

        sendPackage(p, setChangeData(blockArray, locationArray, packet));
    }
    public void sendPacketWest(Player p, double r) {
        PacketContainer packet  = new PacketContainer(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
        ArrayList<WrappedBlockData> blockArray = new ArrayList<>();
        ArrayList<Short> locationArray = new ArrayList<>();

        packet.getSectionPositions().write(0, new BlockPosition(-(int) Math.ceil(r/16), (int) (p.getY()/16), chunk.getZ()));//Chunk coordinates

        blockArray = addGlassToArray(blockArray);

        for (int he = 0; he < 16; he++) {
            for (int i = 0; i < 16; i++) {
                locationArray.add(setShortLocation(0, he, i));//cords within a chunk
            }
        }

        sendPackage(p, setChangeData(blockArray, locationArray, packet));
    }
    public void sendPacketSouth(Player p, double r) {
        PacketContainer packet  = new PacketContainer(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
        ArrayList<WrappedBlockData> blockArray = new ArrayList<>();
        ArrayList<Short> locationArray = new ArrayList<>();

        packet.getSectionPositions().write(0, new BlockPosition(chunk.getX(), (int) (p.getY()/16), (int) Math.ceil(r/16)));//Chunk coordinates

        blockArray = addGlassToArray(blockArray);

        for (int he = 0; he < 16; he++) {
            for (int i = 0; i < 16; i++) {
                locationArray.add(setShortLocation(i, he, 0));//cords within a chunk
            }
        }

        sendPackage(p, setChangeData(blockArray, locationArray, packet));
    }
    public void sendPacketNorth(Player p, double r) {
        PacketContainer packet  = new PacketContainer(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
        ArrayList<WrappedBlockData> blockArray = new ArrayList<>();
        ArrayList<Short> locationArray = new ArrayList<>();

        packet.getSectionPositions().write(0, new BlockPosition(chunk.getX(), (int) (p.getY()/16), -(int) Math.ceil(r/16)));//Chunk coordinates

        blockArray = addGlassToArray(blockArray);

        for (int he = 0; he < 16; he++) {
            for (int i = 0; i < 16; i++) {
                locationArray.add(setShortLocation(i, he, 0));//cords within a chunk
            }
        }

        sendPackage(p, setChangeData(blockArray, locationArray, packet));
    }

    public short setShortLocation(int x, int y, int z) {
        x = x & 0xF;
        y = y & 0xF;
        z = z & 0xF;
        return (short) (x << 8 | z << 4 | y << 0);
    }

    public PacketContainer setChangeData(ArrayList<WrappedBlockData> blockDat, ArrayList<Short> blockPositions, PacketContainer packet) {

        WrappedBlockData[] blockData = blockDat.toArray(new WrappedBlockData[0]);
        Short[] blockLocsShort = blockPositions.toArray(new Short[0]);
        short[] blockLocations = ArrayUtils.toPrimitive(blockLocsShort);


        packet.getBlockDataArrays().writeSafely(0, blockData);
        packet.getShortArrays().writeSafely(0, blockLocations);

        return packet;
    }

    public ArrayList<WrappedBlockData> addGlassToArray(ArrayList<WrappedBlockData> blockArray) {
        for (int i = 0; i <256; i++) {
            blockArray.add(WrappedBlockData.createData(Material.WHITE_STAINED_GLASS));
        }
        return blockArray;
    }

    public void sendPackage(Player p, PacketContainer packet) {
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(p, packet);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
