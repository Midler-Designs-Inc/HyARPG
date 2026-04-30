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
import com.example.hyarpg.configs.ModConfig;
import com.example.hyarpg.configs.Config_World;
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
    private static final int PLACEMENT_SETTINGS = 0x04 | 0x08 | 0x10;

    private static final String[] SPAWNER_BLOCKS = {
            "HyARPG_BlockSpawner_Goblin", "HyARPG_BlockSpawner_Outlanders", "HyARPG_BlockSpawner_Throk", "HyARPG_BlockSpawner_Undead", "HyARPG_BlockSpawner_Void"
    };

    private final Path prefabFolder;
    private List<Path> surfacePrefabs = null;
    private List<Path> aquaticPrefabs = null;
    private List<Path> undergroundPrefabs = null;
    private List<Path> surfaceDungeonPrefabs = null;
    private List<Path> undergroundDungeonPrefabs = null;
    private BlockSelection waywardShrinePrefab = null;
    private boolean waywardShrineLoaded = false;

    public static final java.util.Set<Long> PREFAB_CONTAINER_POSITIONS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public PrefabWorldGenListener(Path prefabFolder) { this.prefabFolder = prefabFolder; }

    public void register(EventRegistry eventRegistry) {
        // ensure prefab subfolders exist
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

        // scan prefab folders once per session
        if (surfacePrefabs == null) surfacePrefabs = scanFolder(prefabFolder.resolve("surface"));
        if (aquaticPrefabs == null) aquaticPrefabs = scanFolder(prefabFolder.resolve("aquatic"));
        if (undergroundPrefabs == null) undergroundPrefabs = scanFolder(prefabFolder.resolve("underground"));
        if (surfaceDungeonPrefabs == null) surfaceDungeonPrefabs = scanFolder(prefabFolder.resolve("surface_dungeon"));
        if (undergroundDungeonPrefabs == null) undergroundDungeonPrefabs = scanFolder(prefabFolder.resolve("underground_dungeon"));

        // load wayward shrine prefab once
        if (!waywardShrineLoaded) {
            waywardShrineLoaded = true;
            waywardShrinePrefab = PrefabStore.get().getAssetPrefabFromAnyPack("Wayward_Shrine.prefab.json");
            if (waywardShrinePrefab == null) LOGGER.warning("[HyARPG] Could not load Wayward_Shrine prefab from any asset pack");
        }

        WorldChunk chunk = event.getChunk();
        World world = chunk.getWorld();
        long worldSeed = world.getWorldConfig().getSeed();
        ChunkGenerator generator = (ChunkGenerator) world.getChunkStore().getGenerator();
        if (generator == null) return;

        Config_World cfg = ModConfig.get().world;
        int chunkMinX = chunk.getX() * CHUNK_SIZE, chunkMaxX = chunkMinX + CHUNK_SIZE - 1;
        int chunkMinZ = chunk.getZ() * CHUNK_SIZE, chunkMaxZ = chunkMinZ + CHUNK_SIZE - 1;

        // dispatch each prefab type against its own region grid
        if (!surfacePrefabs.isEmpty()) {
            int rMinX = Math.floorDiv(chunkMinX - cfg.prefabSurfaceMaxSize, cfg.prefabSurfaceRegionSize), rMaxX = Math.floorDiv(chunkMaxX + cfg.prefabSurfaceMaxSize, cfg.prefabSurfaceRegionSize);
            int rMinZ = Math.floorDiv(chunkMinZ - cfg.prefabSurfaceMaxSize, cfg.prefabSurfaceRegionSize), rMaxZ = Math.floorDiv(chunkMaxZ + cfg.prefabSurfaceMaxSize, cfg.prefabSurfaceRegionSize);
            for (int rx = rMinX; rx <= rMaxX; rx++) for (int rz = rMinZ; rz <= rMaxZ; rz++) processSurface(rx, rz, cfg, worldSeed, chunk, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, generator);
        }
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
        if (waywardShrinePrefab != null) {
            int rMinX = Math.floorDiv(chunkMinX - cfg.prefabWaywardShrineRegionSize / 2, cfg.prefabWaywardShrineRegionSize), rMaxX = Math.floorDiv(chunkMaxX + cfg.prefabWaywardShrineRegionSize / 2, cfg.prefabWaywardShrineRegionSize);
            int rMinZ = Math.floorDiv(chunkMinZ - cfg.prefabWaywardShrineRegionSize / 2, cfg.prefabWaywardShrineRegionSize), rMaxZ = Math.floorDiv(chunkMaxZ + cfg.prefabWaywardShrineRegionSize / 2, cfg.prefabWaywardShrineRegionSize);
            for (int rx = rMinX; rx <= rMaxX; rx++) for (int rz = rMinZ; rz <= rMaxZ; rz++) processWaywardShrine(rx, rz, cfg, worldSeed, chunk, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, generator);
        }
    }

    // surface prefab — min-corner grounded, optional corner shrine
    private void processSurface(int regionX, int regionZ, Config_World cfg, long worldSeed, WorldChunk chunk, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ, ChunkGenerator generator) {
        long regionSeed = (long) regionX * 341873128712L + (long) regionZ * 132897987541L ^ worldSeed ^ 0x1L;
        Random random = new Random(regionSeed);
        if (random.nextDouble() >= Math.max(0.0, Math.min(1.0, cfg.prefabSurfaceSpawnChance))) return;

        int margin = 4;
        int anchorX = regionX * cfg.prefabSurfaceRegionSize + margin + random.nextInt(Math.max(1, cfg.prefabSurfaceRegionSize - margin * 2));
        int anchorZ = regionZ * cfg.prefabSurfaceRegionSize + margin + random.nextInt(Math.max(1, cfg.prefabSurfaceRegionSize - margin * 2));
        BlockSelection buffer = loadPrefab(surfacePrefabs.get(random.nextInt(surfacePrefabs.size())));
        if (buffer == null) return;

        int anchorY = resolveMinCornerAnchorY(buffer, generator, worldSeed, anchorX, anchorZ);
        if (anchorY <= 0 || anchorY >= 318) return;

        pasteSlice(true, true, buffer, chunk, anchorX, anchorY, anchorZ, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);

        // optionally place a wayward shrine in a corner of this surface prefab
        if (waywardShrinePrefab != null && random.nextDouble() < Math.max(0.0, Math.min(1.0, cfg.prefabSurfaceCornerShrineChance))) {
            placeShrineInCorner(buffer, anchorX, anchorY, anchorZ, regionSeed, generator, worldSeed, chunk, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
        }
    }

    // wayward shrine standalone — own region grid, same min-corner grounding as surface
    private void processWaywardShrine(int regionX, int regionZ, Config_World cfg, long worldSeed, WorldChunk chunk, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ, ChunkGenerator generator) {
        long regionSeed = (long) regionX * 341873128712L + (long) regionZ * 132897987541L ^ worldSeed ^ 0x6L;
        Random random = new Random(regionSeed);
        if (random.nextDouble() >= Math.max(0.0, Math.min(1.0, cfg.prefabWaywardShrineSpawnChance))) return;

        int margin = 4;
        int anchorX = regionX * cfg.prefabWaywardShrineRegionSize + margin + random.nextInt(Math.max(1, cfg.prefabWaywardShrineRegionSize - margin * 2));
        int anchorZ = regionZ * cfg.prefabWaywardShrineRegionSize + margin + random.nextInt(Math.max(1, cfg.prefabWaywardShrineRegionSize - margin * 2));

        int anchorY = resolveMinCornerAnchorY(waywardShrinePrefab, generator, worldSeed, anchorX, anchorZ);
        if (anchorY <= 0 || anchorY >= 318) return;

        pasteSlice(true, false, waywardShrinePrefab, chunk, anchorX, anchorY, anchorZ, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
    }

    // place wayward shrine at a corner just outside the given surface prefab's footprint
    private void placeShrineInCorner(BlockSelection parentBuffer, int parentAnchorX, int parentAnchorY, int parentAnchorZ, long regionSeed, ChunkGenerator generator, long worldSeed, WorldChunk chunk, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
        int[] fp = getPrefabFootprint(parentBuffer);
        int parentMinX = parentAnchorX + (fp[0] - parentBuffer.getAnchorX());
        int parentMaxX = parentAnchorX + (fp[1] - parentBuffer.getAnchorX());
        int parentMinZ = parentAnchorZ + (fp[2] - parentBuffer.getAnchorZ());
        int parentMaxZ = parentAnchorZ + (fp[3] - parentBuffer.getAnchorZ());

        // shrine footprint size to offset placement outside parent bounds
        int[] sfp = getPrefabFootprint(waywardShrinePrefab);
        int shrineWidth = sfp[1] - sfp[0];
        int shrineDepth = sfp[3] - sfp[2];

        // pick a random corner and place shrine just outside that corner
        Random cornerRandom = new Random(regionSeed ^ 0xC04E4EL);
        int cornerIndex = cornerRandom.nextInt(4);
        int shrineAnchorX, shrineAnchorZ;
        switch (cornerIndex) {
            case 0 -> { shrineAnchorX = parentMinX - shrineWidth; shrineAnchorZ = parentMinZ - shrineDepth; }
            case 1 -> { shrineAnchorX = parentMaxX; shrineAnchorZ = parentMinZ - shrineDepth; }
            case 2 -> { shrineAnchorX = parentMinX - shrineWidth; shrineAnchorZ = parentMaxZ; }
            default -> { shrineAnchorX = parentMaxX; shrineAnchorZ = parentMaxZ; }
        }

        // ground the shrine to min corner terrain height at its position
        int shrineAnchorY = resolveMinCornerAnchorY(waywardShrinePrefab, generator, worldSeed, shrineAnchorX, shrineAnchorZ);
        if (shrineAnchorY <= 0 || shrineAnchorY >= 318) return;

        pasteSlice(false, false, waywardShrinePrefab, chunk, shrineAnchorX, shrineAnchorY, shrineAnchorZ, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
    }

    // resolve anchor Y by grounding to the minimum of the 4 prefab footprint corners
    private int resolveMinCornerAnchorY(BlockSelection buffer, ChunkGenerator generator, long worldSeed, int anchorX, int anchorZ) {
        int[] fp = getPrefabFootprint(buffer);
        int prefabMinX = anchorX + (fp[0] - buffer.getAnchorX());
        int prefabMaxX = anchorX + (fp[1] - buffer.getAnchorX());
        int prefabMinZ = anchorZ + (fp[2] - buffer.getAnchorZ());
        int prefabMaxZ = anchorZ + (fp[3] - buffer.getAnchorZ());
        int groundY = Math.min(Math.min(generator.getHeight((int)worldSeed, prefabMinX, prefabMinZ), generator.getHeight((int)worldSeed, prefabMaxX, prefabMinZ)), Math.min(generator.getHeight((int)worldSeed, prefabMinX, prefabMaxZ), generator.getHeight((int)worldSeed, prefabMaxX, prefabMaxZ)));
        int prefabBottomOffset = fp[4] - buffer.getAnchorY();
        return groundY - prefabBottomOffset;
    }

    // aquatic — TODO: water detection
    private void processAquatic(int regionX, int regionZ, Config_World cfg, long worldSeed, WorldChunk chunk, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ, ChunkGenerator generator) {}

    // underground prefab — randomized depth with push-down logic
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

        pasteSlice(true, true, buffer, chunk, anchorX, anchorY, anchorZ, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
    }

    // surface dungeon — min-corner grounded with spawners
    private void processSurfaceDungeon(int regionX, int regionZ, Config_World cfg, long worldSeed, WorldChunk chunk, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ, ChunkGenerator generator) {
        long regionSeed = (long) regionX * 341873128712L + (long) regionZ * 132897987541L ^ worldSeed ^ 0x4L;
        Random random = new Random(regionSeed);
        if (random.nextDouble() >= Math.max(0.0, Math.min(1.0, cfg.prefabSurfaceDungeonSpawnChance))) return;

        int margin = 4;
        int anchorX = regionX * cfg.prefabSurfaceDungeonRegionSize + margin + random.nextInt(Math.max(1, cfg.prefabSurfaceDungeonRegionSize - margin * 2));
        int anchorZ = regionZ * cfg.prefabSurfaceDungeonRegionSize + margin + random.nextInt(Math.max(1, cfg.prefabSurfaceDungeonRegionSize - margin * 2));
        BlockSelection buffer = loadPrefab(surfaceDungeonPrefabs.get(random.nextInt(surfaceDungeonPrefabs.size())));
        if (buffer == null) return;

        int anchorY = resolveMinCornerAnchorY(buffer, generator, worldSeed, anchorX, anchorZ);
        if (anchorY <= 0 || anchorY >= 318) return;

        int[] bounds = computeBounds(buffer);
        pasteSlice(true, true, buffer, chunk, anchorX, anchorY, anchorZ, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
        placeSpawners(buffer, bounds, chunk, anchorX, anchorY, anchorZ, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, random, cfg.prefabSurfaceDungeonSpawnerDensity);
    }

    // underground dungeon — depth-resolved with spawners
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
        pasteSlice(true, true, buffer, chunk, anchorX, anchorY, anchorZ, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
        placeSpawners(buffer, bounds, chunk, anchorX, anchorY, anchorZ, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, random, cfg.prefabUndergroundDungeonSpawnerDensity);
    }

    // underground Y — randomized between floor and subsurface with push-down correction
    private int resolveUndergroundAnchorY(BlockSelection buffer, ChunkGenerator generator, long worldSeed, int anchorX, int anchorZ, Random random) {
        int[] b = {Integer.MAX_VALUE, Integer.MIN_VALUE};
        buffer.forEachBlock((x, y, z, block) -> { b[0] = Math.min(b[0], y); b[1] = Math.max(b[1], y); });
        if (b[0] == Integer.MAX_VALUE) return Integer.MIN_VALUE;
        int bufAnchorY = buffer.getAnchorY();
        int prefabTopOffset = b[1] - bufAnchorY, prefabBottomOffset = b[0] - bufAnchorY;
        int surfaceY = generator.getHeight((int) worldSeed, anchorX, anchorZ);
        int anchorY = UNDERGROUND_FLOOR + random.nextInt(Math.max(1, (surfaceY - UNDERGROUND_PADDING) - UNDERGROUND_FLOOR));
        if (anchorY + prefabTopOffset > surfaceY - UNDERGROUND_PADDING) anchorY = surfaceY - UNDERGROUND_PADDING - prefabTopOffset;
        if (anchorY + prefabBottomOffset < UNDERGROUND_FLOOR) anchorY = UNDERGROUND_FLOOR - prefabBottomOffset;
        if (anchorY + prefabTopOffset > surfaceY - UNDERGROUND_PADDING) return Integer.MIN_VALUE;
        if (anchorY + prefabBottomOffset < UNDERGROUND_FLOOR) return Integer.MIN_VALUE;
        return anchorY;
    }

    // scatter spawners deterministically across the full prefab footprint, place only those in this chunk
    private void placeSpawners(BlockSelection buffer, int[] bounds, WorldChunk chunk, int anchorX, int anchorY, int anchorZ, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ, Random random, double density) {
        int bufAnchorX = buffer.getAnchorX(), bufAnchorY = buffer.getAnchorY(), bufAnchorZ = buffer.getAnchorZ();
        int volume = Math.max(1, (bounds[3] - bounds[0]) * (bounds[4] - bounds[1]) * (bounds[5] - bounds[2]));
        int spawnerCount = (int) ((volume / 10000.0) * density);
        if (spawnerCount <= 0) return;

        List<int[]> blockPositions = new ArrayList<>();
        buffer.forEachBlock((x, y, z, block) -> {
            if (block.blockId() == 0) return;
            blockPositions.add(new int[]{anchorX + (x - bufAnchorX), anchorY + (y - bufAnchorY), anchorZ + (z - bufAnchorZ)});
        });
        if (blockPositions.isEmpty()) return;

        int[] spawnerIds = new int[SPAWNER_BLOCKS.length];
        BlockType[] spawnerTypes = new BlockType[SPAWNER_BLOCKS.length];
        for (int i = 0; i < SPAWNER_BLOCKS.length; i++) { spawnerIds[i] = BlockType.getAssetMap().getIndex(SPAWNER_BLOCKS[i]); spawnerTypes[i] = BlockType.getAssetMap().getAsset(spawnerIds[i]); }

        int[] dx = {1, -1, 0, 0, 0, 0}, dy = {0, 0, 1, -1, 0, 0}, dz = {0, 0, 0, 0, 1, -1};

        for (int i = 0; i < spawnerCount; i++) {
            int[] target = blockPositions.get(random.nextInt(blockPositions.size()));
            int[] faceOrder = {0, 1, 2, 3, 4, 5};
            for (int a = 5; a > 0; a--) { int swap = random.nextInt(a + 1); int tmp = faceOrder[a]; faceOrder[a] = faceOrder[swap]; faceOrder[swap] = tmp; }
            int spawnerIndex = random.nextInt(SPAWNER_BLOCKS.length);
            for (int face : faceOrder) {
                int sx = target[0] + dx[face], sy = target[1] + dy[face], sz = target[2] + dz[face];
                if (sy < UNDERGROUND_FLOOR || sy > 318) continue;
                if (sx < chunkMinX || sx > chunkMaxX || sz < chunkMinZ || sz > chunkMaxZ) continue;
                if (chunk.getBlock(sx, sy, sz) != 0) continue;
                if (spawnerTypes[spawnerIndex] == null) continue;
                chunk.setBlock(sx, sy, sz, spawnerIds[spawnerIndex], spawnerTypes[spawnerIndex], 0, 0, PLACEMENT_SETTINGS);
                break;
            }
        }
    }

    // bounding box of all non-air blocks in buffer-local coords
    private int[] computeBounds(BlockSelection buffer) {
        int[] b = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        buffer.forEachBlock((x, y, z, block) -> {
            b[0] = Math.min(b[0], x); b[1] = Math.min(b[1], y); b[2] = Math.min(b[2], z);
            b[3] = Math.max(b[3], x); b[4] = Math.max(b[4], y); b[5] = Math.max(b[5], z);
        });
        return b;
    }

    // paste prefab blocks for this chunk's XZ column, respecting Empty blocks and optional air fill
    private void pasteSlice(boolean fillAir, boolean skipTeleporter, BlockSelection buffer, WorldChunk chunk, int anchorX, int anchorY, int anchorZ, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
        int bufAnchorX = buffer.getAnchorX(), bufAnchorY = buffer.getAnchorY(), bufAnchorZ = buffer.getAnchorZ();
        BlockType airType = BlockType.getAssetMap().getAsset(0);
        int[] b = computeBounds(buffer);

        // air-fill the bounding box to prevent terrain bleed inside the prefab
        if (fillAir && airType != null && b[0] != Integer.MAX_VALUE) {
            for (int x = b[0]; x <= b[3]; x++) for (int y = b[1]; y <= b[4]; y++) for (int z = b[2]; z <= b[5]; z++) {
                int wx = anchorX + (x - bufAnchorX), wy = anchorY + (y - bufAnchorY), wz = anchorZ + (z - bufAnchorZ);
                if (wy < UNDERGROUND_FLOOR || wy > 318) continue;
                if (wx < chunkMinX || wx > chunkMaxX || wz < chunkMinZ || wz > chunkMaxZ) continue;
                chunk.setBlock(wx, wy, wz, 0, airType, 0, 0, PLACEMENT_SETTINGS);
            }
        }

        // paste prefab blocks — Empty blocks always become air, containers get tracked for loot
        buffer.forEachBlock((x, y, z, block) -> {
            if (block.blockId() == 0) return;
            int wx = anchorX + (x - bufAnchorX), wy = anchorY + (y - bufAnchorY), wz = anchorZ + (z - bufAnchorZ);
            if (wy < UNDERGROUND_FLOOR || wy > 318) return;
            if (wx < chunkMinX || wx > chunkMaxX || wz < chunkMinZ || wz > chunkMaxZ) return;
            BlockType blockType = BlockType.getAssetMap().getAsset(block.blockId());
            if (blockType == null) return;

            // Empty and Bench blocks are replaced with air
            if (blockType.getId().equals("Empty") || blockType.getId().startsWith("Bench_") || (skipTeleporter && blockType.getId().equals("Teleporter"))) { if (airType != null) chunk.setBlock(wx, wy, wz, 0, airType, 0, 0, PLACEMENT_SETTINGS); return; }

            // track containers for loot table assignment
            if (blockType.getBlockEntity() != null) { ItemContainerBlock container = blockType.getBlockEntity().getComponent(ItemContainerBlock.getComponentType()); if (container != null) PREFAB_CONTAINER_POSITIONS.add(posKey(wx, wy, wz)); }

            chunk.setBlock(wx, wy, wz, block.blockId(), blockType, block.rotation(), block.filler(), PLACEMENT_SETTINGS);
        });
    }

    // load prefab from disk, results cached by PrefabStore
    private BlockSelection loadPrefab(Path path) {
        try { return PrefabStore.get().getPrefab(path); } catch (Exception e) { LOGGER.warning("[HyARPG] Could not load prefab '" + path.getFileName() + "': " + e); return null; }
    }

    // scan folder for *.prefab.json files
    private List<Path> scanFolder(Path folder) {
        List<Path> result = new ArrayList<>();
        if (!Files.exists(folder)) return result;
        try (var stream = Files.walk(folder)) { stream.filter(p -> p.toString().endsWith(".prefab.json")).forEach(result::add); } catch (IOException e) { LOGGER.log(Level.WARNING, "[HyARPG] Failed to scan prefab folder: " + folder, e); }
        LOGGER.info("[HyARPG] Found " + result.size() + " prefab(s) in " + folder);
        return result;
    }

    // pack world XYZ into a single long key for container position tracking
    public static long posKey(int x, int y, int z) { return ((long)(x & 0xFFFFF) << 40) | ((long)(y & 0xFFFFF) << 20) | (z & 0xFFFFF); }

    // footprint bounds [minX, maxX, minZ, maxZ, minY] in buffer-local coords
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