package com.example.hyarpg.listeners;

// Hytale Imports
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.events.Event_PlayerDeath;

// Java Imports
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class Listeners_Death extends DeathSystems.OnDeathSystem {
    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            PlayerRef.getComponentType()
        );
    }

    // on ComponentAdded Method *required
    @Override
    public void onComponentAdded(
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl DeathComponent deathComponent,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl CommandBuffer<EntityStore> commandBuffer
    ) {
        // Resolve PlayerRef from the entity
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return;
        playerRef.sendMessage(Message.raw("comp added??"));
    }

    // fired when an entity dies and has the player ref component removed
    @Override
    public void onComponentRemoved(
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl DeathComponent component,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl CommandBuffer<EntityStore> commandBuffer
    ) {
        // Resolve PlayerRef from the entity
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return;

        // Post a custom event to your internal mod bus
        ModEventBus.post(new Event_PlayerDeath(playerRef));
    }
}