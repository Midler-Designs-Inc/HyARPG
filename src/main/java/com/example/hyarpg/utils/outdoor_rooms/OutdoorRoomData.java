package com.example.hyarpg.utils.outdoor_rooms;

// Hytale Imports
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

// Java Imports
import javax.annotation.Nullable;
import java.util.*;
import org.joml.Vector3i;

public class OutdoorRoomData {

    @SuppressWarnings("unchecked")
    public static final BuilderCodec<OutdoorRoomData> CODEC = BuilderCodec
        .builder(OutdoorRoomData.class, OutdoorRoomData::new)
        .append(new KeyedCodec<>("MinX", Codec.INTEGER), (d, v) -> d.minX = v, d -> d.minX).add()
        .append(new KeyedCodec<>("MinY", Codec.INTEGER), (d, v) -> d.minY = v, d -> d.minY).add()
        .append(new KeyedCodec<>("MinZ", Codec.INTEGER), (d, v) -> d.minZ = v, d -> d.minZ).add()
        .append(new KeyedCodec<>("MaxX", Codec.INTEGER), (d, v) -> d.maxX = v, d -> d.maxX).add()
        .append(new KeyedCodec<>("MaxY", Codec.INTEGER), (d, v) -> d.maxY = v, d -> d.maxY).add()
        .append(new KeyedCodec<>("MaxZ", Codec.INTEGER), (d, v) -> d.maxZ = v, d -> d.maxZ).add()
        .append(new KeyedCodec<>("FenceY", Codec.INTEGER), (d, v) -> d.fenceY = v, d -> d.fenceY).add()
        .append(new KeyedCodec<>("RoomType", Codec.STRING), (d, v) -> d.designatedRoomType = v, d -> d.designatedRoomType).add()
        .append(
                // Serialize as flat array where each key appears N times matching its count
                // e.g. {Fence_Wood: 2} -> ["Fence_Wood", "Fence_Wood"]
                new KeyedCodec<>("BlockKeys", new ArrayCodec(Codec.STRING, String[]::new)),
                (d, v) -> {
                    d.blockKeysInside.clear();
                    for (String key : v) d.blockKeysInside.merge(key, 1, Integer::sum);
                },
                d -> {
                    List<String> flat = new ArrayList<>();
                    d.blockKeysInside.forEach((key, count) -> {
                        for (int i = 0; i < count; i++) flat.add(key);
                    });
                    return flat.toArray(new String[0]);
                }
        ).add()
        .build();

    private int minX, minY, minZ;
    private int maxX, maxY, maxZ;

    // The Y level of the lowest fence ring — scan bounds are derived from this
    private int fenceY;

    @Nullable
    private String designatedRoomType;

    // Multiset — tracks how many of each block key are in the space (includes fence + floor)
    private Map<String, Integer> blockKeysInside = new HashMap<>();

    // Required for codec deserialization
    private OutdoorRoomData() {}

    public OutdoorRoomData(Vector3i minBound, Vector3i maxBound, int fenceY) {
        this.minX = minBound.x;
        this.minY = minBound.y;
        this.minZ = minBound.z;
        this.maxX = maxBound.x;
        this.maxY = maxBound.y;
        this.maxZ = maxBound.z;
        this.fenceY = fenceY;
    }

    public Vector3i getMinBound() { return new Vector3i(minX, minY, minZ); }
    public Vector3i getMaxBound() { return new Vector3i(maxX, maxY, maxZ); }

    public int getFenceY() { return fenceY; }

    public int getInteriorSizeX() { return maxX - minX + 1; }
    public int getInteriorSizeZ() { return maxZ - minZ + 1; }

    // Returns the full map with counts — used by requirement scoring
    public Map<String, Integer> getBlockCountsInside() { return blockKeysInside; }

    // Increments the count for a block key
    public void addBlockKey(String key) {
        blockKeysInside.merge(key, 1, Integer::sum);
    }

    // Decrements the count for a block key, removing it entirely when count reaches zero
    public void removeBlockKey(String key) {
        blockKeysInside.computeIfPresent(key, (k, count) -> count <= 1 ? null : count - 1);
    }

    // Clears all block key counts — called before a fresh scan
    public void clearBlockKeys() {
        blockKeysInside.clear();
    }

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
        return "OutdoorRoomData{type=" + designatedRoomType
                + ", min=(" + minX + "," + minY + "," + minZ + ")"
                + ", max=(" + maxX + "," + maxY + "," + maxZ + ")"
                + ", sizeX=" + getInteriorSizeX()
                + ", sizeZ=" + getInteriorSizeZ()
                + ", fenceY=" + fenceY + "}";
    }
}