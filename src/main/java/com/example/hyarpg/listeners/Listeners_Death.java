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
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.events.Event_NPCDeath;
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.events.Event_PlayerDeath;

// Java Imports
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class Listeners_Death extends DeathSystems.OnDeathSystem {
    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.or(
            NPCEntity.getComponentType(),
            PlayerRef.getComponentType()
        );
    }

    // fired when something dies
    @Override
    public void onComponentAdded(
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl DeathComponent deathComponent,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl CommandBuffer<EntityStore> commandBuffer
    ) {
        // Resolve PlayerRef from the entity
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        NPCEntity npcEntity = store.getComponent(ref, NPCEntity.getComponentType());

        // broadcast player death
        if (playerRef != null)
            ModEventBus.post(new Event_PlayerDeath(ref, store));
        else if (npcEntity != null)
            ModEventBus.post(new Event_NPCDeath(ref, store));
    }

    // fired when player hits respawn
    @Override
    public void onComponentRemoved(
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl DeathComponent component,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl CommandBuffer<EntityStore> commandBuffer
    ) {}
}