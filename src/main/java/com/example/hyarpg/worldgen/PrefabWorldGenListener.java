package com.example.hyarpg.worldgen;

// Hytale Imports
import com.example.hyarpg.configs.ModConfig;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.math.util.HashUtil;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.buffer.impl.IPrefabBuffer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.events.ChunkPreLoadProcessEvent;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;

// Java Imports
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Scans the mod's prefab folder for *.prefab.json files and seeds them
 * into the world the first time each chunk ever generates.
 *
 * One prefab is chosen at random per eligible region. Density is controlled
 * by a single ModConfig value (prefabDensity, 0.0–1.0) which drives both
 * how large each region is (lower density → larger regions → sparser spawns)
 * and the minimum separation between placements.
 *
 * Placement writes blocks directly onto the chunk buffer using the same
 * PLACEMENT_SETTINGS flags as OreDistanceListener so no live World operations
 * are required for the blocks themselves. Surface Y is resolved via
 * ChunkGenerator.getHeight() which is a pure cached noise function.
 *
 * Cross-chunk overflow is silently clipped — blocks outside this chunk's
 * XZ bounds are skipped. Keep prefabs reasonably sized (under ~24×24 XZ
 * footprint) to avoid visible truncation at chunk edges.
 */
public class PrefabWorldGenListener {

    private static final Logger LOGGER = Logger.getLogger(PrefabWorldGenListener.class.getName());

    private static final int CHUNK_SIZE = 32;

    /**
     * Same placement flags as OreDistanceListener:
     *   0x02  skip setState   (needs live world thread)
     *   0x04  skip particles  (needs live world)
     *   0x08  skip setFiller  (needs reference)
     *   0x10  skip removeFiller (needs reference)
     *   0x200 skip height update (terrain gen recalculates)
     */
    private static final int PLACEMENT_SETTINGS = 0x02 | 0x04 | 0x08 | 0x10 | 0x200;

    // -------------------------------------------------------------------------
    // Density → region/exclusion mapping
    //
    // density 0.0 → regionSize 1024 blocks, exclusionRadius 768
    // density 1.0 → regionSize 128  blocks, exclusionRadius 64
    // -------------------------------------------------------------------------
    private static final int REGION_SIZE_MIN  = 128;
    private static final int REGION_SIZE_MAX  = 1024;
    private static final int EXCLUSION_MIN    = 64;
    private static final int EXCLUSION_MAX    = 768;

    // -------------------------------------------------------------------------

    /** Folder to scan for *.prefab.json files. Set once at startup. */
    private final Path prefabFolder;

    /**
     * Cached list of prefab paths found in the folder.
     * Populated lazily on first chunk event and never refreshed at runtime
     * (restart required to pick up new files, which is fine for world-gen).
     */
    private List<Path> prefabPaths = null;

    public PrefabWorldGenListener(Path prefabFolder) {
        this.prefabFolder = prefabFolder;
    }

    // -------------------------------------------------------------------------

    public void register(EventRegistry eventRegistry) {
        eventRegistry.registerGlobal(
                EventPriority.LAST,
                ChunkPreLoadProcessEvent.class,
                this::onChunkPreLoad
        );
        LOGGER.info("[HyARPG] PrefabWorldGenListener registered, scanning: " + prefabFolder);
    }

    // -------------------------------------------------------------------------

