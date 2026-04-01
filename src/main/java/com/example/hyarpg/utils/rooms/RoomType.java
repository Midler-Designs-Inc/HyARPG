package com.example.hyarpg.utils.rooms;

// Hytale Imports
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;

// Java
import javax.annotation.Nullable;
import java.util.*;

public enum RoomType {

    // --- Empty Rooms --- //
    SMALL_ROOM("Small Room", 0, 3, 3, 3, 5, 5, 5),
    ROOM(      "Room",       1, 6, 6, 6, 9, 9, 9),
    LARGE_ROOM("Large Room", 2, 10, 10, 10, 15, 15, 15),
    BASIC_KITCHEN("Simple Kitchen", 1, 3, 3, 3, 15, 15, 15,
            all(
                any(
                    min(1, blockKeyExact("Bench_Campfire"), "1x Campfire"),
                    min(1, blockKeyExact("Bench_Cooking"), "1x Chef's Stove")
                ),
                min(1, hasCategory("Furniture.Containers"), "1x Storage Container")
            )
    ),
    ADVANCED_KITCHEN("Advanced Kitchen", 2, 3, 3, 3, 15, 15, 15,
            all(
                min(1, blockKeyExact("Bench_Cooking"), "1x Chef's Stove"),
                min(2, hasCategory("Furniture.Containers"), "2x Storage Container")
            )
    ),
    BEDROOM("Simple Bedroom", 1, 3, 3, 3, 9, 9, 9,
            all(
                min(1, hasCategory("Furniture.Beds"), "1x Bed"),
                min(1, blockKeyContains("Wardrobe"), "1x Wardrobe")
            )
    );

    private final String displayName;
    private final int tier;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;

    @Nullable
    private final RequirementGroup requirements;

    RoomType(String displayName, int tier, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.displayName = displayName;
        this.tier = tier;
        this.minX = minX; this.minY = minY; this.minZ = minZ;
        this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
        this.requirements = null;
    }

    RoomType(String displayName, int tier, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, RequirementGroup requirements) {
        this.displayName = displayName;
        this.tier = tier;
        this.minX = minX; this.minY = minY; this.minZ = minZ;
        this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
        this.requirements = requirements;
    }

    public String getDisplayName() { return displayName; }
    public int getTier() { return tier; }

    public boolean matchesSize(int sizeX, int sizeY, int sizeZ) {
        return sizeX >= minX && sizeX <= maxX && sizeY >= minY && sizeY <= maxY && sizeZ >= minZ && sizeZ <= maxZ;
    }

    public int score(Map<String, Integer> blockCounts) {
        if (requirements == null) return 0;
        return requirements.score(blockCounts);
    }

    // Returns e.g. "3x3x3" or "3x3x3 – 15x15x15"
    public String getSizeDescription() {
        if (minX == maxX && minY == maxY && minZ == maxZ) {
            return minX + "x" + minY + "x" + minZ;
        }
        return minX + "x" + minY + "x" + minZ + " \u2013 " + maxX + "x" + maxY + "x" + maxZ;
    }

    // Returns a list of RequirementLine entries for structured rendering
    public List<RequirementLine> getRequirementLines() {
        if (requirements == null) return Collections.emptyList();
        List<RequirementLine> lines = new ArrayList<>();
        collectLines(requirements, lines);
        return lines;
    }

    // A single renderable line in the requirements list
    public static class RequirementLine {
        public enum Type { ITEM, ANY_START, ANY_ITEM, ANY_END }
        public final Type type;
        public final String label; // used for ITEM and ANY_ITEM

        public RequirementLine(Type type, String label) {
            this.type = type;
            this.label = label;
        }
        public RequirementLine(Type type) {
            this.type = type;
            this.label = "";
        }
    }

    private static void collectLines(RequirementGroup group, List<RequirementLine> lines) {
        if (group instanceof DecoRequirement deco) {
            lines.add(new RequirementLine(RequirementLine.Type.ITEM, deco.getDescription()));
        } else if (group instanceof AllRequirements all) {
            for (RequirementGroup child : all.getChildren()) {
                collectLines(child, lines);
            }
        } else if (group instanceof AnyRequirements any) {
            lines.add(new RequirementLine(RequirementLine.Type.ANY_START));
            for (RequirementGroup child : any.getChildren()) {
                if (child instanceof DecoRequirement deco) {
                    lines.add(new RequirementLine(RequirementLine.Type.ANY_ITEM, deco.getDescription()));
                } else {
                    // Nested groups within ANY — flatten
                    collectLines(child, lines);
                }
            }
            lines.add(new RequirementLine(RequirementLine.Type.ANY_END));
        }
    }

    // =========================================================================
    // Classification
    // =========================================================================

    public static final int MAX_INTERIOR_X = 15;
    public static final int MAX_INTERIOR_Y = 15;
    public static final int MAX_INTERIOR_Z = 15;
    public static final int MAX_FLOOD_VOLUME = MAX_INTERIOR_X * MAX_INTERIOR_Y * MAX_INTERIOR_Z;

    @Nullable
    public static RoomType classify(int sizeX, int sizeY, int sizeZ, Map<String, Integer> blockCounts) {
        RoomType best = null;
        int bestScore = -1;
        int bestTier = -1;

        for (RoomType type : values()) {
            if (!type.matchesSize(sizeX, sizeY, sizeZ)) continue;
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

    @Nullable
    public static RoomType classify(int sizeX, int sizeY, int sizeZ) {
        return classify(sizeX, sizeY, sizeZ, Collections.emptyMap());
    }

    // =========================================================================
    // Requirement system
    // =========================================================================

    public interface RequirementGroup {
        int score(Map<String, Integer> blockCounts);
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

    @FunctionalInterface
    public interface BlockPredicate {
        boolean test(String blockKey);

        static BlockType resolve(String blockKey) {
            return BlockType.getAssetMap().getAsset(blockKey);
        }
    }

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

    public static BlockPredicate hasTagType(String tagType) {
        return blockKey -> {
            BlockType bt = BlockPredicate.resolve(blockKey);
            if (bt == null) return false;
            Item item = bt.getItem();
            if (item == null || item.getData() == null) return false;
            String[] categories = item.getCategories();
            if (categories == null) return false;
            for (String c : categories) {
                if (c.contains(tagType)) return true;
            }
            return false;
        };
    }

    // =========================================================================
    // Requirement builders
    // =========================================================================

    public static DecoRequirement min(int count, BlockPredicate predicate, String label) {
        return new DecoRequirement(count, predicate, label);
    }

    public static DecoRequirement min(int count, BlockPredicate predicate) {
        return new DecoRequirement(count, predicate, "min:" + count);
    }

    public static AllRequirements all(RequirementGroup... groups) {
        return new AllRequirements(Arrays.asList(groups));
    }

    public static AnyRequirements any(RequirementGroup... groups) {
        return new AnyRequirements(Arrays.asList(groups));
    }
}