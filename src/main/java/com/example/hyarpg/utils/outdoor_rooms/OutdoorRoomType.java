package com.example.hyarpg.utils.outdoor_rooms;

// Hytale Imports
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;

// Java
import javax.annotation.Nullable;
import java.util.*;

public enum OutdoorRoomType {

    // --- Base Outdoor Spaces (structural classification only, no decoration requirements) --- //
    SMALL_OUTDOOR_SPACE("Small Outdoor Space", 0, 3, 3, 10, 10),
    OUTDOOR_SPACE("Outdoor Space",             1, 11, 11, 20, 20),
    LARGE_OUTDOOR_SPACE("Large Outdoor Space", 2, 21, 21, 28, 28);

//    // --- Garden --- //
//    BASIC_GARDEN("Basic Garden", 1, 3, 3, 28, 28,
//            all(
//                    min(9, blockKeyExact("Terrain_Farmland"), "9x Tilled Dirt"),
//                    min(4, blockKeyExact("Terrain_Water"), "4x Water"),
//                    min(1, hasCategoryContaining("Furniture.Fences"), "1x Fencing")
//            )
//    ),
//    INTERMEDIATE_GARDEN("Intermediate Garden", 2, 3, 3, 28, 28,
//            all(
//                    min(16, blockKeyExact("Terrain_Farmland"), "16x Tilled Dirt"),
//                    min(8, blockKeyExact("Terrain_Water"), "8x Water"),
//                    min(1, hasCategoryContaining("Furniture.Fences"), "1x Fencing"),
//                    min(1, blockKeyContains("Plant_Crop"), "1x Crop")
//            )
//    ),
//
//    // --- Ranch / Pen --- //
//    BASIC_RANCH("Basic Ranch", 1, 10, 10, 28, 28,
//            all(
//                    min(16, hasCategoryContaining("Furniture.Fences"), "16x Fencing"),
//                    min(1, blockKeyExact("Furniture_Trough"), "1x Trough")
//            )
//    ),
//    INTERMEDIATE_RANCH("Intermediate Ranch", 2, 10, 10, 28, 28,
//            all(
//                    min(24, hasCategoryContaining("Furniture.Fences"), "24x Fencing"),
//                    min(2, blockKeyExact("Furniture_Trough"), "2x Trough"),
//                    min(1, blockKeyContains("Hay"), "1x Hay Block")
//            )
//    ),
//
//    // --- Stable --- //
//    BASIC_STABLE("Basic Stable", 1, 10, 10, 28, 28,
//            all(
//                    min(16, hasCategoryContaining("Furniture.Fences"), "16x Fencing"),
//                    min(4, blockKeyContains("Hay"), "4x Hay Block"),
//                    min(1, blockKeyExact("Furniture_Trough"), "1x Trough")
//            )
//    );

    // --- Size caps derived from territory half (28 blocks each side) --- //
    public static final int MAX_INTERIOR_X = 28;
    public static final int MAX_INTERIOR_Z = 28;

    // How many blocks above the fence Y to scan for decorations
    public static final int SCAN_HEIGHT_ABOVE_FENCE = 5;

    private final String displayName;
    private final int tier;
    private final int minX, minZ;
    private final int maxX, maxZ;

    @Nullable
    private final RequirementGroup requirements;

    OutdoorRoomType(String displayName, int tier, int minX, int minZ, int maxX, int maxZ) {
        this.displayName = displayName;
        this.tier = tier;
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.requirements = null;
    }

    OutdoorRoomType(String displayName, int tier, int minX, int minZ, int maxX, int maxZ, RequirementGroup requirements) {
        this.displayName = displayName;
        this.tier = tier;
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.requirements = requirements;
    }

    // =========================================================================
    // Classification
    // =========================================================================

    @Nullable
    public static OutdoorRoomType classify(int sizeX, int sizeZ, Map<String, Integer> blockCounts) {
        OutdoorRoomType best = null;
        int bestScore = -1;
        int bestTier = -1;

        for (OutdoorRoomType type : values()) {
            if (!type.matchesSize(sizeX, sizeZ)) continue;
            int score = type.score(blockCounts);
            if (score < 0) continue;
            if (score > bestScore || (score == bestScore && type.tier > bestTier)) {
                best = type;
                bestScore = score;
                bestTier = type.tier;
            }
        }
        return best;
    }

