package com.example.hyarpg.utils.rooms;

// Hytale Imports
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector3i;

// Java
import javax.annotation.Nullable;
import java.util.UUID;

public class TerritoryData {

    // The light well is 1x3x1, centered in the 57x57x57 territory
    public static final int TERRITORY_HALF = 28; // (57 - 1) / 2

    @SuppressWarnings("unchecked")
    public static final BuilderCodec<TerritoryData> CODEC = BuilderCodec
            .builder(TerritoryData.class, TerritoryData::new)
            .append(new KeyedCodec<>("CenterX", Codec.INTEGER), (d, v) -> d.centerX = v, d -> d.centerX).add()
            .append(new KeyedCodec<>("CenterY", Codec.INTEGER), (d, v) -> d.centerY = v, d -> d.centerY).add()
            .append(new KeyedCodec<>("CenterZ", Codec.INTEGER), (d, v) -> d.centerZ = v, d -> d.centerZ).add()
            .append(new KeyedCodec<>("OwnerUuid", Codec.STRING),
                    (d, v) -> d.ownerUuid = v != null ? UUID.fromString(v) : null,
                    d -> d.ownerUuid != null ? d.ownerUuid.toString() : null).add()
            .build();

    // Center of the territory — the base block of the light well (bottom of the 1x3x1)
    private int centerX, centerY, centerZ;

    @Nullable
    private UUID ownerUuid;

    // Required for codec deserialization
    private TerritoryData() {}

    public TerritoryData(Vector3i lightWellBase, UUID ownerUuid) {
        this.centerX = lightWellBase.x;
        this.centerY = lightWellBase.y;
        this.centerZ = lightWellBase.z;
        this.ownerUuid = ownerUuid;
    }

    public Vector3i getCenter() { return new Vector3i(centerX, centerY, centerZ); }

    @Nullable
    public UUID getOwnerUuid() { return ownerUuid; }

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
                + ", owner=" + ownerUuid
                + ", bounds=(" + getMinX() + "-" + getMaxX()
                + ", " + getMinY() + "-" + getMaxY()
                + ", " + getMinZ() + "-" + getMaxZ() + ")}";
    }
}