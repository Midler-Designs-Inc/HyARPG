package com.example.hyarpg.components;

// Hytale Imports
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class Component_Hunger implements Component<EntityStore> {
    public float value;        // Current hunger
    public float max;          // Max hunger
    public float drainRate;    // Hunger lost per second
    public float accumulator;  // Internal timer for tick accumulation

    // Default no-arg constructor (required for component registration)
    public Component_Hunger() {
        this(120f, 120f, 0.5f);  // Default values
    }

    // Constructor
    public Component_Hunger(float max, float initialValue, float drainRate) {
        this.max = max;
        this.value = initialValue;
        this.drainRate = drainRate;
        this.accumulator = 0f;
    }

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

    // check if the player is starving (hunger is 0)
    public boolean isStarving() {
        return value <= 0f;
    }

    // required for Hytale ECS system
    @Override
    public Component<EntityStore> clone() {
        Component_Hunger copy = new Component_Hunger(max, value, drainRate);
        copy.accumulator = this.accumulator;
        return copy;
    }
}
