package com.example.hyarpg.utils.rooms;

// Hytale Imports
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.util.BsonUtil;

// Java Imports
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

/*
    WorldRoomRegistry: One instance per world. Stores RoomData bucketed by chunk index
    for fast spatial lookup. Persists to rooms.json in the world save path.

    Lifecycle:
    - Module_RoomSystem calls load(world) on world start, then put(worldName, registry)
    - saveAsync(world) is called automatically after any modification
    - saveAndRemove(world) is called on world shutdown
*/
public class WorldRoomRegistry {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String SAVE_FILE = "rooms.json";

    // Global map: worldName -> registry instance
    private static final Map<String, WorldRoomRegistry> REGISTRIES = new ConcurrentHashMap<>();

    // Spatial bucket: chunkIndex -> rooms overlapping that chunk
    private final Long2ObjectMap<List<RoomData>> chunkBuckets = new Long2ObjectOpenHashMap<>();

    // Flat list for full iteration (save, clear, etc.)
    private final List<RoomData> allRooms = new ArrayList<>();

    private final String worldName;

    private WorldRoomRegistry(String worldName) {
        this.worldName = worldName;
    }

    // --- Static lifecycle ---

    // Called by Module_RoomSystem after load completes
    public static void put(String worldName, WorldRoomRegistry registry) {
        REGISTRIES.put(worldName, registry);
    }

    // Called by Module_RoomSystem on world start — returns a future so it's non-blocking
    public static CompletableFuture<WorldRoomRegistry> load(World world) {
        Path path = world.getSavePath().resolve(SAVE_FILE);
        return CompletableFuture.supplyAsync(() -> {
            WorldRoomRegistry registry = new WorldRoomRegistry(world.getName());
            try {
                SaveData saved = (SaveData) RawJsonReader.readSyncWithBak(path, SaveData.CODEC, LOGGER);
                if (saved != null && saved.rooms != null) {
                    for (RoomData room : saved.rooms) {
                        registry.addRoom(room);
                    }
                    LOGGER.at(Level.INFO).log("Loaded %d rooms for world '%s'", saved.rooms.length, world.getName());
                }
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("Failed to load rooms for world '%s': %s", world.getName(), e.getMessage());
            }
            return registry;
        });
    }

    // Called on world shutdown — saves synchronously then removes from global map
    public static void saveAndRemove(World world) {
        WorldRoomRegistry registry = REGISTRIES.get(world.getName());
        if (registry != null) {
            registry.save(world);
            REGISTRIES.remove(world.getName());
        }
    }

    @Nullable
    public static WorldRoomRegistry get(String worldName) {
        return REGISTRIES.get(worldName);
    }

    @Nullable
    public static WorldRoomRegistry get(World world) {
        return REGISTRIES.get(world.getName());
    }

    // --- Room registration ---

    public void addRoom(RoomData room) {
        allRooms.add(room);
        for (long chunkIndex : getOverlappingChunks(room)) {
            chunkBuckets.computeIfAbsent(chunkIndex, k -> new ArrayList<>()).add(room);
        }
    }

    public void removeRoom(RoomData room) {
        allRooms.remove(room);
        for (long chunkIndex : getOverlappingChunks(room)) {
            List<RoomData> bucket = chunkBuckets.get(chunkIndex);
            if (bucket != null) {
                bucket.remove(room);
                if (bucket.isEmpty()) chunkBuckets.remove(chunkIndex);
            }
        }
    }

    // --- Spatial lookup ---

