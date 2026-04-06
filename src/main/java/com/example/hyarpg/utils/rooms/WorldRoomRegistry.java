package com.example.hyarpg.utils.rooms;

// Hytale Imports
import com.example.hyarpg.utils.outdoor_rooms.OutdoorRoomData;
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

public class WorldRoomRegistry {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String SAVE_FILE = "rooms.json";

    private static final Map<String, WorldRoomRegistry> REGISTRIES = new ConcurrentHashMap<>();

    // --- Rooms ---
    private final Long2ObjectMap<List<RoomData>> chunkBuckets = new Long2ObjectOpenHashMap<>();
    private final List<RoomData> allRooms = new ArrayList<>();

    // --- Outdoor Rooms ---
    private final Long2ObjectMap<List<OutdoorRoomData>> outdoorChunkBuckets = new Long2ObjectOpenHashMap<>();
    private final List<OutdoorRoomData> allOutdoorRooms = new ArrayList<>();

    // --- Territories ---
    private final Long2ObjectMap<List<TerritoryData>> territoryChunkBuckets = new Long2ObjectOpenHashMap<>();
    private final List<TerritoryData> allTerritories = new ArrayList<>();

    private final String worldName;

    private WorldRoomRegistry(String worldName) {
        this.worldName = worldName;
    }

    // --- Static lifecycle ---
    public static void put(String worldName, WorldRoomRegistry registry) {
        REGISTRIES.put(worldName, registry);
    }

