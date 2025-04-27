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

    public PacketSender(Chunk chunk, FileConfiguration config, int r) {
        this.chunk = chunk;
        this.config = config;
        this.r = r;
        this.h = config.getDouble("Radiation_Safe_Zone_Height");
    }

    public void sendPacketNorthSouth(Player p, int x, int y, int z, Material material) {
        PacketContainer packet  = new PacketContainer(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
        ArrayList<WrappedBlockData> blockArray = new ArrayList<>();
        ArrayList<Short> locationArray = new ArrayList<>();

        packet.getSectionPositions().write(0, new BlockPosition(x, y, z));//Chunk coordinates

        blockArray = addBlockToArray(blockArray, material, 256);

        for (int he = 0; he < 16; he++) {
            for (int i = 0; i < 16; i++) {
                locationArray.add(setShortLocation(i, he, r));//cords within a chunk
            }
        }

        sendPackage(p, setChangeData(blockArray, locationArray, packet));
    }

    public void sendPacketWestEast(Player p, int x, int y, int z, Material material) {
        PacketContainer packet  = new PacketContainer(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
        ArrayList<WrappedBlockData> blockArray = new ArrayList<>();
        ArrayList<Short> locationArray = new ArrayList<>();

        packet.getSectionPositions().write(0, new BlockPosition(x, y, z));//Chunk coordinates

        blockArray = addBlockToArray(blockArray, material, 256);

        for (int he = 0; he < 16; he++) {
            for (int i = 0; i < 16; i++) {
                locationArray.add(setShortLocation(r, he, i));//cords within a chunk
            }
        }

        sendPackage(p, setChangeData(blockArray, locationArray, packet));
    }

    public void destroyWall(Player p, int x, int y, int z) {
        PacketContainer packet  = new PacketContainer(PacketType.Play.Server.BLOCK_CHANGE);
        //ArrayList<WrappedBlockData> blockArray = new ArrayList<>();
        //ArrayList<Short> locationArray = new ArrayList<>();

        packet.getSectionPositions().writeSafely(0, new BlockPosition(x/16, y/16, z/16));

        //blockArray = addBlockToArray(blockArray, Material.RED_STAINED_GLASS, 1);
        //locationArray.add(setShortLocation(x, y, z));

        packet.getBlockData().writeSafely(0, WrappedBlockData.createData(Material.RED_STAINED_GLASS));
        packet.getShorts().writeSafely(0, setShortLocation(x, y, z));

        //p.sendMessage("x: " + x + "    y: " + y + "   z: " + z);
        //p.sendMessage("x: " + x/16 + "    y: " + y/16 + "   z: " + z/16);

        sendPackage(p, packet/*setChangeData(blockArray, locationArray, packet)*/);
    }

    public ArrayList<WrappedBlockData> addBlockToArray(ArrayList<WrappedBlockData> blockArray, Material material, int times) {
        for (int i = 0; i <times; i++) {
            blockArray.add(WrappedBlockData.createData(material));
        }
        return blockArray;
    }

    public short setShortLocation(int x, int y, int z) {
        x = x & 0xF;
        y = y & 0xF;
        z = z & 0xF;
        return (short) (x << 8 | z << 4 | y);
    }

    public PacketContainer setChangeData(ArrayList<WrappedBlockData> blockDat, ArrayList<Short> blockPositions, PacketContainer packet) {

        WrappedBlockData[] blockData = blockDat.toArray(new WrappedBlockData[0]);
        Short[] blockLocsShort = blockPositions.toArray(new Short[0]);
        short[] blockLocations = ArrayUtils.toPrimitive(blockLocsShort);


        packet.getBlockDataArrays().writeSafely(0, blockData);
        packet.getShortArrays().writeSafely(0, blockLocations);

        return packet;
    }

    private void sendPackage(Player p, PacketContainer packet) {
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(p, packet);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
