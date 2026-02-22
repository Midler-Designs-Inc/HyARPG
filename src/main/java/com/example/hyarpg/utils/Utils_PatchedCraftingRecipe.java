package com.example.hyarpg.utils;

// Hytale Imports
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;

public class Utils_PatchedCraftingRecipe extends CraftingRecipe {
    public Utils_PatchedCraftingRecipe(CraftingRecipe other) {
        super(other);
    }

    public void setKnowledgeRequired(boolean required) {
        this.knowledgeRequired = required;
    }
}