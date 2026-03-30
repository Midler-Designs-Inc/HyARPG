package com.example.hyarpg.listeners;

// Hytale Imports
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

// Mod Imports
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.events.Event_PlaceBlock;

// Java Imports
import javax.annotation.Nonnull;

public class Listeners_PlaceBlock extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

    public Listeners_PlaceBlock() {
        super(PlaceBlockEvent.class);
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull PlaceBlockEvent event) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        ModEventBus.post(new Event_PlaceBlock(ref, store, commandBuffer, event));
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}