    public static CompletableFuture<WorldRoomRegistry> load(World world) {
        Path path = world.getSavePath().resolve(SAVE_FILE);
        return CompletableFuture.supplyAsync(() -> {
            WorldRoomRegistry registry = new WorldRoomRegistry(world.getName());
            try {
                SaveData saved = (SaveData) RawJsonReader.readSyncWithBak(path, SaveData.CODEC, LOGGER);
                if (saved != null) {
                    if (saved.rooms != null) {
                        for (RoomData room : saved.rooms) registry.addRoom(room);
                        LOGGER.at(Level.INFO).log("Loaded %d rooms for world '%s'", saved.rooms.length, world.getName());
                    }
                    if (saved.outdoorRooms != null) {
                        for (OutdoorRoomData room : saved.outdoorRooms) registry.addOutdoorRoom(room);
                        LOGGER.at(Level.INFO).log("Loaded %d outdoor rooms for world '%s'", saved.outdoorRooms.length, world.getName());
                    }
                    if (saved.territories != null) {
                        for (TerritoryData territory : saved.territories) registry.addTerritory(territory);
                        LOGGER.at(Level.INFO).log("Loaded %d territories for world '%s'", saved.territories.length, world.getName());
                    }
                }
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("Failed to load rooms for world '%s': %s", world.getName(), e.getMessage());
            }
            return registry;
        });
    }

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
        for (long chunkIndex : getOverlappingChunks(room.getMinBound(), room.getMaxBound())) {
            chunkBuckets.computeIfAbsent(chunkIndex, k -> new ArrayList<>()).add(room);
        }
    }

    public void removeRoom(RoomData room) {
        allRooms.remove(room);
        for (long chunkIndex : getOverlappingChunks(room.getMinBound(), room.getMaxBound())) {
            List<RoomData> bucket = chunkBuckets.get(chunkIndex);
            if (bucket != null) {
                bucket.remove(room);
                if (bucket.isEmpty()) chunkBuckets.remove(chunkIndex);
            }
        }
    }

    // --- Outdoor room registration ---

    public void addOutdoorRoom(OutdoorRoomData room) {
        allOutdoorRooms.add(room);
        for (long chunkIndex : getOverlappingChunks(room.getMinBound(), room.getMaxBound())) {
            outdoorChunkBuckets.computeIfAbsent(chunkIndex, k -> new ArrayList<>()).add(room);
        }
    }

    public void removeOutdoorRoom(OutdoorRoomData room) {
        allOutdoorRooms.remove(room);
        for (long chunkIndex : getOverlappingChunks(room.getMinBound(), room.getMaxBound())) {
            List<OutdoorRoomData> bucket = outdoorChunkBuckets.get(chunkIndex);
            if (bucket != null) {
                bucket.remove(room);
                if (bucket.isEmpty()) outdoorChunkBuckets.remove(chunkIndex);
            }
        }
    }

    // --- Territory registration ---

    public void addTerritory(TerritoryData territory) {
        allTerritories.add(territory);
        Vector3i min = new Vector3i(territory.getMinX(), territory.getMinY(), territory.getMinZ());
        Vector3i max = new Vector3i(territory.getMaxX(), territory.getMaxY(), territory.getMaxZ());
        for (long chunkIndex : getOverlappingChunks(min, max)) {
            territoryChunkBuckets.computeIfAbsent(chunkIndex, k -> new ArrayList<>()).add(territory);
        }
    }

    public void removeTerritory(TerritoryData territory) {
        allTerritories.remove(territory);
        Vector3i min = new Vector3i(territory.getMinX(), territory.getMinY(), territory.getMinZ());
        Vector3i max = new Vector3i(territory.getMaxX(), territory.getMaxY(), territory.getMaxZ());
        for (long chunkIndex : getOverlappingChunks(min, max)) {
            List<TerritoryData> bucket = territoryChunkBuckets.get(chunkIndex);
            if (bucket != null) {
                bucket.remove(territory);
                if (bucket.isEmpty()) territoryChunkBuckets.remove(chunkIndex);
            }
        }
    }

    // Removes all rooms that fall within the territory bounds
    public void removeRoomsInTerritory(TerritoryData territory) {
        List<RoomData> toRemove = new ArrayList<>();
        for (RoomData room : allRooms) {
            Vector3i center = new Vector3i(room.getCenterX(), room.getCenterY(), room.getCenterZ());
            if (territory.contains(center.x, center.y, center.z)) {
                toRemove.add(room);
            }
        }
        for (RoomData room : toRemove) removeRoom(room);
    }

    // --- Spatial lookup: rooms ---

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

    // --- Spatial lookup: outdoor rooms ---

    @Nullable
    public OutdoorRoomData getOutdoorRoomAt(int x, int y, int z) {
        long centerChunk = ChunkUtil.indexChunkFromBlock(x, z);
        int chunkX = ChunkUtil.xOfChunkIndex(centerChunk);
        int chunkZ = ChunkUtil.zOfChunkIndex(centerChunk);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long neighborIndex = ChunkUtil.indexChunk(chunkX + dx, chunkZ + dz);
                List<OutdoorRoomData> bucket = outdoorChunkBuckets.get(neighborIndex);
                if (bucket == null) continue;
                for (OutdoorRoomData room : bucket) {
                    if (room.containsInterior(x, y, z)) return room;
                }
            }
        }
        return null;
    }

    public List<OutdoorRoomData> getOutdoorRoomsNear(int x, int y, int z) {
        List<OutdoorRoomData> results = new ArrayList<>();
        long centerChunk = ChunkUtil.indexChunkFromBlock(x, z);
        int chunkX = ChunkUtil.xOfChunkIndex(centerChunk);
        int chunkZ = ChunkUtil.zOfChunkIndex(centerChunk);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long neighborIndex = ChunkUtil.indexChunk(chunkX + dx, chunkZ + dz);
                List<OutdoorRoomData> bucket = outdoorChunkBuckets.get(neighborIndex);
                if (bucket == null) continue;
                for (OutdoorRoomData room : bucket) {
                    if (room.containsWithWalls(x, y, z) && !results.contains(room)) {
                        results.add(room);
                    }
                }
            }
        }
        return results;
    }

    // --- Spatial lookup: territories ---

    @Nullable
    public TerritoryData getTerritoryAt(int x, int y, int z) {
        long centerChunk = ChunkUtil.indexChunkFromBlock(x, z);
        int chunkX = ChunkUtil.xOfChunkIndex(centerChunk);
        int chunkZ = ChunkUtil.zOfChunkIndex(centerChunk);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long neighborIndex = ChunkUtil.indexChunk(chunkX + dx, chunkZ + dz);
                List<TerritoryData> bucket = territoryChunkBuckets.get(neighborIndex);
                if (bucket == null) continue;
                for (TerritoryData territory : bucket) {
                    if (territory.contains(x, y, z)) return territory;
                }
            }
        }
        return null;
    }

    // Finds a territory whose light well base is at exactly this position
    @Nullable
    public TerritoryData getTerritoryByLightWell(int x, int y, int z) {
        for (TerritoryData territory : allTerritories) {
            Vector3i center = territory.getCenter();
            if (center.x == x && center.y == y && center.z == z) return territory;
        }
        return null;
    }

    public List<RoomData> getAllRooms() { return allRooms; }
    public List<OutdoorRoomData> getAllOutdoorRooms() { return allOutdoorRooms; }
    public List<TerritoryData> getAllTerritories() { return allTerritories; }

    public void clear() {
        allRooms.clear();
        chunkBuckets.clear();
        allOutdoorRooms.clear();
        outdoorChunkBuckets.clear();
        allTerritories.clear();
        territoryChunkBuckets.clear();
    }

    // --- Persistence ---

    public void saveAsync(World world) {
        CompletableFuture.runAsync(() -> save(world));
    }

    public void save(World world) {
        try {
            SaveData saveData = new SaveData();
            saveData.rooms = allRooms.toArray(new RoomData[0]);
            saveData.outdoorRooms = allOutdoorRooms.toArray(new OutdoorRoomData[0]);
            saveData.territories = allTerritories.toArray(new TerritoryData[0]);
            BsonUtil.writeDocument(
                    world.getSavePath().resolve(SAVE_FILE),
                    SaveData.CODEC.encode(saveData).asDocument()
            );
            LOGGER.at(Level.INFO).log("Saved %d rooms, %d outdoor rooms, %d territories for world '%s'",
                    allRooms.size(), allOutdoorRooms.size(), allTerritories.size(), worldName);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Failed to save rooms for world '%s': %s", worldName, e.getMessage());
        }
    }

    // --- Helpers ---

    private static List<Long> getOverlappingChunks(Vector3i min, Vector3i max) {
        List<Long> chunks = new ArrayList<>();
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

    @Nullable
    public OutdoorRoomData findMatchingOutdoorRoom(OutdoorRoomData candidate) {
        int cx = candidate.getCenterX();
        int cy = candidate.getCenterY();
        int cz = candidate.getCenterZ();
        int sx = candidate.getInteriorSizeX();
        int sz = candidate.getInteriorSizeZ();

        for (OutdoorRoomData room : allOutdoorRooms) {
            if (room.getCenterX() == cx
                    && room.getCenterY() == cy
                    && room.getCenterZ() == cz
                    && room.getInteriorSizeX() == sx
                    && room.getInteriorSizeZ() == sz) {
                return room;
            }
        }
        return null;
    }

    // --- Save data wrapper ---
    public static class SaveData {
        @SuppressWarnings("unchecked")
        public static final BuilderCodec<SaveData> ROOMS_CODEC = BuilderCodec
            .builder(SaveData.class, SaveData::new)
            .append(
                    new KeyedCodec<>("Rooms", new ArrayCodec(RoomData.CODEC, RoomData[]::new)),
                    (d, v) -> d.rooms = v,
                    d -> d.rooms
            ).add()
            .build();

        @SuppressWarnings("unchecked")
        public static final BuilderCodec<SaveData> OUTDOOR_ROOMS_CODEC = BuilderCodec
            .builder(SaveData.class, SaveData::new, ROOMS_CODEC)
            .append(
                    new KeyedCodec<>("OutdoorRooms", new ArrayCodec(OutdoorRoomData.CODEC, OutdoorRoomData[]::new)),
                    (d, v) -> d.outdoorRooms = v,
                    d -> d.outdoorRooms
            ).add()
            .build();

        @SuppressWarnings("unchecked")
        public static final BuilderCodec<SaveData> CODEC = BuilderCodec
            .builder(SaveData.class, SaveData::new, OUTDOOR_ROOMS_CODEC)
            .append(
                    new KeyedCodec<>("Territories", new ArrayCodec(TerritoryData.CODEC, TerritoryData[]::new)),
                    (d, v) -> d.territories = v,
                    d -> d.territories
            ).add()
            .build();

        public RoomData[] rooms;
        public OutdoorRoomData[] outdoorRooms;
        public TerritoryData[] territories;
    }
}