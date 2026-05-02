package com.example.hyarpg.worldgen;

// Hytale Imports
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class PrefabRecord {

    public enum Type { SURFACE, UNDERGROUND, SURFACE_DUNGEON, UNDERGROUND_DUNGEON, SHRINE }

    public static final BuilderCodec<PrefabRecord> CODEC = BuilderCodec
        .builder(PrefabRecord.class, PrefabRecord::new)
        .append(new KeyedCodec<>("Type",     Codec.STRING),  (d, v) -> d.type     = Type.valueOf(v), d -> d.type.name()).add()
        .append(new KeyedCodec<>("Filename", Codec.STRING),  (d, v) -> d.filename = v,               d -> d.filename).add()
        .append(new KeyedCodec<>("AnchorX",  Codec.INTEGER), (d, v) -> d.anchorX  = v,               d -> d.anchorX).add()
        .append(new KeyedCodec<>("AnchorY",  Codec.INTEGER), (d, v) -> d.anchorY  = v,               d -> d.anchorY).add()
        .append(new KeyedCodec<>("AnchorZ",  Codec.INTEGER), (d, v) -> d.anchorZ  = v,               d -> d.anchorZ).add()
        .append(new KeyedCodec<>("MinX",     Codec.INTEGER), (d, v) -> d.minX     = v,               d -> d.minX).add()
        .append(new KeyedCodec<>("MinY",     Codec.INTEGER), (d, v) -> d.minY     = v,               d -> d.minY).add()
        .append(new KeyedCodec<>("MinZ",     Codec.INTEGER), (d, v) -> d.minZ     = v,               d -> d.minZ).add()
        .append(new KeyedCodec<>("MaxX",     Codec.INTEGER), (d, v) -> d.maxX     = v,               d -> d.maxX).add()
        .append(new KeyedCodec<>("MaxY",     Codec.INTEGER), (d, v) -> d.maxY     = v,               d -> d.maxY).add()
        .append(new KeyedCodec<>("MaxZ",     Codec.INTEGER), (d, v) -> d.maxZ     = v,               d -> d.maxZ).add()
        .build();

    // what was placed and where
    private Type   type;
    private String filename;
    private int    anchorX, anchorY, anchorZ;

    // world-space 3D bounds for player containment checks
    private int minX, minY, minZ;
    private int maxX, maxY, maxZ;

    // required for codec deserialization
    private PrefabRecord() {}

    public PrefabRecord(Type type, String filename, int anchorX, int anchorY, int anchorZ, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.type     = type;
        this.filename = filename;
        this.anchorX  = anchorX; this.anchorY = anchorY; this.anchorZ = anchorZ;
        this.minX = minX; this.minY = minY; this.minZ = minZ;
        this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
    }

    // check if a world position falls inside this prefab's 3D bounds
    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    // horizontal distance from anchor — used by compass to find closest
    public double distanceTo(int x, int z) {
        return Math.sqrt((anchorX - x) * (anchorX - x) + (anchorZ - z) * (anchorZ - z));
    }

    public Type   getType()     { return type; }
    public String getFilename() { return filename; }
    public int    getAnchorX()  { return anchorX; }
    public int    getAnchorY()  { return anchorY; }
    public int    getAnchorZ()  { return anchorZ; }
    public int    getMinX()     { return minX; }
    public int    getMinY()     { return minY; }
    public int    getMinZ()     { return minZ; }
    public int    getMaxX()     { return maxX; }
    public int    getMaxY()     { return maxY; }
    public int    getMaxZ()     { return maxZ; }
}
