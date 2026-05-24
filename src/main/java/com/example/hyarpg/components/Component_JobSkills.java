package com.example.hyarpg.components;

// Hytale Imports
import com.example.hyarpg.HyARPGPlugin;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.configs.ModConfig;
import com.hypixel.hytale.server.core.util.EventTitleUtil;

public class Component_JobSkills implements Component<EntityStore> {

    // XP curve constants pulled from config — level is always derived from these, never stored
    private final int xpToFirstLevel     = ModConfig.get().experience.xp_to_first_job_level;
    private final float xpPerLevelModifier = ModConfig.get().experience.xp_increase_per_job_level_modifier;

    // persisted XP pool per job — level is derived on the fly via calculateLevelFromXP()
    public int alchemyXp      = 0;
    public int barteringXp    = 0;
    public int beastmasteryXp = 0;
    public int buildingXp     = 0;
    public int cookingXp      = 0;
    public int craftingXp     = 0;
    public int exploringXp    = 0;
    public int farmingXp      = 0;
    public int fishingXp      = 0;
    public int loggingXp      = 0;
    public int miningXp       = 0;
    public int performingXp   = 0;
    public int thieveryXp     = 0;

    // persisted component data — one codec entry per job XP pool
    public static final BuilderCodec<Component_JobSkills> CODEC = BuilderCodec.builder(Component_JobSkills.class, Component_JobSkills::new)
        .append(new KeyedCodec<>("AlchemyXp",      Codec.INTEGER), (c, v) -> c.alchemyXp      = v, c -> c.alchemyXp).add()
        .append(new KeyedCodec<>("BarteringXp",    Codec.INTEGER), (c, v) -> c.barteringXp    = v, c -> c.barteringXp).add()
        .append(new KeyedCodec<>("BeastmasteryXp", Codec.INTEGER), (c, v) -> c.beastmasteryXp = v, c -> c.beastmasteryXp).add()
        .append(new KeyedCodec<>("BuildingXp",     Codec.INTEGER), (c, v) -> c.buildingXp     = v, c -> c.buildingXp).add()
        .append(new KeyedCodec<>("CookingXp",      Codec.INTEGER), (c, v) -> c.cookingXp      = v, c -> c.cookingXp).add()
        .append(new KeyedCodec<>("CraftingXp",     Codec.INTEGER), (c, v) -> c.craftingXp     = v, c -> c.craftingXp).add()
        .append(new KeyedCodec<>("ExploringXp",    Codec.INTEGER), (c, v) -> c.exploringXp    = v, c -> c.exploringXp).add()
        .append(new KeyedCodec<>("FarmingXp",      Codec.INTEGER), (c, v) -> c.farmingXp      = v, c -> c.farmingXp).add()
        .append(new KeyedCodec<>("FishingXp",      Codec.INTEGER), (c, v) -> c.fishingXp      = v, c -> c.fishingXp).add()
        .append(new KeyedCodec<>("LoggingXp",      Codec.INTEGER), (c, v) -> c.loggingXp      = v, c -> c.loggingXp).add()
        .append(new KeyedCodec<>("MiningXp",       Codec.INTEGER), (c, v) -> c.miningXp       = v, c -> c.miningXp).add()
        .append(new KeyedCodec<>("PerformingXp",   Codec.INTEGER), (c, v) -> c.performingXp   = v, c -> c.performingXp).add()
        .append(new KeyedCodec<>("ThieveryXp",     Codec.INTEGER), (c, v) -> c.thieveryXp     = v, c -> c.thieveryXp).add()
        .build();

    // default no-arg constructor (required for component registration)
    public Component_JobSkills() {}

    public int calculateLevelFromXP(int xp) {
        if (xp <= 0) return 1;
        double growthFactor = 1 + xpPerLevelModifier;
        double epsilon = 1e-6;
        double L = Math.log(1 + (xp * xpPerLevelModifier) / (double) xpToFirstLevel + epsilon) / Math.log(growthFactor);
        return (int) Math.floor(L) + 1;
    }

