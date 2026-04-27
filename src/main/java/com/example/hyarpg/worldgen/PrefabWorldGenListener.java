package com.example.hyarpg.worldgen;

// Hytale Imports
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.events.ChunkPreLoadProcessEvent;
import com.hypixel.hytale.server.spawning.world.system.ChunkSpawningSystems;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;

// Mod Imports
import com.example.hyarpg.configs.ModConfig;

// Java Imports
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PrefabWorldGenListener {

    private static final Logger LOGGER = Logger.getLogger(PrefabWorldGenListener.class.getName());
    private static final int CHUNK_SIZE = 32;
    // Skip operations that require a live world/reference at pre-load time
    private static final int PLACEMENT_SETTINGS = 0x02 | 0x04 | 0x08 | 0x10 | 0x200;

    private final Path prefabFolder;
    private List<Path> prefabPaths = null;

    public PrefabWorldGenListener(Path prefabFolder) { this.prefabFolder = prefabFolder; }

    public void register(EventRegistry eventRegistry) {
        if (!Files.exists(prefabFolder)) {
            try { Files.createDirectories(prefabFolder); } catch (IOException e) { LOGGER.warning("[HyARPG] Could not create prefab folder: " + prefabFolder + ": " + e); }
        }
        eventRegistry.registerGlobal(EventPriority.LAST, ChunkPreLoadProcessEvent.class, this::onChunkPreLoad);
        LOGGER.info("[HyARPG] PrefabWorldGenListener registered, folder: " + prefabFolder);
    }

    private void onChunkPreLoad(ChunkPreLoadProcessEvent event) {
        if (!event.isNewlyGenerated()) return;

        // Scan prefab folder once per session
        if (prefabPaths == null) prefabPaths = scanPrefabFolder();
        if (prefabPaths.isEmpty()) return;

        // Gather chunk/world context
        WorldChunk chunk = event.getChunk();
        World world = chunk.getWorld();
        long worldSeed = world.getWorldConfig().getSeed();

        ChunkGenerator generator;
        try { generator = (ChunkGenerator) world.getChunkStore().getGenerator(); } catch (Exception e) { LOGGER.warning("[HyARPG] Could not get ChunkGenerator: " + e); return; }
        if (generator == null) return;

        // Find all regions that could have a prefab overlapping into this chunk
        int regionSize = ModConfig.get().world.prefabRegionSize;
        int maxPrefabSize = ModConfig.get().world.prefabMaxSize;
        int chunkMinX = chunk.getX() * CHUNK_SIZE, chunkMaxX = chunkMinX + CHUNK_SIZE - 1;
        int chunkMinZ = chunk.getZ() * CHUNK_SIZE, chunkMaxZ = chunkMinZ + CHUNK_SIZE - 1;
        int regionMinX = Math.floorDiv(chunkMinX - maxPrefabSize, regionSize), regionMaxX = Math.floorDiv(chunkMaxX + maxPrefabSize, regionSize);
        int regionMinZ = Math.floorDiv(chunkMinZ - maxPrefabSize, regionSize), regionMaxZ = Math.floorDiv(chunkMaxZ + maxPrefabSize, regionSize);

        // Process each candidate region, pasting any blocks that fall in this chunk's column
        for (int rx = regionMinX; rx <= regionMaxX; rx++) {
            for (int rz = regionMinZ; rz <= regionMaxZ; rz++) {
                processRegion(rx, rz, regionSize, worldSeed, chunk, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, generator);
            }
        }
    }

    // Determine if this region spawns a prefab and paste the slice that overlaps this chunk
    private void processRegion(int regionX, int regionZ, int regionSize, long worldSeed, WorldChunk chunk, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ, ChunkGenerator generator) {
        long regionSeed = (long) regionX * 341873128712L + (long) regionZ * 132897987541L ^ worldSeed;
        Random random = new Random(regionSeed);

        if (random.nextDouble() >= Math.max(0.0, Math.min(1.0, ModConfig.get().world.prefabSpawnChance))) return;

        // Pick deterministic anchor and prefab for this region
        int margin = 4;
        int anchorX = regionX * regionSize + margin + random.nextInt(Math.max(1, regionSize - margin * 2));
        int anchorZ = regionZ * regionSize + margin + random.nextInt(Math.max(1, regionSize - margin * 2));
        BlockSelection buffer = loadPrefab(prefabPaths.get(random.nextInt(prefabPaths.size())));
        if (buffer == null) return;

        // Resolve surface Y at anchor
        int anchorY = generator.getHeight((int) worldSeed, anchorX, anchorZ);
        if (anchorY <= 0 || anchorY >= 318) return;

        pasteSlice(buffer, chunk, anchorX, anchorY, anchorZ, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
    }

    // Write only the blocks from this prefab that fall within the chunk's XZ column
    private void pasteSlice(BlockSelection buffer, WorldChunk chunk, int anchorX, int anchorY, int anchorZ, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
        int bufAnchorX = buffer.getAnchorX(), bufAnchorY = buffer.getAnchorY(), bufAnchorZ = buffer.getAnchorZ();
        BlockType airType = BlockType.getAssetMap().getAsset(0);

        // Compute bounding box from actual block positions
        int[] b = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        buffer.forEachBlock((x, y, z, block) -> {
            b[0] = Math.min(b[0], x); b[1] = Math.min(b[1], y); b[2] = Math.min(b[2], z);
            b[3] = Math.max(b[3], x); b[4] = Math.max(b[4], y); b[5] = Math.max(b[5], z);
        });

        // Fill bounding box with air to mark positions as touched so terrain gen skips them
        if (airType != null && b[0] != Integer.MAX_VALUE) {
            for (int x = b[0]; x <= b[3]; x++) {
                for (int y = b[1]; y <= b[4]; y++) {
                    for (int z = b[2]; z <= b[5]; z++) {
                        int wx = anchorX + (x - bufAnchorX), wy = anchorY + (y - bufAnchorY), wz = anchorZ + (z - bufAnchorZ);
                        if (wy < 1 || wy > 318) continue;
                        if (wx < chunkMinX || wx > chunkMaxX || wz < chunkMinZ || wz > chunkMaxZ) continue;
                        chunk.setBlock(wx, wy, wz, 0, airType, 0, 0, PLACEMENT_SETTINGS);
                    }
                }
            }
        }

        // Paste actual prefab blocks over the cleared area
        buffer.forEachBlock((x, y, z, block) -> {
            if (block.blockId() == 0) return;
            int wx = anchorX + (x - bufAnchorX), wy = anchorY + (y - bufAnchorY), wz = anchorZ + (z - bufAnchorZ);
            if (wy < 1 || wy > 318) return;
            if (wx < chunkMinX || wx > chunkMaxX || wz < chunkMinZ || wz > chunkMaxZ) return;
            BlockType blockType = BlockType.getAssetMap().getAsset(block.blockId());
            if (blockType == null) return;
            chunk.setBlock(wx, wy, wz, block.blockId(), blockType, block.rotation(), block.filler(), PLACEMENT_SETTINGS);
        });
    }

    // Load a prefab from disk via PrefabStore (results are cached)
    private BlockSelection loadPrefab(Path path) {
        try { return PrefabStore.get().getPrefab(path); } catch (Exception e) { LOGGER.warning("[HyARPG] Could not load prefab '" + path.getFileName() + "': " + e); return null; }
    }

    // Scan the prefab folder for *.prefab.json files, creating the folder if needed
    private List<Path> scanPrefabFolder() {
        List<Path> result = new ArrayList<>();
        if (!Files.exists(prefabFolder)) return result;
        try (var stream = Files.walk(prefabFolder)) {
            stream.filter(p -> p.toString().endsWith(".prefab.json")).forEach(result::add);
        } catch (IOException e) { LOGGER.log(Level.WARNING, "[HyARPG] Failed to scan prefab folder: " + prefabFolder, e); }
        LOGGER.info("[HyARPG] Found " + result.size() + " prefab(s) in " + prefabFolder);
        return result;
    }
}