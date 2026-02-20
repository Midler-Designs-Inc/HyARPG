package com.example.hyarpg.listeners;

// Hytale Imports
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.events.Event_EntityPreDamaged;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

// Java Imports
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class Listeners_Damage extends DamageEventSystem {

    @Override
    public void handle(int i, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer, @NonNullDecl Damage damage) {
        // if the damage was canceled do nothing
        if (damage.isCancelled()) return;

        // get the defender and make sure there is a relevant damage source, otherwise do nothing
        Ref<EntityStore> defender = archetypeChunk.getReferenceTo(i);
        Damage.Source source = damage.getSource();
        if (source instanceof Damage.EntitySource entitySource) {
            Ref<EntityStore> attacker = entitySource.getRef();
            if (!attacker.isValid()) return;

            ModEventBus.post(new Event_EntityPreDamaged(attacker, defender, store, commandBuffer, damage));
        }
    }

    // determines what entities get picked up, (filtering for NPCs and Players)
    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.or(
            NPCEntity.getComponentType(),
            PlayerRef.getComponentType()
        );
    }

    // this group get here let's the event fire pre damage, removing this makes the event fire post damage
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

}