    // calculate total XP required to reach the start of a specific level
    public int calculateTotalXPForLevel(int targetLevel) {
        double growthFactor = 1 + xpPerLevelModifier;
        double total = xpToFirstLevel * (Math.pow(growthFactor, targetLevel - 1) - 1) / xpPerLevelModifier;
        return (int) Math.max(0, Math.round(total));
    }

    // calculate XP progress within the current level as a 0-1 float
    public float calculateLevelProgress(int xp) {
        int currentLevel      = calculateLevelFromXP(xp);
        int xpForCurrentLevel = calculateTotalXPForLevel(currentLevel);
        int xpForNextLevel    = calculateTotalXPForLevel(currentLevel + 1);
        double progress = (double)(xp - xpForCurrentLevel) / (xpForNextLevel - xpForCurrentLevel);
        return (float) Math.max(0.0, Math.min(1.0, progress));
    }

    // award XP to a job, fire a level-up notification for each level gained, and return the updated XP total
    public void awardXP(PlayerRef playerRef, String jobId, int xpGained) {
        // get applicable variables/values
        int currentXp   = getXP(jobId);
        int levelBefore = calculateLevelFromXP(currentXp);
        int newXP       = currentXp + xpGained;
        int levelAfter  = calculateLevelFromXP(newXP);

        // notify if the player gained at least one level
        if (levelAfter > levelBefore) {
            EventTitleUtil.showEventTitleToPlayer(
                playerRef,
                Message.raw(String.valueOf(levelAfter)),
                Message.raw(jobId.toUpperCase() + " LEVEL UP"),
                true
            );
        }

        // update the xp value of the applicable job
        setXP(jobId, newXP);
    }

    // get a job's xp by ID
    public int getXP(String jobId) {
        return switch (jobId) {
            case "Alchemy"      -> alchemyXp;
            case "Bartering"    -> barteringXp;
            case "Beastmastery" -> beastmasteryXp;
            case "Building"     -> buildingXp;
            case "Cooking"      -> cookingXp;
            case "Crafting"     -> craftingXp;
            case "Exploring"    -> exploringXp;
            case "Farming"      -> farmingXp;
            case "Fishing"      -> fishingXp;
            case "Logging"      -> loggingXp;
            case "Mining"       -> miningXp;
            case "Performing"   -> performingXp;
            case "Thievery"     -> thieveryXp;
            default -> 0;
        };
    }

    // set a job's xp by ID
    public void setXP(String jobId, int xp) {
        switch (jobId) {
            case "Alchemy"      -> alchemyXp      = xp;
            case "Bartering"    -> barteringXp    = xp;
            case "Beastmastery" -> beastmasteryXp = xp;
            case "Building"     -> buildingXp     = xp;
            case "Cooking"      -> cookingXp      = xp;
            case "Crafting"     -> craftingXp     = xp;
            case "Exploring"    -> exploringXp    = xp;
            case "Farming"      -> farmingXp      = xp;
            case "Fishing"      -> fishingXp      = xp;
            case "Logging"      -> loggingXp      = xp;
            case "Mining"       -> miningXp       = xp;
            case "Performing"   -> performingXp   = xp;
            case "Thievery"     -> thieveryXp     = xp;
        }
    }

    public void resetAllXP() {
        alchemyXp = barteringXp = beastmasteryXp = buildingXp = cookingXp = craftingXp =
                exploringXp = farmingXp = fishingXp = loggingXp = miningXp = performingXp = thieveryXp = 0;
    }

    // built in method for returning component type for this component defined on the main plugin class
    public static ComponentType<EntityStore, Component_JobSkills> getComponentType() {
        return HyARPGPlugin.getInstance().componentTypeJobSkills;
    }

    // required for Hytale ECS system
    @Override
    public Component<EntityStore> clone() {
        return new Component_JobSkills();
    }
}