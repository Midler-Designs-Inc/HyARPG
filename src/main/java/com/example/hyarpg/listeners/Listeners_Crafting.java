package com.example.hyarpg.listeners;

import com.hypixel.hytale.builtin.crafting.component.CraftingManager;
import com.hypixel.hytale.builtin.crafting.system.PlayerCraftingSystems;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.event.events.ecs.CraftRecipeEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.events.Event_PlayerCrafted;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class Listeners_Crafting extends EntityEventSystem<EntityStore, CraftRecipeEvent.Post> {

    public Listeners_Crafting() {
        super(CraftRecipeEvent.Post.class);
    }

    @Override
    public void handle(int i,
                       @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
                       @NonNullDecl Store<EntityStore> store,
                       @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
                       @NonNullDecl CraftRecipeEvent.Post event
    ) {
        if (event.isCancelled()) return;

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
        CraftingRecipe recipe = event.getCraftedRecipe();
        if (recipe == null || recipe.getPrimaryOutput() == null) return;

        String itemId = recipe.getPrimaryOutput().getItemId();
        if (itemId == null) return;

        ModEventBus.post(new Event_PlayerCrafted(ref, store, commandBuffer, event));
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}