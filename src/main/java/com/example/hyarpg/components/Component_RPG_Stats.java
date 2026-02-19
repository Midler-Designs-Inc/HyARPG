package com.example.hyarpg.components;

// Hytale Imports
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;

// Java Imports
import java.util.Random;

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
    public Component_RPG_Stats() { this(1, 0, 0); }

    // Constructor
    public Component_RPG_Stats(
        int level, double xp, int skillPoints
    ) {
        this.level = level;
        this.xp = xp;
        this.skillPoints = skillPoints;
    }

    // Method to get how much total XP is needed to reach a specific level
    public double calculateTotalXPRequiredToReachTargetLevel(int targetLevel) {
        double growthFactor = 1 + xpPerLevelModifier;
        double cumulativeXPToCurrentLevel = xpToFirstLevel * (Math.pow(growthFactor, (targetLevel - 1)) - 1) / xpPerLevelModifier;

        return Math.max(0, Math.round(cumulativeXPToCurrentLevel));
    }

    public double calculateXPRequiredToLevelUp() {
        double growthFactor = 1 + xpPerLevelModifier;
        double cumulativeXPToCurrentLevel = xpToFirstLevel * (Math.pow(growthFactor, level) - 1) / xpPerLevelModifier;

        return Math.max(0, Math.round(cumulativeXPToCurrentLevel - xp));
    }

    // Method to get the players current level based on their total XP
    public int calculateLevelFromXP() {
        if (xp < xpToFirstLevel) return 1;

        double growthFactor = 1 + xpPerLevelModifier;

        // Add tiny epsilon to account for floating point rounding
        double epsilon = 1e-6;
        double L = Math.log(1 + (xp * xpPerLevelModifier) / xpToFirstLevel + epsilon) / Math.log(growthFactor);

        return (int) Math.floor(L);
    }

    // Method to get the percentage of current level as a 0-1 integer
    public float calculateLevelProgress() {
        // XP required to reach the start of the current level and next level
        double xpForCurrentLevelStart = calculateTotalXPRequiredToReachTargetLevel(level);
        double xpForNextLevelStart = calculateTotalXPRequiredToReachTargetLevel(level + 1);

        // Calculate the percentage of the way into the next level 0-1
        double progress = (xp - xpForCurrentLevelStart) / (xpForNextLevelStart - xpForCurrentLevelStart);

        // Clamp just in case
        return (float) Math.max(0.0, Math.min(1.0, progress));
    }

    // Method to award XP
    public void awardXP(int enemyLevel, PlayerRef playerRef) {
        // Calculate level difference (positive if enemy is higher)
        int levelDiff = enemyLevel - level;

        // Clamp level difference between -10 and +10
        if (levelDiff > 10) levelDiff = 10;
        if (levelDiff < -10) levelDiff = -10;

        // Scale factor: -10 → 0%, 0 → 100%, +10 → 300%
        float scaleFactor;
        if (levelDiff >= 0) {
            // Enemy same or higher level: linear scale 100% → 300%
            scaleFactor = 1.0f + (levelDiff / 10.0f) * 2.0f;
        } else {
            // Enemy lower level: linear scale 0% → 100%
            scaleFactor = 1.0f + (levelDiff / 10.0f);
        }

        // Calculate scaled XP and ensure it's not negative
        int xpGained = Math.max(Math.round(xpGainedFromEqualLevelMonster * scaleFactor), 0);

        // apply the XP
        xp += xpGained;

        // while xp required to level up is 0 then level up
        while (calculateXPRequiredToLevelUp() <= 0) {
            levelUp(playerRef);
        }
    }

    // Method to level up
    public void levelUp(PlayerRef playerRef) {
        this.level += 1;
        this.skillPoints += 2;

        // Create the level up messages
        Message smallText = Message.raw("LEVEL UP");
        Message bigText = Message.raw(String.valueOf(level));

        try {
            // Directly show the event title
            EventTitleUtil.showEventTitleToPlayer(
                playerRef,
                bigText,
                smallText,
                true
            );
        } catch (Exception e) {}
    }

    // required for Hytale ECS system
    @Override
    public Component<EntityStore> clone() {
        Component_RPG_Stats copy = new Component_RPG_Stats(level, xp, skillPoints);
        return copy;
    }
}
