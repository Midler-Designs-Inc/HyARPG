package com.example.hyarpg.utils.items;

// Hytale Imports
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.metadata.ItemDisplayMetadata;
import com.hypixel.hytale.server.core.inventory.ItemStack;

// Mod Imports
import com.example.hyarpg.modules.Module_CombatSystem;
import com.example.hyarpg.utils.StatTypeInfo;
import com.example.hyarpg.utils.affixes.Affix;
import com.example.hyarpg.utils.affixes.AffixPool;
import com.example.hyarpg.utils.affixes.StatType;

// Java Imports
import org.bson.BsonDocument;
import org.bson.BsonValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
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

        // apply custom name/description overrides
        stack = applyDisplayMetadata(stack);

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

    // returns a new item stack with a custom name and description override
    @Nonnull
    private static ItemStack applyDisplayMetadata(@Nonnull ItemStack stack) {
        // read encoded implicits and affixes from stack metadata
        String[] implicitStrings = stack.getFromMetadataOrNull("implicits", Codec.STRING_ARRAY);
        String[] affixStrings    = stack.getFromMetadataOrNull("affixes",   Codec.STRING_ARRAY);

        // compose display name as "{Rarity} {localized item name}" using Message.join
        String translationKey = Objects.requireNonNullElse(stack.getItem().getTranslationProperties().getName(), "Gear");
        Message displayName = Message.translation(translationKey);

        // split implicits into weapon damage (shown bold at top) and regular implicits
        List<String> weaponDamageImplicits = getWeaponDamageImplicits(stack);
        List<String> regularImplicitLines = new ArrayList<>();
        if (implicitStrings != null) {
            for (String implicit : implicitStrings) {
                if (!weaponDamageImplicits.contains(implicit)) {
                    String[] parts = implicit.split("\\|");
                    if (parts.length < 3) continue;
                    regularImplicitLines.add(parts[2]);
                }
            }
        }

        // build bold colored weapon damage message shown above everything else
        Message weaponDamage = Message.empty();
        for (String line : weaponDamageImplicits) {
            // split into [stat, value, display] and skip malformed entries
            String[] parts = line.split("\\|");
            if (parts.length < 3) continue;

            // match stat name against damage type keys to resolve a display color
            String statName = parts[0];
            Color color = null;
            for (Map.Entry<String, Color> entry : Module_CombatSystem.DAMAGE_COLORS.entrySet()) {
                if (statName.contains(entry.getKey().toUpperCase())) {
                    color = entry.getValue();
                    break;
                }
            }

            // build bold message with color if one was resolved
            Message damageLine = Message.raw(parts[2] + "\n").bold(true);
            if (color != null) damageLine = damageLine.color(color);
            weaponDamage = weaponDamage.insert(damageLine);
        }

        // gear score line
        Message gearScoreLine = Message.empty();
        Integer gearScore = stack.getFromMetadataOrNull("GearScore", Codec.INTEGER);
        if (gearScore != null) gearScoreLine = Message.raw("Gear Score: " + gearScore);

        // append regular implicit display strings
        StringBuilder desc = new StringBuilder();
        for (String line : regularImplicitLines) desc.append(line).append("\n");

        // separate affixes from implicits with a blank line if both are present
        if (affixStrings != null) {
            if (!regularImplicitLines.isEmpty()) desc.append("\n");

            // loop over affixes
            for (String affix : affixStrings) {
                // split into [stat, value, tier] and skip malformed entries
                String[] parts = affix.split("\\|");
                if (parts.length < 3) continue;

                // look up the affix by stat name to get its display template
                Affix affixDef = AffixPool.getAffixByStatName(parts[0]);
                if (affixDef == null) continue;

                // parse value and tier
                float value = Float.parseFloat(parts[1]);
                int tier = (int) Float.parseFloat(parts[2]);

                // format display string with bullet and tier in brackets on the right
                String display = affixDef.display().replace("%s", String.format("%.1f", value)).replace("%%", "%");
                desc.append("• [T").append(tier).append("] ").append(display).append("\n");
            }
        }

        // compose final description as weapon damage + gear score + rest
        Message description = weaponDamage
                .insert(gearScoreLine)
                .insert(Message.raw("\n\n" + desc.toString().stripTrailing()));

        // return the updated item stack with the overridden name and description
        return stack.withMetadata(ItemDisplayMetadata.KEYED_CODEC, new ItemDisplayMetadata(displayName, description));
    }

    // returns raw text of a custom description override
    public static String buildSlotDescription(@Nonnull ItemStack stack) {
        String[] implicitStrings = stack.getFromMetadataOrNull("implicits", Codec.STRING_ARRAY);
        String[] affixStrings    = stack.getFromMetadataOrNull("affixes",   Codec.STRING_ARRAY);

        StringBuilder desc = new StringBuilder();

        // gear score
        Integer gearScore = stack.getFromMetadataOrNull("GearScore", Codec.INTEGER);
        if (gearScore != null) desc.append("Gear Score: ").append(gearScore).append("\n\n");

        // regular implicits (skip weapon damage stats)
        if (implicitStrings != null) {
            List<String> weaponDamageImplicits = getWeaponDamageImplicits(stack);
            for (String implicit : implicitStrings) {
                if (weaponDamageImplicits.contains(implicit)) continue;
                String[] parts = implicit.split("\\|");
                if (parts.length < 3) continue;
                desc.append(parts[2]).append("\n");
            }
        }

        // affixes
        if (affixStrings != null) {
            if (desc.length() > 0) desc.append("\n");
            for (String affix : affixStrings) {
                String[] parts = affix.split("\\|");
                if (parts.length < 3) continue;
                Affix affixDef = AffixPool.getAffixByStatName(parts[0]);
                if (affixDef == null) continue;
                float value = Float.parseFloat(parts[1]);
                int tier = (int) Float.parseFloat(parts[2]);
                String display = affixDef.display().replace("%s", String.format("%.1f", value)).replace("%%", "%");
                desc.append("• [T").append(tier).append("] ").append(display).append("\n");
            }
        }

        return desc.toString().stripTrailing();
    }
}