package com.example.hyarpg.worldgen;

import com.hypixel.hytale.builtin.worldgen.WorldGenPlugin;
import com.hypixel.hytale.builtin.worldgen.modifier.WorldGenModifier;
import com.hypixel.hytale.builtin.worldgen.modifier.event.EventType;
import com.hypixel.hytale.builtin.worldgen.modifier.event.ModifyEvent;
import com.hypixel.hytale.builtin.worldgen.modifier.event.ModifyEvents;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.util.PrefabUtil;
import com.hypixel.hytale.server.worldgen.biome.Biome;
import com.hypixel.hytale.server.worldgen.chunk.populator.PrefabPopulator;
import com.hypixel.hytale.server.worldgen.container.PrefabContainer;
import com.hypixel.hytale.server.worldgen.loader.WorldGenPrefabLoader;
import com.hypixel.hytale.server.worldgen.loader.WorldGenPrefabSupplier;
import com.hypixel.hytale.server.worldgen.loader.prefab.PrefabPatternGeneratorJsonLoader;
import com.hypixel.hytale.server.worldgen.prefab.PrefabPatternGenerator;

import java.util.List;

/**
 * Configuration for ore spawn distance zones.
 *
 * Each OreZone defines one ore type with a distance range from the spawn point.
 * Spawn chance scales as a triangle wave — peaking at the midpoint of the range
 * and fading to zero at both edges.
 *
 * WEIGHT FORMULA:
 *   mid    = (min + max) / 2.0
 *   weight = 1.0 - (|distance - mid| / halfRange)  clamped [0.0, 1.0]
 *
 * Example: copper 0–10k, iron 5k–15k
 *   At  5k:   copper=1.0, iron=0.0
 *   At  7.5k: copper=0.5, iron=0.5
 *   At 10k:   copper=0.0, iron=1.0
 */
public class OreDistanceConfig {

    public final double spawnX, spawnY, spawnZ;
    public final List<OreZone> zones;

    public OreDistanceConfig(double spawnX, double spawnY, double spawnZ, List<OreZone> zones) {
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        this.spawnZ = spawnZ;
        this.zones  = List.copyOf(zones);
    }

    public static class OreZone {

        /** Block name as used in Hytale assets e.g. "Ore_Copper_Stone" */
        public final String oreBlockName;

        /**
         * The stone blocks this ore can replace when generating a vein.
         * Should match the rock types naturally present at this depth/region.
         * e.g. "Rock_Stone", "Rock_Basalt"
         */
        public final String[] replaceableBlocks;

        public final double minDistance;
        public final double maxDistance;
        public final double midDistance;
        private final double halfRange;

        /** Veins attempted per chunk at peak weight (midpoint). */
        public final int veinsPerChunk;

        /** Min blocks per vein. */
        public final int minVeinSize;

        /** Max blocks per vein. */
        public final int maxVeinSize;

        /** Minimum Y level this ore can spawn at. */
        public final int minY;

        /** Maximum Y level this ore can spawn at. */
        public final int maxY;

        public OreZone(String oreBlockName, String[] replaceableBlocks,
                       double minDistance, double maxDistance,
                       int veinsPerChunk, int minVeinSize, int maxVeinSize,
                       int minY, int maxY) {
            if (minDistance >= maxDistance)
                throw new IllegalArgumentException(
                        "minDistance must be < maxDistance for ore: " + oreBlockName);
            if (minVeinSize > maxVeinSize)
                throw new IllegalArgumentException(
                        "minVeinSize must be <= maxVeinSize for ore: " + oreBlockName);
            this.oreBlockName      = oreBlockName;
            this.replaceableBlocks = replaceableBlocks;
            this.minDistance       = minDistance;
            this.maxDistance       = maxDistance;
            this.midDistance       = (minDistance + maxDistance) / 2.0;
            this.halfRange         = (maxDistance - minDistance) / 2.0;
            this.veinsPerChunk     = veinsPerChunk;
            this.minVeinSize       = minVeinSize;
            this.maxVeinSize       = maxVeinSize;
            this.minY              = minY;
            this.maxY              = maxY;
        }

        /**
         * Triangle wave weight: 1.0 at midpoint, 0.0 at edges and outside range.
         * Used as spawn chance per vein attempt.
         */
        public double weightAt(double distance) {
            if (distance < minDistance || distance > maxDistance) return 0.0;
            return Math.max(0.0, 1.0 - (Math.abs(distance - midDistance) / halfRange));
        }
    }
}