    // Structural classification only — no block counts needed, just size
    @Nullable
    public static OutdoorRoomType classifyStructural(int sizeX, int sizeZ) {
        return classify(sizeX, sizeZ, Collections.emptyMap());
    }

    public boolean matchesSize(int sizeX, int sizeZ) {
        return sizeX >= minX && sizeX <= maxX && sizeZ >= minZ && sizeZ <= maxZ;
    }

    public int score(Map<String, Integer> blockCounts) {
        if (requirements == null) return 0;
        return requirements.score(blockCounts);
    }

    public String getDisplayName() { return displayName; }
    public int getTier() { return tier; }

    // =========================================================================
    // Predicate factories
    // =========================================================================

    public static BlockPredicate blockKeyExact(String key) {
        return blockKey -> blockKey.equals(key);
    }

    public static BlockPredicate blockKeyContains(String substring) {
        return blockKey -> blockKey.contains(substring);
    }

    public static BlockPredicate hasCategory(String category) {
        return blockKey -> {
            BlockType bt = BlockPredicate.resolve(blockKey);
            if (bt == null) return false;
            Item item = bt.getItem();
            if (item == null) return false;
            String[] categories = item.getCategories();
            if (categories == null) return false;
            for (String c : categories) {
                if (c.equals(category)) return true;
            }
            return false;
        };
    }

    public static BlockPredicate hasCategoryContaining(String substring) {
        return blockKey -> {
            BlockType bt = BlockPredicate.resolve(blockKey);
            if (bt == null) return false;
            Item item = bt.getItem();
            if (item == null) return false;
            String[] categories = item.getCategories();
            if (categories == null) return false;
            for (String c : categories) {
                if (c.contains(substring)) return true;
            }
            return false;
        };
    }

    public static DecoRequirement min(int count, BlockPredicate predicate, String label) {
        return new DecoRequirement(count, predicate, label);
    }

    // =========================================================================
    // Requirement builders
    // =========================================================================

    public static AllRequirements all(RequirementGroup... groups) {
        return new AllRequirements(Arrays.asList(groups));
    }

    public static AnyRequirements any(RequirementGroup... groups) {
        return new AnyRequirements(Arrays.asList(groups));
    }

    // =========================================================================
    // Requirement system (mirrors RoomType)
    // =========================================================================

    public interface RequirementGroup {
        int score(Map<String, Integer> blockCounts);
    }

    @FunctionalInterface
    public interface BlockPredicate {
        static BlockType resolve(String blockKey) {
            return BlockType.getAssetMap().getAsset(blockKey);
        }
        boolean test(String blockKey);
    }

    public static class DecoRequirement implements RequirementGroup {
        private final int minCount;
        private final BlockPredicate predicate;
        private final String description;

        public DecoRequirement(int minCount, BlockPredicate predicate, String description) {
            this.minCount = minCount;
            this.predicate = predicate;
            this.description = description;
        }

        @Override
        public int score(Map<String, Integer> blockCounts) {
            int count = 0;
            for (Map.Entry<String, Integer> entry : blockCounts.entrySet()) {
                if (predicate.test(entry.getKey())) count += entry.getValue();
            }
            if (count < minCount) return -1;
            return count;
        }

        public String getDescription() { return description; }
    }

    public static class AllRequirements implements RequirementGroup {
        private final List<RequirementGroup> children;

        public AllRequirements(List<RequirementGroup> children) { this.children = children; }
        public List<RequirementGroup> getChildren() { return children; }

        @Override
        public int score(Map<String, Integer> blockCounts) {
            int total = 0;
            for (RequirementGroup child : children) {
                int s = child.score(blockCounts);
                if (s < 0) return -1;
                total += s;
            }
            return total;
        }
    }

    public static class AnyRequirements implements RequirementGroup {
        private final List<RequirementGroup> children;

        public AnyRequirements(List<RequirementGroup> children) { this.children = children; }
        public List<RequirementGroup> getChildren() { return children; }

        @Override
        public int score(Map<String, Integer> blockCounts) {
            int best = -1;
            for (RequirementGroup child : children) {
                int s = child.score(blockCounts);
                if (s >= 0 && s > best) best = s;
            }
            return best;
        }
    }
}