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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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

    public enum Corner { NORTH_EAST, NORTH_WEST, SOUTH_EAST, SOUTH_WEST }
    public static Map<Corner, List<Short>> cornerLocations = new EnumMap<>(Corner.class);
    static {
        for (Corner c : Corner.values()) {
            cornerLocations.put(c, new ArrayList<>());
        }
    }
    public static Map<Corner, PacketContainer> cornerTemplates = new EnumMap<>(Corner.class);
    static {
        for (Corner c : Corner.values()) {
            cornerTemplates.put(c, new PacketContainer(PacketType.Play.Server.MULTI_BLOCK_CHANGE));
        }
    }
    private static final Map<Corner, int[]> points = new EnumMap<>(Corner.class);
    static {
        for (Corner c : Corner.values()) {
            points.put(c, new int[2]); // not cornerTemplates
        }
    }

    public enum AxisTemplate {
        X_AXIS,
        Z_AXIS,
    }
    private final Logger log;

    public PacketSender() {
        log = Logger.getLogger("miguel");
    }

    public static void updateLocationArrays(int radius) {
        ArrayList<Short> locationArrayXAxis = new ArrayList<>();
        ArrayList<Short> locationArrayZAxis = new ArrayList<>();

        for (int he = 0; he < 16; he++) {
            for (int i = 0; i < 16; i++) {
                locationArrayXAxis.add(setShortLocation(i, he, radius));//cords within a chunk
                locationArrayZAxis.add(setShortLocation(radius, he, i));
            }
        }
        int radiusOffsetInChunk = Math.floorMod(radius, 16); ;
        points.clear();
        points.put(Corner.SOUTH_EAST, new int[]{radiusOffsetInChunk, radiusOffsetInChunk});
        points.put(Corner.SOUTH_WEST, new int[]{radiusOffsetInChunk - 1, radiusOffsetInChunk});
        points.put(Corner.NORTH_EAST, new int[]{radiusOffsetInChunk - 1, radiusOffsetInChunk - 1});
        points.put(Corner.NORTH_WEST, new int[]{radiusOffsetInChunk, radiusOffsetInChunk - 1});

        fillCornerArray(radius);
        writeArraysToPackets(locationArrayXAxis, locationArrayZAxis);
    }

    public static void fillCornerArray(int radius) {
        for (Corner c : Corner.values()) {
            cornerLocations.get(c).clear();
            for (int h = 0; h < 16; h++) {
                for (int i = 0; i <= points.get(c)[0]; i++) {
                    cornerLocations.get(c).add(setShortLocation(i, h, radius));
                }
                for (int i = 0; i < points.get(c)[1]; i++) {
                    cornerLocations.get(c).add(setShortLocation(radius, h, i));
                    123//Kornery są źle , chunki mają bloki zawsze od lewej górnej
                }
            }
        }
    }

    public static void writeArraysToPackets(ArrayList<Short> xAxisLocations, ArrayList<Short> zAxisLocations) {
        WrappedBlockData[] blockData = materialArray.toArray(new WrappedBlockData[0]);

        short[] blockLocations = ArrayUtils.toPrimitive(xAxisLocations.toArray(new Short[0]));
        packetTemplateXAxis.getBlockDataArrays().writeSafely(0, blockData);
        packetTemplateXAxis.getShortArrays().writeSafely(0, blockLocations);

        blockLocations = ArrayUtils.toPrimitive(zAxisLocations.toArray(new Short[0]));
        packetTemplateZAxis.getBlockDataArrays().writeSafely(0, blockData);
        packetTemplateZAxis.getShortArrays().writeSafely(0, blockLocations);

        for (Corner c : Corner.values()) {
            blockData = getMaterialArray(cornerLocations.get(c).size(), new ArrayList<>());
            blockLocations = ArrayUtils.toPrimitive(cornerLocations.get(c).toArray(new Short[0]));
            cornerTemplates.get(c).getBlockDataArrays().writeSafely(0, blockData);
            cornerTemplates.get(c).getShortArrays().writeSafely(0, blockLocations);
        }
    }

    private static WrappedBlockData[] getMaterialArray(int size, ArrayList<WrappedBlockData> array) {
        for (int i = 0; i < size; i++) {
            array.add(WrappedBlockData.createData(Material.WHITE_STAINED_GLASS));
        }
        return array.toArray(new WrappedBlockData[0]);
    }

    private static short setShortLocation(int x, int y, int z) {
        x = x & 0xF;
        y = y & 0xF;
        z = z & 0xF;
        return (short) (x << 8 | z << 4 | y);
    }

    public PacketContainer writeCoordinatesToCorner(int radius, Corner corner, int h) {
        int[] coords = new int[2];
        PacketContainer packet;
        switch (corner) {
            case Corner.SOUTH_EAST -> {
                coords[0] = radius;
                coords[1] = radius;
            }
            case Corner.SOUTH_WEST -> {
                coords[0] = -radius;
                coords[1] = radius;
            }
            case Corner.NORTH_WEST -> {
                coords[0] = -radius;
                coords[1] = -radius;
            }
            case Corner.NORTH_EAST -> {
                coords[0] = radius;
                coords[1] = -radius;
            }
        }
        packet = cornerTemplates.get(corner).deepClone();
        packet.getSectionPositions().write(0, new BlockPosition(coords[0], h, coords[1]));
        return packet;
    }

    public PacketContainer writeChunkCoordinatesIntoPacket(AxisTemplate axis, int x, int y, int z) {
        PacketContainer packet;
        switch (axis) {
            case X_AXIS -> packet = packetTemplateXAxis.deepClone();
            case Z_AXIS -> packet = packetTemplateZAxis.deepClone();
            default -> throw new IllegalArgumentException("Unknown axis: " + axis);
        }

        packet.getSectionPositions().write(0, new BlockPosition(x, y, z));//Chunk coordinates
        return packet;
    }

    public void sendPackage(Player p, PacketContainer packet) {
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(p, packet);
        } catch (InvocationTargetException e) {
            log.warning("Failed to send packet to " + p);
        }
    }
    public void sendPackages(Player p, List<PacketContainer> packets) {
        for (PacketContainer packet : packets) {
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(p, packet);
            } catch (InvocationTargetException e) {
                log.warning("Failed to send packet to " + p);
            }
        }
    }
}
