package com.example.hyarpg.events;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.event.events.ecs.CraftRecipeEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class Event_PlayerCrafted {

    private final Ref<EntityStore> playerRef;
    private final Store<EntityStore> store;
    private final CommandBuffer<EntityStore> commandBuffer;
    private final CraftRecipeEvent.Post craftEvent;

    public Event_PlayerCrafted(Ref<EntityStore> playerRef,
       Store<EntityStore> store,
       CommandBuffer<EntityStore> commandBuffer,
       CraftRecipeEvent.Post craftEvent
    ) {
        this.playerRef = playerRef;
        this.store = store;
        this.commandBuffer = commandBuffer;
        this.craftEvent = craftEvent;
    }

    public Ref<EntityStore> getPlayerRef() {
        return playerRef;
    }

    public Store<EntityStore> getStore() {
        return store;
    }

    public CommandBuffer<EntityStore> getCommandBuffer() {
        return commandBuffer;
    }

    public CraftingRecipe getRecipe() {
        return craftEvent.getCraftedRecipe();
    }

    public String getCraftedItemId() {
        return craftEvent.getCraftedRecipe().getPrimaryOutput().getItemId();
    }

    public int getQuantity() {
        return craftEvent.getQuantity();
    }
}