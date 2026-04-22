package com.example.hyarpg.components;

// Hytale Imports
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class Component_Hunger implements Component<EntityStore> {
    public float value = 120f;        // Current hunger
    public float max = 120f;          // Max hunger

    public static final BuilderCodec<Component_Hunger> CODEC = BuilderCodec.builder(
        Component_Hunger.class, Component_Hunger::new
    )
    .append(new KeyedCodec<>("HungerLevel", Codec.FLOAT),
        ((comp, value) -> comp.value = value),
        comp -> comp.value
    )
    .add()
    .build();

    // Constructor
    public Component_Hunger() {}

    // get current hunger percent of max hunger
    public float getPercentage() {
        return value / max;
    }

    // restore hunger value
    public void restore(float amount) {
        value = Math.min(value + amount, max);
    }

    // drain hunger value
    public void drain(float amount) {
        value = Math.max(value - amount, 0f);
    }

    // set hunger value
    public void setOnDeath() {
        value = max * .5f;
    }

    // check if the player is starving (hunger is 0)
    public boolean isStarving() {
        return value <= 0f;
    }

    // required for Hytale ECS system
    @Override
    public Component<EntityStore> clone() {
        Component_Hunger copy = new Component_Hunger();
        return copy;
    }
}
