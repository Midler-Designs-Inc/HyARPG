package com.example.hyarpg.components;

// Hytale Imports
import com.example.hyarpg.configs.ModConfig;
import com.example.hyarpg.modules.Module_RaidSystem.RaidHudState;
import com.example.hyarpg.utils.codecs.Codec_SkillLibrary;
import com.example.hyarpg.utils.rooms.RoomData;
import com.example.hyarpg.utils.rooms.TerritoryData;
import com.example.hyarpg.utils.skills.SkillLibrary;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;

// Mod Imports
import com.example.hyarpg.utils.affixes.EntityStats;
import com.example.hyarpg.utils.affixes.StatMapper;
import com.example.hyarpg.utils.affixes.StatType;

// Java Imports

import java.util.Arrays;
import java.util.stream.Stream;

public class Component_RPG_Player implements Component<EntityStore> {
    // Constructor properties
    public int level;
    public double xp;
    public int skillPoints;

    // Constant properties
    public final int xpToFirstLevel = ModConfig.get().experience.xp_to_first_level;
    public final float xpPerLevelModifier = ModConfig.get().experience.xp_increase_per_level_modifier;
    public final int xpGainedFromEqualLevelMonster = ModConfig.get().experience.xp_gained_from_equal_level_kill;

    // players gear score (average of gear score on equipped items)
    public int gearScore = 0;
    public ItemStack mainHandItem;
    public ItemStack offHandItem;

    // equipped abilityIDs
    public String primaryAbility = null;
    public String secondaryAbility;
    public String ultimateAbility;

    // equipped ability icons
    public String primaryAbilityIcon = null;
    public String secondaryAbilityIcon;
    public String ultimateAbilityIcon;

    // stat class to hold affix stats
    public EntityStats stats = new EntityStats();

    // combat trackers
    public long blockStart = System.nanoTime();

    // store skill trees
    public SkillLibrary skillLibrary;

    // store if the player is inside a room or not
    public RoomData room;
    public TerritoryData territory;

    // player raid parameters
    public long lastBaseRaid;
    public long lastPlayerRaid;
    public RaidHudState activeRaidHudState = null;
    public String nextRaid = null;

    // tracks when the player last logged out so offline time can be excluded from raid cooldowns
    public long lastLogoutTime;

    // player settings
    public boolean showLootDrops = true;
    public boolean showCombatText = true;

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
        .append(new KeyedCodec<>("SkillLibrary", Codec_SkillLibrary.SKILL_LIBRARY_CODEC),
            (comp, v) -> comp.skillLibrary = v,
            comp -> comp.skillLibrary
        ).add()
        .append(new KeyedCodec<>("UltimateAbilityId", Codec.STRING),
            (comp, v) -> comp.ultimateAbility = v,
            comp -> comp.ultimateAbility
        ).add()
        .append(new KeyedCodec<>("UltimateAbilityIconId", Codec.STRING),
            (comp, v) -> comp.ultimateAbilityIcon = v,
            comp -> comp.ultimateAbilityIcon
        ).add()
        .append(new KeyedCodec<>("PrimaryAbilityId", Codec.STRING),
            (comp, v) -> comp.primaryAbility = v,
            comp -> comp.primaryAbility
        ).add()
        .append(new KeyedCodec<>("PrimaryAbilityIconId", Codec.STRING),
            (comp, v) -> comp.primaryAbilityIcon = v,
            comp -> comp.primaryAbilityIcon
        ).add()
        .append(new KeyedCodec<>("SecondaryAbilityId", Codec.STRING),
            (comp, v) -> comp.secondaryAbility = v,
            comp -> comp.secondaryAbility
        ).add()
        .append(new KeyedCodec<>("SecondaryAbilityIconId", Codec.STRING),
            (comp, v) -> comp.secondaryAbilityIcon = v,
            comp -> comp.secondaryAbilityIcon
        ).add()
        .append(new KeyedCodec<>("HyARPG_RPGPlayer_ShowLootDrops", Codec.BOOLEAN),
            ((comp, value) -> comp.showLootDrops = value),
            comp -> comp.showLootDrops
        ).add()
        .append(new KeyedCodec<>("HyARPG_RPGPlayer_ShowCombatText", Codec.BOOLEAN),
            ((comp, value) -> comp.showCombatText = value),
            comp -> comp.showCombatText
        ).add()
        .append(new KeyedCodec<>("HyARPG_RPGPlayer_LastBaseRaid", Codec.LONG),
            ((comp, value) -> comp.lastBaseRaid = value),
            comp -> comp.lastBaseRaid
        ).add()
        .append(new KeyedCodec<>("HyARPG_RPGPlayer_LastPlayerRaid", Codec.LONG),
            ((comp, value) -> comp.lastPlayerRaid = value),
            comp -> comp.lastPlayerRaid
        ).add()
        .append(new KeyedCodec<>("HyARPG_RPGPlayer_NextRaid", Codec.STRING),
                ((comp, value) -> comp.nextRaid = value),
                comp -> comp.nextRaid
        ).add()
        .append(new KeyedCodec<>("HyARPG_RPGPlayer_LastLogoutTime", Codec.LONG),
                ((comp, value) -> comp.lastLogoutTime = value),
                comp -> comp.lastLogoutTime
        ).add()
        .build();

