package com.example.hyarpg.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class Event_EnemyDamaged {
    private final Ref<EntityStore> attacker;
    private final Ref<EntityStore> defender;
    private final Store<EntityStore> store;

    public Event_EnemyDamaged(Ref<EntityStore> attacker, Ref<EntityStore> defender, Store<EntityStore> store) {
        this.attacker = attacker;
        this.defender = defender;
        this.store = store;
    }

    public Ref<EntityStore> getAttacker() {
        return attacker;
    }

    public Ref<EntityStore> getDefender() {
        return defender;
    }

    public Store<EntityStore> getStore() {
        return store;
    }
}
