package com.example.hyarpg.components;

// Hytale Imports
import com.example.hyarpg.HyARPGPlugin;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class Component_HomingMissile implements Component<EntityStore> {

    // target to home in on once armed
    public final Ref<EntityStore> targetRef;
    public final Ref<EntityStore> casterRef;

    // radians per second to steer toward target
    public final float turnRate;

    // seconds before homing activates — lets the arc play out first
    public float armTime;

    public float slowTime = 0f;

    public boolean blockCollisionDisabled = false;

    public static final BuilderCodec<Component_HomingMissile> CODEC = BuilderCodec.builder(Component_HomingMissile.class, Component_HomingMissile::new).build();

    public static ComponentType<EntityStore, Component_HomingMissile> getComponentType() {
        return HyARPGPlugin.getInstance().componentTypeHomingMissile;
    }

    public Component_HomingMissile(Ref<EntityStore> casterRef, Ref<EntityStore> targetRef, float turnRate, float armTime) {
        this.casterRef = casterRef;
        this.targetRef = targetRef;
        this.turnRate = turnRate;
        this.armTime = armTime;
    }

    // codec no-arg constructor — fields set at spawn time, not persisted
    private Component_HomingMissile() {
        this.targetRef = null;
        this.casterRef = null;
        this.turnRate = 5.0f;
        this.armTime = 0.35f;
    }

    @Override
    public Component<EntityStore> clone() {
        return new Component_HomingMissile(casterRef, targetRef, turnRate, armTime);
    }
}