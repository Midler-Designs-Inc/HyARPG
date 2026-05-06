package com.example.hyarpg.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CubeCombineRecipeList {

    // gear category string — same as salvage page
    private static final String GEAR_CATEGORY = "Items.HyARPG.Gear";

    // what kind of modification a modifier recipe performs — extend to add new rune types
    public enum ModifierType {
        POWER_UP,            // sets gear score to min(playerLevel, runeMaxLevel)
        REROLL_AFFIX_VALUES, // rerolls existing affix values within their tier ranges
        REROLL_AFFIXES       // replaces affixes entirely with new random rolls
    }

    // a standard combine recipe — consumes inputs and produces a new output item
    public record Recipe(Map<String, Integer> inputs, String outputItemId, int outputQuantity) {

        // score how well a given set of item counts matches this recipe
        // any foreign item immediately disqualifies the recipe
        public double score(Map<String, Integer> provided) {
            for (String itemId : provided.keySet()) {
if (!inputs.containsKey(itemId)) return 0.0;
            }
            double total = 0.0;
            for (Map.Entry<String, Integer> req : inputs.entrySet()) {
int have = provided.getOrDefault(req.getKey(), 0);
if (have == 0) return 0.0;
total += Math.min(1.0, (double) have / req.getValue());
            }
            return total / inputs.size();
        }

        // true only when all requirements are fully satisfied
        public boolean isSatisfied(Map<String, Integer> provided) {
            for (Map.Entry<String, Integer> req : inputs.entrySet()) {
if (provided.getOrDefault(req.getKey(), 0) < req.getValue()) return false;
            }
            return true;
        }
    }

    // a modifier recipe — requires exactly 1 gear item + 1 specific rune, modifies the gear in place
    // consumes exactly 1 rune regardless of stack size, gear item stays in its slot with updated metadata
    public record ModifierRecipe(java.util.Set<String> runeItemIds, ModifierType modifierType, int maxLevel) {

        // matches when the grid has exactly 1 gear item, all required runes present, and nothing else
        public boolean matches(Map<String, Integer> provided) {
            int gearCount = 0;
            java.util.Set<String> satisfiedRunes = new java.util.HashSet<>();
            for (String itemId : provided.keySet()) {
if (this.runeItemIds.contains(itemId)) {
    satisfiedRunes.add(itemId);
} else if (isGearItem(itemId)) {
    gearCount++;
} else {
    return false; // foreign item — disqualify
}
            }
            return gearCount == 1 && satisfiedRunes.equals(this.runeItemIds);
        }
    }

    // checks whether an item id belongs to a mod gear item via its asset categories
    public static boolean isGearItem(@javax.annotation.Nonnull String itemId) {
        com.hypixel.hytale.server.core.asset.type.item.config.Item item = com.hypixel.hytale.server.core.asset.type.item.config.Item.getAssetMap().getAsset(itemId);
        if (item == null) return false;
        String[] categories = item.getCategories();
        if (categories == null) return false;
        for (String cat : categories) {
            if (GEAR_CATEGORY.equals(cat)) return true;
        }
        return false;
    }

    // builder for readable combine recipe declarations
    public static class RecipeBuilder {
        private final Map<String, Integer> inputs = new HashMap<>();
        private String outputItemId;
        private int outputQuantity = 1;

        public RecipeBuilder requires(String itemId, int quantity) { inputs.put(itemId, quantity); return this; }
        public RecipeBuilder requires(String itemId) { return requires(itemId, 1); }
        public RecipeBuilder outputs(String itemId, int quantity) { this.outputItemId = itemId; this.outputQuantity = quantity; return this; }
        public RecipeBuilder outputs(String itemId) { return outputs(itemId, 1); }
        public Recipe build() { return new Recipe(Map.copyOf(inputs), outputItemId, outputQuantity); }
    }

    // all registered standard combine recipes
    private static final List<Recipe> RECIPES = new ArrayList<>();

    // all registered modifier recipes
    private static final List<ModifierRecipe> MODIFIER_RECIPES = new ArrayList<>();

    static {
        // 3 shard dust -> 1 shard dust of higher tier
        RECIPES.add(new RecipeBuilder().requires("Ingredient_Uncommon_Shard_Dust", 3).outputs("Ingredient_Rare_Shard_Dust").build());
        RECIPES.add(new RecipeBuilder().requires("Ingredient_Rare_Shard_Dust", 3).outputs("Ingredient_Epic_Shard_Dust").build());
        RECIPES.add(new RecipeBuilder().requires("Ingredient_Epic_Shard_Dust", 3).outputs("Ingredient_Legendary_Shard_Dust").build());

        // 3 shards -> 1 shard of higher tier
        RECIPES.add(new RecipeBuilder().requires("Uncommon_Shards", 3).outputs("Rare_Shards").build());
        RECIPES.add(new RecipeBuilder().requires("Rare_Shards", 3).outputs("Epic_Shards").build());
        RECIPES.add(new RecipeBuilder().requires("Epic_Shards", 3).outputs("Legendary_Shards").build());

        // 3 pickaxes -> 1 pickaxe same tier
        RECIPES.add(new RecipeBuilder().requires("Tool_Pickaxe_Crude", 3).outputs("Tool_Pickaxe_Crude").build());
        RECIPES.add(new RecipeBuilder().requires("Tool_Pickaxe_Copper", 3).outputs("Tool_Pickaxe_Copper").build());
        RECIPES.add(new RecipeBuilder().requires("Tool_Pickaxe_Iron", 3).outputs("Tool_Pickaxe_Iron").build());
        RECIPES.add(new RecipeBuilder().requires("Tool_Pickaxe_Thorium", 3).outputs("Tool_Pickaxe_Thorium").build());
        RECIPES.add(new RecipeBuilder().requires("Tool_Pickaxe_Cobalt", 3).outputs("Tool_Pickaxe_Cobalt").build());
        RECIPES.add(new RecipeBuilder().requires("Tool_Pickaxe_Adamantite", 3).outputs("Tool_Pickaxe_Adamantite").build());
        RECIPES.add(new RecipeBuilder().requires("Tool_Pickaxe_Mithril", 3).outputs("Tool_Pickaxe_Mithril").build());

        // 3 powering runes -> 1 powering rune higher tier
        RECIPES.add(new RecipeBuilder().requires("Fragmented_Rune_Of_Powering", 3).outputs("Minor_Rune_Of_Powering").build());
        RECIPES.add(new RecipeBuilder().requires("Minor_Rune_Of_Powering", 3).outputs("Rune_Of_Powering").build());
        RECIPES.add(new RecipeBuilder().requires("Rune_Of_Powering", 3).outputs("Major_Rune_Of_Powering").build());
        RECIPES.add(new RecipeBuilder().requires("Major_Rune_Of_Powering", 3).outputs("Divine_Rune_Of_Powering").build());

        // powering runes — gear score upgrade, capped at rune's item level
        MODIFIER_RECIPES.add(new ModifierRecipe(java.util.Set.of("Fragmented_Rune_Of_Powering"), ModifierType.POWER_UP, 30));
        MODIFIER_RECIPES.add(new ModifierRecipe(java.util.Set.of("Minor_Rune_Of_Powering"),      ModifierType.POWER_UP, 60));
        MODIFIER_RECIPES.add(new ModifierRecipe(java.util.Set.of("Rune_Of_Powering"),             ModifierType.POWER_UP, 90));
        MODIFIER_RECIPES.add(new ModifierRecipe(java.util.Set.of("Major_Rune_Of_Powering"),       ModifierType.POWER_UP, 120));
        MODIFIER_RECIPES.add(new ModifierRecipe(java.util.Set.of("Divine_Rune_Of_Powering"),      ModifierType.POWER_UP, 999999999));
    }

    // find the best-matching standard recipe for the given item counts
    public static Recipe findBestMatch(Map<String, Integer> provided) {
        Recipe best = null;
        double bestScore = 0.0;
        for (Recipe recipe : RECIPES) {
            double score = recipe.score(provided);
            if (score > bestScore) { bestScore = score; best = recipe; }
        }
        return best;
    }

    // find the best-matching standard recipe that is also fully satisfiable
    public static Recipe findCraftable(Map<String, Integer> provided) {
        Recipe best = null;
        double bestScore = 0.0;
        for (Recipe recipe : RECIPES) {
            if (!recipe.isSatisfied(provided)) continue;
            double score = recipe.score(provided);
            if (score > bestScore) { bestScore = score; best = recipe; }
        }
        return best;
    }

    // find a matching modifier recipe for the given grid contents — returns null if none match
    public static ModifierRecipe findModifierRecipe(Map<String, Integer> provided) {
        for (ModifierRecipe recipe : MODIFIER_RECIPES) {
            if (recipe.matches(provided)) return recipe;
        }
        return null;
    }

    public static List<Recipe> getAll() { return List.copyOf(RECIPES); }
    public static List<ModifierRecipe> getAllModifiers() { return List.copyOf(MODIFIER_RECIPES); }
}