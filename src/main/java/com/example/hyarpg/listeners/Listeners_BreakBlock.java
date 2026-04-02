package com.example.hyarpg.listeners;

// Hytale Imports
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.events.Event_BreakBlock;

// Java
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import javax.annotation.Nonnull;

public class Listeners_BreakBlock extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    public Listeners_BreakBlock() {
        super(BreakBlockEvent.class);
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull BreakBlockEvent event) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        ModEventBus.post(new Event_BreakBlock(ref, store, commandBuffer, event));
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
//        return PlayerRef.getComponentType();
    }
}