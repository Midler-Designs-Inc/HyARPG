package com.example.hyarpg.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CubeCombineRecipeList {

    // a single recipe: a required set of item ids with quantities, and the output item id + quantity
    public record Recipe(Map<String, Integer> inputs, String outputItemId, int outputQuantity) {

        // score how well a given set of item counts matches this recipe
        public double score(Map<String, Integer> provided) {
            // a foreign item in the grid means this recipe is not a candidate
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

        // true only when all requirements are fully satisfied — partial matches can score but cannot actually craft
        public boolean isSatisfied(Map<String, Integer> provided) {
            for (Map.Entry<String, Integer> req : inputs.entrySet()) {
                if (provided.getOrDefault(req.getKey(), 0) < req.getValue()) return false;
            }
            return true;
        }
    }

    // builder for readable recipe declarations
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

    // all registered cube recipes
    private static final List<Recipe> RECIPES = new ArrayList<>();

    static {
        // 3 shard dust -> 1 shard dust of higher tier
        RECIPES.add(new RecipeBuilder()
            .requires("Ingredient_Uncommon_Shard_Dust", 3)
            .outputs("Ingredient_Rare_Shard_Dust")
            .build());
        RECIPES.add(new RecipeBuilder()
            .requires("Ingredient_Rare_Shard_Dust", 3)
            .outputs("Ingredient_Epic_Shard_Dust")
            .build());
        RECIPES.add(new RecipeBuilder()
            .requires("Ingredient_Epic_Shard_Dust", 3)
            .outputs("Ingredient_Legendary_Shard_Dust")
            .build());

        // 3 shards -> 1 shard of higher tier
        RECIPES.add(new RecipeBuilder()
            .requires("Uncommon_Shards", 3)
            .outputs("Rare_Shards")
            .build());
        RECIPES.add(new RecipeBuilder()
            .requires("Rare_Shards", 3)
            .outputs("Epic_Shards")
            .build());
        RECIPES.add(new RecipeBuilder()
            .requires("Epic_Shards", 3)
            .outputs("Legendary_Shards")
            .build());
    }

    // find the best-matching recipe for the given item counts — returns null if nothing scores above zero
    public static Recipe findBestMatch(Map<String, Integer> provided) {
        Recipe best = null;
        double bestScore = 0.0;
        for (Recipe recipe : RECIPES) {
            double score = recipe.score(provided);
            if (score > bestScore) {
                bestScore = score;
                best = recipe;
            }
        }
        return best;
    }

    // find the best-matching recipe that is also fully satisfiable — returns null if no recipe can be completed
    public static Recipe findCraftable(Map<String, Integer> provided) {
        Recipe best = null;
        double bestScore = 0.0;
        for (Recipe recipe : RECIPES) {
            if (!recipe.isSatisfied(provided)) continue;
            double score = recipe.score(provided);
            if (score > bestScore) {
                bestScore = score;
                best = recipe;
            }
        }
        return best;
    }

    public static List<Recipe> getAll() { return List.copyOf(RECIPES); }
}
