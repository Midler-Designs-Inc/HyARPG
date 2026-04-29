package com.example.hyarpg.worldgen;

// Hytale Imports
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.ChunkColumn;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.events.ChunkPreLoadProcessEvent;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;

// Mod Imports
import com.example.hyarpg.configs.ModConfig;
import com.example.hyarpg.configs.Config_World;

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
    private static final int UNDERGROUND_FLOOR = -30;
    private static final int UNDERGROUND_PADDING = 20;

    // Skip operations that require a live world/reference at pre-load time
    private static final int PLACEMENT_SETTINGS = 0x04 | 0x08 | 0x10 | 0x200;

    // Spawner block types to randomly choose from for dungeon prefabs
    private static final String[] SPAWNER_BLOCKS = {
        "HyARPG_BlockSpawner_Goblin",
        "HyARPG_BlockSpawner_Outlanders",
        "HyARPG_BlockSpawner_Throk",
        "HyARPG_BlockSpawner_Undead",
        "HyARPG_BlockSpawner_Void"
    };

    // prefab lists
    private final Path prefabFolder;
    private List<Path> surfacePrefabs = null;
    private List<Path> aquaticPrefabs = null;
    private List<Path> undergroundPrefabs = null;
    private List<Path> surfaceDungeonPrefabs = null;
    private List<Path> undergroundDungeonPrefabs = null;

    // static list of prefab containers
    public static final java.util.Set<Long> PREFAB_CONTAINER_POSITIONS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public PrefabWorldGenListener(Path prefabFolder) { this.prefabFolder = prefabFolder; }

    public void register(EventRegistry eventRegistry) {
        // Create all prefab subfolders on startup if they don't exist
        for (String sub : new String[]{"surface", "underground", "surface_dungeon", "underground_dungeon"}) {
            Path dir = prefabFolder.resolve(sub);
            if (!Files.exists(dir)) {
                try { Files.createDirectories(dir); } catch (IOException e) { LOGGER.warning("[HyARPG] Could not create prefab folder: " + dir + ": " + e); }
            }
        }
        eventRegistry.registerGlobal(EventPriority.FIRST, ChunkPreLoadProcessEvent.class, this::onChunkPreLoad);
        LOGGER.info("[HyARPG] PrefabWorldGenListener registered, folder: " + prefabFolder);
    }

    private void onChunkPreLoad(ChunkPreLoadProcessEvent event) {
        if (!event.isNewlyGenerated()) return;

        // Scan prefab subfolders once per session
        if (surfacePrefabs == null) surfacePrefabs = scanFolder(prefabFolder.resolve("surface"));
        if (aquaticPrefabs == null) aquaticPrefabs = scanFolder(prefabFolder.resolve("aquatic"));
        if (undergroundPrefabs == null) undergroundPrefabs = scanFolder(prefabFolder.resolve("underground"));
        if (surfaceDungeonPrefabs == null) surfaceDungeonPrefabs = scanFolder(prefabFolder.resolve("surface_dungeon"));
        if (undergroundDungeonPrefabs == null) undergroundDungeonPrefabs = scanFolder(prefabFolder.resolve("underground_dungeon"));

        // Gather chunk/world context
        WorldChunk chunk = event.getChunk();
        World world = chunk.getWorld();
        long worldSeed = world.getWorldConfig().getSeed();

        ChunkGenerator generator = (ChunkGenerator) world.getChunkStore().getGenerator();
        if (generator == null) return;

        Config_World cfg = ModConfig.get().world;
        int chunkMinX = chunk.getX() * CHUNK_SIZE, chunkMaxX = chunkMinX + CHUNK_SIZE - 1;
        int chunkMinZ = chunk.getZ() * CHUNK_SIZE, chunkMaxZ = chunkMinZ + CHUNK_SIZE - 1;

        // Each type operates on its own independent region grid
        if (!surfacePrefabs.isEmpty()) {
            int rMinX = Math.floorDiv(chunkMinX - cfg.prefabSurfaceMaxSize, cfg.prefabSurfaceRegionSize), rMaxX = Math.floorDiv(chunkMaxX + cfg.prefabSurfaceMaxSize, cfg.prefabSurfaceRegionSize);
            int rMinZ = Math.floorDiv(chunkMinZ - cfg.prefabSurfaceMaxSize, cfg.prefabSurfaceRegionSize), rMaxZ = Math.floorDiv(chunkMaxZ + cfg.prefabSurfaceMaxSize, cfg.prefabSurfaceRegionSize);
            for (int rx = rMinX; rx <= rMaxX; rx++) for (int rz = rMinZ; rz <= rMaxZ; rz++) processSurface(rx, rz, cfg, worldSeed, chunk, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, generator);
        }

//        if (!aquaticPrefabs.isEmpty()) {
//            int rMinX = Math.floorDiv(chunkMinX - cfg.prefabAquaticMaxSize, cfg.prefabAquaticRegionSize), rMaxX = Math.floorDiv(chunkMaxX + cfg.prefabAquaticMaxSize, cfg.prefabAquaticRegionSize);
//            int rMinZ = Math.floorDiv(chunkMinZ - cfg.prefabAquaticMaxSize, cfg.prefabAquaticRegionSize), rMaxZ = Math.floorDiv(chunkMaxZ + cfg.prefabAquaticMaxSize, cfg.prefabAquaticRegionSize);
//            for (int rx = rMinX; rx <= rMaxX; rx++) for (int rz = rMinZ; rz <= rMaxZ; rz++) processAquatic(rx, rz, cfg, worldSeed, chunk, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, generator);
//        }

        if (!undergroundPrefabs.isEmpty()) {
            int rMinX = Math.floorDiv(chunkMinX - cfg.prefabUndergroundMaxSize, cfg.prefabUndergroundRegionSize), rMaxX = Math.floorDiv(chunkMaxX + cfg.prefabUndergroundMaxSize, cfg.prefabUndergroundRegionSize);
            int rMinZ = Math.floorDiv(chunkMinZ - cfg.prefabUndergroundMaxSize, cfg.prefabUndergroundRegionSize), rMaxZ = Math.floorDiv(chunkMaxZ + cfg.prefabUndergroundMaxSize, cfg.prefabUndergroundRegionSize);
            for (int rx = rMinX; rx <= rMaxX; rx++) for (int rz = rMinZ; rz <= rMaxZ; rz++) processUnderground(rx, rz, cfg, worldSeed, chunk, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, generator);
        }

        if (!surfaceDungeonPrefabs.isEmpty()) {
            int rMinX = Math.floorDiv(chunkMinX - cfg.prefabSurfaceDungeonMaxSize, cfg.prefabSurfaceDungeonRegionSize), rMaxX = Math.floorDiv(chunkMaxX + cfg.prefabSurfaceDungeonMaxSize, cfg.prefabSurfaceDungeonRegionSize);
            int rMinZ = Math.floorDiv(chunkMinZ - cfg.prefabSurfaceDungeonMaxSize, cfg.prefabSurfaceDungeonRegionSize), rMaxZ = Math.floorDiv(chunkMaxZ + cfg.prefabSurfaceDungeonMaxSize, cfg.prefabSurfaceDungeonRegionSize);
            for (int rx = rMinX; rx <= rMaxX; rx++) for (int rz = rMinZ; rz <= rMaxZ; rz++) processSurfaceDungeon(rx, rz, cfg, worldSeed, chunk, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, generator);
        }

        if (!undergroundDungeonPrefabs.isEmpty()) {
            int rMinX = Math.floorDiv(chunkMinX - cfg.prefabUndergroundDungeonMaxSize, cfg.prefabUndergroundDungeonRegionSize), rMaxX = Math.floorDiv(chunkMaxX + cfg.prefabUndergroundDungeonMaxSize, cfg.prefabUndergroundDungeonRegionSize);
            int rMinZ = Math.floorDiv(chunkMinZ - cfg.prefabUndergroundDungeonMaxSize, cfg.prefabUndergroundDungeonRegionSize), rMaxZ = Math.floorDiv(chunkMaxZ + cfg.prefabUndergroundDungeonMaxSize, cfg.prefabUndergroundDungeonRegionSize);
            for (int rx = rMinX; rx <= rMaxX; rx++) for (int rz = rMinZ; rz <= rMaxZ; rz++) processUndergroundDungeon(rx, rz, cfg, worldSeed, chunk, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, generator);
        }
    }

    // Surface: anchor placed on terrain surface Y
    private void processSurface(int regionX, int regionZ, Config_World cfg, long worldSeed, WorldChunk chunk, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ, ChunkGenerator generator) {
        long regionSeed = (long) regionX * 341873128712L + (long) regionZ * 132897987541L ^ worldSeed ^ 0x1L;
        Random random = new Random(regionSeed);
        if (random.nextDouble() >= Math.max(0.0, Math.min(1.0, cfg.prefabSurfaceSpawnChance))) return;

        int margin = 4;
        int anchorX = regionX * cfg.prefabSurfaceRegionSize + margin + random.nextInt(Math.max(1, cfg.prefabSurfaceRegionSize - margin * 2));
        int anchorZ = regionZ * cfg.prefabSurfaceRegionSize + margin + random.nextInt(Math.max(1, cfg.prefabSurfaceRegionSize - margin * 2));
        BlockSelection buffer = loadPrefab(surfacePrefabs.get(random.nextInt(surfacePrefabs.size())));
        if (buffer == null) return;

        // Sample 4 corners of the prefab footprint
        int[] prefabBoundsXZ = getPrefabFootprint(buffer);
        int prefabMinX = anchorX + (prefabBoundsXZ[0] - buffer.getAnchorX());
        int prefabMaxX = anchorX + (prefabBoundsXZ[1] - buffer.getAnchorX());
        int prefabMinZ = anchorZ + (prefabBoundsXZ[2] - buffer.getAnchorZ());
        int prefabMaxZ = anchorZ + (prefabBoundsXZ[3] - buffer.getAnchorZ());

        int h1 = generator.getHeight((int)worldSeed, prefabMinX, prefabMinZ);
        int h2 = generator.getHeight((int)worldSeed, prefabMaxX, prefabMinZ);
        int h3 = generator.getHeight((int)worldSeed, prefabMinX, prefabMaxZ);
        int h4 = generator.getHeight((int)worldSeed, prefabMaxX, prefabMaxZ);
        int groundY = Math.min(Math.min(h1, h2), Math.min(h3, h4));

        // Offset anchorY so prefab bottom aligns with groundY
        int prefabBottomOffset = prefabBoundsXZ[4] - buffer.getAnchorY(); // min Y offset from anchor
        int anchorY = groundY - prefabBottomOffset;
        if (anchorY <= 0 || anchorY >= 318) return;

        pasteSlice(false, buffer, chunk, anchorX, anchorY, anchorZ, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
    }

    // Aquatic: anchor placed at water floor — TODO: implement water detection
    private void processAquatic(int regionX, int regionZ, Config_World cfg, long worldSeed, WorldChunk chunk, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ, ChunkGenerator generator) {
//        long regionSeed = (long) regionX * 341873128712L + (long) regionZ * 132897987541L ^ worldSeed ^ 0x2L;
//        Random random = new Random(regionSeed);
//        if (random.nextDouble() >= Math.max(0.0, Math.min(1.0, cfg.prefabAquaticSpawnChance))) return;
//
//        int margin = 4;
//        int anchorX = regionX * cfg.prefabAquaticRegionSize + margin + random.nextInt(Math.max(1, cfg.prefabAquaticRegionSize - margin * 2));
//        int anchorZ = regionZ * cfg.prefabAquaticRegionSize + margin + random.nextInt(Math.max(1, cfg.prefabAquaticRegionSize - margin * 2));
//        BlockSelection buffer = loadPrefab(aquaticPrefabs.get(random.nextInt(aquaticPrefabs.size())));
//        if (buffer == null) return;
//
//        // TODO: detect ocean floor Y — using surface Y as placeholder until water detection is implemented
//        int anchorY = generator.getHeight((int) worldSeed, anchorX, anchorZ);
//        if (anchorY <= 0 || anchorY >= 318) return;
//
//        pasteSlice(buffer, chunk, anchorX, anchorY, anchorZ, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
    }

    // Underground: anchor randomized between subsurface and UNDERGROUND_FLOOR with push-down logic
    private void processUnderground(int regionX, int regionZ, Config_World cfg, long worldSeed, WorldChunk chunk, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ, ChunkGenerator generator) {
        long regionSeed = (long) regionX * 341873128712L + (long) regionZ * 132897987541L ^ worldSeed ^ 0x3L;
        Random random = new Random(regionSeed);
        if (random.nextDouble() >= Math.max(0.0, Math.min(1.0, cfg.prefabUndergroundSpawnChance))) return;

        int margin = 4;
        int anchorX = regionX * cfg.prefabUndergroundRegionSize + margin + random.nextInt(Math.max(1, cfg.prefabUndergroundRegionSize - margin * 2));
        int anchorZ = regionZ * cfg.prefabUndergroundRegionSize + margin + random.nextInt(Math.max(1, cfg.prefabUndergroundRegionSize - margin * 2));
        BlockSelection buffer = loadPrefab(undergroundPrefabs.get(random.nextInt(undergroundPrefabs.size())));
        if (buffer == null) return;

        int anchorY = resolveUndergroundAnchorY(buffer, generator, worldSeed, anchorX, anchorZ, random);
        if (anchorY == Integer.MIN_VALUE) return;

        pasteSlice(true, buffer, chunk, anchorX, anchorY, anchorZ, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
    }

    // Surface dungeon: same as surface but also places spawner blocks
    private void processSurfaceDungeon(int regionX, int regionZ, Config_World cfg, long worldSeed, WorldChunk chunk, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ, ChunkGenerator generator) {
        long regionSeed = (long) regionX * 341873128712L + (long) regionZ * 132897987541L ^ worldSeed ^ 0x4L;
        Random random = new Random(regionSeed);
        if (random.nextDouble() >= Math.max(0.0, Math.min(1.0, cfg.prefabSurfaceDungeonSpawnChance))) return;

        int margin = 4;
        int anchorX = regionX * cfg.prefabSurfaceDungeonRegionSize + margin + random.nextInt(Math.max(1, cfg.prefabSurfaceDungeonRegionSize - margin * 2));
        int anchorZ = regionZ * cfg.prefabSurfaceDungeonRegionSize + margin + random.nextInt(Math.max(1, cfg.prefabSurfaceDungeonRegionSize - margin * 2));
        BlockSelection buffer = loadPrefab(surfaceDungeonPrefabs.get(random.nextInt(surfaceDungeonPrefabs.size())));
        if (buffer == null) return;

        // Sample 4 corners of the prefab footprint
        int[] prefabBoundsXZ = getPrefabFootprint(buffer);
        int prefabMinX = anchorX + (prefabBoundsXZ[0] - buffer.getAnchorX());
        int prefabMaxX = anchorX + (prefabBoundsXZ[1] - buffer.getAnchorX());
        int prefabMinZ = anchorZ + (prefabBoundsXZ[2] - buffer.getAnchorZ());
        int prefabMaxZ = anchorZ + (prefabBoundsXZ[3] - buffer.getAnchorZ());

        int h1 = generator.getHeight((int)worldSeed, prefabMinX, prefabMinZ);
        int h2 = generator.getHeight((int)worldSeed, prefabMaxX, prefabMinZ);
        int h3 = generator.getHeight((int)worldSeed, prefabMinX, prefabMaxZ);
        int h4 = generator.getHeight((int)worldSeed, prefabMaxX, prefabMaxZ);
        int groundY = Math.min(Math.min(h1, h2), Math.min(h3, h4));

        // Offset anchorY so prefab bottom aligns with groundY
        int prefabBottomOffset = prefabBoundsXZ[4] - buffer.getAnchorY(); // min Y offset from anchor
        int anchorY = groundY - prefabBottomOffset;
        if (anchorY <= 0 || anchorY >= 318) return;

        int[] bounds = computeBounds(buffer);
        pasteSlice(true, buffer, chunk, anchorX, anchorY, anchorZ, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
        placeSpawners(buffer, bounds, chunk, anchorX, anchorY, anchorZ, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, random, cfg.prefabSurfaceDungeonSpawnerDensity);
    }

    // Underground dungeon: same as underground but also places spawner blocks
    private void processUndergroundDungeon(int regionX, int regionZ, Config_World cfg, long worldSeed, WorldChunk chunk, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ, ChunkGenerator generator) {
        long regionSeed = (long) regionX * 341873128712L + (long) regionZ * 132897987541L ^ worldSeed ^ 0x5L;
        Random random = new Random(regionSeed);
        if (random.nextDouble() >= Math.max(0.0, Math.min(1.0, cfg.prefabUndergroundDungeonSpawnChance))) return;

        int margin = 4;
        int anchorX = regionX * cfg.prefabUndergroundDungeonRegionSize + margin + random.nextInt(Math.max(1, cfg.prefabUndergroundDungeonRegionSize - margin * 2));
        int anchorZ = regionZ * cfg.prefabUndergroundDungeonRegionSize + margin + random.nextInt(Math.max(1, cfg.prefabUndergroundDungeonRegionSize - margin * 2));
        BlockSelection buffer = loadPrefab(undergroundDungeonPrefabs.get(random.nextInt(undergroundDungeonPrefabs.size())));
        if (buffer == null) return;

        int anchorY = resolveUndergroundAnchorY(buffer, generator, worldSeed, anchorX, anchorZ, random);
        if (anchorY == Integer.MIN_VALUE) return;

        int[] bounds = computeBounds(buffer);
        pasteSlice(true, buffer, chunk, anchorX, anchorY, anchorZ, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
        placeSpawners(buffer, bounds, chunk, anchorX, anchorY, anchorZ, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, random, cfg.prefabUndergroundDungeonSpawnerDensity);
    }

    // Shared underground Y resolution with push-down logic
    private int resolveUndergroundAnchorY(BlockSelection buffer, ChunkGenerator generator, long worldSeed, int anchorX, int anchorZ, Random random) {
        int[] b = {Integer.MAX_VALUE, Integer.MIN_VALUE};
        buffer.forEachBlock((x, y, z, block) -> { b[0] = Math.min(b[0], y); b[1] = Math.max(b[1], y); });
        if (b[0] == Integer.MAX_VALUE) return Integer.MIN_VALUE;
        int bufAnchorY = buffer.getAnchorY();
        int prefabTopOffset = b[1] - bufAnchorY;
        int prefabBottomOffset = b[0] - bufAnchorY;

        int surfaceY = generator.getHeight((int) worldSeed, anchorX, anchorZ);
        int anchorY = UNDERGROUND_FLOOR + random.nextInt(Math.max(1, (surfaceY - UNDERGROUND_PADDING) - UNDERGROUND_FLOOR));

        if (anchorY + prefabTopOffset > surfaceY - UNDERGROUND_PADDING) anchorY = surfaceY - UNDERGROUND_PADDING - prefabTopOffset;
        if (anchorY + prefabBottomOffset < UNDERGROUND_FLOOR) anchorY = UNDERGROUND_FLOOR - prefabBottomOffset;
        if (anchorY + prefabTopOffset > surfaceY - UNDERGROUND_PADDING) return Integer.MIN_VALUE;
        if (anchorY + prefabBottomOffset < UNDERGROUND_FLOOR) return Integer.MIN_VALUE;

        return anchorY;
    }

    // Place spawner blocks at random positions near existing prefab blocks within the bounding box
    private void placeSpawners(BlockSelection buffer, int[] bounds, WorldChunk chunk, int anchorX, int anchorY, int anchorZ, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ, Random random, double density) {
        int bufAnchorX = buffer.getAnchorX(), bufAnchorY = buffer.getAnchorY(), bufAnchorZ = buffer.getAnchorZ();
        int volumeX = bounds[3] - bounds[0], volumeY = bounds[4] - bounds[1], volumeZ = bounds[5] - bounds[2];
        int volume = Math.max(1, volumeX * volumeY * volumeZ);
        int spawnerCount = (int) ((volume / 10000.0) * density);
        if (spawnerCount <= 0) return;

        // Build list of ALL prefab block world positions regardless of chunk bounds
        List<int[]> blockPositions = new ArrayList<>();
        buffer.forEachBlock((x, y, z, block) -> {
            if (block.blockId() == 0) return;
            int wx = anchorX + (x - bufAnchorX), wy = anchorY + (y - bufAnchorY), wz = anchorZ + (z - bufAnchorZ);
            blockPositions.add(new int[]{wx, wy, wz});
        });
        if (blockPositions.isEmpty()) return;

        // Resolve spawner block types once
        int[] spawnerIds = new int[SPAWNER_BLOCKS.length];
        BlockType[] spawnerTypes = new BlockType[SPAWNER_BLOCKS.length];
        for (int i = 0; i < SPAWNER_BLOCKS.length; i++) {
            spawnerIds[i] = BlockType.getAssetMap().getIndex(SPAWNER_BLOCKS[i]);
            spawnerTypes[i] = BlockType.getAssetMap().getAsset(spawnerIds[i]);
        }

        int[] dx = {1, -1, 0, 0, 0, 0};
        int[] dy = {0, 0, 1, -1, 0, 0};
        int[] dz = {0, 0, 0, 0, 1, -1};

        // Generate ALL spawner positions deterministically, place only those in this chunk
        for (int i = 0; i < spawnerCount; i++) {
            int[] target = blockPositions.get(random.nextInt(blockPositions.size()));

            int[] faceOrder = {0, 1, 2, 3, 4, 5};
            for (int a = 5; a > 0; a--) { int swap = random.nextInt(a + 1); int tmp = faceOrder[a]; faceOrder[a] = faceOrder[swap]; faceOrder[swap] = tmp; }

            int spawnerIndex = random.nextInt(SPAWNER_BLOCKS.length);

            for (int face : faceOrder) {
                int sx = target[0] + dx[face], sy = target[1] + dy[face], sz = target[2] + dz[face];
                if (sy < UNDERGROUND_FLOOR || sy > 318) continue;

                // Only place if in this chunk, but always consume the random state
                if (sx < chunkMinX || sx > chunkMaxX || sz < chunkMinZ || sz > chunkMaxZ) continue;
                if (chunk.getBlock(sx, sy, sz) != 0) continue;
                if (spawnerTypes[spawnerIndex] == null) continue;
                chunk.setBlock(sx, sy, sz, spawnerIds[spawnerIndex], spawnerTypes[spawnerIndex], 0, 0, PLACEMENT_SETTINGS);
                break;
            }
        }
    }

    // Compute bounding box from actual block positions
    private int[] computeBounds(BlockSelection buffer) {
        int[] b = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        buffer.forEachBlock((x, y, z, block) -> {
            b[0] = Math.min(b[0], x); b[1] = Math.min(b[1], y); b[2] = Math.min(b[2], z);
            b[3] = Math.max(b[3], x); b[4] = Math.max(b[4], y); b[5] = Math.max(b[5], z);
        });
        return b;
    }

    // Write only the blocks from this prefab that fall within the chunk's XZ column
    private void pasteSlice(boolean fillAir, BlockSelection buffer, WorldChunk chunk, int anchorX, int anchorY, int anchorZ, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
        int bufAnchorX = buffer.getAnchorX(), bufAnchorY = buffer.getAnchorY(), bufAnchorZ = buffer.getAnchorZ();
        BlockType airType = BlockType.getAssetMap().getAsset(0);

        // Compute bounding box from actual block positions
        int[] b = computeBounds(buffer);

        // Fill bounding box with air to mark positions as touched so terrain gen skips them
        if (fillAir && airType != null && b[0] != Integer.MAX_VALUE) {
            for (int x = b[0]; x <= b[3]; x++) {
                for (int y = b[1]; y <= b[4]; y++) {
                    for (int z = b[2]; z <= b[5]; z++) {
                        int wx = anchorX + (x - bufAnchorX), wy = anchorY + (y - bufAnchorY), wz = anchorZ + (z - bufAnchorZ);
                        if (wy < UNDERGROUND_FLOOR || wy > 318) continue;
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
            if (wy < UNDERGROUND_FLOOR || wy > 318) return;
            if (wx < chunkMinX || wx > chunkMaxX || wz < chunkMinZ || wz > chunkMaxZ) return;
            BlockType blockType = BlockType.getAssetMap().getAsset(block.blockId());
            if (blockType == null) return;

            // Skip benches
            if (blockType.getId().startsWith("Bench_")) return;

            // Track prefab container positions for loot table assignment
            if (blockType.getBlockEntity() != null) {
                ItemContainerBlock container = blockType.getBlockEntity().getComponent(ItemContainerBlock.getComponentType());
                if (container != null) {
                    PREFAB_CONTAINER_POSITIONS.add(posKey(wx, wy, wz));
                }
            }

            // set the block in place
            chunk.setBlock(wx, wy, wz, block.blockId(), blockType, block.rotation(), block.filler(), PLACEMENT_SETTINGS);
        });
    }

    // Load a prefab from disk via PrefabStore (results are cached)
    private BlockSelection loadPrefab(Path path) {
        try { return PrefabStore.get().getPrefab(path); } catch (Exception e) { LOGGER.warning("[HyARPG] Could not load prefab '" + path.getFileName() + "': " + e); return null; }
    }

    // Scan a folder for *.prefab.json files
    private List<Path> scanFolder(Path folder) {
        List<Path> result = new ArrayList<>();
        if (!Files.exists(folder)) return result;
        try (var stream = Files.walk(folder)) {
            stream.filter(p -> p.toString().endsWith(".prefab.json")).forEach(result::add);
        } catch (IOException e) { LOGGER.log(Level.WARNING, "[HyARPG] Failed to scan prefab folder: " + folder, e); }
        LOGGER.info("[HyARPG] Found " + result.size() + " prefab(s) in " + folder);
        return result;
    }

    // get a position key for container positions
    public static long posKey(int x, int y, int z) {
        return ((long)(x & 0xFFFFF) << 40) | ((long)(y & 0xFFFFF) << 20) | (z & 0xFFFFF);
    }

    // Returns [minX, maxX, minZ, maxZ, minY] in buffer-local coords
    private int[] getPrefabFootprint(BlockSelection buffer) {
        int[] b = {Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE};
        buffer.forEachBlock((x, y, z, block) -> {
            if (block.blockId() == 0) return;
            b[0] = Math.min(b[0], x); b[1] = Math.max(b[1], x);
            b[2] = Math.min(b[2], z); b[3] = Math.max(b[3], z);
            b[4] = Math.min(b[4], y);
        });
        return b;
    }
}