    private void onChunkPreLoad(ChunkPreLoadProcessEvent event) {
        if (!event.isNewlyGenerated()) return;

        // Lazy-load prefab list
        if (prefabPaths == null) {
            prefabPaths = scanPrefabFolder();
        }
        if (prefabPaths.isEmpty()) return;


        // get density and Clamp to valid range
        double density = ModConfig.get().world.prefabDensity;
        density = Math.max(0.0, Math.min(1.0, density));

        // Map density → region size (inverse: higher density = smaller regions)
        int regionSize = (int) Math.round(REGION_SIZE_MAX - density * (REGION_SIZE_MAX - REGION_SIZE_MIN));

        // Map density → exclusion radius (inverse: higher density = tighter exclusion)
        int exclusionRadius = (int) Math.round(EXCLUSION_MAX - density * (EXCLUSION_MAX - EXCLUSION_MIN));

        WorldChunk chunk = event.getChunk();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        // Determine which region this chunk falls into
        int chunkCentreBlockX = chunkX * CHUNK_SIZE + CHUNK_SIZE / 2;
        int chunkCentreBlockZ = chunkZ * CHUNK_SIZE + CHUNK_SIZE / 2;

        int regionX = Math.floorDiv(chunkCentreBlockX, regionSize);
        int regionZ = Math.floorDiv(chunkCentreBlockZ, regionSize);

        // Each region has exactly one candidate anchor point, deterministically
        // derived from the region coords and world seed so it never changes.
        World world = chunk.getWorld();
        int worldSeed = world.getWorldConfig().getSeed();

        long regionSeed = HashUtil.hash((long) worldSeed, (long) regionX, (long) regionZ);
        Random regionRandom = new Random(regionSeed);

        // Pick the anchor block within this region
        int anchorBlockX = regionX * regionSize + regionRandom.nextInt(regionSize);
        int anchorBlockZ = regionZ * regionSize + regionRandom.nextInt(regionSize);

        // Only the chunk that contains the anchor is responsible for placement
        int anchorChunkX = Math.floorDiv(anchorBlockX, CHUNK_SIZE);
        int anchorChunkZ = Math.floorDiv(anchorBlockZ, CHUNK_SIZE);
        if (anchorChunkX != chunkX || anchorChunkZ != chunkZ) return;

        // Exclusion check: scan neighbouring regions to make sure no closer
        // anchor would land within exclusionRadius of this one.
        if (isExcluded(anchorBlockX, anchorBlockZ,
                regionX, regionZ, regionSize, exclusionRadius, worldSeed)) {
            return;
        }

        // Pick a prefab randomly (uniform weight — users control what's in folder)
        Path prefabPath = prefabPaths.get(regionRandom.nextInt(prefabPaths.size()));

        // Resolve surface Y via ChunkGenerator (pure noise, no chunk data needed)
        ChunkGenerator generator = (ChunkGenerator) world.getChunkStore().getGenerator(world);
        // getGenerator can throw WorldMapLoadException; treat failure as skip
        if (generator == null) return;

        int surfaceY = generator.getHeight(worldSeed, anchorBlockX, anchorBlockZ);
        if (surfaceY <= 0 || surfaceY >= 318) return; // ocean void or sky edge

        // Load and paste the prefab
        try {
            IPrefabBuffer buffer = loadPrefab(prefabPath);
            if (buffer == null) return;

            pasteIntoChunk(buffer, chunk, anchorBlockX, surfaceY, anchorBlockZ,
                    chunkX, chunkZ);

            LOGGER.log(Level.FINE,
                    "[HyARPG] Placed prefab ''{0}'' at ({1}, {2}, {3})",
                    new Object[]{prefabPath.getFileName(), anchorBlockX, surfaceY, anchorBlockZ});

        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                    "[HyARPG] Failed to place prefab ''{0}'': {1}",
                    new Object[]{prefabPath.getFileName(), e});
        }
    }

    // -------------------------------------------------------------------------

    /**
     * Returns true if another region's anchor would land closer than
     * exclusionRadius to (ax, az), meaning we should skip this placement.
     *
     * We only need to check the ring of immediately neighbouring regions
     * because the exclusion radius is capped at REGION_SIZE_MAX so a region
     * two steps away can never be close enough to matter.
     */
    private boolean isExcluded(int ax, int az,
                               int regionX, int regionZ,
                               int regionSize, int exclusionRadius,
                               int worldSeed) {
        int searchRadius = (int) Math.ceil((double) exclusionRadius / regionSize) + 1;
        long exclusionRadius2 = (long) exclusionRadius * exclusionRadius;

        for (int drx = -searchRadius; drx <= searchRadius; drx++) {
            for (int drz = -searchRadius; drz <= searchRadius; drz++) {
                if (drx == 0 && drz == 0) continue;

                int nrx = regionX + drx;
                int nrz = regionZ + drz;

                long neighbourSeed = HashUtil.hash((long) worldSeed, (long) nrx, (long) nrz);
                Random neighbourRandom = new Random(neighbourSeed);

                int nax = nrx * regionSize + neighbourRandom.nextInt(regionSize);
                int naz = nrz * regionSize + neighbourRandom.nextInt(regionSize);

                long dx = ax - nax;
                long dz = az - naz;
                if (dx * dx + dz * dz < exclusionRadius2) {
                    // A neighbour anchor is closer than exclusion radius.
                    // The lower-seed region wins — consistent across all chunks.
                    if (neighbourSeed < HashUtil.hash((long) worldSeed,
                            (long) regionX, (long) regionZ)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------

    /**
     * Iterates the prefab buffer and writes blocks into the chunk,
     * clipping anything that falls outside this chunk's XZ column.
     * Uses PLACEMENT_SETTINGS to skip operations that need a live world.
     *
     * The prefab anchor is placed at (anchorX, anchorY, anchorZ) in world
     * space. The buffer's own anchor offsets are subtracted so the prefab's
     * designated anchor block lands exactly on the surface.
     */
    private void pasteIntoChunk(IPrefabBuffer buffer, WorldChunk chunk,
                                int anchorX, int anchorY, int anchorZ,
                                int chunkX, int chunkZ) {
        int bufAnchorX = buffer.getAnchorX();
        int bufAnchorY = buffer.getAnchorY();
        int bufAnchorZ = buffer.getAnchorZ();

        buffer.forEach(
                IPrefabBuffer.iterateAllColumns(),
                // Block consumer
                (x, y, z, blockId, holder, supportValue, blockRotation,
                 filler, call, fluidId, fluidLevel) -> {

                    if (blockId == 0) return; // skip air

                    int wx = anchorX + (x - bufAnchorX);
                    int wy = anchorY + (y - bufAnchorY);
                    int wz = anchorZ + (z - bufAnchorZ);

                    if (wy < 1 || wy > 318) return;

                    // Clip to this chunk's XZ column
                    if (Math.floorDiv(wx, CHUNK_SIZE) != chunkX) return;
                    if (Math.floorDiv(wz, CHUNK_SIZE) != chunkZ) return;

                    com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType blockType =
                            com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType
                                    .getAssetMap().getAsset(blockId);
                    if (blockType == null) return;

                    chunk.setBlock(wx, wy, wz, blockId, blockType,
                            blockRotation, filler, PLACEMENT_SETTINGS);
                },
                // Entity consumer — skip, no live world at pre-load
                (x, z, entityWrappers, call) -> {},
                // Child prefab consumer — skip nested prefabs for simplicity
                (x, y, z, path, fitHeightmap, inheritSeed,
                 inheritHeightCondition, weights, rotation, call) -> {},
                // PrefabBufferCall with no rotation (North-facing)
                new com.hypixel.hytale.server.core.prefab.selection.buffer.PrefabBufferCall(
                        new Random(), com.hypixel.hytale.server.core.prefab.PrefabRotation.ROTATION_0)
        );
    }

    // -------------------------------------------------------------------------

    /**
     * Loads a prefab buffer from disk via PrefabStore.
     * PrefabStore caches results so repeated loads are cheap.
     */
    private IPrefabBuffer loadPrefab(Path path) {
        try {
            // BlockSelection implements IPrefabBuffer
            return (IPrefabBuffer) PrefabStore.get().getPrefab(path);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                    "[HyARPG] Could not load prefab ''{0}'': {1}",
                    new Object[]{path.getFileName(), e});
            return null;
        }
    }

    // -------------------------------------------------------------------------

    /**
     * Scans the prefab folder for *.prefab.json files.
     * Returns an empty list (with a warning) if the folder doesn't exist yet.
     */
    private List<Path> scanPrefabFolder() {
        List<Path> result = new ArrayList<>();

        if (!Files.exists(prefabFolder)) {
            try {
                Files.createDirectories(prefabFolder);
                LOGGER.info("[HyARPG] Created prefab folder: " + prefabFolder
                        + " — drop .prefab.json files here to seed them into the world.");
            } catch (IOException e) {
                LOGGER.log(Level.WARNING,
                        "[HyARPG] Could not create prefab folder: " + prefabFolder, e);
            }
            return result;
        }

        try (var stream = Files.walk(prefabFolder)) {
            stream.filter(p -> p.toString().endsWith(".prefab.json"))
                    .forEach(result::add);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING,
                    "[HyARPG] Failed to scan prefab folder: " + prefabFolder, e);
        }

        LOGGER.info("[HyARPG] Found " + result.size()
                + " prefab(s) in " + prefabFolder);
        return result;
    }
}