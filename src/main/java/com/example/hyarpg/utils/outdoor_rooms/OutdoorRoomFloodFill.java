package com.example.hyarpg.utils.outdoor_rooms;

// Hytale Imports
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.protocol.DrawType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;

// Java Imports
import javax.annotation.Nullable;
import java.util.*;

public class OutdoorRoomFloodFill {

    @Nullable
    public static OutdoorRoomData detectOutdoorSpaceFromPlacedBlock(World world, Vector3i placedBlockPos, BlockType placingBlockType) {
        if (!isBoundary(placingBlockType)) return null;

        // Find the lowest fence ring Y at or below the placed block
        int fenceY = findLowestBoundaryRingY(world, placedBlockPos);
        if (fenceY == Integer.MIN_VALUE) return null;

        return detectOutdoorSpace(world, new Vector3i(placedBlockPos.x, fenceY, placedBlockPos.z));
    }

    @Nullable
    public static OutdoorRoomData detectOutdoorSpaceFromBrokenBlock(World world, Vector3i brokenBlockPos) {
        // After a break, scan downward from the broken position to find any remaining ring
        int fenceY = findLowestBoundaryRingY(world, brokenBlockPos);
        if (fenceY == Integer.MIN_VALUE) return null;

        return detectOutdoorSpace(world, new Vector3i(brokenBlockPos.x, fenceY, brokenBlockPos.z));
    }

    @Nullable
    private static OutdoorRoomData detectOutdoorSpace(World world, Vector3i fencePos) {
        int fenceY = fencePos.y;

        // --- Walk the four cardinal directions to find perimeter extents --- //
        int maxPosX = walkAxis(world, fencePos, 1, 0, fenceY);
        int minNegX = walkAxis(world, fencePos, -1, 0, fenceY);
        int maxPosZ = walkAxis(world, fencePos, 0, 1, fenceY);
        int minNegZ = walkAxis(world, fencePos, 0, -1, fenceY);

        if (maxPosX == Integer.MIN_VALUE || minNegX == Integer.MIN_VALUE
                || maxPosZ == Integer.MIN_VALUE || minNegZ == Integer.MIN_VALUE) return null;

        // Interior bounds (one block inside the fence perimeter)
        int interiorMinX = minNegX + 1;
        int interiorMaxX = maxPosX - 1;
        int interiorMinZ = minNegZ + 1;
        int interiorMaxZ = maxPosZ - 1;

        // Size check
        int sizeX = interiorMaxX - interiorMinX + 1;
        int sizeZ = interiorMaxZ - interiorMinZ + 1;
        if (sizeX < 1 || sizeZ < 1) return null;
        if (sizeX > OutdoorRoomType.MAX_INTERIOR_X || sizeZ > OutdoorRoomType.MAX_INTERIOR_Z) return null;

        // --- Validate all four sides and corners are continuous boundary blocks --- //
        if (!validatePerimeter(world, minNegX, maxPosX, minNegZ, maxPosZ, fenceY)) return null;

        // --- Validate solid floor covering the full footprint including under fence --- //
        if (!validateFloor(world, minNegX, maxPosX, minNegZ, maxPosZ, fenceY)) return null;

        // --- Structural classification passes — build the data object --- //
        OutdoorRoomType structuralType = OutdoorRoomType.classifyStructural(sizeX, sizeZ);
        if (structuralType == null) return null;

        // Y bounds: floor level to fence + scan height
        int minY = fenceY - 1;
        int maxY = fenceY + OutdoorRoomType.SCAN_HEIGHT_ABOVE_FENCE;

        OutdoorRoomData space = new OutdoorRoomData(
                new Vector3i(interiorMinX, minY, interiorMinZ),
                new Vector3i(interiorMaxX, maxY, interiorMaxZ),
                fenceY
        );
        space.setDesignatedRoomType(structuralType.getDisplayName());
        return space;
    }

    // --- Perimeter walking --- //

    // Walk along one axis (dx/dz) from the seed, return the fence wall position or MIN_VALUE if no wall found
    private static int walkAxis(World world, Vector3i seed, int dx, int dz, int fenceY) {
        int limit = OutdoorRoomType.MAX_INTERIOR_X + 2; // +2 accounts for the fence walls themselves
        for (int i = 1; i <= limit; i++) {
            int x = seed.x + dx * i;
            int z = seed.z + dz * i;
            BlockType bt = getBlockType(world, x, fenceY, z);
            if (bt == null) return Integer.MIN_VALUE;
            if (isBoundary(bt)) return dx != 0 ? x : z;
        }
        return Integer.MIN_VALUE;
    }

    // --- Perimeter validation --- //

    // Validate all four sides and all four corners are continuous boundary blocks at fenceY
    private static boolean validatePerimeter(World world, int minX, int maxX, int minZ, int maxZ, int fenceY) {
        // North and south walls (Z edges, walk along X)
        for (int x = minX; x <= maxX; x++) {
            if (!isBoundaryAt(world, x, fenceY, minZ)) return false;
            if (!isBoundaryAt(world, x, fenceY, maxZ)) return false;
        }
        // West and east walls (X edges, walk along Z)
        for (int z = minZ; z <= maxZ; z++) {
            if (!isBoundaryAt(world, minX, fenceY, z)) return false;
            if (!isBoundaryAt(world, maxX, fenceY, z)) return false;
        }
        return true;
    }

