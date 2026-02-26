package com.example.hyarpg.components;

// Hytale Imports
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class Component_Thirst implements Component<EntityStore> {
    public float value = 120f;        // Current thirst
    public float max = 120f;          // Max thirst
    public float drainRate = 0.02f;    // Thirst lost per second

    public static final BuilderCodec<Component_Thirst> CODEC = BuilderCodec.builder(
        Component_Thirst.class, Component_Thirst::new
    )
        .append(new KeyedCodec<>("Thirst", Codec.FLOAT),
            ((comp, value) -> comp.value = value),
            comp -> comp.value
        )
        .add()
        .build();

    // Constructor
    public Component_Thirst() {}

    // get current thirst percent of max thirst
    public float getPercentage() {
        return value / max;
    }

    // restore thirst value
    public void restore(float amount) {
        value = Math.min(value + amount, max);
    }

    // drain thirst value
    public void drain(float amount) {
        value = Math.max(value - amount, 0f);
    }

    // set thirst value
    public void setOnDeath() {
        value = max * .5f;
    }

    // check if the player is starving (thirst is 0)
    public boolean isStarving() {
        return value <= 0f;
    }

    // required for Hytale ECS system
    @Override
    public Component<EntityStore> clone() {
        Component_Thirst copy = new Component_Thirst();
        return copy;
    }
}
