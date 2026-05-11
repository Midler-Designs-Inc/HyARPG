package com.example.hyarpg.utils.items;

import com.example.hyarpg.utils.StatTypeInfo;
import com.example.hyarpg.utils.affixes.Affix;
import com.example.hyarpg.utils.affixes.AffixPool;
import com.example.hyarpg.utils.affixes.StatType;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import org.bson.BsonDocument;
import org.bson.BsonValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class ItemFactory {

    // rarity to affix count mapping
    private static final Map<String, Integer> RARITY_TO_AFFIX_COUNT = Map.of(
        "Common", 0,
        "Uncommon", 1,
        "Rare", 2,
        "Epic", 3,
        "Legendary", 4
    );

    // weighted rarity roll — used when rarity is not explicitly provided
    private static final String[] RARITY_POOL;
    static {
        // Common: 50, Uncommon: 25, Rare: 15, Epic: 7, Legendary: 3 = 100 entries
        List<String> pool = new ArrayList<>(100);
        for (int i = 0; i < 50; i++) pool.add("Common");
        for (int i = 0; i < 25; i++) pool.add("Uncommon");
        for (int i = 0; i < 15; i++) pool.add("Rare");
        for (int i = 0; i < 7; i++) pool.add("Epic");
        for (int i = 0; i < 3; i++) pool.add("Legendary");
        RARITY_POOL = pool.toArray(new String[0]);
    }

    // maps each weapon/armor sub-type to its ordered slot component types (index 0-2, slot 4 is always shard)
    public static final Map<String, List<String>> ALLOWED_COMPONENTS = Map.ofEntries(
        // 1H weapons
        Map.entry("Axe",       List.of("Axe Head", "Shaft", "Handle")),
        Map.entry("Club",      List.of("Club Head", "Shaft", "Handle")),
        Map.entry("Shield",    List.of("Shield Frame", "Shield Body", "Handle")),
        Map.entry("Spear",     List.of("Spear Head", "Shaft", "Handle")),
        Map.entry("Sword",     List.of("Blade", "Hilt", "Handle")),

        // 2H weapons
        Map.entry("Battleaxe", List.of("Battleaxe Head", "Shaft", "Handle")),
        Map.entry("Claws",     List.of("Claw Blades", "Hilt", "Handle")),
        Map.entry("Daggers",   List.of("Short Blade", "Hilt", "Handle")),
        Map.entry("Longsword", List.of("Long Blade", "Hilt", "Handle")),
        Map.entry("Mace",      List.of("Mace Head", "Shaft", "Handle")),
        Map.entry("Scythe",    List.of("Scythe Blade", "Shaft", "Handle")),
        Map.entry("Sickles",   List.of("Curved Blade", "Shaft", "Handle")),

        // ranged weapons
        Map.entry("Crossbow",  List.of("Crossbow Head", "String", "Handle")),
        Map.entry("Kunai",     List.of("Kunai Blade", "Hilt", "Handle")),
        Map.entry("Longbow",   List.of("Longbow Body", "String", "Handle")),
        Map.entry("Shortbow",  List.of("Shortbow Body", "String", "Handle")),

        // magic weapons
        Map.entry("Spellbook", List.of("Book Binding", "Book Pages", "Magic Core")),
        Map.entry("Staff",     List.of("Staff Head", "Shaft", "Magic Core")),
        Map.entry("Wand",      List.of("Wand Body", "Handle", "Magic Core")),

        // metal armor
        Map.entry("Metal Helmet",    List.of("Metal Helmet Shell", "Straps & Buckles", "Padding")),
        Map.entry("Metal Chest",   List.of("Metal Chest Shell", "Straps & Buckles", "Padding")),
        Map.entry("Metal Gloves",  List.of("Metal Gloves Shell", "Straps & Buckles", "Padding")),
        Map.entry("Metal Pants",   List.of("Metal Pants Shell", "Straps & Buckles", "Padding")),

        // leather armor
        Map.entry("Leather Hood",     List.of("Leather Hood Panel", "Straps & Buckles", "Stitching")),
        Map.entry("Leather Vest",    List.of("Leather Vest Panel", "Straps & Buckles", "Stitching")),
        Map.entry("Leather Gloves",   List.of("Leather Gloves Panel", "Straps & Buckles", "Stitching")),
        Map.entry("Leather Pants",    List.of("Leather Pants Panel", "Straps & Buckles", "Stitching")),

        // cloth armor
        Map.entry("Cloth Hood",    List.of("Cloth Hood Panel", "Stitching", "Embellishments")),
        Map.entry("Cloth Tunic",   List.of("Cloth Tunic Panel", "Stitching", "Embellishments")),
        Map.entry("Cloth Gloves",  List.of("Cloth Gloves Panel", "Stitching", "Embellishments")),
        Map.entry("Cloth Pants",   List.of("Cloth Pants Panel", "Stitching", "Embellishments"))
    );

    // pre-built component index: type -> tier -> list of item ids, populated once on server start via buildComponentIndex()
    public static final Map<String, Map<Integer, List<String>>> COMPONENT_INDEX = new ConcurrentHashMap<>();

    // cache of item id to raw CraftingComponent bson — shared with crafting page
    private static final Map<String, BsonDocument> CRAFTING_COMPONENT_CACHE = new ConcurrentHashMap<>();

    // category string -> display name e.g. "Heads_and_Blades" -> "Heads & Blades"
    private static final Map<String, String> COMPONENT_CATEGORY_DISPLAY = new ConcurrentHashMap<>();

    // itemId -> crafting category string e.g. "Weapon_Component_Axe_Head_T1" -> "Heads & Blades"
    public static final Map<String, String> COMPONENT_CATEGORY_INDEX = new ConcurrentHashMap<>();

    // itemId -> list of ingredient display strings e.g. ["4x Ingredient_Bar_Copper"]
    public static final Map<String, List<String>> COMPONENT_RECIPE_INDEX = new ConcurrentHashMap<>();

    // structured recipe inputs cache — populated once at startup, used by salvage page
    public static final Map<String, List<RecipeInput>> COMPONENT_RECIPE_INPUTS = new ConcurrentHashMap<>();

    // record to hold structured recipe input data
    public record RecipeInput(String itemId, String displayName, int quantity) {}

    // converts a bench category string to a display name e.g. "Heads_and_Blades" -> "Heads & Blades"
    private static String categoryToDisplay(@Nonnull String raw) {
        return COMPONENT_CATEGORY_DISPLAY.computeIfAbsent(raw, k -> k.replace("_and_", " & ").replace("_", " "));
    }

    // builds the component index by scanning all Weapon_Component_ and Armor_Component_ assets
    public static void buildComponentIndex() {
        Map<String, Item> allItems = Item.getAssetMap().getAssetMap();
        for (Map.Entry<String, Item> entry : allItems.entrySet()) {
            String id = entry.getKey();
            if (!id.startsWith("Weapon_Component_") && !id.startsWith("Armor_Component_")) continue;

            Path assetPath = Item.getAssetMap().getPath(id);
            if (assetPath == null) continue;

            BsonDocument doc;
            try { doc = BsonDocument.parse(Files.readString(assetPath)); } catch (Exception e) { continue; }

            // cache CraftingComponent block
            BsonValue componentVal = doc.get("CraftingComponent");
            if (componentVal == null || !componentVal.isDocument()) continue;
            BsonDocument component = componentVal.asDocument();
            CRAFTING_COMPONENT_CACHE.put(id, component);

            BsonValue typeVal = component.get("type");
            BsonValue tierVal = component.get("tier");
            if (typeVal == null || !typeVal.isString()) continue;
            if (tierVal == null || !tierVal.isInt32()) continue;

            String type = typeVal.asString().getValue();
            int tier = tierVal.asInt32().getValue();

            COMPONENT_INDEX.computeIfAbsent(type, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(tier, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(id);

            // index crafting category from BenchRequirement[0].Categories[0] — stored as raw e.g. "Heads_and_Blades"
            try {
                BsonValue recipeVal = doc.get("Recipe");
                if (recipeVal != null && recipeVal.isDocument()) {
                    BsonValue benchVal = recipeVal.asDocument().get("BenchRequirement");
                    if (benchVal != null && benchVal.isArray() && !benchVal.asArray().isEmpty()) {
                        BsonDocument benchReq = benchVal.asArray().get(0).asDocument();
                        BsonValue catsVal = benchReq.get("Categories");
                        if (catsVal != null && catsVal.isArray() && !catsVal.asArray().isEmpty()) {
                            COMPONENT_CATEGORY_INDEX.put(id, catsVal.asArray().get(0).asString().getValue());
                        }
                    }
                }
            } catch (Exception ignored) {}

            // index recipe inputs — both formatted display strings and structured data for salvage page
            try {
                BsonDocument recipe = doc.get("Recipe").asDocument();
                List<String> displayInputs = new ArrayList<>();
                List<RecipeInput> structuredInputs = new ArrayList<>();
                for (BsonValue inputVal : recipe.getArray("Input")) {
                    BsonDocument input = inputVal.asDocument();
                    String itemInputId = input.getString("ItemId").getValue();
                    int quantity = input.getInt32("Quantity").getValue();
                    String displayName = itemInputId.replace("Ingredient_", "").replace("_", " ");
                    displayInputs.add(quantity + "x " + displayName);
                    structuredInputs.add(new RecipeInput(itemInputId, displayName, quantity));
                }
                COMPONENT_RECIPE_INDEX.put(id, displayInputs);
                COMPONENT_RECIPE_INPUTS.put(id, structuredInputs);
            } catch (Exception ignored) {}
        }
    }

    // creates a fully built item stack with components, implicits, affixes and gear score. any null component will be randomly selected from the index for that slot type and tier
    @Nullable
    public static ItemStack createItem(@Nonnull String itemId, int gearScore, @Nullable String comp1, @Nullable String comp2, @Nullable String comp3) {
        // get the base item asset
        Item item = Item.getAssetMap().getAsset(itemId);
        if (item == null) return null;

        // derive weapon type and rarity from item id
        String weaponType = deriveItemType(itemId);
        String rarity = deriveRarity(itemId);
        if (weaponType == null || rarity == null) return null;

        // derive tier from item level
        int tier = item.getItemLevel() / 10;

        // get the slot type list for this weapon type
        List<String> slotTypes = ALLOWED_COMPONENTS.get(weaponType);
        if (slotTypes == null) return null;

        // resolve any missing components randomly from the index
        String[] components = {comp1, comp2, comp3};
        for (int i = 0; i < 3; i++) {
            if (components[i] == null) {
                components[i] = randomComponent(slotTypes.get(i), tier);
                if (components[i] == null) return null;
            }
        }

        // build the stack and encode components
        ItemStack stack = new ItemStack(itemId);
        stack = stack.withMetadata("components", Codec.STRING_ARRAY, components);

        // determine 2H multiplier — only applies to weapons, not armor
        boolean isTwoHanded = itemId.startsWith("Weapon_") && !isOneHanded(itemId);
        float twoHandedMultiplier = isTwoHanded ? 2.0f : 1.0f;

        // read implicits from each component and apply them
        List<String> implicitStrings = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            BsonDocument comp = readCraftingComponent(components[i]);
            if (comp == null) continue;
            BsonValue implicitsVal = comp.get("implicits");
            if (implicitsVal == null || !implicitsVal.isArray()) continue;

            for (BsonValue entry : implicitsVal.asArray()) {
                if (!entry.isDocument()) continue;
                BsonDocument implicit = entry.asDocument();
                BsonValue statVal = implicit.get("stat");
                BsonValue minVal = implicit.get("min");
                BsonValue maxVal = implicit.get("max");
                if (statVal == null || minVal == null || maxVal == null) continue;

                double min = minVal.isDouble() ? minVal.asDouble().getValue() : (double) minVal.asInt32().getValue();
                double max = maxVal.isDouble() ? maxVal.asDouble().getValue() : (double) maxVal.asInt32().getValue();
                float value = StatTypeInfo.rollValue((float) min, (float) max) * twoHandedMultiplier;

                String display = statVal.asString().getValue();
                try {
                    StatType stat = StatType.valueOf(statVal.asString().getValue());
                    display = StatTypeInfo.getDisplay(stat, value, value);
                } catch (Exception ignored) {}

                implicitStrings.add(statVal.asString().getValue() + "|" + value + "|" + display);
            }
        }
        stack = stack.withMetadata("implicits", Codec.STRING_ARRAY, implicitStrings.toArray(new String[0]));

        // roll and apply affixes based on rarity
        int affixCount = RARITY_TO_AFFIX_COUNT.getOrDefault(rarity, 0);
        List<String> affixStrings = new ArrayList<>();
        if (affixCount > 0) {
            List<Affix> affixes = AffixPool.randomAffixes(affixCount);
            for (Affix affix : affixes) {
                affix.rollTier(gearScore);
                float affixValue = affix.value() * twoHandedMultiplier;
                affixStrings.add(affix.stat() + "|" + affixValue + "|" + affix.tier());
            }
        }
        stack = stack.withMetadata("affixes", Codec.STRING_ARRAY, affixStrings.toArray(new String[0]));

        // apply gear score from player level
        stack = stack.withMetadata("GearScore", Codec.INTEGER, gearScore);

        return stack;
    }

    // derives the weapon type string from an item id e.g. "Weapon_Axe_Copper_Common" -> "Axe"
    @Nullable
    public static String deriveItemType(@Nonnull String itemId) {
        if (itemId.startsWith("Weapon_")) {
            String stripped = itemId.substring("Weapon_".length());
            int nextUnderscore = stripped.indexOf('_');
            return nextUnderscore > 0 ? stripped.substring(0, nextUnderscore) : stripped;
        } else if (itemId.startsWith("Armor_")) {
            String[] parts = itemId.split("_");
            // piece is second-to-last segment (last is rarity): Head, Chest, Hands, Legs
            String piece = parts[parts.length - 2];
            String material = parts[1]; // Mithril, Leather, Cloth

            return switch (material) {
                case "Leather" -> switch (piece) {
                    case "Head"  -> "Leather Hood";
                    case "Chest" -> "Leather Vest";
                    case "Hands" -> "Leather Gloves";
                    case "Legs"  -> "Leather Pants";
                    default -> null;
                };
                case "Cloth" -> switch (piece) {
                    case "Head"  -> "Cloth Hood";
                    case "Chest" -> "Cloth Tunic";
                    case "Hands" -> "Cloth Gloves";
                    case "Legs"  -> "Cloth Pants";
                    default -> null;
                };
                default -> switch (piece) {
                    case "Head"  -> "Metal Helmet";
                    case "Chest" -> "Metal Chest";
                    case "Hands" -> "Metal Gloves";
                    case "Legs"  -> "Metal Pants";
                    default -> null;
                };
            };
        }
        return null;
    }

    // derives the rarity string from an item id e.g. "Weapon_Axe_Copper_Common" -> "Common"
    @Nullable
    public static String deriveRarity(@Nonnull String itemId) {
        for (String r : RARITY_TO_AFFIX_COUNT.keySet())
            if (itemId.endsWith("_" + r)) return r;
        return null;
    }

    // picks a random component item id from the index for the given type and tier
    @Nullable
    private static String randomComponent(@Nonnull String type, int tier) {
        Map<Integer, List<String>> tierMap = COMPONENT_INDEX.get(type);
        if (tierMap == null) return null;
        List<String> candidates = tierMap.get(tier);
        if (candidates == null || candidates.isEmpty()) return null;
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    // reads and caches the CraftingComponent block from a raw asset json
    @Nullable
    public static BsonDocument readCraftingComponent(@Nonnull String itemId) {
        return CRAFTING_COMPONENT_CACHE.computeIfAbsent(itemId, id -> {
            Path assetPath = Item.getAssetMap().getPath(id);
            if (assetPath == null) return null;
            try {
                BsonDocument doc = BsonDocument.parse(Files.readString(assetPath));
                BsonValue component = doc.get("CraftingComponent");
                return (component != null && component.isDocument()) ? component.asDocument() : null;
            } catch (Exception e) {
                return null;
            }
        });
    }

    // returns a list of weapon damage implicit strings from an item stack's metadata - format of each entry: "STAT_TYPE|value|display"
    @Nonnull
    public static List<String> getWeaponDamageImplicits(@Nonnull ItemStack stack) {
        String[] implicits = stack.getFromMetadataOrNull("implicits", Codec.STRING_ARRAY);
        if (implicits == null) return Collections.emptyList();

        List<String> result = new ArrayList<>();
        for (String implicit : implicits) {
            String[] parts = implicit.split("\\|");
            if (parts.length < 3) continue;
            try {
                StatType stat = StatType.valueOf(parts[0]);
                if (StatTypeInfo.isWeaponDamageStat(stat)) result.add(implicit);
            } catch (Exception ignored) {}
        }
        return result;
    }

    // returns true if (Utility.Compatible == true in its asset json, checks parent if not found on child)
    public static boolean isOneHanded(@Nonnull String itemId) {
        Path assetPath = Item.getAssetMap().getPath(itemId);
        if (assetPath == null) return false;
        try {
            BsonDocument doc = BsonDocument.parse(Files.readString(assetPath));

            // check child first — if found, use it regardless of parent
            BsonValue utilityVal = doc.get("Utility");
            if (utilityVal != null && utilityVal.isDocument()) {
                BsonValue compatibleVal = utilityVal.asDocument().get("Compatible");
                if (compatibleVal != null && compatibleVal.isBoolean())
                    return compatibleVal.asBoolean().getValue();
            }

            // not found on child — walk up to parent if one exists
            BsonValue parentVal = doc.get("Parent");
            if (parentVal == null || !parentVal.isString()) return false;
            String parentId = parentVal.asString().getValue();

            // Get the parent asset
            Path parentPath = Item.getAssetMap().getPath(parentId);
            if (parentPath == null) return false;

            // Read the parent asset and check the utility
            BsonDocument parentDoc = BsonDocument.parse(Files.readString(parentPath));
            BsonValue parentUtilityVal = parentDoc.get("Utility");
            if (parentUtilityVal == null || !parentUtilityVal.isDocument()) return false;

            // get the utility value
            BsonValue compatibleVal = parentUtilityVal.asDocument().get("Compatible");
            return compatibleVal != null && compatibleVal.isBoolean() && compatibleVal.asBoolean().getValue();

        } catch (Exception e) {
            return false;
        }
    }
}