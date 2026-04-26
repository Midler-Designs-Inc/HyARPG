package com.example.hyarpg.worldgen;

import com.example.hyarpg.worldgen.OreDistanceConfig.OreZone;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.command.commands.world.chunk.ChunkLoadedCommand;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.events.ChunkEvent;
import com.hypixel.hytale.server.core.universe.world.events.ChunkPreLoadProcessEvent;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OreDistanceListener {

    private static final Logger LOGGER = Logger.getLogger(OreDistanceListener.class.getName());

    private static final int CHUNK_SIZE = 32;

    /**
     * ChunkPreLoadProcessEvent fires BEFORE terrain generation populates the
     * chunk with stone. Ore placed here gets written into an empty chunk;
     * terrain gen then fills in stone around the pre-placed ore blocks,
     * leaving them naturally embedded.
     *
     * Because world and reference are null at this stage, we must skip every
     * setBlock operation that dereferences them:
     *   0x02 — skip setState        (needs world)
     *   0x04 — skip particles       (needs world)
     *   0x08 — skip setFiller       (needs reference)
     *   0x10 — skip removeFiller    (needs reference)
     *   0x200 — skip height update  (terrain gen recalculates anyway)
     */
    private static final int PLACEMENT_SETTINGS = 0x02 | 0x04 | 0x08 | 0x10 | 0x200;

    private final OreDistanceConfig config;

    private List<ResolvedOre> resolvedOres = null;

    public OreDistanceListener(OreDistanceConfig config) {
        this.config = config;
    }

    public void register(EventRegistry eventRegistry) {
        eventRegistry.registerGlobal(
                EventPriority.LAST,
                ChunkPreLoadProcessEvent.class,
                (ChunkPreLoadProcessEvent event) -> onChunkPreLoad(event)
        );
        LOGGER.info("[HyARPG] OreDistanceListener registered");
    }

    // -------------------------------------------------------------------------

    private void onChunkPreLoad(ChunkPreLoadProcessEvent event) {
        if (!event.isNewlyGenerated()) return;

        if (resolvedOres == null || resolvedOres.isEmpty()) {
            resolvedOres = resolveOres();
        }
        if (resolvedOres == null || resolvedOres.isEmpty()) return;

        WorldChunk chunk = event.getChunk();

        double centreX = (chunk.getX() * CHUNK_SIZE) + (CHUNK_SIZE / 2.0);
        double centreZ = (chunk.getZ() * CHUNK_SIZE) + (CHUNK_SIZE / 2.0);
        double distance = euclidean2D(centreX, centreZ, config.spawnX, config.spawnZ);

        int chunkWorldX = chunk.getX() * CHUNK_SIZE;
        int chunkWorldZ = chunk.getZ() * CHUNK_SIZE;

        for (ResolvedOre resolved : resolvedOres) {
            double weight = resolved.zone.weightAt(distance);
            if (weight <= 0.0) continue;

            long chunkSeed = (long) chunk.getX() * 341873128712L
                    + (long) chunk.getZ() * 132897987541L;
            Random random = new Random(chunkSeed ^ (long) resolved.oreId * 6364136223846793005L);

            int attempts = resolved.zone.veinsPerChunk;
            for (int i = 0; i < attempts; i++) {
                if (random.nextDouble() > weight) continue;

                int margin = 4;
                int x = chunkWorldX + margin + random.nextInt(CHUNK_SIZE - margin * 2);
                int z = chunkWorldZ + margin + random.nextInt(CHUNK_SIZE - margin * 2);
                int y = resolved.zone.minY + random.nextInt(
                        Math.max(1, resolved.zone.maxY - resolved.zone.minY + 1));

                int veinSize = resolved.zone.minVeinSize
                        + random.nextInt(Math.max(1,
                        resolved.zone.maxVeinSize - resolved.zone.minVeinSize + 1));

                generateVein(chunk, resolved, x, y, z, veinSize, random);
            }
        }
    }

    /**
     * Places a tight blob of ore centred at (cx, cy, cz).
     *
     * Since terrain has not been generated yet, we cannot use isReplaceable —
     * the chunk is empty. Instead we use a fixed radius of 1, which scatters
     * each block within a 3x3x3 cube around a short random walk. Terrain gen
     * fills in stone around these pre-placed blocks, embedding them naturally.
     *
     * The walk advances ~1 block per step so the vein has a natural elongated
     * shape rather than a perfect sphere.
     */
    private void generateVein(WorldChunk chunk, ResolvedOre resolved,
                              int cx, int cy, int cz,
                              int size, Random rand) {
        float fx = cx;
        float fy = cy;
        float fz = cz;

        for (int i = 0; i < size; i++) {
            // Advance the walk one step in a random direction
            int axis = rand.nextInt(3);
            int dir  = rand.nextBoolean() ? 1 : -1;
            if (axis == 0) fx += dir;
            else if (axis == 1) fy += dir * 0.5f; // gentler vertical drift
            else fz += dir;

            int x = Math.round(fx);
            int y = Math.round(fy);
            int z = Math.round(fz);

            // Place a small cluster at this step (radius 1 = up to 7 blocks)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        // Skip corners to keep clusters roundish
                        if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 2) continue;
                        if (rand.nextFloat() > 0.7f) continue; // thin it out

                        int bx = x + dx;
                        int by = y + dy;
                        int bz = z + dz;

                        if (by < 1 || by > 310) continue;
                        if (Math.floorDiv(bx, CHUNK_SIZE) != chunk.getX()) continue;
                        if (Math.floorDiv(bz, CHUNK_SIZE) != chunk.getZ()) continue;

                        chunk.setBlock(bx, by, bz, resolved.oreId, resolved.oreType,
                                0, 0, PLACEMENT_SETTINGS);
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------

    private List<ResolvedOre> resolveOres() {
        var assetMap = BlockType.getAssetMap();
        if (assetMap == null) {
            LOGGER.log(Level.INFO, "[HyARPG] OreDistanceListener: asset map not ready yet, will retry");
            return null;
        }

        List<ResolvedOre> result = new ArrayList<>();

        for (OreZone zone : config.zones) {
            int oreId;
            BlockType oreType;
            try {
                oreId   = assetMap.getIndex(zone.oreBlockName);
                oreType = BlockType.fromString(zone.oreBlockName);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING,
                        "[HyARPG] OreDistanceListener: exception resolving ''{0}'': {1}",
                        new Object[]{zone.oreBlockName, e});
                continue;
            }

            if (oreId == Integer.MIN_VALUE || oreType == null) {
                LOGGER.log(Level.WARNING,
                        "[HyARPG] OreDistanceListener: unknown ore block ''{0}'' — skipping",
                        zone.oreBlockName);
                continue;
            }

            int[] replaceableIds = new int[zone.replaceableBlocks.length];
            boolean allResolved = true;
            for (int i = 0; i < zone.replaceableBlocks.length; i++) {
                int id;
                try {
                    id = assetMap.getIndex(zone.replaceableBlocks[i]);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING,
                            "[HyARPG] OreDistanceListener: exception resolving replaceable ''{0}'' for ''{1}'': {2}",
                            new Object[]{zone.replaceableBlocks[i], zone.oreBlockName, e});
                    allResolved = false;
                    break;
                }
                if (id == Integer.MIN_VALUE) {
                    LOGGER.log(Level.WARNING,
                            "[HyARPG] OreDistanceListener: unknown replaceable block ''{0}'' for ore ''{1}'' — skipping zone",
                            new Object[]{zone.replaceableBlocks[i], zone.oreBlockName});
                    allResolved = false;
                    break;
                }
                replaceableIds[i] = id;
            }
            if (!allResolved) continue;

            result.add(new ResolvedOre(zone, oreId, oreType, replaceableIds));
            LOGGER.log(Level.INFO,
                    "[HyARPG] OreDistanceListener: registered ''{0}'' ({1}–{2} blocks from spawn, {3} veins/chunk)",
                    new Object[]{zone.oreBlockName, (int) zone.minDistance,
                            (int) zone.maxDistance, zone.veinsPerChunk});
        }

        return result;
    }

    private static double euclidean2D(double x1, double z1, double x2, double z2) {
        double dx = x1 - x2, dz = z1 - z2;
        return Math.sqrt(dx * dx + dz * dz);
    }

    // -------------------------------------------------------------------------

    private static class ResolvedOre {
        final OreZone zone;
        final int oreId;
        final BlockType oreType;
        final int[] replaceableIds;

        ResolvedOre(OreZone zone, int oreId, BlockType oreType, int[] replaceableIds) {
            this.zone           = zone;
            this.oreId          = oreId;
            this.oreType        = oreType;
            this.replaceableIds = replaceableIds;
        }

        boolean isReplaceable(int blockId) {
            for (int id : replaceableIds) {
                if (id == blockId) return true;
            }
            return false;
        }
    }
}