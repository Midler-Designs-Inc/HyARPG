package com.example.hyarpg.worldgen;

import java.util.List;

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

        // Block name as used in Hytale assets e.g. "Ore_Copper_Stone"
        public final String oreBlockName;

        // The blocks this ore can replace when generating a vein. e.g. "Rock_Stone", "Rock_Basalt"
        public final String[] replaceableBlocks;

        public final double minDistance;
        public final double maxDistance;
        public final double midDistance;
        private final double halfRange;

        // Veins attempted per chunk at peak weight (midpoint)
        public final int veinsPerChunk;

        // Min blocks per vein
        public final int minVeinSize;

        // Max blocks per vein
        public final int maxVeinSize;

        // Minimum Y level this ore can spawn at
        public final int minY;

        // Maximum Y level this ore can spawn at
        public final int maxY;

        public OreZone(String oreBlockName, String[] replaceableBlocks, double minDistance, double maxDistance, int veinsPerChunk, int minVeinSize, int maxVeinSize, int minY, int maxY) {
            if (minDistance >= maxDistance)
                throw new IllegalArgumentException("minDistance must be < maxDistance for ore: " + oreBlockName);
            if (minVeinSize > maxVeinSize)
                throw new IllegalArgumentException("minVeinSize must be <= maxVeinSize for ore: " + oreBlockName);

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

        public double weightAt(double distance) {
            if (distance < minDistance || distance > maxDistance) return 0.0;
            return Math.max(0.0, 1.0 - (Math.abs(distance - midDistance) / halfRange));
        }
    }
}