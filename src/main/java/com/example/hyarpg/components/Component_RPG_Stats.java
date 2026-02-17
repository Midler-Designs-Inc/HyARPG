package com.example.hyarpg.components;

// Hytale Imports
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class Component_RPG_Stats implements Component<EntityStore> {
    // Constructor properties
    public int level;
    public double xp;
    public int skillPoints;

    // Constant properties
    public final int xpToFirstLevel = 10;
    public final float xpPerLevelModifier = 0.1f;
    public final int xpGainedFromEqualLevelMonster = 1;

    // Register properties that needs to get persisted
    public static final BuilderCodec<Component_RPG_Stats> CODEC = BuilderCodec.builder(
            Component_RPG_Stats.class, Component_RPG_Stats::new
        )
        .append(new KeyedCodec<>("RPGStatsLevel", Codec.INTEGER),
            ((comp, value) -> comp.level = value),
            comp -> comp.level
        ).add()
        .append(new KeyedCodec<>("RPGStatsXP", Codec.DOUBLE),
                ((comp, value) -> comp.xp = value),
                comp -> comp.xp
        ).add()
        .append(new KeyedCodec<>("RPGStatsSkillPoints", Codec.INTEGER),
                ((comp, value) -> comp.skillPoints = value),
                comp -> comp.skillPoints
        ).add()
        .build();

    // Default no-arg constructor (required for component registration)
    public Component_RPG_Stats() {
        this(1, 0, 0);
    }

    // Constructor
    public Component_RPG_Stats(
        int level,
        double xp,
        int skillPoints
    ) {
        this.level = level;
        this.xp = xp;
        this.skillPoints = skillPoints;
    }

    // Method to get how much more XP is needed to hit the next level
    public double calculateXPRequiredToLevelUp() {
        // Step 1: Calculate cumulative XP required for each level up to next
        double startingXP = xpToFirstLevel;
        double cumulativeXP = xpToFirstLevel;

        // loop over each level between level 2 and the players level
        for (int lvl = 2; lvl <= level; lvl++) {
            startingXP += startingXP * xpPerLevelModifier;
            cumulativeXP += startingXP;
        }

        // Step 2: XP required for next level = cumulative XP up to next level
        double requiredXP = cumulativeXP;

        // Step 3: Remaining XP = next level threshold - current XP
        return Math.max(0, requiredXP - xp);
    }

    // Method to get the players current level based on their total XP
    public int  calculateLevelFromXP() {
        // if current XP is less than what's required to hit level 1 just return 1
        if (xp < xpToFirstLevel) return 1;
        double N = Math.log(1 + (xp * xpPerLevelModifier) / xpToFirstLevel) / Math.log(1 + xpPerLevelModifier);

        // Convert to int for the player's level
        return (int) Math.floor(N) + 1;
    }

    // required for Hytale ECS system
    @Override
    public Component<EntityStore> clone() {
        Component_RPG_Stats copy = new Component_RPG_Stats(level, xp, skillPoints);
        return copy;
    }
}
