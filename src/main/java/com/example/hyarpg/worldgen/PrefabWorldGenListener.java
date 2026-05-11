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

    // constants
    private static final Logger LOGGER = Logger.getLogger(PrefabWorldGenListener.class.getName());
    private static final int CHUNK_SIZE = 32;
    private static final int UNDERGROUND_FLOOR = -30;
    private static final int UNDERGROUND_PADDING = 20;
    private static final int PLACEMENT_SETTINGS = 0x02 | 0x04 | 0x08 | 0x10;
    private static final String[] SPAWNER_BLOCKS = {
            "HyARPG_BlockSpawner_Goblin",
            "HyARPG_BlockSpawner_Outlanders",
            "HyARPG_BlockSpawner_Throk",
            "HyARPG_BlockSpawner_Undead",
            "HyARPG_BlockSpawner_Void"
    };

    // prefab lists and shrine — loaded once per session on first chunk gen
    private final Path prefabFolder;
    private List<Path> surfacePrefabs = null;
    private List<Path> aquaticPrefabs = null;
    private List<Path> undergroundPrefabs = null;
    private List<Path> surfaceDungeonPrefabs = null;
    private List<Path> undergroundDungeonPrefabs = null;
    private BlockSelection waywardShrinePrefab = null;
    private boolean waywardShrineLoaded = false;

    // per-prefab bounds and footprint cached so we never re-scan the same prefab twice
    private static final java.util.concurrent.ConcurrentHashMap<BlockSelection, int[]> BOUNDS_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.ConcurrentHashMap<BlockSelection, int[]> FOOTPRINT_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    public PrefabWorldGenListener(Path prefabFolder) { this.prefabFolder = prefabFolder; }

    public void register(EventRegistry eventRegistry) {
        // make sure all prefab subfolders exist so the game has somewhere to look
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

        // scan prefab folders the first time a chunk is generated this session
        if (surfacePrefabs == null) surfacePrefabs = scanFolder(prefabFolder.resolve("surface"));
//        if (aquaticPrefabs == null) aquaticPrefabs = scanFolder(prefabFolder.resolve("aquatic"));
        if (undergroundPrefabs == null) undergroundPrefabs = scanFolder(prefabFolder.resolve("underground"));
//        if (surfaceDungeonPrefabs == null) surfaceDungeonPrefabs = scanFolder(prefabFolder.resolve("surface_dungeon"));
//        if (undergroundDungeonPrefabs == null) undergroundDungeonPrefabs = scanFolder(prefabFolder.resolve("underground_dungeon"));

        // load the wayward shrine prefab from asset packs the first time we need it
        if (!waywardShrineLoaded) {
            waywardShrineLoaded = true;
            waywardShrinePrefab = PrefabStore.get().getAssetPrefabFromAnyPack("Wayward_Shrine.prefab.json");
            if (waywardShrinePrefab == null) LOGGER.warning("[HyARPG] Could not load Wayward_Shrine prefab from any asset pack");
        }

        // gather chunk and world context needed by all placement methods
        WorldChunk chunk = event.getChunk();
        World world = chunk.getWorld();
        long worldSeed = world.getWorldConfig().getSeed();
        ChunkGenerator generator = (ChunkGenerator) world.getChunkStore().getGenerator();
        if (generator == null) return;

        // work out which regions overlap this chunk for each prefab type, then process each one
        Config_World cfg = ModConfig.get().world;
        int chunkMinX = chunk.getX() * CHUNK_SIZE, chunkMaxX = chunkMinX + CHUNK_SIZE - 1;
        int chunkMinZ = chunk.getZ() * CHUNK_SIZE, chunkMaxZ = chunkMinZ + CHUNK_SIZE - 1;

        if (!surfacePrefabs.isEmpty()) {
            int rMinX = Math.floorDiv(chunkMinX - cfg.prefabSurfaceMaxSize, cfg.prefabSurfaceRegionSize), rMaxX = Math.floorDiv(chunkMaxX + cfg.prefabSurfaceMaxSize, cfg.prefabSurfaceRegionSize);
            int rMinZ = Math.floorDiv(chunkMinZ - cfg.prefabSurfaceMaxSize, cfg.prefabSurfaceRegionSize), rMaxZ = Math.floorDiv(chunkMaxZ + cfg.prefabSurfaceMaxSize, cfg.prefabSurfaceRegionSize);
            for (int rx = rMinX; rx <= rMaxX; rx++) for (int rz = rMinZ; rz <= rMaxZ; rz++) processSurface(rx, rz, cfg, worldSeed, chunk, world, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, generator);
        }
        if (!undergroundPrefabs.isEmpty()) {
            int rMinX = Math.floorDiv(chunkMinX - cfg.prefabUndergroundMaxSize, cfg.prefabUndergroundRegionSize), rMaxX = Math.floorDiv(chunkMaxX + cfg.prefabUndergroundMaxSize, cfg.prefabUndergroundRegionSize);
            int rMinZ = Math.floorDiv(chunkMinZ - cfg.prefabUndergroundMaxSize, cfg.prefabUndergroundRegionSize), rMaxZ = Math.floorDiv(chunkMaxZ + cfg.prefabUndergroundMaxSize, cfg.prefabUndergroundRegionSize);
            for (int rx = rMinX; rx <= rMaxX; rx++) for (int rz = rMinZ; rz <= rMaxZ; rz++) processUnderground(rx, rz, cfg, worldSeed, chunk, world, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, generator);
        }
//        if (!surfaceDungeonPrefabs.isEmpty()) {
//            int rMinX = Math.floorDiv(chunkMinX - cfg.prefabSurfaceDungeonMaxSize, cfg.prefabSurfaceDungeonRegionSize), rMaxX = Math.floorDiv(chunkMaxX + cfg.prefabSurfaceDungeonMaxSize, cfg.prefabSurfaceDungeonRegionSize);
//            int rMinZ = Math.floorDiv(chunkMinZ - cfg.prefabSurfaceDungeonMaxSize, cfg.prefabSurfaceDungeonRegionSize), rMaxZ = Math.floorDiv(chunkMaxZ + cfg.prefabSurfaceDungeonMaxSize, cfg.prefabSurfaceDungeonRegionSize);
//            for (int rx = rMinX; rx <= rMaxX; rx++) for (int rz = rMinZ; rz <= rMaxZ; rz++) processSurfaceDungeon(rx, rz, cfg, worldSeed, chunk, world, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, generator);
//        }
//        if (!undergroundDungeonPrefabs.isEmpty()) {
//            int rMinX = Math.floorDiv(chunkMinX - cfg.prefabUndergroundDungeonMaxSize, cfg.prefabUndergroundDungeonRegionSize), rMaxX = Math.floorDiv(chunkMaxX + cfg.prefabUndergroundDungeonMaxSize, cfg.prefabUndergroundDungeonRegionSize);
//            int rMinZ = Math.floorDiv(chunkMinZ - cfg.prefabUndergroundDungeonMaxSize, cfg.prefabUndergroundDungeonRegionSize), rMaxZ = Math.floorDiv(chunkMaxZ + cfg.prefabUndergroundDungeonMaxSize, cfg.prefabUndergroundDungeonRegionSize);
//            for (int rx = rMinX; rx <= rMaxX; rx++) for (int rz = rMinZ; rz <= rMaxZ; rz++) processUndergroundDungeon(rx, rz, cfg, worldSeed, chunk, world, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, generator);
//        }
        if (waywardShrinePrefab != null) {
            int rMinX = Math.floorDiv(chunkMinX - cfg.prefabWaywardShrineRegionSize / 2, cfg.prefabWaywardShrineRegionSize), rMaxX = Math.floorDiv(chunkMaxX + cfg.prefabWaywardShrineRegionSize / 2, cfg.prefabWaywardShrineRegionSize);
            int rMinZ = Math.floorDiv(chunkMinZ - cfg.prefabWaywardShrineRegionSize / 2, cfg.prefabWaywardShrineRegionSize), rMaxZ = Math.floorDiv(chunkMaxZ + cfg.prefabWaywardShrineRegionSize / 2, cfg.prefabWaywardShrineRegionSize);
            for (int rx = rMinX; rx <= rMaxX; rx++) for (int rz = rMinZ; rz <= rMaxZ; rz++) processWaywardShrine(rx, rz, cfg, worldSeed, chunk, world, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, generator);
        }
    }

    // pick a surface prefab for this region, ground it, paste it, and record its placement
    private void processSurface(int regionX, int regionZ, Config_World cfg, long worldSeed, WorldChunk chunk, World world, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ, ChunkGenerator generator) {
        long regionSeed = (long) regionX * 341873128712L + (long) regionZ * 132897987541L ^ worldSeed ^ 0x1L;
        Random random = new Random(regionSeed);
        if (random.nextDouble() >= Math.max(0.0, Math.min(1.0, cfg.prefabSurfaceSpawnChance))) return;

        // resolve anchor position and pick which prefab to place
        int margin = 4;
        int anchorX = regionX * cfg.prefabSurfaceRegionSize + margin + random.nextInt(Math.max(1, cfg.prefabSurfaceRegionSize - margin * 2));
        int anchorZ = regionZ * cfg.prefabSurfaceRegionSize + margin + random.nextInt(Math.max(1, cfg.prefabSurfaceRegionSize - margin * 2));
        Path prefabPath = surfacePrefabs.get(random.nextInt(surfacePrefabs.size()));
        BlockSelection buffer = loadPrefab(prefabPath);
        if (buffer == null) return;

        // ground the prefab to terrain and bail if placement is out of bounds
        int anchorY = resolveMinCornerAnchorY(buffer, generator, worldSeed, anchorX, anchorZ);
        if (anchorY <= 0 || anchorY >= 318) return;

        // place this chunk's slice of the prefab and register it
        pasteSlice(true, true, buffer, chunk, anchorX, anchorY, anchorZ, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
        recordPlacement(PrefabRecord.Type.SURFACE, prefabPath.getFileName().toString(), buffer, world, anchorX, anchorY, anchorZ);
    }

    // pick a wayward shrine for this region, ground it, paste it, and record its placement
    private void processWaywardShrine(int regionX, int regionZ, Config_World cfg, long worldSeed, WorldChunk chunk, World world, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ, ChunkGenerator generator) {
        long regionSeed = (long) regionX * 341873128712L + (long) regionZ * 132897987541L ^ worldSeed ^ 0x6L;
        Random random = new Random(regionSeed);
        if (random.nextDouble() >= Math.max(0.0, Math.min(1.0, cfg.prefabWaywardShrineSpawnChance))) return;

        // resolve anchor position within the region
        int margin = 4;
        int anchorX = regionX * cfg.prefabWaywardShrineRegionSize + margin + random.nextInt(Math.max(1, cfg.prefabWaywardShrineRegionSize - margin * 2));
        int anchorZ = regionZ * cfg.prefabWaywardShrineRegionSize + margin + random.nextInt(Math.max(1, cfg.prefabWaywardShrineRegionSize - margin * 2));

        // ground the shrine to terrain and bail if placement is out of bounds
        int anchorY = resolveMinCornerAnchorY(waywardShrinePrefab, generator, worldSeed, anchorX, anchorZ);
        if (anchorY <= 0 || anchorY >= 318) return;

        // place this chunk's slice of the shrine and register it
        pasteSlice(true, false, waywardShrinePrefab, chunk, anchorX, anchorY, anchorZ, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
        recordPlacement(PrefabRecord.Type.SHRINE, "Wayward_Shrine.prefab.json", waywardShrinePrefab, world, anchorX, anchorY, anchorZ);
    }

    // pick an underground prefab for this region, find a valid depth, paste it, and record its placement
    private void processUnderground(int regionX, int regionZ, Config_World cfg, long worldSeed, WorldChunk chunk, World world, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ, ChunkGenerator generator) {
        long regionSeed = (long) regionX * 341873128712L + (long) regionZ * 132897987541L ^ worldSeed ^ 0x3L;
        Random random = new Random(regionSeed);
        if (random.nextDouble() >= Math.max(0.0, Math.min(1.0, cfg.prefabUndergroundSpawnChance))) return;

        // resolve anchor position and pick which prefab to place
        int margin = 4;
        int anchorX = regionX * cfg.prefabUndergroundRegionSize + margin + random.nextInt(Math.max(1, cfg.prefabUndergroundRegionSize - margin * 2));
        int anchorZ = regionZ * cfg.prefabUndergroundRegionSize + margin + random.nextInt(Math.max(1, cfg.prefabUndergroundRegionSize - margin * 2));
        Path prefabPath = undergroundPrefabs.get(random.nextInt(undergroundPrefabs.size()));
        BlockSelection buffer = loadPrefab(prefabPath);
        if (buffer == null) return;

        // find a valid underground depth, bail if it won't fit
        int anchorY = resolveUndergroundAnchorY(buffer, generator, worldSeed, anchorX, anchorZ, random);
        if (anchorY == Integer.MIN_VALUE) return;

        // place this chunk's slice of the prefab and register it
        pasteSlice(true, true, buffer, chunk, anchorX, anchorY, anchorZ, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
        recordPlacement(PrefabRecord.Type.UNDERGROUND, prefabPath.getFileName().toString(), buffer, world, anchorX, anchorY, anchorZ);
    }

    // pick a surface dungeon for this region, ground it, scatter spawners, paste it, and record its placement
    private void processSurfaceDungeon(int regionX, int regionZ, Config_World cfg, long worldSeed, WorldChunk chunk, World world, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ, ChunkGenerator generator) {
        long regionSeed = (long) regionX * 341873128712L + (long) regionZ * 132897987541L ^ worldSeed ^ 0x4L;
        Random random = new Random(regionSeed);
        if (random.nextDouble() >= Math.max(0.0, Math.min(1.0, cfg.prefabSurfaceDungeonSpawnChance))) return;

        // resolve anchor position and pick which prefab to place
        int margin = 4;
        int anchorX = regionX * cfg.prefabSurfaceDungeonRegionSize + margin + random.nextInt(Math.max(1, cfg.prefabSurfaceDungeonRegionSize - margin * 2));
        int anchorZ = regionZ * cfg.prefabSurfaceDungeonRegionSize + margin + random.nextInt(Math.max(1, cfg.prefabSurfaceDungeonRegionSize - margin * 2));
        Path prefabPath = surfaceDungeonPrefabs.get(random.nextInt(surfaceDungeonPrefabs.size()));
        BlockSelection buffer = loadPrefab(prefabPath);
        if (buffer == null) return;

        // ground the prefab to terrain and bail if placement is out of bounds
        int anchorY = resolveMinCornerAnchorY(buffer, generator, worldSeed, anchorX, anchorZ);
        if (anchorY <= 0 || anchorY >= 318) return;

        // place this chunk's slice of the prefab, scatter enemy spawners, and register it
        int[] bounds = computeBounds(buffer);
        pasteSlice(true, true, buffer, chunk, anchorX, anchorY, anchorZ, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
        placeSpawners(buffer, bounds, chunk, anchorX, anchorY, anchorZ, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, random, cfg.prefabSurfaceDungeonSpawnerDensity);
        recordPlacement(PrefabRecord.Type.SURFACE_DUNGEON, prefabPath.getFileName().toString(), buffer, world, anchorX, anchorY, anchorZ);
    }

    // pick an underground dungeon for this region, find a valid depth, scatter spawners, paste it, and record its placement
    private void processUndergroundDungeon(int regionX, int regionZ, Config_World cfg, long worldSeed, WorldChunk chunk, World world, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ, ChunkGenerator generator) {
        long regionSeed = (long) regionX * 341873128712L + (long) regionZ * 132897987541L ^ worldSeed ^ 0x5L;
        Random random = new Random(regionSeed);
        if (random.nextDouble() >= Math.max(0.0, Math.min(1.0, cfg.prefabUndergroundDungeonSpawnChance))) return;

        // resolve anchor position and pick which prefab to place
        int margin = 4;
        int anchorX = regionX * cfg.prefabUndergroundDungeonRegionSize + margin + random.nextInt(Math.max(1, cfg.prefabUndergroundDungeonRegionSize - margin * 2));
        int anchorZ = regionZ * cfg.prefabUndergroundDungeonRegionSize + margin + random.nextInt(Math.max(1, cfg.prefabUndergroundDungeonRegionSize - margin * 2));
        Path prefabPath = undergroundDungeonPrefabs.get(random.nextInt(undergroundDungeonPrefabs.size()));
        BlockSelection buffer = loadPrefab(prefabPath);
        if (buffer == null) return;

        // find a valid underground depth, bail if it won't fit
        int anchorY = resolveUndergroundAnchorY(buffer, generator, worldSeed, anchorX, anchorZ, random);
        if (anchorY == Integer.MIN_VALUE) return;

        // place this chunk's slice of the prefab, scatter enemy spawners, and register it
        int[] bounds = computeBounds(buffer);
        pasteSlice(true, true, buffer, chunk, anchorX, anchorY, anchorZ, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
        placeSpawners(buffer, bounds, chunk, anchorX, anchorY, anchorZ, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, random, cfg.prefabUndergroundDungeonSpawnerDensity);
        recordPlacement(PrefabRecord.Type.UNDERGROUND_DUNGEON, prefabPath.getFileName().toString(), buffer, world, anchorX, anchorY, anchorZ);
    }

    // ground a surface prefab to the lowest terrain corner under its footprint
    private int resolveMinCornerAnchorY(BlockSelection buffer, ChunkGenerator generator, long worldSeed, int anchorX, int anchorZ) {
        int[] fp = getPrefabFootprint(buffer);
        int prefabMinX = anchorX + (fp[0] - buffer.getAnchorX());
        int prefabMaxX = anchorX + (fp[1] - buffer.getAnchorX());
        int prefabMinZ = anchorZ + (fp[2] - buffer.getAnchorZ());
        int prefabMaxZ = anchorZ + (fp[3] - buffer.getAnchorZ());
        int groundY = Math.min(
                Math.min(generator.getHeight((int)worldSeed, prefabMinX, prefabMinZ), generator.getHeight((int)worldSeed, prefabMaxX, prefabMinZ)),
                Math.min(generator.getHeight((int)worldSeed, prefabMinX, prefabMaxZ), generator.getHeight((int)worldSeed, prefabMaxX, prefabMaxZ))
        );
        int prefabBottomOffset = fp[4] - buffer.getAnchorY();
        return groundY - prefabBottomOffset;
    }

    // find a random valid Y for an underground prefab, pushing it down if it clips the surface
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

    // scatter enemy spawners across the prefab interior at the configured density
    private void placeSpawners(BlockSelection buffer, int[] bounds, WorldChunk chunk, int anchorX, int anchorY, int anchorZ, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ, Random random, double density) {
        int bufAnchorX = buffer.getAnchorX(), bufAnchorY = buffer.getAnchorY(), bufAnchorZ = buffer.getAnchorZ();
        int volume = Math.max(1, (bounds[3] - bounds[0]) * (bounds[4] - bounds[1]) * (bounds[5] - bounds[2]));
        int spawnerCount = (int) ((volume / 1000.0) * density);
        if (spawnerCount <= 0) return;

        // collect every solid block position in the prefab as candidate spawn surfaces
        final List<int[]> allPositions = new ArrayList<>();
        buffer.forEachBlock((x, y, z, block) -> {
            if (block.blockId() == 0) return;
            allPositions.add(new int[]{anchorX + (x - bufAnchorX), anchorY + (y - bufAnchorY), anchorZ + (z - bufAnchorZ)});
        });
        if (allPositions.isEmpty()) return;

        // narrow candidates to the inner 75% radius and lower 65% of height to keep spawners interior
        int centerX = anchorX + (bounds[0] + bounds[3]) / 2 - bufAnchorX;
        int centerZ = anchorZ + (bounds[2] + bounds[5]) / 2 - bufAnchorZ;
        int minY = anchorY + (bounds[1] - bufAnchorY);
        int maxY = minY + (int)((bounds[4] - bounds[1]) * 0.65);
        double maxRadius = Math.max(bounds[3] - bounds[0], bounds[5] - bounds[2]) * 0.75 / 2.0;
        List<int[]> blockPositions = new ArrayList<>();
        for (int[] pos : allPositions) {
            double ddx = pos[0] - centerX, ddz = pos[2] - centerZ;
            if (pos[1] <= maxY && Math.sqrt(ddx * ddx + ddz * ddz) <= maxRadius) blockPositions.add(pos);
        }
        if (blockPositions.isEmpty()) blockPositions = allPositions;

        // resolve spawner block type assets once before the placement loop
        int[] spawnerIds = new int[SPAWNER_BLOCKS.length];
        BlockType[] spawnerTypes = new BlockType[SPAWNER_BLOCKS.length];
        for (int i = 0; i < SPAWNER_BLOCKS.length; i++) {
            spawnerIds[i] = BlockType.getAssetMap().getIndex(SPAWNER_BLOCKS[i]);
            spawnerTypes[i] = BlockType.getAssetMap().getAsset(spawnerIds[i]);
        }

        // place each spawner on the top face of a random interior block, only within this chunk
        int[] dx = {1, -1, 0, 0, 0, 0}, dy = {0, 0, 1, -1, 0, 0}, dz = {0, 0, 0, 0, 1, -1};
        for (int i = 0; i < spawnerCount; i++) {
            int[] target = blockPositions.get(random.nextInt(blockPositions.size()));
            int[] faceOrder = {0, 1, 2, 3, 4, 5};
            for (int a = 5; a > 0; a--) { int swap = random.nextInt(a + 1); int tmp = faceOrder[a]; faceOrder[a] = faceOrder[swap]; faceOrder[swap] = tmp; }
            int spawnerIndex = random.nextInt(SPAWNER_BLOCKS.length);
            for (int face : faceOrder) {
                if (dy[face] != 1) continue;
                int sx = target[0] + dx[face], sy = target[1] + dy[face], sz = target[2] + dz[face];
                if (sy < UNDERGROUND_FLOOR || sy > 318) continue;
                if (sx < chunkMinX || sx > chunkMaxX || sz < chunkMinZ || sz > chunkMaxZ) continue;
                if (chunk.getBlock(sx, sy, sz) != 0) continue;
                if (spawnerTypes[spawnerIndex] == null) continue;
                chunk.setBlock(sx, sy, sz, spawnerIds[spawnerIndex], spawnerTypes[spawnerIndex], 0, 0, PLACEMENT_SETTINGS & ~0x02);
                break;
            }
        }
    }

    // write every block in this chunk's XZ column that belongs to the prefab, clearing terrain first
    private void pasteSlice(boolean fillAir, boolean skipTeleporter, BlockSelection buffer, WorldChunk chunk, int anchorX, int anchorY, int anchorZ, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
        int bufAnchorX = buffer.getAnchorX(), bufAnchorY = buffer.getAnchorY(), bufAnchorZ = buffer.getAnchorZ();
        BlockType airType = BlockType.getAssetMap().getAsset(0);
        int[] b = computeBounds(buffer);

        // flood the bounding box with air so terrain doesn't bleed through the prefab interior
        if (fillAir && airType != null && b[0] != Integer.MAX_VALUE) {
            int airSettings = PLACEMENT_SETTINGS | 0x02;
            for (int x = b[0]; x <= b[3]; x++) for (int y = b[1]; y <= b[4]; y++) for (int z = b[2]; z <= b[5]; z++) {
                int wx = anchorX + (x - bufAnchorX), wy = anchorY + (y - bufAnchorY), wz = anchorZ + (z - bufAnchorZ);
                if (wy < UNDERGROUND_FLOOR || wy > 318) continue;
                if (wx < chunkMinX || wx > chunkMaxX || wz < chunkMinZ || wz > chunkMaxZ) continue;
                chunk.setBlock(wx, wy, wz, 0, airType, 0, 0, airSettings);
            }
        }

        // place each prefab block, replacing Empty/Bench/Teleporter blocks with air
        buffer.forEachBlock((x, y, z, block) -> {
            if (block.blockId() == 0) return;
            int wx = anchorX + (x - bufAnchorX), wy = anchorY + (y - bufAnchorY), wz = anchorZ + (z - bufAnchorZ);
            if (wy < UNDERGROUND_FLOOR || wy > 318) return;
            if (wx < chunkMinX || wx > chunkMaxX || wz < chunkMinZ || wz > chunkMaxZ) return;
            BlockType blockType = BlockType.getAssetMap().getAsset(block.blockId());
            if (blockType == null) return;
            if (blockType.getId().equals("Empty") || blockType.getId().startsWith("Bench_") || (skipTeleporter && blockType.getId().equals("Teleporter"))) {
                if (airType != null) chunk.setBlock(wx, wy, wz, 0, airType, 0, 0, PLACEMENT_SETTINGS);
                return;
            }
            chunk.setBlock(wx, wy, wz, block.blockId(), blockType, block.rotation(), block.filler(), PLACEMENT_SETTINGS);
        });
    }

    // derive world-space 3D bounds from the prefab's local bounds and anchor, then push it to the registry
    private void recordPlacement(PrefabRecord.Type type, String filename, BlockSelection buffer, World world, int anchorX, int anchorY, int anchorZ) {
        PrefabRegistry registry = PrefabRegistry.get(world);
        if (registry == null) return;
        int[] b = computeBounds(buffer);
        int bufAX = buffer.getAnchorX(), bufAY = buffer.getAnchorY(), bufAZ = buffer.getAnchorZ();
        int minX = anchorX + (b[0] - bufAX), minY = anchorY + (b[1] - bufAY), minZ = anchorZ + (b[2] - bufAZ);
        int maxX = anchorX + (b[3] - bufAX), maxY = anchorY + (b[4] - bufAY), maxZ = anchorZ + (b[5] - bufAZ);
        registry.add(new PrefabRecord(type, filename, anchorX, anchorY, anchorZ, minX, minY, minZ, maxX, maxY, maxZ), world);
    }

    // load a prefab from disk — PrefabStore handles caching so repeat loads are free
    private BlockSelection loadPrefab(Path path) {
        try { return PrefabStore.get().getPrefab(path); } catch (Exception e) { LOGGER.warning("[HyARPG] Could not load prefab '" + path.getFileName() + "': " + e); return null; }
    }

    // walk a folder and return every prefab file found
    private List<Path> scanFolder(Path folder) {
        List<Path> result = new ArrayList<>();
        if (!Files.exists(folder)) return result;
        try (var stream = Files.walk(folder)) {
            stream.filter(p -> p.toString().endsWith(".prefab.json")).forEach(result::add);
        } catch (IOException e) { LOGGER.log(Level.WARNING, "[HyARPG] Failed to scan prefab folder: " + folder, e); }
        LOGGER.info("[HyARPG] Found " + result.size() + " prefab(s) in " + folder);
        return result;
    }

    // compute and cache the full 3D bounding box of non-air blocks in local prefab coords
    private int[] computeBounds(BlockSelection buffer) {
        return BOUNDS_CACHE.computeIfAbsent(buffer, b -> {
            int[] r = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
            b.forEachBlock((x, y, z, block) -> {
                r[0] = Math.min(r[0], x); r[1] = Math.min(r[1], y); r[2] = Math.min(r[2], z);
                r[3] = Math.max(r[3], x); r[4] = Math.max(r[4], y); r[5] = Math.max(r[5], z);
            });
            return r;
        });
    }

    // compute and cache the XZ footprint and min Y of a prefab — used for surface grounding
    private int[] getPrefabFootprint(BlockSelection buffer) {
        return FOOTPRINT_CACHE.computeIfAbsent(buffer, b -> {
            int[] r = {Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE};
            b.forEachBlock((x, y, z, block) -> {
                if (block.blockId() == 0) return;
                r[0] = Math.min(r[0], x); r[1] = Math.max(r[1], x);
                r[2] = Math.min(r[2], z); r[3] = Math.max(r[3], z);
                r[4] = Math.min(r[4], y);
            });
            return r;
        });
    }
}