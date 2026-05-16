package com.example.hyarpg.utils.outdoor_rooms;

// Hytale Imports
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.protocol.DrawType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;

// Java Imports
import javax.annotation.Nullable;
import java.util.*;
import org.joml.Vector3i;

public class OutdoorRoomFloodFill {
    
    public static List<OutdoorRoomData> detectOutdoorSpacesFromPlacedBlock(World world, Vector3i placedPos, BlockType placingBlockType) {
        List<OutdoorRoomData> results = new ArrayList<>();

        // Scan 7 Y layers: one below placed block up through fence + 5 above
        for (int dy = -1; dy <= 5; dy++) {
            int scanY = placedPos.y + dy;
            detectAtLayer(world, placedPos, placingBlockType, scanY, placedPos, placingBlockType, null, results);
        }

        return results;
    }

    public static List<OutdoorRoomData> detectOutdoorSpacesFromBrokenBlock(World world, Vector3i brokenPos) {
        List<OutdoorRoomData> results = new ArrayList<>();

        // Broken block is already gone from world — pass as pendingRemove so floor/perimeter checks treat it as absent
        for (int dy = -1; dy <= 5; dy++) {
            int scanY = brokenPos.y + dy;
            detectAtLayer(world, brokenPos, null, scanY, null, null, brokenPos, results);
        }

        return results;
    }

    // Layer scan
    private static void detectAtLayer(World world, Vector3i origin, @Nullable BlockType originBlockType, int scanY, @Nullable Vector3i pendingAdd, @Nullable BlockType pendingAddType, @Nullable Vector3i pendingRemove, List<OutdoorRoomData> results) {
        List<Vector3i> scanOrigins = Arrays.asList(
                new Vector3i(origin.x - 1, origin.y, origin.z - 1),  // NW
                new Vector3i(origin.x + 1, origin.y, origin.z - 1),  // NE
                new Vector3i(origin.x - 1, origin.y, origin.z + 1),  // SW
                new Vector3i(origin.x + 1, origin.y, origin.z + 1)   // SE
        );

        for (Vector3i scanOrigin : scanOrigins) {
            OutdoorRoomData space = detectFromOrigin(world, scanOrigin, scanY, pendingAdd, pendingAddType, pendingRemove);
            if (space == null) continue;

            // Deduplicate — don't add the same bounding box twice
            boolean duplicate = results.stream().anyMatch(r ->
                    r.getCenterX() == space.getCenterX() &&
                            r.getCenterZ() == space.getCenterZ() &&
                            r.getInteriorSizeX() == space.getInteriorSizeX() &&
                            r.getInteriorSizeZ() == space.getInteriorSizeZ()
            );
            if (!duplicate) results.add(space);
        }
    }

    // Scan origin resolution (shared wall handling)
    private static List<Vector3i> resolveScanOrigins(World world, Vector3i origin, int scanY,
                                                     @Nullable Vector3i pendingAdd, @Nullable BlockType pendingAddType,
                                                     @Nullable Vector3i pendingRemove) {
        // Check if adjacent blocks are immediately fences with no air gap — signals a shared wall
        boolean fenceImmediateN = isBoundaryAt(world, origin.x,     scanY, origin.z - 1, pendingAdd, pendingAddType, pendingRemove);
        boolean fenceImmediateS = isBoundaryAt(world, origin.x,     scanY, origin.z + 1, pendingAdd, pendingAddType, pendingRemove);
        boolean fenceImmediateE = isBoundaryAt(world, origin.x + 1, scanY, origin.z,     pendingAdd, pendingAddType, pendingRemove);
        boolean fenceImmediateW = isBoundaryAt(world, origin.x - 1, scanY, origin.z,     pendingAdd, pendingAddType, pendingRemove);

        List<Vector3i> origins = new ArrayList<>();

        // Corner of 4 rooms — fence immediate in all 4 directions, offset diagonally
        if (fenceImmediateN && fenceImmediateS && fenceImmediateE && fenceImmediateW) {
            origins.add(new Vector3i(origin.x - 1, origin.y, origin.z - 1));
            origins.add(new Vector3i(origin.x + 1, origin.y, origin.z - 1));
            origins.add(new Vector3i(origin.x - 1, origin.y, origin.z + 1));
            origins.add(new Vector3i(origin.x + 1, origin.y, origin.z + 1));
            return origins;
        }

        // Shared N/S wall — fence immediate on N or S, offset E and W
        if (fenceImmediateN || fenceImmediateS) {
            origins.add(new Vector3i(origin.x - 1, origin.y, origin.z));
            origins.add(new Vector3i(origin.x + 1, origin.y, origin.z));
            return origins;
        }

        // Shared E/W wall — fence immediate on E or W, offset N and S
        if (fenceImmediateE || fenceImmediateW) {
            origins.add(new Vector3i(origin.x, origin.y, origin.z - 1));
            origins.add(new Vector3i(origin.x, origin.y, origin.z + 1));
            return origins;
        }

        // Normal case — no immediate fence neighbors, scan from origin directly
        origins.add(new Vector3i(origin.x, origin.y, origin.z));
        return origins;
    }


