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
    ROOM("Room", 1, 6, 6, 6, 9, 9, 9),
    LARGE_ROOM("Large Room", 2, 10, 10, 10, 15, 15, 15),

    // --- Kitchen --- //
    BASIC_KITCHEN("Basic Kitchen", 3, 3, 3, 3, 15, 15, 15,
        all(
            any(
                min(1, blockKeyExact("Bench_Campfire"), "1x Campfire"),
                min(1, blockKeyExact("Bench_Cooking"), "1x Chef's Stove")
            ),
            any(
                min(1, blockKeyExact("Furniture_Village_Barrel"), "1x Barrel"),
                min(1, blockKeyExact("Furniture_Village_Crate"), "1x Crate")
            ),
            min(1, isLighting(), "1x Lighting")
        )
    ),
    INTERMEDIATE_KITCHEN("Intermediate Kitchen", 4, 3, 3, 3, 15, 15, 15,
        all(
            min(1, blockKeyExact("Bench_Campfire"), "1x Campfire"),
            min(1, blockKeyExact("Bench_Cooking"), "1x Chef's Stove"),
            any(
                min(1, blockKeyExact("Furniture_Village_Barrel"), "1x Barrel"),
                min(1, blockKeyExact("Furniture_Village_Crate"), "1x Crate")
            ),
            min(2, isLighting(), "2x Lighting")
        )
    ),

    // --- Alchemy --- //
    BASIC_ALCHEMY("Basic Alchemy", 1, 3, 3, 3, 15, 15, 15,
            all(
                    min(1, blockKeyExact("Bench_Alchemy"), "1x Alchemy Bench"),
                    any(
                        min(1, blockKeyExact("Potion_Empty"), "1x Empty Potion"),
                        min(1, blockKeyExact("Potion_Empty_Small"), "1x Small Empty Potion"),
                        min(1, blockKeyExact("Potion_Empty_Large"), "1x Large Empty Potion")
                    ),
                    min(1, isLighting(), "1x Lighting")
            )
    ),
    INTERMEDIATE_ALCHEMY("Intermediate Alchemy", 2, 3, 3, 3, 15, 15, 15,
            all(
                    min(1, blockKeyExact("Bench_Alchemy"), "1x Alchemy Bench"),
                    any(
                            min(1, blockKeyExact("Potion_Empty"), "1x Empty Potion"),
                            min(1, blockKeyExact("Potion_Empty_Small"), "1x Small Empty Potion"),
                            min(1, blockKeyExact("Potion_Empty_Large"), "1x Large Empty Potion")
                    ),
                    any(
                            min(1, blockKeyExact("Alchemy_Cauldron"), "1x Cauldron"),
                            min(1, blockKeyExact("Alchemy_Cauldron_Big"), "1x Large Cauldron")
                    ),
                    any(
                            min(1, blockKeyExact("Plant_Crop_Mana1"), "1x Mana Crop (T1)"),
                            min(1, blockKeyExact("Plant_Crop_Mana2"), "1x Mana Crop (T2)"),
                            min(1, blockKeyExact("Plant_Crop_Mana3"), "1x Mana Crop (T3)")
                    ),
                    any(
                            min(1, blockKeyExact("Plant_Crop_Health1"), "1x Health Crop (T1)"),
                            min(1, blockKeyExact("Plant_Crop_Health2"), "1x Health Crop (T2)"),
                            min(1, blockKeyExact("Plant_Crop_Health3"), "1x Health Crop (T3)")
                    ),
                    min(2, isLighting(), "2x Lighting")
            )
    ),
    ADVANCED_ALCHEMY("Advanced Alchemy", 3, 3, 3, 3, 15, 15, 15,
            all(
                    min(1, blockKeyExact("Bench_Alchemy"), "1x Alchemy Bench"),
                    any(
                            min(1, blockKeyExact("Potion_Empty"), "1x Empty Potion"),
                            min(1, blockKeyExact("Potion_Empty_Small"), "1x Small Empty Potion"),
                            min(1, blockKeyExact("Potion_Empty_Large"), "1x Large Empty Potion")
                    ),
                    min(1, blockKeyExact("Alchemy_Cauldron_Big"), "1x Large Cauldron"),
                    any(
                            min(1, blockKeyExact("Plant_Crop_Mana1"), "1x Mana Crop (T1)"),
                            min(1, blockKeyExact("Plant_Crop_Mana2"), "1x Mana Crop (T2)"),
                            min(1, blockKeyExact("Plant_Crop_Mana3"), "1x Mana Crop (T3)")
                    ),
                    any(
                            min(1, blockKeyExact("Plant_Crop_Health1"), "1x Health Crop (T1)"),
                            min(1, blockKeyExact("Plant_Crop_Health2"), "1x Health Crop (T2)"),
                            min(1, blockKeyExact("Plant_Crop_Health3"), "1x Health Crop (T3)")
                    ),
                    any(
                            min(1, blockKeyExact("Plant_Crop_Stamina1"), "1x Stamina Crop (T1)"),
                            min(1, blockKeyExact("Plant_Crop_Stamina2"), "1x Stamina Crop (T2)"),
                            min(1, blockKeyExact("Plant_Crop_Stamina3"), "1x Stamina Crop (T3)")
                    ),
                    min(1, blockKeyExact("Furniture_Royal_Magic_Potion_Glow"), "1x Magic Potion Glow"),
                    min(1, isLighting(), "1x Lighting")
            )
    ),

    // --- Arcanist --- //
    BASIC_ARCANIST("Basic Arcanist", 1, 3, 3, 3, 15, 15, 15,
            all(
                    min(1, blockKeyExact("Bench_Arcane"), "1x Arcane Bench"),
                    any(
                            min(1, blockKeyExact("Deco_Book_Pile_Small"), "1x Small Book Pile"),
                            min(1, blockKeyExact("Deco_Book_Pile_Large"), "1x Large Book Pile")
                    ),
                    min(1, blockKeyExact("Furniture_Ancient_Candle"), "1x Ancient Candle")
            )
    ),
    INTERMEDIATE_ARCANIST("Intermediate Arcanist", 2, 3, 3, 3, 15, 15, 15,
            all(
                    min(1, blockKeyExact("Bench_Arcane"), "1x Arcane Bench"),
                    any(
                            min(1, blockKeyExact("Deco_Book_Pile_Small"), "1x Small Book Pile"),
                            min(1, blockKeyExact("Deco_Book_Pile_Large"), "1x Large Book Pile")
                    ),
                    min(2, blockKeyExact("Furniture_Ancient_Candle"), "2x Ancient Candle"),
                    any(
                            min(1, blockKeyExact("Book_Magic_Air"), "1x Air Spellbook"),
                            min(1, blockKeyExact("Book_Magic_Void"), "1x Void Spellbook"),
                            min(1, blockKeyExact("Book_Magic_Fire"), "1x Fire Spellbook"),
                            min(1, blockKeyExact("Book_Magic_Ice"), "1x Ice Spellbook")
                    )
            )
    ),
    ADVANCED_ARCANIST("Advanced Arcanist", 3, 3, 3, 3, 15, 15, 15,
            all(
                    min(1, blockKeyExact("Bench_Arcane"), "1x Arcane Bench"),
                    any(
                            min(1, blockKeyExact("Deco_Book_Pile_Small"), "1x Small Book Pile"),
                            min(1, blockKeyExact("Deco_Book_Pile_Large"), "1x Large Book Pile")
                    ),
                    min(2, blockKeyExact("Furniture_Ancient_Candle"), "2x Ancient Candle"),
                    any(
                            min(1, blockKeyExact("Book_Magic_Air"), "1x Air Spellbook"),
                            min(1, blockKeyExact("Book_Magic_Void"), "1x Void Spellbook"),
                            min(1, blockKeyExact("Book_Magic_Fire"), "1x Fire Spellbook"),
                            min(1, blockKeyExact("Book_Magic_Ice"), "1x Ice Spellbook")
                    ),
                    min(1, hasTagType("Type_Family_Gem"), "1x Gem")
            )
    ),

    // --- Forge --- //
    BASIC_FORGE("Basic Forge", 1, 3, 3, 3, 15, 15, 15,
            all(
                    any(
                            min(1, blockKeyExact("Bench_Furnace"), "1x Furnace"),
                            min(1, blockKeyExact("Bench_Armour_HyARPG"), "1x Armour Bench"),
                            min(1, blockKeyExact("Bench_Weapon_HyARPG"), "1x Weapon Bench")
                    ),
                    any(
                            min(1, blockKeyExact("Deco_Iron_Stack"), "1x Iron Stack")
                    ),
                    min(1, isLighting(), "1x Lighting")
            )
    ),
    INTERMEDIATE_FORGE("Intermediate Forge", 2, 3, 3, 3, 15, 15, 15,
            all(
                    any(
                            min(1, blockKeyExact("Bench_Armour_HyARPG"), "1x Armour Bench"),
                            min(1, blockKeyExact("Bench_Weapon_HyARPG"), "1x Weapon Bench")
                    ),
                    min(1, blockKeyExact("Bench_Furnace"), "1x Furnace"),
                    min(1, blockKeyExact("Bench_Salvage"), "1x Salvage Bench"),
                    min(1, blockKeyExact("Deco_Iron_Stack"), "1x Iron Stack"),
                    min(2, isLighting(), "2x Lighting")
            )
    ),
    ADVANCED_FORGE("Advanced Forge", 3, 3, 3, 3, 15, 15, 15,
            all(
                    min(1, blockKeyExact("Bench_Armour_HyARPG"), "1x Armour Bench"),
                    min(1, blockKeyExact("Bench_Weapon_HyARPG"), "1x Weapon Bench"),
                    min(1, blockKeyExact("Bench_Furnace"), "1x Furnace"),
                    min(1, blockKeyExact("Bench_Salvage"), "1x Salvage Bench"),
                    min(1, blockKeyExact("Deco_Iron_Stack"), "1x Iron Stack"),
                    min(2, isLighting(), "2x Lighting")
            )
    ),

    // --- Tailoring --- //
    BASIC_TAILOR("Basic Tailor", 1, 3, 3, 3, 15, 15, 15,
            all(
                    min(1, blockKeyExact("Bench_Leatherworking_HyARPG"), "1x Leatherworking Bench"),
                    min(1, blockKeyExact("Bench_Tannery"), "1x Tannery"),
                    min(1, isLighting(), "1x Lighting")
            )
    ),
    INTERMEDIATE_TAILOR("Intermediate Tailor", 2, 3, 3, 3, 15, 15, 15,
            all(
                    min(1, blockKeyExact("Bench_Leatherworking_HyARPG"), "1x Leatherworking Bench"),
                    min(1, blockKeyExact("Bench_Tannery"), "1x Tannery"),
                    min(1, blockKeyExact("Bench_Salvage"), "1x Salvage Bench"),
                    min(2, isLighting(), "2x Lighting")
            )
    ),
    ADVANCED_TAILOR("Advanced Tailor", 3, 3, 3, 3, 15, 15, 15,
            all(
                    min(1, blockKeyExact("Bench_Leatherworking_HyARPG"), "1x Leatherworking Bench"),
                    min(2, blockKeyExact("Bench_Tannery"), "2x Tannery"),
                    min(1, blockKeyExact("Bench_Salvage"), "1x Salvage Bench"),
                    min(2, isLighting(), "2x Lighting")
            )
    );

    public static final int MAX_INTERIOR_X = 15;
    public static final int MAX_INTERIOR_Y = 15;
    public static final int MAX_INTERIOR_Z = 15;
    public static final int MAX_FLOOD_VOLUME = MAX_INTERIOR_X * MAX_INTERIOR_Y * MAX_INTERIOR_Z;
    private final String displayName;
    private final int tier;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;
    @Nullable
    private final RequirementGroup requirements;

    RoomType(String displayName, int tier, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.displayName = displayName;
        this.tier = tier;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.requirements = null;
    }

    RoomType(String displayName, int tier, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, RequirementGroup requirements) {
        this.displayName = displayName;
        this.tier = tier;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.requirements = requirements;
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

    public static BlockPredicate blockKeyExact(String key) {
        return blockKey -> blockKey.equals(key);
    }

    // =========================================================================
    // Classification
    // =========================================================================

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

    public static DecoRequirement min(int count, BlockPredicate predicate, String label) {
        return new DecoRequirement(count, predicate, label);
    }

    public static DecoRequirement min(int count, BlockPredicate predicate) {
        return new DecoRequirement(count, predicate, "min:" + count);
    }

    // =========================================================================
    // Requirement system
    // =========================================================================

    public static AllRequirements all(RequirementGroup... groups) {
        return new AllRequirements(Arrays.asList(groups));
    }

    public static AnyRequirements any(RequirementGroup... groups) {
        return new AnyRequirements(Arrays.asList(groups));
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getTier() {
        return tier;
    }

    public boolean matchesSize(int sizeX, int sizeY, int sizeZ) {
        return sizeX >= minX && sizeX <= maxX && sizeY >= minY && sizeY <= maxY && sizeZ >= minZ && sizeZ <= maxZ;
    }

    // =========================================================================
    // Predicate factories
    // =========================================================================

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

    public static BlockPredicate isLighting() {
        return blockKey -> {
            if (blockKey.equals("Wood_Torch_Wall")) return true;
            BlockType bt = BlockPredicate.resolve(blockKey);
            if (bt == null) return false;
            Item item = bt.getItem();
            if (item == null) return false;
            String[] categories = item.getCategories();
            if (categories == null) return false;
            for (String c : categories) {
                if (c.contains("Furniture.Lighting")) return true;
            }
            return false;
        };
    }

    // =========================================================================
    // Requirement builders
    // =========================================================================

    // A single renderable line in the requirements list
    public static class RequirementLine {
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

        public enum Type {ITEM, ANY_START, ANY_ITEM, ANY_END}
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

        public String getDescription() {
            return description;
        }
    }

    public static class AllRequirements implements RequirementGroup {
        private final List<RequirementGroup> children;

        public AllRequirements(List<RequirementGroup> children) {
            this.children = children;
        }

        public List<RequirementGroup> getChildren() {
            return children;
        }

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

        public AnyRequirements(List<RequirementGroup> children) {
            this.children = children;
        }

        public List<RequirementGroup> getChildren() {
            return children;
        }

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