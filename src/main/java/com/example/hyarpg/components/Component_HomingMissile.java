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

    // damage type & value
    public final String damageType;
    public final float damageValue;

    // aoe damage values
    public final float aoeDamageRange;
    public final float aoeDamageHeight;

    // radians per second to steer toward target
    public final float turnRate;

    // seconds before homing activates — lets expression of movement play out first
    public float armTime;

    // track how long the projectile has been slow or stationary, gets removed if sits for too long
    public float slowTime = 0f;

    // track if this projectile has had collision turned off or not
    public boolean blockCollisionDisabled = false;

    public static final BuilderCodec<Component_HomingMissile> CODEC = BuilderCodec.builder(Component_HomingMissile.class, Component_HomingMissile::new).build();

    public static ComponentType<EntityStore, Component_HomingMissile> getComponentType() {
        return HyARPGPlugin.getInstance().componentTypeHomingMissile;
    }

    public Component_HomingMissile(Ref<EntityStore> casterRef, Ref<EntityStore> targetRef, float turnRate, float armTime, String damageType, float damageValue, float aoeDamageRange, float aoeDamageHeight) {
        this.casterRef = casterRef;
        this.targetRef = targetRef;
        this.turnRate = turnRate;
        this.armTime = armTime;
        this.aoeDamageRange = aoeDamageRange;
        this.aoeDamageHeight = aoeDamageHeight;
        this.damageType = damageType;
        this.damageValue = damageValue;
    }

    public Component_HomingMissile(Ref<EntityStore> casterRef, Ref<EntityStore> targetRef, float turnRate, float armTime, String damageType, float damageValue) {
        this.casterRef = casterRef;
        this.targetRef = targetRef;
        this.turnRate = turnRate;
        this.armTime = armTime;
        this.aoeDamageRange = 0f;
        this.aoeDamageHeight = 0f;
        this.damageType = damageType;
        this.damageValue = damageValue;
    }

    // codec no-arg constructor — fields set at spawn time, not persisted
    private Component_HomingMissile() {
        this.targetRef = null;
        this.casterRef = null;
        this.turnRate = 5.0f;
        this.armTime = 0.35f;
        this.damageType = "Physical";
        this.damageValue = 1;
        this.aoeDamageRange = 0f;
        this.aoeDamageHeight = 0f;
    }

    @Override
    public Component<EntityStore> clone() {
        return new Component_HomingMissile(casterRef, targetRef, turnRate, armTime, damageType, damageValue, aoeDamageRange, aoeDamageHeight);
    }
}