    // Cross scan and room detection from a single origin
    @Nullable
    private static OutdoorRoomData detectFromOrigin(World world, Vector3i scanOrigin, int scanY, @Nullable Vector3i pendingAdd, @Nullable BlockType pendingAddType, @Nullable Vector3i pendingRemove) {
        // Cross scan: find the first fence in each cardinal direction
        int northZ = crossScan(world, scanOrigin, scanY, 0,  -1, pendingAdd, pendingAddType, pendingRemove);
        int southZ = crossScan(world, scanOrigin, scanY, 0,   1, pendingAdd, pendingAddType, pendingRemove);
        int eastX  = crossScan(world, scanOrigin, scanY, 1,   0, pendingAdd, pendingAddType, pendingRemove);
        int westX  = crossScan(world, scanOrigin, scanY, -1,  0, pendingAdd, pendingAddType, pendingRemove);

        // All 4 directions must hit a fence
        if (northZ == Integer.MIN_VALUE || southZ == Integer.MIN_VALUE ||
                eastX == Integer.MIN_VALUE || westX == Integer.MIN_VALUE) return null;

        // --- Derive interior bounds from the 4 wall hits --- //
        int interiorMinX = westX + 1;
        int interiorMaxX = eastX - 1;
        int interiorMinZ = northZ + 1;
        int interiorMaxZ = southZ - 1;

        int sizeX = interiorMaxX - interiorMinX + 1;
        int sizeZ = interiorMaxZ - interiorMinZ + 1;

        if (sizeX < 1 || sizeZ < 1) return null;
        if (sizeX > OutdoorRoomType.MAX_INTERIOR_X || sizeZ > OutdoorRoomType.MAX_INTERIOR_Z) return null;

        // --- Validate perimeter using exactly the 4 wall coordinates from the cross scan --- //
        if (!validatePerimeter(world, westX, eastX, northZ, southZ, scanY, pendingAdd, pendingAddType, pendingRemove)) return null;

        // --- Validate solid floor at ringY-1 across full footprint including under fence --- //
        if (!validateFloor(world, westX, eastX, northZ, southZ, scanY, pendingAdd, pendingRemove)) return null;

        // --- Structural classification --- //
        OutdoorRoomType structuralType = OutdoorRoomType.classifyStructural(sizeX, sizeZ);
        if (structuralType == null) return null;

        OutdoorRoomData space = new OutdoorRoomData(
                new Vector3i(interiorMinX, scanY - 1, interiorMinZ),
                new Vector3i(interiorMaxX, scanY + OutdoorRoomType.SCAN_HEIGHT_ABOVE_FENCE, interiorMaxZ),
                scanY
        );
        space.setDesignatedRoomType(structuralType.getDisplayName());
        return space;
    }