    // Default no-arg constructor (required for component registration)
    public Component_RPG_Player() { this(1, 0, 0); }

    // Constructor
    public Component_RPG_Player(int level, double xp, int skillPoints) {
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
    public void calculateGearScore(Ref<EntityStore> ref, Store<EntityStore> store) {
        int totalLevel = 0;
        int count = 0;

        // Active hand item (weapon) — checks Tool first, falls back to Hotbar
        ItemStack mainHand = InventoryComponent.getItemInHand(store, ref);
        if (mainHand != null && !mainHand.isEmpty()) {
            Integer level = mainHand.getFromMetadataOrNull("GearScore", Codec.INTEGER);
            if (level != null) {
                totalLevel += level;
                count++;
            };
        }

        // Off hand (utility active slot)
        InventoryComponent.Utility utilityComp = store.getComponent(ref, InventoryComponent.Utility.getComponentType());
        ItemStack offHand = utilityComp != null ? utilityComp.getActiveItem() : null;
        boolean applyOffHand = mainHand == null || mainHand.getItem().getUtility().isCompatible();
        if (offHand != null && !offHand.isEmpty() && applyOffHand) {
            Integer level = offHand.getFromMetadataOrNull("GearScore", Codec.INTEGER);
            if (level != null) {
                totalLevel += level;
                count++;
            };
        }

        // Armor slots
        InventoryComponent.Armor armorComp = store.getComponent(ref, InventoryComponent.Armor.getComponentType());
        if (armorComp != null) {
            ItemContainer armor = armorComp.getInventory();
            for (short i = 0; i < armor.getCapacity(); i++) {
                ItemStack armorPiece = armor.getItemStack(i);
                if (armorPiece == null || armorPiece.isEmpty()) continue;

                Integer level = armorPiece.getFromMetadataOrNull("GearScore", Codec.INTEGER);
                if (level != null) {
                    totalLevel += level;
                    count++;
                }
            }
        }

        this.gearScore = count > 0 ? Math.max(0, totalLevel / count) : 0;
    }

    // Calculate the players stats based on gear affixes
    public void calculateAffixStats(Ref<EntityStore> ref, Store<EntityStore> store) {
        try {
            // create a new instance of player stats
            EntityStats newStats = new EntityStats();

            // Armor slots
            InventoryComponent.Armor armorComp = store.getComponent(ref, InventoryComponent.Armor.getComponentType());
            if (armorComp != null) {
                ItemContainer armor = armorComp.getInventory();
                applyStack(newStats, armor.getItemStack((short) ItemArmorSlot.Head.ordinal()));
                applyStack(newStats, armor.getItemStack((short) ItemArmorSlot.Chest.ordinal()));
                applyStack(newStats, armor.getItemStack((short) ItemArmorSlot.Hands.ordinal()));
                applyStack(newStats, armor.getItemStack((short) ItemArmorSlot.Legs.ordinal()));
            }

            // Weapon / Utility
            ItemStack mainHand = InventoryComponent.getItemInHand(store, ref);
            if (mainHand != null && !mainHand.isEmpty()) applyStack(newStats, mainHand);

            InventoryComponent.Utility utilityComp = store.getComponent(ref, InventoryComponent.Utility.getComponentType());
            ItemStack offHand = utilityComp != null ? utilityComp.getActiveItem() : null;
            boolean applyOffHand = mainHand == null || mainHand.getItem().getUtility().isCompatible();
            if(offHand != null && !offHand.isEmpty() && applyOffHand) applyStack(newStats,  offHand);


            // Merge skill tree bonuses into the affix stats before applying
            if (skillLibrary != null) newStats.merge(skillLibrary.getSkillStats());

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
            int ammoIndex = DefaultEntityStatTypes.getAmmo();
            int barrierOnBlockStatIndex = EntityStatType.getAssetMap().getIndex("BarrierOnBlock");

            // set players max resources based on stats instance
            statMap.putModifier(healthIndex, "FLAT_LIFE", new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.ADDITIVE, stats.getFlatResource("Life")));
            statMap.putModifier(healthIndex, "INCREASED_LIFE", new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.MULTIPLICATIVE, 1f + (stats.getIncreasedResource("Life") / 100f)));
            statMap.putModifier(manaIndex, "FLAT_MANA", new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.ADDITIVE, stats.getFlatResource("Mana")));
            statMap.putModifier(manaIndex, "INCREASED_MANA", new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.MULTIPLICATIVE, 1f + (stats.getIncreasedResource("Mana") / 100f)));
            statMap.putModifier(staminaIndex, "FLAT_STAMINA", new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.ADDITIVE, stats.getFlatResource("Stamina")));
            statMap.putModifier(staminaIndex, "INCREASED_STAMINA", new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.MULTIPLICATIVE, 1f + (stats.getIncreasedResource("Stamina") / 100f)));
            statMap.putModifier(ammoIndex, "ADDED_AMMO", new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.ADDITIVE, stats.getAddedAmmo()));

            // update the entity stats for the resources
            statMap.update();

            // set players max barrier on block based on stats instance
            EntityStatValue healthStat = statMap.get(healthIndex);
            float maxBarrier = healthStat.getMax() * (stats.getBarrierOnBlock() / 100);
            statMap.putModifier(barrierOnBlockStatIndex, "BASE_BARRIER_ON_BLOCK", new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.ADDITIVE, maxBarrier));

            // update the entity stats for the barrier on block now
            statMap.update();

            // get the movement speed manager from player
            MovementManager movementManager = store.getComponent(ref, MovementManager.getComponentType());

            // reset to base speed
            movementManager.applyDefaultSettings();

            // get the current settings
            MovementSettings movementSettings = movementManager.getSettings();

            // Apply RPG modifier
            float speedBonus = 1.0f + (stats.getRunSpeedPercent() * 0.01f);
            movementSettings.forwardSprintSpeedMultiplier *= speedBonus;

            // Push the updates to the client
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            movementManager.update(playerRef.getPacketHandler());

        } catch (Exception e) {}
    }

    private static void applyStack(EntityStats stats, ItemStack stack) {
        try {
            if (stack == null) return;

            // get the affixes and implicits
            String[] affixes  = stack.getFromMetadataOrNull("affixes",   Codec.STRING_ARRAY);
            String[] implicits = stack.getFromMetadataOrNull("implicits", Codec.STRING_ARRAY);

            // combine into one array to process together
            if (affixes == null && implicits == null) return;
            String[] combined = Stream.concat(affixes  != null
                    ? Arrays.stream(affixes)
                    : Stream.empty(), implicits != null
                        ? Arrays.stream(implicits)
                        : Stream.empty()).toArray(String[]::new);

            // loop over the returned affixes/implicits and set the stats
            for (String affixData : combined) {
                String[] parts = affixData.split("\\|");
                if (parts.length != 3) continue; // fail-safe against bad data

                // extract the affix id and prep a value var
                String affixId = parts[0];

                // map the id to a stat and update its value
                StatType type = StatMapper.fromAffixId(affixId);
                stats.add(type, Float.parseFloat(parts[1]));
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
    public void awardXP(PlayerRef playerRef, double xpGained) {
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
