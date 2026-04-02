package com.example.hyarpg.utils.rooms;

// Hytale Import
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.protocol.DrawType;
import com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.npc.interactions.SpawnNPCInteraction;
import com.hypixel.hytale.server.spawning.SpawningPlugin;

// Java Import
import javax.annotation.Nullable;
import java.util.*;

public class RoomFloodFill {

    @Nullable
    public static RoomData detectRoomFromPlacedBlock(World world, Vector3i placedBlockPos, BlockType placingBlockType) {
        List<Vector3i> pendingSolids = buildPendingSolids(placedBlockPos, placingBlockType);
        return detectRoomFromPlacedBlock(world, placedBlockPos, pendingSolids);
    }

    @Nullable
    private static RoomData detectRoomFromPlacedBlock(World world, Vector3i placedBlockPos, List<Vector3i> pendingSolids) {
        Set<String> attempted = new HashSet<>();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    int nx = placedBlockPos.x + dx;
                    int ny = placedBlockPos.y + dy;
                    int nz = placedBlockPos.z + dz;

                    BlockType bt = getBlockTypeWithPending(world, nx, ny, nz, pendingSolids);
                    if (bt == null || isStructural(bt)) continue;

                    // Deduplicate seeds — diagonal neighbors may flood fill the same room
                    String seedKey = nx + "," + ny + "," + nz;
                    if (!attempted.add(seedKey)) continue;

                    RoomData result = detectRoom(world, new Vector3i(nx, ny, nz), pendingSolids);
                    if (result != null) return result;
                }
            }
        }
        return null;
    }

    @Nullable
    public static RoomData detectRoomFromBrokenBlock(World world, Vector3i brokenBlockPos) {
        return detectRoom(world, brokenBlockPos, Collections.emptyList());
    }

    @Nullable
    private static RoomData detectRoom(World world, Vector3i seedPos, List<Vector3i> pendingSolids) {
        Set<Long> visited = new HashSet<>();
        Deque<Vector3i> queue = new ArrayDeque<>();
        queue.add(seedPos);
        visited.add(packPos(seedPos.x, seedPos.y, seedPos.z));

        int minX = seedPos.x, maxX = seedPos.x;
        int minY = seedPos.y, maxY = seedPos.y;
        int minZ = seedPos.z, maxZ = seedPos.z;

        // Track only air blocks queued (not wall neighbors added to visited)
        int queuedCount = 1;

        while (!queue.isEmpty()) {
            Vector3i pos = queue.poll();

            // Volume check uses queuedCount — wall neighbors in visited don't count
            if (queuedCount > RoomType.MAX_FLOOD_VOLUME)
                return null;

            if (pos.x < minX) minX = pos.x;
            if (pos.x > maxX) maxX = pos.x;
            if (pos.y < minY) minY = pos.y;
            if (pos.y > maxY) maxY = pos.y;
            if (pos.z < minZ) minZ = pos.z;
            if (pos.z > maxZ) maxZ = pos.z;

            if (maxX - minX + 1 > RoomType.MAX_INTERIOR_X || maxY - minY + 1 > RoomType.MAX_INTERIOR_Y || maxZ - minZ + 1 > RoomType.MAX_INTERIOR_Z)
                return null;

            int[][] neighbors = {
                {pos.x + 1, pos.y, pos.z},
                {pos.x - 1, pos.y, pos.z},
                {pos.x, pos.y + 1, pos.z},
                {pos.x, pos.y - 1, pos.z},
                {pos.x, pos.y, pos.z + 1},
                {pos.x, pos.y, pos.z - 1},
            };

            for (int[] n : neighbors) {
                long key = packPos(n[0], n[1], n[2]);
                if (visited.contains(key)) continue;
                visited.add(key);

                BlockType bt = getBlockTypeWithPending(world, n[0], n[1], n[2], pendingSolids);
                if (bt == null) {
                    return null;
                }

                if (isStructural(bt)) {
                    // Wall boundary — added to visited to prevent revisiting but NOT queued
                    // and NOT counted toward volume limit
                    continue;
                }

                // Air block — queue it and count it
                queue.add(new Vector3i(n[0], n[1], n[2]));
                queuedCount++;
            }
        }

        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;

        RoomType type = RoomType.classify(sizeX, sizeY, sizeZ);
        if (type == null) return null;

        RoomData room = new RoomData(
                new Vector3i(minX, minY, minZ),
                new Vector3i(maxX, maxY, maxZ)
        );
        room.setDesignatedRoomType(type.getDisplayName());
        return room;
    }

    private static List<Vector3i> buildPendingSolids(Vector3i placedBlockPos, @Nullable BlockType blockType) {
        List<Vector3i> pending = new ArrayList<>();
        pending.add(placedBlockPos);

        if (blockType == null) return pending;

        String hitboxType = blockType.getHitboxType();
        if (hitboxType == null || hitboxType.equals("Full")) return pending;

        BlockBoundingBoxes hitbox = BlockBoundingBoxes.getAssetMap().getAsset(hitboxType);
        if (hitbox == null) return pending;

        BlockBoundingBoxes.RotatedVariantBoxes variant = hitbox.get(0);
        if (variant == null) return pending;

        Box bounds = variant.getBoundingBox();

        int minBlockY = (int) Math.floor(bounds.min.y);
        int maxBlockY = (int) Math.ceil(bounds.max.y) - 1;

        for (int dy = minBlockY; dy <= maxBlockY; dy++) {
            if (dy == 0) continue;
            pending.add(new Vector3i(placedBlockPos.x, placedBlockPos.y + dy, placedBlockPos.z));
        }

        return pending;
    }

    @Nullable
    private static BlockType getBlockTypeWithPending(World world, int x, int y, int z, List<Vector3i> pendingSolids) {
        for (Vector3i p : pendingSolids) {
            if (x == p.x && y == p.y && z == p.z) return BlockType.UNKNOWN;
        }
        return getBlockType(world, x, y, z);
    }

    @Nullable
    private static BlockType getBlockType(World world, int x, int y, int z) {
        if (y < 0 || y >= 320) return BlockType.EMPTY;
        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);
        if (chunk == null) return BlockType.UNKNOWN;
        int blockId = chunk.getBlock(x, y, z);
        return BlockType.getAssetMap().getAsset(blockId);
    }

    public static boolean isStructural(BlockType bt) {
        if (bt == null) return false;
        if (bt.getMaterial() != BlockMaterial.Solid) return false;

        if (bt.getDrawType() == DrawType.Cube) return true;

        String hitboxType = bt.getHitboxType();
        if (hitboxType != null && (hitboxType.contains("Door") || hitboxType.contains("Window"))) return true;

        // Category check for things like trapdoors that aren't caught by hitbox name
        Item item = bt.getItem();
        if (item == null) return false;
        String[] categories = item.getCategories();
        if (categories == null) return false;
        return Arrays.stream(categories).anyMatch("Furniture.Doors"::equals);
    }

    private static long packPos(int x, int y, int z) {
        return ((long)(x & 0xFFFFF) << 40) | ((long)(y & 0xFFFFF) << 20) | (z & 0xFFFFF);
    }
}