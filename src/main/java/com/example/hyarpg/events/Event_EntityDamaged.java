package com.example.hyarpg.events;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class Event_EntityDamaged {
    private final Ref<EntityStore> attacker;
    private final Ref<EntityStore> defender;
    private final Store<EntityStore> store;
    private final CommandBuffer<EntityStore> commandBuffer;
    private final Damage damage;

    public Event_EntityDamaged(Ref<EntityStore> attacker, Ref<EntityStore> defender, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer, Damage damage) {
        this.attacker = attacker;
        this.defender = defender;
        this.store = store;
        this.commandBuffer = commandBuffer;
        this.damage = damage;
    }

    public Ref<EntityStore> getAttacker() { return attacker; }

    public Ref<EntityStore> getDefender() { return defender; }

    public Store<EntityStore> getStore() { return store; }

    public CommandBuffer<EntityStore> getCommandBuffer() { return commandBuffer; }

    public Damage getDamage() { return damage; }
}
