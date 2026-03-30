package com.example.hyarpg.events;

// Hytale Imports
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record Event_BreakBlock(
        Ref<EntityStore> ref,
        Store<EntityStore> store,
        CommandBuffer<EntityStore> commandBuffer,
        BreakBlockEvent event
) {}
