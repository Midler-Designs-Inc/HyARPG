package com.example.hyarpg.components;

// Hytale Imports
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.player.DamageInfo;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelParticle;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;

// Mod Imports
import com.example.hyarpg.utils.affixes.EntityStats;
import com.example.hyarpg.utils.affixes.StatMapper;
import com.example.hyarpg.utils.affixes.StatType;

import static com.example.hyarpg.modules.Module_RPG_System.componentTypeRPGPlayer;

public class Component_RPG_Player implements Component<EntityStore> {
    // Constructor properties
    public int level;
    public double xp;
    public int skillPoints;

    // Constant properties
    public final int xpToFirstLevel = 10;
    public final float xpPerLevelModifier = 0.1f;
    public final int xpGainedFromEqualLevelMonster = 1;

    // players gear score (average of gear score on equipped items)
    public int gearScore = 0;
    public ItemStack mainHandItem;
    public ItemStack offHandItem;

    // stat class to hold affix stats
    public EntityStats stats = new EntityStats();

    // combat trackers
    public long blockStart = System.nanoTime();

    // Register properties that needs to be persisted
    public static final BuilderCodec<Component_RPG_Player> CODEC = BuilderCodec.builder(
            Component_RPG_Player.class, Component_RPG_Player::new
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
    public Component_RPG_Player() { this(1, 0, 0); }

    // Constructor
    public Component_RPG_Player(
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

    // Calculate the players gear score
    public void calculateGearScore(Player player) {
        Inventory inventory = player.getInventory();
        int totalLevel = 0;
        int count = 6;

        // Active hand item (weapon)
        ItemStack inHand = inventory.getItemInHand();
        if (!ItemStack.isEmpty(inHand)) {
            Integer level = inHand.getFromMetadataOrNull("GearScore", Codec.INTEGER);
            if (level != null) totalLevel += level;
        }

        // Off hand item
        ItemStack offHand = inventory.getUtilityItem();
        if (!ItemStack.isEmpty(offHand)) {
            Integer level = offHand.getFromMetadataOrNull("GearScore", Codec.INTEGER);
            if (level != null) totalLevel += level;
        }
        else count--;

        // Armor slots
        ItemContainer armor = inventory.getArmor();
        for (short i = 0; i < armor.getCapacity(); i++) {
            ItemStack armorPiece = armor.getItemStack(i);
            if (ItemStack.isEmpty(armorPiece)) continue;
            Integer level = armorPiece.getFromMetadataOrNull("GearScore", Codec.INTEGER);
            if (level != null) totalLevel += level;
        }

        // return value or 0, whichever is higher
        this.gearScore = Math.max(0, totalLevel / count);
    }

    // Calculate the players stats based on gear affixes
    public void calculateAffixStats(Ref<EntityStore> ref, Store<EntityStore> store) {
        try {
            //get the rpg player comp and player comp
            Player player = store.getComponent(ref, Player.getComponentType());
            if(player == null) return;

            // create a new instance of player stats
            EntityStats newStats = new EntityStats();

            // get the players inventory and armor slots
            Inventory inventory = player.getInventory();
            ItemContainer armor = inventory.getArmor();

            //  Armor Slots
            applyStack(newStats, armor.getItemStack((short) ItemArmorSlot.Head.ordinal()));
            applyStack(newStats, armor.getItemStack((short) ItemArmorSlot.Chest.ordinal()));
            applyStack(newStats, armor.getItemStack((short) ItemArmorSlot.Hands.ordinal()));
            applyStack(newStats, armor.getItemStack((short) ItemArmorSlot.Legs.ordinal()));

            // Weapons / Utility
            applyStack(newStats, inventory.getItemInHand());
            applyStack(newStats, inventory.getUtilityItem());

            // set the new instance of stats
            stats = newStats;

            // Update the player resource values as necessary
            applyStatsToPlayer(ref, store);
        } catch (Exception e) {}
    }

    // apply the stats instance to the player as applicable
    private void applyStatsToPlayer(Ref<EntityStore> ref, Store<EntityStore> store) {
        try {
            //get the rpg player comp and player comp
            Player player = store.getComponent(ref, Player.getComponentType());
            if(player == null) return;

            // get the stat map component from the player
            ComponentType<EntityStore, EntityStatMap> statMapType = EntityStatsModule.get().getEntityStatMapComponentType();
            EntityStatMap statMap = store.getComponent(ref, statMapType);

            // Get the health stat from the stat map
            int healthIndex = DefaultEntityStatTypes.getHealth();
            int staminaIndex = DefaultEntityStatTypes.getStamina();
            int manaIndex = DefaultEntityStatTypes.getMana();

            // set players max resources based on gear
            statMap.putModifier(healthIndex, "BASE_LIFE", new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.ADDITIVE, stats.getLife()));
            statMap.putModifier(staminaIndex, "BASE_STAMINA", new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.ADDITIVE, stats.getStamina()));
            statMap.putModifier(manaIndex, "BASE_MANA", new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.ADDITIVE, stats.getMana()));

            // update the entity stats
            statMap.update();

            // get the movement speed manager from player
            MovementManager movementManager = store.getComponent(ref, MovementManager.getComponentType());

            // reset to base speed
            movementManager.applyDefaultSettings();

            // get the current settings
            MovementSettings movementSettings = movementManager.getSettings();

            // Apply RPG modifier
            float speedBonus = 1.0f + (stats.getRunSpeedMultiplier() * 0.01f);
            movementSettings.forwardSprintSpeedMultiplier *= speedBonus;

            // Push the updates to the client
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            movementManager.update(playerRef.getPacketHandler());

        } catch (Exception e) {}
    }

    // get the item from the stack and try to apply its affixes
    private static void applyStack(EntityStats stats, ItemStack stack) {
        try {
            // if stack is null bail
            if (stack == null) return;

            // get the affixes or bail
            String[] affixes = stack.getFromMetadataOrNull("affixes", Codec.STRING_ARRAY);
            if (affixes == null) return;

            // loop over the returned affixes and set the stats
            for (String affixData : affixes) {
                // Expected format assumption (important): "Stat_Flat_Life|25|T5" "Stat_Increased_Fire_Damage|10|T5"
                String[] parts = affixData.split("\\|");
                if (parts.length != 3) continue; // fail-safe against bad data

                // extract the affix id and prep a value var
                String affixId = parts[0];
                float value;

                // try to get the value or bail if it fails
                try {
                    value = Float.parseFloat(parts[1]);
                } catch (NumberFormatException e) {
                    continue; // ignore malformed values safely
                }

                // map the id to a stat and update it's value
                StatType type = StatMapper.fromAffixId(affixId);
                stats.add(type, value);
            }
        } catch (Exception e) {}
    }

    // Method to award XP
    public void awardXP(int enemyLevel, int enemyRarity, PlayerRef playerRef) {
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

        // Calculate scaled XP and rarity bonus
        float xpSummedBase = xpGainedFromEqualLevelMonster * scaleFactor;
        float rarityBonus = xpSummedBase * (enemyRarity * .33f);
        int xpGained = Math.max(Math.round(xpSummedBase + rarityBonus), 0);

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
        Component_RPG_Player copy = new Component_RPG_Player(level, xp, skillPoints);
        return copy;
    }
}
