package com.example.hyarpg.components;

// Hytale Imports
import com.example.hyarpg.HyARPGPlugin;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class Component_Simulacrum implements Component<EntityStore> {

    // the player who cast the simulacrum — used as caster ref for missile damage
    public final Ref<EntityStore> casterRef;

    // countdown until next missile volley
    public float missileTimer = 5.0f;

    public static final BuilderCodec<Component_Simulacrum> CODEC = BuilderCodec.builder(Component_Simulacrum.class, Component_Simulacrum::new).build();

    public static ComponentType<EntityStore, Component_Simulacrum> getComponentType() {
        return HyARPGPlugin.getInstance().componentTypeSimulacrum;
    }

    public Component_Simulacrum(Ref<EntityStore> casterRef) {
        this.casterRef = casterRef;
    }

    private Component_Simulacrum() {
        this.casterRef = null;
    }

    @Override
    public Component<EntityStore> clone() {
        return new Component_Simulacrum(casterRef);
    }
}