    // Walk from origin in (dx, dz) direction at scanY, return coordinate of first fence hit or MIN_VALUE
    private static int crossScan(World world, Vector3i origin, int scanY, int dx, int dz,
                                 @Nullable Vector3i pendingAdd, @Nullable BlockType pendingAddType,
                                 @Nullable Vector3i pendingRemove) {
        int limit = OutdoorRoomType.MAX_INTERIOR_X + 2;
        for (int i = 1; i <= limit; i++) {
            int x = origin.x + dx * i;
            int z = origin.z + dz * i;
            long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
            WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);
            if (chunk == null) return Integer.MIN_VALUE;
            if (isBoundaryAt(world, x, scanY, z, pendingAdd, pendingAddType, pendingRemove))
                return dx != 0 ? x : z;
        }
        return Integer.MIN_VALUE;
    }

    // Validate all four sides are continuous fence using exactly the wall coords from the cross scan
    private static boolean validatePerimeter(World world, int minX, int maxX, int minZ, int maxZ, int fenceY,
                                             @Nullable Vector3i pendingAdd, @Nullable BlockType pendingAddType,
                                             @Nullable Vector3i pendingRemove) {
        // North and south walls (Z edges, walk along X)
        for (int x = minX; x <= maxX; x++) {
            if (!isBoundaryAt(world, x, fenceY, minZ, pendingAdd, pendingAddType, pendingRemove)) return false;
            if (!isBoundaryAt(world, x, fenceY, maxZ, pendingAdd, pendingAddType, pendingRemove)) return false;
        }
        // West and east walls (X edges, walk along Z)
        for (int z = minZ; z <= maxZ; z++) {
            if (!isBoundaryAt(world, minX, fenceY, z, pendingAdd, pendingAddType, pendingRemove)) return false;
            if (!isBoundaryAt(world, maxX, fenceY, z, pendingAdd, pendingAddType, pendingRemove)) return false;
        }
        return true;
    }

    // Validate solid floor at fenceY-1 across full footprint including under fence
    private static boolean validateFloor(World world, int minX, int maxX, int minZ, int maxZ, int fenceY,
                                         @Nullable Vector3i pendingAdd, @Nullable Vector3i pendingRemove) {
        int floorY = fenceY - 1;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockType bt = getBlockTypeWithPending(world, x, floorY, z, pendingAdd, pendingRemove);
                if (bt == null || !isSolidFloor(bt)) return false;
            }
        }
        return true;
    }

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

    // Fence check by block key naming convention — all fences contain _Fence or Fence_
    public static boolean isBoundary(BlockType bt) {
        if (bt == null) return false;
        String id = bt.getId();
        if (id == null) return false;
        return id.contains("_Fence") || id.contains("Fence_");
    }

    // Solid floor: any solid cube block counts as a valid floor tile
    private static boolean isSolidFloor(BlockType bt) {
        if (bt == null) return false;
        if (bt.getMaterial() != BlockMaterial.Solid) return false;
        return bt.getDrawType() == DrawType.Cube;
    }

    // Checks if a position is a boundary block, accounting for pending add/remove and the pending block's type
    private static boolean isBoundaryAt(World world, int x, int y, int z, @Nullable Vector3i pendingAdd, @Nullable BlockType pendingAddType, @Nullable Vector3i pendingRemove) {
        if (pendingRemove != null && x == pendingRemove.x && y == pendingRemove.y && z == pendingRemove.z) return false;
        if (pendingAdd != null && x == pendingAdd.x && y == pendingAdd.y && z == pendingAdd.z) return isBoundary(pendingAddType);
        return isBoundary(getBlockType(world, x, y, z));
    }

    // Returns block type accounting for pending add/remove
    private static BlockType getBlockTypeWithPending(World world, int x, int y, int z, @Nullable Vector3i pendingAdd, @Nullable Vector3i pendingRemove) {
        if (pendingRemove != null && x == pendingRemove.x && y == pendingRemove.y && z == pendingRemove.z) return null;
        if (pendingAdd != null && x == pendingAdd.x && y == pendingAdd.y && z == pendingAdd.z) return BlockType.UNKNOWN;
        return getBlockType(world, x, y, z);
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