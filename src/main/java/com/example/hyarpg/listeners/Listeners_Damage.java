package com.example.hyarpg.listeners;

// Hytale Imports
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.events.Event_EntityDamaged;
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
            // loop over all players and broadcast the message
            for (PlayerRef player : Universe.get().getPlayers()) {
                player.sendMessage(Message.raw("Something just took damage"));
            }

            ModEventBus.post(new Event_EntityDamaged(attacker, defender, store, commandBuffer, damage));
        }
    }

    // determines what entities get picked up, (filtering for NPCs and Players)
    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            NPCEntity.getComponentType(),
            PlayerRef.getComponentType()
        );
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

}