    // --- Floor validation --- //

    // Validate a fully solid floor at fenceY-1 covering the entire footprint including under fence
    private static boolean validateFloor(World world, int minX, int maxX, int minZ, int maxZ, int fenceY) {
        int floorY = fenceY - 1;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockType bt = getBlockType(world, x, floorY, z);
                if (bt == null || !isSolidFloor(bt)) return false;
            }
        }
        return true;
    }

    // --- Lowest boundary ring search --- //

    // Search downward from the placed block to find the lowest Y where a full boundary ring exists
    private static int findLowestBoundaryRingY(World world, Vector3i pos) {
        // Search from the placed Y downward (multi-height fences — we want the lowest ring)
        for (int dy = 0; dy <= 4; dy++) {
            int testY = pos.y - dy;
            BlockType bt = getBlockType(world, pos.x, testY, pos.z);
            if (bt == null || !isBoundary(bt)) break; // Stop if no longer a fence block
            // Check if a valid ring exists at this Y by attempting full detection
            int maxPosX = walkAxis(world, new Vector3i(pos.x, testY, pos.z), 1, 0, testY);
            int minNegX = walkAxis(world, new Vector3i(pos.x, testY, pos.z), -1, 0, testY);
            int maxPosZ = walkAxis(world, new Vector3i(pos.x, testY, pos.z), 0, 1, testY);
            int minNegZ = walkAxis(world, new Vector3i(pos.x, testY, pos.z), 0, -1, testY);
            if (maxPosX == Integer.MIN_VALUE || minNegX == Integer.MIN_VALUE
                    || maxPosZ == Integer.MIN_VALUE || minNegZ == Integer.MIN_VALUE) continue;
            if (validatePerimeter(world, minNegX, maxPosX, minNegZ, maxPosZ, testY)) return testY;
        }
        return Integer.MIN_VALUE;
    }

    // --- Content scan --- //

    // Scan all blocks within the outdoor space bounds (interior + fence perimeter + floor)
    // Fence and floor blocks are included so requirements can match on them
    public static void scanOutdoorContents(World world, OutdoorRoomData space) {
        space.clearBlockKeys();
        Map<String, Integer> rawCounts = new HashMap<>();

        int fenceY = space.getFenceY();
        Vector3i min = space.getMinBound();
        Vector3i max = space.getMaxBound();

        // Expand scan to include the fence perimeter (one block outside interior bounds)
        int scanMinX = min.x - 1;
        int scanMaxX = max.x + 1;
        int scanMinZ = min.z - 1;
        int scanMaxZ = max.z + 1;

        // Floor Y is fenceY-1, scan up to fenceY + SCAN_HEIGHT_ABOVE_FENCE
        int scanMinY = fenceY - 1;
        int scanMaxY = fenceY + OutdoorRoomType.SCAN_HEIGHT_ABOVE_FENCE;

        for (int x = scanMinX; x <= scanMaxX; x++) {
            for (int y = scanMinY; y <= scanMaxY; y++) {
                for (int z = scanMinZ; z <= scanMaxZ; z++) {
                    long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
                    WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);
                    if (chunk == null) continue;
                    int blockId = chunk.getBlock(x, y, z);
                    BlockType bt = BlockType.getAssetMap().getAsset(blockId);
                    if (bt != null) rawCounts.merge(bt.getId(), 1, Integer::sum);
                }
            }
        }

        for (Map.Entry<String, Integer> entry : rawCounts.entrySet()) {
            space.addBlockKey(entry.getKey());
        }
    }

    // --- Block type checks --- //

    // Boundary blocks: fences, gates, and doors — defines the outdoor space perimeter
    public static boolean isBoundary(BlockType bt) {
        if (bt == null) return false;

        // Gates and doors count as boundary (same spirit as isStructural door handling)
        String hitboxType = bt.getHitboxType();
        if (hitboxType != null && (hitboxType.contains("Door") || hitboxType.contains("Gate"))) return true;

        // Category check for fences and gates
        Item item = bt.getItem();
        if (item == null) return false;
        String[] categories = item.getCategories();
        if (categories == null) return false;
        return Arrays.stream(categories).anyMatch(c ->
                c.contains("Furniture.Fences") || c.contains("Furniture.Gates") || c.equals("Furniture.Doors")
        );
    }

    // Solid floor: any solid cube block counts as a valid floor tile
    private static boolean isSolidFloor(BlockType bt) {
        if (bt == null) return false;
        if (bt.getMaterial() != BlockMaterial.Solid) return false;
        return bt.getDrawType() == DrawType.Cube;
    }

    private static boolean isBoundaryAt(World world, int x, int y, int z) {
        BlockType bt = getBlockType(world, x, y, z);
        return isBoundary(bt);
    }

    @Nullable
    private static BlockType getBlockType(World world, int x, int y, int z) {
        if (y < 0 || y >= 320) return null;
        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);
        if (chunk == null) return null;
        int blockId = chunk.getBlock(x, y, z);
        return BlockType.getAssetMap().getAsset(blockId);
    }
}