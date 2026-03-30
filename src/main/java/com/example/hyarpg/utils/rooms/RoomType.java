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
            min(1, blockKeyExact("Bench_Campfire")),
            min(1, hasCategory("Furniture.Container"))
        )
    ),
    ADVANCED_KITCHEN("Advanced Kitchen", 2, 3, 3, 3, 15, 15, 15,
        all(
            any(
                min(1, blockKeyExact("Bench_Campfire")),
                min(1, blockKeyExact("Bench_Cooking"))
            ),
            min(1, hasCategory("Furniture.Container"))
        )
    ),
    BEDROOM("Simple Bedroom", 1, 3, 3, 3, 9, 9, 9,
        all(
            min(1, hasCategory("Furniture.Beds")),
            min(1, blockKeyContains("Wardrobe"))
        )
    );

    private final String displayName;
    private final int tier;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;

    @Nullable
    private final RequirementGroup requirements;

    // Size-only constructor
    RoomType(String displayName, int tier, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.displayName = displayName;
        this.tier = tier;
        this.minX = minX; this.minY = minY; this.minZ = minZ;
        this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
        this.requirements = null;
    }

    // Constructor with decoration requirements
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
        return sizeX >= minX && sizeX <= maxX
                && sizeY >= minY && sizeY <= maxY
                && sizeZ >= minZ && sizeZ <= maxZ;
    }

    // Checks requirements met, -1 if requirements are not met (disqualified), 0 if no requirements (size-only match)
    public int score(Set<String> blockKeysInside) {
        if (requirements == null) return 0;
        return requirements.score(blockKeysInside);
    }

    // --- Classification ---

    // Max possible interior — used as flood fill cap
    public static final int MAX_INTERIOR_X = 15;
    public static final int MAX_INTERIOR_Y = 15;
    public static final int MAX_INTERIOR_Z = 15;
    public static final int MAX_FLOOD_VOLUME = MAX_INTERIOR_X * MAX_INTERIOR_Y * MAX_INTERIOR_Z; // 3375

    // Returns the best matching room type for the given dimensions and contents.
    // Priority: highest requirement score, then highest tier on tie.
    @Nullable
    public static RoomType classify(int sizeX, int sizeY, int sizeZ, Set<String> blockKeysInside) {
        RoomType best = null;
        int bestScore = -1;
        int bestTier = -1;

        for (RoomType type : values()) {
            if (!type.matchesSize(sizeX, sizeY, sizeZ)) continue;
            int score = type.score(blockKeysInside);
            if (score < 0) continue; // requirements not met

            if (score > bestScore || (score == bestScore && type.tier > bestTier)) {
                best = type;
                bestScore = score;
                bestTier = type.tier;
            }
        }
        return best;
    }

    // Overload for size-only classification (no decoration context available)
    @Nullable
    public static RoomType classify(int sizeX, int sizeY, int sizeZ) {
        return classify(sizeX, sizeY, sizeZ, Collections.emptySet());
    }

    // =========================================================================
    // Requirement system
    // =========================================================================

    // A group of requirements combined with ALL or ANY logic.
    public interface RequirementGroup {
        int score(Set<String> blockKeysInside);
    }

    // A single decoration requirement: min N blocks matching a predicate.
    public static class DecoRequirement implements RequirementGroup {
        private final int minCount;
        private final BlockPredicate predicate;
        private final String description; // for future serialization

        public DecoRequirement(int minCount, BlockPredicate predicate, String description) {
            this.minCount = minCount;
            this.predicate = predicate;
            this.description = description;
        }

        @Override
        public int score(Set<String> blockKeysInside) {
            int count = 0;
            for (String key : blockKeysInside) {
                if (predicate.test(key)) count++;
            }
            if (count < minCount) return -1; // not met
            return count; // score = how many matched (more = better)
        }

        public String getDescription() { return description; }
    }

    // ALL: every child must be met. Score = sum of all child scores.
    public static class AllRequirements implements RequirementGroup {
        private final List<RequirementGroup> children;

        public AllRequirements(List<RequirementGroup> children) {
            this.children = children;
        }

        @Override
        public int score(Set<String> blockKeysInside) {
            int total = 0;
            for (RequirementGroup child : children) {
                int s = child.score(blockKeysInside);
                if (s < 0) return -1; // any failure disqualifies
                total += s;
            }
            return total;
        }
    }

    // ANY: at least one child must be met. Score = highest child score.
    public static class AnyRequirements implements RequirementGroup {
        private final List<RequirementGroup> children;

        public AnyRequirements(List<RequirementGroup> children) {
            this.children = children;
        }

        @Override
        public int score(Set<String> blockKeysInside) {
            int best = -1;
            for (RequirementGroup child : children) {
                int s = child.score(blockKeysInside);
                if (s >= 0 && s > best) best = s;
            }
            return best; // -1 if none matched
        }
    }

    // Predicate for matching block keys
    @FunctionalInterface
    public interface BlockPredicate {
        boolean test(String blockKey);

        // Resolve the BlockType from the key for category/tag checks
        static BlockType resolve(String blockKey) {
            return BlockType.getAssetMap().getAsset(blockKey);
        }
    }

    // =========================================================================
    // Predicate factory methods — use these when defining room requirements
    // =========================================================================

    // Block key is exactly this string
    public static BlockPredicate blockKeyExact(String key) {
        return blockKey -> blockKey.equals(key);
    }

    // Block key contains this substring
    public static BlockPredicate blockKeyContains(String substring) {
        return blockKey -> blockKey.contains(substring);
    }

    // Block's item has a category that includes this string exactly
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

    // Block's item has a category that contains this substring
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

    // Block's item data has a tag type that includes this value
    // Usage: hasTagType("Furniture") matches items with tags.type containing "Furniture"
    public static BlockPredicate hasTagType(String tagType) {
        return blockKey -> {
            BlockType bt = BlockPredicate.resolve(blockKey);
            if (bt == null) return false;
            Item item = bt.getItem();
            if (item == null || item.getData() == null) return false;
            // Tags are stored in AssetExtraInfo.Data — access via getCategories pattern
            // This will need to be wired to your actual tag access pattern
            // Placeholder: check category contains the tag type
            String[] categories = item.getCategories();
            if (categories == null) return false;
            for (String c : categories) {
                if (c.contains(tagType)) return true;
            }
            return false;
        };
    }

    // =========================================================================
    // Requirement builder helpers — use these when defining room requirements
    // =========================================================================

    // Require at least N blocks matching the predicate
    public static DecoRequirement min(int count, BlockPredicate predicate) {
        return new DecoRequirement(count, predicate, "min:" + count);
    }

    // All of these requirements must be met
    public static AllRequirements all(RequirementGroup... groups) {
        return new AllRequirements(Arrays.asList(groups));
    }

    // At least one of these requirements must be met
    public static AnyRequirements any(RequirementGroup... groups) {
        return new AnyRequirements(Arrays.asList(groups));
    }
}