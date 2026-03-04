package com.example.hyarpg.worldgen;

import com.example.hyarpg.worldgen.OreDistanceConfig.OreZone;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.events.ChunkPreLoadProcessEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OreDistanceListener {

    private static final Logger LOGGER = Logger.getLogger(OreDistanceListener.class.getName());

    private static final int CHUNK_SIZE   = 32;
    private static final int SKIP_PARTICLES = 4; // matches flag used by other mod

    private final OreDistanceConfig config;

    /** Resolved at first chunk event, once assets are fully loaded. */
    private List<ResolvedOre> resolvedOres;

    public OreDistanceListener(OreDistanceConfig config) {
        this.config = config;
    }

    public void register(EventRegistry eventRegistry) {
        eventRegistry.registerGlobal(
                EventPriority.LATE,
                ChunkPreLoadProcessEvent.class,
                (ChunkPreLoadProcessEvent event) -> onChunkPreLoad(event)
        );
        LOGGER.info("[HyARPG] OreDistanceListener registered");
    }

    // -------------------------------------------------------------------------

    private void onChunkPreLoad(ChunkPreLoadProcessEvent event) {
        if (!event.isNewlyGenerated()) return;

        WorldChunk chunk = event.getChunk();

        if (resolvedOres == null) {
            resolvedOres = resolveOres();
        }
        if (resolvedOres.isEmpty()) return;

        // Chunk centre in world block coordinates
        double centreX = (chunk.getX() * CHUNK_SIZE) + (CHUNK_SIZE / 2.0);
        double centreZ = (chunk.getZ() * CHUNK_SIZE) + (CHUNK_SIZE / 2.0);
        // Use mid-Y as the vertical component for distance — ore veins span a
        // narrow Y range so this gives a consistent horizontal-dominant distance
        double centreY = 155.0;

        double distance = euclidean3D(
                centreX, centreY, centreZ,
                config.spawnX, config.spawnY, config.spawnZ
        );

        // Deterministic seed matching the other mod's proven pattern
        long chunkSeed = (long)chunk.getX() * 341873128712L
                + (long)chunk.getZ() * 132897987541L;
        Random random = new Random(chunkSeed);

        int chunkWorldX = chunk.getX() << 5;
        int chunkWorldZ = chunk.getZ() << 5;

        for (ResolvedOre resolved : resolvedOres) {
            double weight = resolved.zone.weightAt(distance);
            if (weight <= 0.0) continue; // ore not eligible at this distance

            // Attempt veinsPerChunk veins — each attempt gated by spawn chance
            // which is the triangle-wave weight. At the midpoint every attempt
            // succeeds; near the edges most fail. This produces the natural
            // fade-in/fade-out of ore density at zone boundaries.
            int attempts = resolved.zone.veinsPerChunk;
            for (int i = 0; i < attempts; i++) {
                if (random.nextDouble() > weight) continue; // weight gates spawn chance

                // Pick a random position within the chunk
                int x = chunkWorldX + random.nextInt(CHUNK_SIZE);
                int z = chunkWorldZ + random.nextInt(CHUNK_SIZE);
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
     * Places a single ore vein centred at (centerX, centerY, centerZ).
     * Uses the same spherical scatter algorithm as the other mod so veins
     * look visually consistent with any other ores on the server.
     */
    private void generateVein(WorldChunk chunk, ResolvedOre resolved,
                              int centerX, int centerY, int centerZ,
                              int size, Random rand) {
        for (int i = 0; i < size; i++) {
            float progress = (float) i / (float) size;
            float angle1 = rand.nextFloat() * (float) Math.PI * 2.0f;
            float angle2 = rand.nextFloat() * (float) Math.PI * 2.0f;
            int offsetX = (int)(Math.cos(angle1) * progress * 2.0f);
            int offsetY = (int)(Math.sin(angle1) * Math.cos(angle2) * progress * 2.0f);
            int offsetZ = (int)(Math.sin(angle2) * progress * 2.0f);

            int x = centerX + offsetX;
            int y = centerY + offsetY;
            int z = centerZ + offsetZ;

            int clusterRadius = 1 + rand.nextInt(2);

            for (int dx = -clusterRadius; dx <= clusterRadius; dx++) {
                for (int dy = -clusterRadius; dy <= clusterRadius; dy++) {
                    for (int dz = -clusterRadius; dz <= clusterRadius; dz++) {
                        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        if (dist <= clusterRadius + rand.nextFloat() * 0.5f) {
                            int bx = x + dx;
                            int by = y + dy;
                            int bz = z + dz;

                            // Stay within world bounds and within this chunk
                            if (by < 1 || by > 310) continue;
                            if ((bx >> 5) != chunk.getX()) continue;
                            if ((bz >> 5) != chunk.getZ()) continue;

                            int currentBlock = chunk.getBlock(bx, by, bz);
                            if (!resolved.isReplaceable(currentBlock)) continue;

                            chunk.setBlock(
                                    bx, by, bz,
                                    resolved.oreId,
                                    resolved.oreType,
                                    0,
                                    0,
                                    SKIP_PARTICLES
                            );
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------

    /**
     * Resolves all configured OreZone string names to integer IDs and BlockType
     * objects. Called once on the first chunk event — assets are fully loaded
     * by that point. Logs warnings for unresolvable names without crashing.
     */
    private List<ResolvedOre> resolveOres() {
        List<ResolvedOre> result = new ArrayList<>();
        var assetMap = BlockType.getAssetMap();

        for (OreZone zone : config.zones) {
            // Resolve ore block
            int oreId = assetMap.getIndex(zone.oreBlockName);
            BlockType oreType = BlockType.fromString(zone.oreBlockName);
            if (oreId == Integer.MIN_VALUE || oreType == null) {
                LOGGER.log(Level.WARNING,
                        "[HyARPG] OreDistanceListener: unknown ore block ''{0}'' — skipping",
                        zone.oreBlockName);
                continue;
            }

            // Resolve each replaceable block
            int[] replaceableIds = new int[zone.replaceableBlocks.length];
            boolean allResolved = true;
            for (int i = 0; i < zone.replaceableBlocks.length; i++) {
                int id = assetMap.getIndex(zone.replaceableBlocks[i]);
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
                    new Object[]{zone.oreBlockName, (int)zone.minDistance,
                            (int)zone.maxDistance, zone.veinsPerChunk});
        }

        return result;
    }

    private static double euclidean3D(double x1, double y1, double z1,
                                      double x2, double y2, double z2) {
        double dx = x1 - x2, dy = y1 - y2, dz = z1 - z2;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
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