package com.example.hyarpg.utils.rooms;

// Hytale Imports
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.math.vector.Vector3i;

// Java Imports
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RoomData {

    @SuppressWarnings("unchecked")
    public static final BuilderCodec<RoomData> CODEC = BuilderCodec
            .builder(RoomData.class, RoomData::new)
            .append(new KeyedCodec<>("MinX", Codec.INTEGER), (d, v) -> d.minX = v, d -> d.minX).add()
            .append(new KeyedCodec<>("MinY", Codec.INTEGER), (d, v) -> d.minY = v, d -> d.minY).add()
            .append(new KeyedCodec<>("MinZ", Codec.INTEGER), (d, v) -> d.minZ = v, d -> d.minZ).add()
            .append(new KeyedCodec<>("MaxX", Codec.INTEGER), (d, v) -> d.maxX = v, d -> d.maxX).add()
            .append(new KeyedCodec<>("MaxY", Codec.INTEGER), (d, v) -> d.maxY = v, d -> d.maxY).add()
            .append(new KeyedCodec<>("MaxZ", Codec.INTEGER), (d, v) -> d.maxZ = v, d -> d.maxZ).add()
            .append(new KeyedCodec<>("RoomType", Codec.STRING), (d, v) -> d.designatedRoomType = v, d -> d.designatedRoomType).add()
            .append(
                    new KeyedCodec<>("BlockKeys", new ArrayCodec(Codec.STRING, String[]::new)),
                    (d, v) -> d.blockKeysInside = new HashSet<>(Arrays.asList(v)),
                    d -> d.blockKeysInside.toArray(new String[0])
            ).add()
            .build();

    private int minX, minY, minZ;
    private int maxX, maxY, maxZ;

    @Nullable
    private String designatedRoomType;

    private Set<String> blockKeysInside = new HashSet<>();

    // Required for codec deserialization
    private RoomData() {}

    public RoomData(Vector3i minBound, Vector3i maxBound) {
        this.minX = minBound.x;
        this.minY = minBound.y;
        this.minZ = minBound.z;
        this.maxX = maxBound.x;
        this.maxY = maxBound.y;
        this.maxZ = maxBound.z;
    }

    public Vector3i getMinBound() { return new Vector3i(minX, minY, minZ); }
    public Vector3i getMaxBound() { return new Vector3i(maxX, maxY, maxZ); }

    public int getInteriorSizeX() { return maxX - minX + 1; }
    public int getInteriorSizeY() { return maxY - minY + 1; }
    public int getInteriorSizeZ() { return maxZ - minZ + 1; }

    public Set<String> getBlockKeysInside() { return blockKeysInside; }

    @Nullable
    public String getDesignatedRoomType() { return designatedRoomType; }
    public void setDesignatedRoomType(@Nullable String type) { this.designatedRoomType = type; }

    public boolean containsInterior(int x, int y, int z) {
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    public boolean containsWithWalls(int x, int y, int z) {
        return x >= minX - 1 && x <= maxX + 1
                && y >= minY - 1 && y <= maxY + 1
                && z >= minZ - 1 && z <= maxZ + 1;
    }

    public int getCenterX() { return (minX + maxX) / 2; }
    public int getCenterY() { return (minY + maxY) / 2; }
    public int getCenterZ() { return (minZ + maxZ) / 2; }

    @Override
    public String toString() {
        return "RoomData{type=" + designatedRoomType
                + ", min=(" + minX + "," + minY + "," + minZ + ")"
                + ", max=(" + maxX + "," + maxY + "," + maxZ + ")"
                + ", sizeX=" + getInteriorSizeX()
                + ", sizeY=" + getInteriorSizeY()
                + ", sizeZ=" + getInteriorSizeZ() + "}";
    }
}