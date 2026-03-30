package com.example.hyarpg.utils.rooms;

// Hytale Imports
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;

// Java Imports
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RoomManager {

    private static RoomManager instance;

    private final List<TerritoryRoomRegistry> territories = new CopyOnWriteArrayList<>();

    private RoomManager() {}

    public static RoomManager get() {
        if (instance == null) instance = new RoomManager();
        return instance;
    }

    // --- Territory Terminal Lifecycle ---
    public TerritoryRoomRegistry registerTerritory(Vector3i terminalPos, double radius) {
        TerritoryRoomRegistry registry = new TerritoryRoomRegistry(terminalPos, radius);
        territories.add(registry);
        return registry;
    }

    public void removeTerritory(Vector3i terminalPos) {
        territories.removeIf(t -> t.getTerminalPosition().equals(terminalPos));
    }

    // Returns the territory registry for this terminal position, or null
    @Nullable
    public TerritoryRoomRegistry getTerritoryAt(Vector3i terminalPos) {
        for (TerritoryRoomRegistry t : territories) {
            if (t.getTerminalPosition().equals(terminalPos)) return t;
        }
        return null;
    }

    // Returns any territory whose radius contains this world position
    @Nullable
    public TerritoryRoomRegistry getTerritoryContaining(double x, double y, double z) {
        for (TerritoryRoomRegistry t : territories) {
            if (t.isInTerritory(x, y, z)) return t;
        }
        return null;
    }

    // Call this when the territory terminal is broken — de-designates all rooms
    public void onTerritoryTerminalBroken(Vector3i terminalPos) {
        TerritoryRoomRegistry registry = getTerritoryAt(terminalPos);
        if (registry != null) {
            registry.clearAll();
            removeTerritory(terminalPos);
            // TODO: notify players
        }
    }

    // Call this when the territory terminal is placed — re-scans for rooms
    public void onTerritoryTerminalPlaced(World world, Vector3i terminalPos, double radius) {
        TerritoryRoomRegistry registry = registerTerritory(terminalPos, radius);
        // TODO: optionally do a full territory scan to find existing rooms
    }

    // --- Helpers ---



    // Scans the interior of a room and records all non-structural block keys
//    private void scanRoomContents(World world, RoomData room) {
//        Vector3i min = room.getMinBound();
//        Vector3i max = room.getMaxBound();
//
//        for (int x = min.x; x <= max.x; x++) {
//            for (int y = min.y; y <= max.y; y++) {
//                for (int z = min.z; z <= max.z; z++) {
//                    long chunkIndex = com.hypixel.hytale.math.util.ChunkUtil.indexChunkFromBlock(x, z);
//                    com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk chunk =
//                            world.getChunkIfInMemory(chunkIndex);
//                    if (chunk == null) continue;
//                    int blockId = chunk.getBlock(x, y, z);
//                    BlockType bt = BlockType.getAssetMap().getAsset(blockId);
//                    if (bt != null && !isStructural(bt) && bt.getMaterial() != com.hypixel.hytale.protocol.BlockMaterial.Empty) {
//                        room.getBlockKeysInside().add(bt.getId());
//                    }
//                }
//            }
//        }
//    }

    // Placeholder for future room recipe matching logic
    private void reevaluateRoomRecipe(RoomData room) {
        // TODO: match room.getBlockKeysInside() against room recipe library
        // Update room.setDesignatedRoomType() if a specific recipe matches
        // Notify player if designation changed
    }
}