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
    int r;
    double h;

    public PacketSender(Chunk chunk, FileConfiguration config, int r) {
        this.chunk = chunk;
        this.config = config;
        this.r = r;
        this.h = config.getDouble("Radiation_Safe_Zone_Height");
    }

    public void sendPacket(Player p) {
        int v = Math.min(p.getClientViewDistance(), p.getViewDistance());
        if (chunk.getX() >= 0) {
            int min;
            int max;
            if (chunk.getZ() >= 0) {
                min = v;
                max = (int) (Math.ceil(r/16) - chunk.getZ());
            } else {
                min = (int) (Math.ceil(r/16) + chunk.getZ());
                max = v + 1;
            }
            for (int z = -min; z < max; z++) {
                for (int h = -1; h < 2; h++) {
                    sendPacketEast(p, (int) (p.getY()/16) + h, chunk.getZ() + z);
                }
            }
        } else {
            int min;
            int max;
            if (chunk.getZ() >= 0) {
                min = v;
                max = (int) (Math.ceil(r/16) - Math.abs(chunk.getZ()));
            } else {
                min = (int) (Math.ceil(r/16) - Math.abs(chunk.getZ()));
                max = v + 1;
            }
            for (int z = -min; z < max; z++) {
                for (int h = -1; h < 2; h++) {
                    sendPacketWest(p, (int) (p.getY()/16) + h, chunk.getZ() + z);
                }
            }
        }

        if (chunk.getZ() >= 0) {
            int min;
            int max;
            if (chunk.getX() >= 0) {
                min = v;
                max = (int) (Math.ceil(r/16) - Math.abs(chunk.getZ()));
            } else {
                min = (int) (Math.ceil(r/16) - Math.abs(chunk.getZ())) + 1;
                max = v;
                p.sendMessage(min + " " + max);
            }
            for (int z = -min; z < max; z++) {
                for (int h = -1; h < 2; h++) {
                    sendPacketSouth(p, chunk.getX() + z, (int) (p.getY()/16) + h);
                    p.sendMessage("Sented packet");
                }
            }
        } else {
            int min;
            int max;
            if (chunk.getX() >= 0) {
                min = v;
                max = (int) (Math.ceil(r/16) - Math.abs(chunk.getX()));
            } else {
                min = (int) (Math.ceil(r/16) - Math.abs(chunk.getX()));
                max = v + 1;
            }
            for (int z = -min; z < max; z++) {
                for (int h = -1; h < 2; h++) {
                    sendPacketNorth(p, (int) (p.getY()/16) + h, chunk.getX() + z);
                }
            }
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
    public void sendPacketWest(Player p, int y, int z) {
        PacketContainer packet  = new PacketContainer(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
        ArrayList<WrappedBlockData> blockArray = new ArrayList<>();
        ArrayList<Short> locationArray = new ArrayList<>();

        packet.getSectionPositions().write(0, new BlockPosition(-(int) Math.ceil(r/16), y, z));//Chunk coordinates

        blockArray = addGlassToArray(blockArray);

        for (int he = 0; he < 16; he++) {
            for (int i = 0; i < 16; i++) {
                locationArray.add(setShortLocation(0, he, i));//cords within a chunk
            }
        }

        sendPackage(p, setChangeData(blockArray, locationArray, packet));
    }
    public void sendPacketSouth(Player p, int x, int y) {
        PacketContainer packet  = new PacketContainer(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
        ArrayList<WrappedBlockData> blockArray = new ArrayList<>();
        ArrayList<Short> locationArray = new ArrayList<>();

        packet.getSectionPositions().write(0, new BlockPosition(x, y, (int) Math.ceil(r/16)));//Chunk coordinates

        blockArray = addGlassToArray(blockArray);

        for (int he = 0; he < 16; he++) {
            for (int i = 0; i < 16; i++) {
                locationArray.add(setShortLocation(i, he, 0));//cords within a chunk
            }
        }

        sendPackage(p, setChangeData(blockArray, locationArray, packet));
    }
    public void sendPacketNorth(Player p, int x, int y) {
        PacketContainer packet  = new PacketContainer(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
        ArrayList<WrappedBlockData> blockArray = new ArrayList<>();
        ArrayList<Short> locationArray = new ArrayList<>();

        packet.getSectionPositions().write(0, new BlockPosition(x, y, -(int) Math.ceil(r/16)));//Chunk coordinates

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

    public void sendPacketNorthSouth(Player p, int x, int y, int z) {
        PacketContainer packet  = new PacketContainer(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
        ArrayList<WrappedBlockData> blockArray = new ArrayList<>();
        ArrayList<Short> locationArray = new ArrayList<>();

        packet.getSectionPositions().write(0, new BlockPosition(x, y, z));//Chunk coordinates

        blockArray = addGlassToArray(blockArray);

        for (int he = 0; he < 16; he++) {
            for (int i = 0; i < 16; i++) {
                locationArray.add(setShortLocation(i, he, r));//cords within a chunk
            }
        }

        sendPackage(p, setChangeData(blockArray, locationArray, packet));
    }

    public void sendPacketWestEast(Player p, int x, int y, int z) {
        PacketContainer packet  = new PacketContainer(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
        ArrayList<WrappedBlockData> blockArray = new ArrayList<>();
        ArrayList<Short> locationArray = new ArrayList<>();

        packet.getSectionPositions().write(0, new BlockPosition(x, y, z));//Chunk coordinates

        blockArray = addGlassToArray(blockArray);

        for (int he = 0; he < 16; he++) {
            for (int i = 0; i < 16; i++) {
                locationArray.add(setShortLocation(r, he, i));//cords within a chunk
            }
        }

        sendPackage(p, setChangeData(blockArray, locationArray, packet));
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
