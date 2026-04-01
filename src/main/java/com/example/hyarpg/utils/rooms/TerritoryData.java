package com.example.hyarpg.utils.rooms;

// Hytale Imports
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector3i;

public class TerritoryData {

    // The light well is 1x3x1, centered in the 57x57x57 territory
    public static final int TERRITORY_HALF = 28; // (57 - 1) / 2

    @SuppressWarnings("unchecked")
    public static final BuilderCodec<TerritoryData> CODEC = BuilderCodec
            .builder(TerritoryData.class, TerritoryData::new)
            .append(new KeyedCodec<>("CenterX", Codec.INTEGER), (d, v) -> d.centerX = v, d -> d.centerX).add()
            .append(new KeyedCodec<>("CenterY", Codec.INTEGER), (d, v) -> d.centerY = v, d -> d.centerY).add()
            .append(new KeyedCodec<>("CenterZ", Codec.INTEGER), (d, v) -> d.centerZ = v, d -> d.centerZ).add()
            .build();

    // Center of the territory — the base block of the light well (bottom of the 1x3x1)
    private int centerX, centerY, centerZ;

    // Required for codec deserialization
    private TerritoryData() {}

    public TerritoryData(Vector3i lightWellBase) {
        this.centerX = lightWellBase.x;
        this.centerY = lightWellBase.y;
        this.centerZ = lightWellBase.z;
    }

    public Vector3i getCenter() { return new Vector3i(centerX, centerY, centerZ); }

    // Min/max bounds of the 57x57x57 territory
    public int getMinX() { return centerX - TERRITORY_HALF; }
    public int getMinY() { return centerY - TERRITORY_HALF + 1; }
    public int getMinZ() { return centerZ - TERRITORY_HALF; }
    public int getMaxX() { return centerX + TERRITORY_HALF; }
    public int getMaxY() { return centerY + TERRITORY_HALF + 1; }
    public int getMaxZ() { return centerZ + TERRITORY_HALF; }

    public boolean contains(int x, int y, int z) {
        return x >= getMinX() && x <= getMaxX()
                && y >= getMinY() && y <= getMaxY()
                && z >= getMinZ() && z <= getMaxZ();
    }

    // The light well occupies the base position + 2 blocks above (1x3x1)
    public boolean isLightWellBlock(int x, int y, int z) {
        return x == centerX && z == centerZ
                && y >= centerY && y <= centerY + 2;
    }

    @Override
    public String toString() {
        return "TerritoryData{center=(" + centerX + "," + centerY + "," + centerZ + ")"
                + ", bounds=(" + getMinX() + "-" + getMaxX()
                + ", " + getMinY() + "-" + getMaxY()
                + ", " + getMinZ() + "-" + getMaxZ() + ")}";
    }
}