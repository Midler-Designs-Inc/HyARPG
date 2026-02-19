package com.example.hyarpg.components;

// Hytale Imports
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Java Imports
import java.util.Random;

public class Component_RPG_Enemy implements Component<EntityStore> {
    Random random = new Random();

    // Constructor properties
    public int level;
    public int monsterRarity = 0;

    // Register properties that needs to get persisted (none, just register codec in this case)
    public static final BuilderCodec<Component_RPG_Enemy> CODEC = BuilderCodec.builder( Component_RPG_Enemy.class, Component_RPG_Enemy::new ).build();

    // Default no-arg constructor (required for component registration)
    public Component_RPG_Enemy() { this(1); }

    // Constructor
    public Component_RPG_Enemy(int level) {
        this.level = level;
        this.monsterRarity = rollMonsterRarity();
    }

    // Randomly roll monster rarity
    public int rollMonsterRarity () {
        int rarityInt = 0;
        double roll = random.nextDouble(); // 0.0 <= roll < 1.0

        if (roll < 0.60) rarityInt = 0; // 60% chance
        else if (roll < 0.96) rarityInt = 1; // next 36%
        else if (roll < 0.99) rarityInt = 2; // next 3%
        else rarityInt = 3; // remaining 1%

        return rarityInt;
    }

    // Get the intended glow effect for the monster rarity
    public String getRarityString () {
        return switch (monsterRarity) {
            case 1 -> "Magical";
            case 2 -> "Rare";
            case 3 -> "Elite";
            default -> "";
        };
    }

    // required for Hytale ECS system
    @Override
    public Component<EntityStore> clone() {
        Component_RPG_Enemy copy = new Component_RPG_Enemy(level);
        return copy;
    }
}
