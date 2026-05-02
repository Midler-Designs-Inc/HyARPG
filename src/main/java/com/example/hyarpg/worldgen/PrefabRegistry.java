package com.example.hyarpg.worldgen;

// Hytale Imports
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.util.BsonUtil;
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.events.Event_WorldStart;

// Java Imports
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class PrefabRegistry {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String SAVE_FILE = "prefabs.json";

    // one registry per world, keyed by world name
    private static final Map<String, PrefabRegistry> REGISTRIES = new ConcurrentHashMap<>();

    // flat list — thread-safe since placements come from chunk gen threads
    private final CopyOnWriteArrayList<PrefabRecord> records = new CopyOnWriteArrayList<>();
    private final String worldName;

    // debounced save — fires 3 seconds after the last placement, cancels any pending save if more arrive
    private final ScheduledExecutorService saveScheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> pendingSave = null;

    private PrefabRegistry(String worldName) { this.worldName = worldName; }

    // wire up world start lifecycle on the mod event bus
    public static void register() {
        ModEventBus.register(Event_WorldStart.class, event -> {
            World world = event.world();
            load(world).thenAccept(registry -> {
                REGISTRIES.put(world.getName(), registry);
                LOGGER.at(Level.INFO).log("[PrefabRegistry] Loaded %d prefab records for world '%s'", registry.records.size(), world.getName());
            });
        });
    }

    // load from disk asynchronously, returns empty registry if no save file exists yet
    private static CompletableFuture<PrefabRegistry> load(World world) {
        Path path = world.getSavePath().resolve(SAVE_FILE);
        return CompletableFuture.supplyAsync(() -> {
            PrefabRegistry registry = new PrefabRegistry(world.getName());
            try {
                SaveData saved = (SaveData) RawJsonReader.readSyncWithBak(path, SaveData.CODEC, LOGGER);
                if (saved != null && saved.records != null)
                    for (PrefabRecord record : saved.records) registry.records.add(record);
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("[PrefabRegistry] Failed to load for world '%s': %s", world.getName(), e.getMessage());
            }
            return registry;
        });
    }

    // save to disk synchronously
    private void save(World world) {
        try {
            SaveData data = new SaveData();
            data.records = records.toArray(new PrefabRecord[0]);
            BsonUtil.writeDocument(world.getSavePath().resolve(SAVE_FILE), SaveData.CODEC.encode(data).asDocument());
            LOGGER.at(Level.INFO).log("[PrefabRegistry] Saved %d prefab records for world '%s'", records.size(), worldName);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("[PrefabRegistry] Failed to save for world '%s': %s", worldName, e.getMessage());
        }
    }

    // record a newly placed prefab and schedule a debounced save 3 seconds from now
    public void add(PrefabRecord record, World world) {
        records.add(record);

        // cancel any pending save and reschedule — fires 3s after the last placement
        if (pendingSave != null && !pendingSave.isDone()) pendingSave.cancel(false);
        pendingSave = saveScheduler.schedule(() -> save(world), 3, TimeUnit.SECONDS);
    }

    // find all records whose 3D bounds contain the given world position
    public List<PrefabRecord> getAt(int x, int y, int z) {
        List<PrefabRecord> result = new ArrayList<>();
        for (PrefabRecord r : records) if (r.contains(x, y, z)) result.add(r);
        return result;
    }

    // find the closest record of a given type to an XZ position, or null if none within maxDistance
    @Nullable
    public PrefabRecord getClosest(PrefabRecord.Type type, int x, int z, double maxDistance) {
        PrefabRecord closest = null;
        double closestDist = maxDistance;
        for (PrefabRecord r : records) {
            if (r.getType() != type) continue;
            double dist = r.distanceTo(x, z);
            if (dist < closestDist) { closestDist = dist; closest = r; }
        }
        return closest;
    }

    // --- static access ---

    @Nullable public static PrefabRegistry get(World world)      { return REGISTRIES.get(world.getName()); }
    @Nullable public static PrefabRegistry get(String worldName) { return REGISTRIES.get(worldName); }

    // cancel any pending debounced save and do a final synchronous flush before removing
    public static void saveAndRemove(World world) {
        PrefabRegistry registry = REGISTRIES.remove(world.getName());
        if (registry == null) return;
        if (registry.pendingSave != null && !registry.pendingSave.isDone()) registry.pendingSave.cancel(false);
        registry.saveScheduler.shutdownNow();
        registry.save(world);
    }

    // --- persistence wrapper ---

    @SuppressWarnings("unchecked")
    private static class SaveData {
        public static final BuilderCodec<SaveData> CODEC = BuilderCodec
                .builder(SaveData.class, SaveData::new)
                .append(new KeyedCodec<>("Records", new ArrayCodec<>(PrefabRecord.CODEC, PrefabRecord[]::new)),
                        (d, v) -> d.records = v, d -> d.records).add()
                .build();

        public PrefabRecord[] records;
    }
}