    @Nullable
    public RoomData getRoomAt(int x, int y, int z) {
        long centerChunk = ChunkUtil.indexChunkFromBlock(x, z);
        int chunkX = ChunkUtil.xOfChunkIndex(centerChunk);
        int chunkZ = ChunkUtil.zOfChunkIndex(centerChunk);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long neighborIndex = ChunkUtil.indexChunk(chunkX + dx, chunkZ + dz);
                List<RoomData> bucket = chunkBuckets.get(neighborIndex);
                if (bucket == null) continue;
                for (RoomData room : bucket) {
                    if (room.containsInterior(x, y, z)) return room;
                }
            }
        }
        return null;
    }

    // Returns all rooms whose shell overlaps this position (for invalidation on block change)
    public List<RoomData> getRoomsNear(int x, int y, int z) {
        List<RoomData> results = new ArrayList<>();
        long centerChunk = ChunkUtil.indexChunkFromBlock(x, z);
        int chunkX = ChunkUtil.xOfChunkIndex(centerChunk);
        int chunkZ = ChunkUtil.zOfChunkIndex(centerChunk);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long neighborIndex = ChunkUtil.indexChunk(chunkX + dx, chunkZ + dz);
                List<RoomData> bucket = chunkBuckets.get(neighborIndex);
                if (bucket == null) continue;
                for (RoomData room : bucket) {
                    if (room.containsWithWalls(x, y, z) && !results.contains(room)) {
                        results.add(room);
                    }
                }
            }
        }
        return results;
    }

    public List<RoomData> getAllRooms() { return allRooms; }

    public void clear() {
        allRooms.clear();
        chunkBuckets.clear();
    }

    // --- Persistence ---

    // Non-blocking async save — called after any modification
    public void saveAsync(World world) {
        CompletableFuture.runAsync(() -> save(world));
    }

    // Blocking save — used on shutdown only
    public void save(World world) {
        try {
            SaveData saveData = new SaveData();
            saveData.rooms = allRooms.toArray(new RoomData[0]);
            BsonUtil.writeDocument(
                    world.getSavePath().resolve(SAVE_FILE),
                    SaveData.CODEC.encode(saveData).asDocument()
            );
            LOGGER.at(Level.INFO).log("Saved %d rooms for world '%s'", allRooms.size(), worldName);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Failed to save rooms for world '%s': %s", worldName, e.getMessage());
        }
    }

    // --- Helpers ---

    private static List<Long> getOverlappingChunks(RoomData room) {
        List<Long> chunks = new ArrayList<>();
        Vector3i min = room.getMinBound();
        Vector3i max = room.getMaxBound();

        int minChunkX = ChunkUtil.chunkCoordinate(min.x);
        int maxChunkX = ChunkUtil.chunkCoordinate(max.x);
        int minChunkZ = ChunkUtil.chunkCoordinate(min.z);
        int maxChunkZ = ChunkUtil.chunkCoordinate(max.z);

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                chunks.add(ChunkUtil.indexChunk(cx, cz));
            }
        }
        return chunks;
    }

    // Identifies a room by its center point and interior dimensions
    @Nullable
    public RoomData findMatchingRoom(RoomData candidate) {
        int cx = candidate.getCenterX();
        int cy = candidate.getCenterY();
        int cz = candidate.getCenterZ();
        int sx = candidate.getInteriorSizeX();
        int sy = candidate.getInteriorSizeY();
        int sz = candidate.getInteriorSizeZ();

        for (RoomData room : allRooms) {
            if (room.getCenterX() == cx
                    && room.getCenterY() == cy
                    && room.getCenterZ() == cz
                    && room.getInteriorSizeX() == sx
                    && room.getInteriorSizeY() == sy
                    && room.getInteriorSizeZ() == sz) {
                return room;
            }
        }
        return null;
    }

    // --- Save data wrapper ---
    @SuppressWarnings("unchecked")
    public static class SaveData {
        public static final BuilderCodec<SaveData> CODEC = BuilderCodec
                .builder(SaveData.class, SaveData::new)
                .append(
                        new KeyedCodec<>("Rooms", new ArrayCodec(RoomData.CODEC, RoomData[]::new)),
                        (d, v) -> d.rooms = v,
                        d -> d.rooms
                ).add()
                .build();

        public RoomData[] rooms;
    }
}