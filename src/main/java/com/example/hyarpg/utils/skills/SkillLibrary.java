package com.example.hyarpg.utils.skills;

// Mod Imports
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.utils.abilities.juggernaut.*;
import com.example.hyarpg.utils.abilities.ranger.*;
import com.example.hyarpg.utils.abilities.knight.*;
import com.example.hyarpg.utils.affixes.EntityStats;
import com.example.hyarpg.utils.affixes.StatType;
import com.hypixel.hytale.builtin.deployables.DeployablesUtils;
import com.hypixel.hytale.builtin.deployables.component.DeployableProjectileComponent;
import com.hypixel.hytale.builtin.deployables.component.DeployableProjectileShooterComponent;
import com.hypixel.hytale.builtin.deployables.config.DeployableConfig;
import com.hypixel.hytale.builtin.deployables.config.DeployableSpawner;
import com.hypixel.hytale.builtin.deployables.config.DeployableTurretConfig;
import com.hypixel.hytale.builtin.deployables.interaction.SpawnDeployableAtHitLocationInteraction;
import com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig;
import com.hypixel.hytale.server.core.modules.projectile.interaction.ProjectileInteraction;

// Java Imports
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SkillLibrary {

    // Internal Properties
    private final Map<String, SkillTree> REGISTRY = new LinkedHashMap<>();
    private final String version;

    // Derived state — not serialized, recalculated on load and after any allocation/refund
    private transient EntityStats skillStats = new EntityStats();

    public SkillLibrary(String version) {
        this.version = version;
        registerKnightTree();
        registerJuggernautTree();
        registerRangerTree();
    }

    // Gson deserialization only — no tree registration, recalculate() called after load
    private SkillLibrary() {
        this.version = null;
    }

    // Tree Registrations
    public void registerKnightTree() {
        Map<String, SkillNode> nodes = new LinkedHashMap<>();
        Map<String, SkillTree.GridPosition> layout = new LinkedHashMap<>();

        // ---- Increased Axe Damage Nodes ---- //
        nodes.put("Knight_Axes_IncreasedDamage_1", new SkillNode("Knight_Axes_IncreasedDamage_1", "Increase axe damage by 1% per rank.", "Skills_Axes_IncreasedDamage_1.png", StatType.AXE_DAMAGE_PERCENT, 1f, 1, 10, List.of(), "1.0.0"));
        layout.put("Knight_Axes_IncreasedDamage_1", new SkillTree.GridPosition(0, 0));

        nodes.put("Knight_Axes_IncreasedDamage_2", new SkillNode("Knight_Axes_IncreasedDamage_2", "Increase axe damage by 3% per rank.", "Skills_Axes_IncreasedDamage_2.png", StatType.AXE_DAMAGE_PERCENT, 3f, 1, 5, List.of(Requirement.nodeRank("Knight_Axes_IncreasedDamage_1", 10)), "1.0.0"));
        layout.put("Knight_Axes_IncreasedDamage_2", new SkillTree.GridPosition(0, 1));

        nodes.put("Knight_Axes_IncreasedDamage_3", new SkillNode("Knight_Axes_IncreasedDamage_3", "Increase axe damage by 25% per rank.", "Skills_Axes_IncreasedDamage_3.png", StatType.AXE_DAMAGE_PERCENT, 25f, 1, 1, List.of(Requirement.nodeRank("Knight_Axes_IncreasedDamage_2", 5)), "1.0.0"));
        layout.put("Knight_Axes_IncreasedDamage_3", new SkillTree.GridPosition(0, 2));

        // ---- Increased Club Damage Nodes ---- //
        nodes.put("Knight_Clubs_IncreasedDamage_1", new SkillNode("Knight_Clubs_IncreasedDamage_1", "Increase club damage by 1% per rank.", "Skills_Clubs_IncreasedDamage_1.png", StatType.CLUB_DAMAGE_PERCENT, 1f, 1, 10, List.of(), "1.0.0"));
        layout.put("Knight_Clubs_IncreasedDamage_1", new SkillTree.GridPosition(2, 0));

        nodes.put("Knight_Clubs_IncreasedDamage_2", new SkillNode("Knight_Clubs_IncreasedDamage_2", "Increase club damage by 3% per rank.", "Skills_Clubs_IncreasedDamage_2.png", StatType.CLUB_DAMAGE_PERCENT, 3f, 1, 5, List.of(Requirement.nodeRank("Knight_Clubs_IncreasedDamage_1", 10)), "1.0.0"));
        layout.put("Knight_Clubs_IncreasedDamage_2", new SkillTree.GridPosition(2, 1));

        nodes.put("Knight_Clubs_IncreasedDamage_3", new SkillNode("Knight_Clubs_IncreasedDamage_3", "Increase club damage by 25% per rank.", "Skills_Clubs_IncreasedDamage_3.png", StatType.CLUB_DAMAGE_PERCENT, 25f, 1, 1, List.of(Requirement.nodeRank("Knight_Clubs_IncreasedDamage_2", 5)), "1.0.0"));
        layout.put("Knight_Clubs_IncreasedDamage_3", new SkillTree.GridPosition(2, 2));

        // ---- Increased Sword Damage Nodes ---- //
        nodes.put("Knight_Swords_IncreasedDamage_1", new SkillNode("Knight_Swords_IncreasedDamage_1", "Increase sword damage by 1% per rank.", "Skills_Swords_IncreasedDamage_1.png", StatType.SWORD_DAMAGE_PERCENT, 1f, 1, 10, List.of(), "1.0.0"));
        layout.put("Knight_Swords_IncreasedDamage_1", new SkillTree.GridPosition(4, 0));

        nodes.put("Knight_Swords_IncreasedDamage_2", new SkillNode("Knight_Swords_IncreasedDamage_2", "Increase sword damage by 3% per rank.", "Skills_Swords_IncreasedDamage_2.png", StatType.SWORD_DAMAGE_PERCENT, 3f, 1, 5, List.of(Requirement.nodeRank("Knight_Swords_IncreasedDamage_1", 10)), "1.0.0"));
        layout.put("Knight_Swords_IncreasedDamage_2", new SkillTree.GridPosition(4, 1));

        nodes.put("Knight_Swords_IncreasedDamage_3", new SkillNode("Knight_Swords_IncreasedDamage_3", "Increase sword damage by 25% per rank.", "Skills_Swords_IncreasedDamage_3.png", StatType.SWORD_DAMAGE_PERCENT, 25f, 1, 1, List.of(Requirement.nodeRank("Knight_Swords_IncreasedDamage_2", 5)), "1.0.0"));
        layout.put("Knight_Swords_IncreasedDamage_3", new SkillTree.GridPosition(4, 2));

        // ---- Increased Stability Nodes ---- //
        nodes.put("Knight_Shields_IncreasedStability_1", new SkillNode("Knight_Shields_IncreasedStability_1", "Increase stability while wielding a shield by 1% per rank.", "Skills_Shields_IncreasedStability_1.png", StatType.SHIELD_STABILITY_PERCENT, 1f, 1, 10, List.of(), "1.0.0"));
        layout.put("Knight_Shields_IncreasedStability_1", new SkillTree.GridPosition(6, 0));

        nodes.put("Knight_Shields_IncreasedStability_2", new SkillNode("Knight_Shields_IncreasedStability_2", "Increase stability while wielding a shield by 3% per rank.", "Skills_Shields_IncreasedStability_2.png", StatType.SHIELD_STABILITY_PERCENT, 3f, 1, 5, List.of(Requirement.nodeRank("Knight_Shields_IncreasedStability_1", 10)), "1.0.0"));
        layout.put("Knight_Shields_IncreasedStability_2", new SkillTree.GridPosition(6, 1));

        nodes.put("Knight_Shields_IncreasedStability_3", new SkillNode("Knight_Shields_IncreasedStability_3", "Increase stability while wielding a shield by 25% per rank.", "Skills_Shields_IncreasedStability_3.png", StatType.SHIELD_STABILITY_PERCENT, 25f, 1, 1, List.of(Requirement.nodeRank("Knight_Shields_IncreasedStability_2", 3)), "1.0.0"));
        layout.put("Knight_Shields_IncreasedStability_3", new SkillTree.GridPosition(6, 2));

        // ---- Taunt Ability ---- //
        nodes.put("Knight_LearnAbility_Taunt", new SkillNode("Knight_LearnAbility_Taunt", "Learn 'Taunt': An activated ability that taunts all nearby enemies to target you.", "Skills_LearnAbility_AoETaunt.png", new Taunt(), 4, 1, List.of(Requirement.treePoints("Knight", 10)), "1.0.0"));
        layout.put("Knight_LearnAbility_Taunt", new SkillTree.GridPosition(8, 0));

        // ---- Fortify Ability ---- //
        nodes.put("Knight_IncreasedBarrierOnBlock", new SkillNode("Knight_IncreasedBarrierOnBlock", "Learn 'Fortify': A passive ability that causes a barrier equal to 1% of your max life (per rank) to generate while blocking. Combat damage will be applied to the barrier before life.", "Skills_IncreasedBarrierOnBlock.png", StatType.BARRIER_ON_BLOCK, 2, 1, 5, List.of(Requirement.treePoints("Knight", 10)), "1.0.0"));
        layout.put("Knight_IncreasedBarrierOnBlock", new SkillTree.GridPosition(8, 2));

        // ---- Rallying Cry Ability ---- //
        nodes.put("Knight_LearnAbility_RallyingCry", new SkillNode("Knight_LearnAbility_RallyingCry", "Learn 'Rallying Cry': An activated ultimate ability that dramatically increases the physical attack and defense of all nearby players for a short period of time.", "Skills_LearnAbility_RallyingCry.png", new Rallying_Cry(), 6, 1, List.of(Requirement.treePoints("Knight", 10)), "1.0.0"));
        layout.put("Knight_LearnAbility_RallyingCry", new SkillTree.GridPosition(8, 4));

        // ---- Increased Physical Resistance Nodes ---- //
        nodes.put("Knight_Physical_IncreasedResistance_1", new SkillNode("Knight_Physical_IncreasedResistance_1", "Increase physical damage resistance by 1% per rank.", "Skills_Physical_IncreasedResistance_1.png", StatType.PHYSICAL_RESIST_PERCENT, 1f, 1, 10, List.of(Requirement.treePoints("Knight", 10)), "1.0.0"));
        layout.put("Knight_Physical_IncreasedResistance_1", new SkillTree.GridPosition(0, 4));

        nodes.put("Knight_Physical_IncreasedResistance_2", new SkillNode("Knight_Physical_IncreasedResistance_2", "Increase physical damage resistance by 3% per rank.", "Skills_Physical_IncreasedResistance_2.png", StatType.PHYSICAL_RESIST_PERCENT, 3f, 1, 5, List.of(Requirement.treePoints("Knight", 15)), "1.0.0"));
        layout.put("Knight_Physical_IncreasedResistance_2", new SkillTree.GridPosition(0, 5));

        nodes.put("Knight_Physical_IncreasedResistance_3", new SkillNode("Knight_Physical_IncreasedResistance_3", "Increase physical damage resistance by 25% per rank.", "Skills_Physical_IncreasedResistance_3.png", StatType.PHYSICAL_RESIST_PERCENT, 25f, 1, 1, List.of(Requirement.treePoints("Knight", 20)), "1.0.0"));
        layout.put("Knight_Physical_IncreasedResistance_3", new SkillTree.GridPosition(0, 6));

        // ---- Parry Window Nodes ---- //
        nodes.put("Knight_Parry_IncreasedChance_1", new SkillNode("Knight_Parry_IncreasedChance_1", "Increase parry window by 0.05 seconds per rank.", "Skills_Parry_IncreasedChance_1.png", StatType.PARRY_WINDOW_FLAT, .05f, 1, 10, List.of(Requirement.treePoints("Knight", 10)), "1.0.0"));
        layout.put("Knight_Parry_IncreasedChance_1", new SkillTree.GridPosition(2, 4));

        nodes.put("Knight_Parry_IncreasedChance_2", new SkillNode("Knight_Parry_IncreasedChance_2", "Increase parry window by 0.10 seconds per rank.", "Skills_Parry_IncreasedChance_2.png", StatType.PARRY_WINDOW_FLAT, .1f, 1, 5, List.of(Requirement.treePoints("Knight", 15)), "1.0.0"));
        layout.put("Knight_Parry_IncreasedChance_2", new SkillTree.GridPosition(2, 5));

        // ---- Register the Tree ---- //
        REGISTRY.put("Knight", new SkillTree("Knight", "Knight", "Weapon", List.of(), List.of(), "1.0.0", nodes, layout, 8, 6));
    }
    public void registerJuggernautTree() {
        Map<String, SkillNode> nodes = new LinkedHashMap<>();
        Map<String, SkillTree.GridPosition> layout = new LinkedHashMap<>();

        // ---- Increased Battleaxe Damage Nodes ---- //
        nodes.put("Juggernaut_Battleaxes_IncreasedDamage_1", new SkillNode("Juggernaut_Battleaxes_IncreasedDamage_1", "Increase battleaxe damage by 1% per rank.", "Juggernaut_Battleaxes_IncreasedDamage_1.png", StatType.BATTLEAXE_DAMAGE_PERCENT, 1f, 1, 10, List.of(), "1.0.0"));
        layout.put("Juggernaut_Battleaxes_IncreasedDamage_1", new SkillTree.GridPosition(0, 0));

        nodes.put("Juggernaut_Battleaxes_IncreasedDamage_2", new SkillNode("Juggernaut_Battleaxes_IncreasedDamage_2", "Increase battleaxe damage by 3% per rank.", "Juggernaut_Battleaxes_IncreasedDamage_2.png", StatType.BATTLEAXE_DAMAGE_PERCENT, 3f, 1, 5, List.of(Requirement.nodeRank("Juggernaut_Battleaxes_IncreasedDamage_1", 10)), "1.0.0"));
        layout.put("Juggernaut_Battleaxes_IncreasedDamage_2", new SkillTree.GridPosition(0, 1));

        nodes.put("Juggernaut_Battleaxes_IncreasedDamage_3", new SkillNode("Juggernaut_Battleaxes_IncreasedDamage_3", "Increase battleaxe damage by 25% per rank.", "Juggernaut_Battleaxes_IncreasedDamage_3.png", StatType.BATTLEAXE_DAMAGE_PERCENT, 25f, 1, 1, List.of(Requirement.nodeRank("Juggernaut_Battleaxes_IncreasedDamage_2", 5)), "1.0.0"));
        layout.put("Juggernaut_Battleaxes_IncreasedDamage_3", new SkillTree.GridPosition(0, 2));

        // ---- Increased Mace Damage Nodes ---- //
        nodes.put("Juggernaut_Maces_IncreasedDamage_1", new SkillNode("Juggernaut_Maces_IncreasedDamage_1", "Increase mace damage by 1% per rank.", "Juggernaut_Maces_IncreasedDamage_1.png", StatType.MACE_DAMAGE_PERCENT, 1f, 1, 10, List.of(), "1.0.0"));
        layout.put("Juggernaut_Maces_IncreasedDamage_1", new SkillTree.GridPosition(2, 0));

        nodes.put("Juggernaut_Maces_IncreasedDamage_2", new SkillNode("Juggernaut_Maces_IncreasedDamage_2", "Increase mace damage by 3% per rank.", "Juggernaut_Maces_IncreasedDamage_2.png", StatType.MACE_DAMAGE_PERCENT, 3f, 1, 5, List.of(Requirement.nodeRank("Juggernaut_Maces_IncreasedDamage_1", 10)), "1.0.0"));
        layout.put("Juggernaut_Maces_IncreasedDamage_2", new SkillTree.GridPosition(2, 1));

        nodes.put("Juggernaut_Maces_IncreasedDamage_3", new SkillNode("Juggernaut_Maces_IncreasedDamage_3", "Increase mace damage by 25% per rank.", "Juggernaut_Maces_IncreasedDamage_3.png", StatType.MACE_DAMAGE_PERCENT, 25f, 1, 1, List.of(Requirement.nodeRank("Juggernaut_Maces_IncreasedDamage_2", 5)), "1.0.0"));
        layout.put("Juggernaut_Maces_IncreasedDamage_3", new SkillTree.GridPosition(2, 2));

        // ---- Increased Longsword Damage Nodes ---- //
        nodes.put("Juggernaut_Longswords_IncreasedDamage_1", new SkillNode("Juggernaut_Longswords_IncreasedDamage_1", "Increase longsword damage by 1% per rank.", "Juggernaut_Longswords_IncreasedDamage_1.png", StatType.LONGSWORD_DAMAGE_PERCENT, 1f, 1, 10, List.of(), "1.0.0"));
        layout.put("Juggernaut_Longswords_IncreasedDamage_1", new SkillTree.GridPosition(4, 0));

        nodes.put("Juggernaut_Longswords_IncreasedDamage_2", new SkillNode("Juggernaut_Longswords_IncreasedDamage_2", "Increase longsword damage by 3% per rank.", "Juggernaut_Longswords_IncreasedDamage_2.png", StatType.LONGSWORD_DAMAGE_PERCENT, 3f, 1, 5, List.of(Requirement.nodeRank("Juggernaut_Longswords_IncreasedDamage_1", 10)), "1.0.0"));
        layout.put("Juggernaut_Longswords_IncreasedDamage_2", new SkillTree.GridPosition(4, 1));

        nodes.put("Juggernaut_Longswords_IncreasedDamage_3", new SkillNode("Juggernaut_Longswords_IncreasedDamage_3", "Increase longsword damage by 25% per rank.", "Juggernaut_Longswords_IncreasedDamage_3.png", StatType.LONGSWORD_DAMAGE_PERCENT, 25f, 1, 1, List.of(Requirement.nodeRank("Juggernaut_Longswords_IncreasedDamage_2", 5)), "1.0.0"));
        layout.put("Juggernaut_Longswords_IncreasedDamage_3", new SkillTree.GridPosition(4, 2));

        // ---- Increased Stability Nodes ---- //
        nodes.put("Juggernaut_IncreasedStability_1", new SkillNode("Juggernaut_IncreasedStability_1", "Increase stability by 1% per rank.", "Juggernaut_IncreasedStability_1.png", StatType.STABILITY_PERCENT, 1f, 1, 10, List.of(), "1.0.0"));
        layout.put("Juggernaut_IncreasedStability_1", new SkillTree.GridPosition(6, 0));

        nodes.put("Juggernaut_IncreasedStability_2", new SkillNode("Juggernaut_IncreasedStability_2", "Increase stability by 3% per rank.", "Juggernaut_IncreasedStability_2.png", StatType.STABILITY_PERCENT, 3f, 1, 5, List.of(Requirement.nodeRank("Juggernaut_IncreasedStability_1", 10)), "1.0.0"));
        layout.put("Juggernaut_IncreasedStability_2", new SkillTree.GridPosition(6, 1));

        nodes.put("Juggernaut_IncreasedStability_3", new SkillNode("Juggernaut_IncreasedStability_3", "Increase stability by 25% per rank.", "Juggernaut_IncreasedStability_3.png", StatType.STABILITY_PERCENT, 25f, 1, 1, List.of(Requirement.nodeRank("Juggernaut_IncreasedStability_2", 3)), "1.0.0"));
        layout.put("Juggernaut_IncreasedStability_3", new SkillTree.GridPosition(6, 2));

        // ---- Cyclone Ability ---- //
        nodes.put("Juggernaut_LearnAbility_Cyclone", new SkillNode("Juggernaut_LearnAbility_Cyclone", "Learn 'Cyclone': A channeled ability causing the user to spin in a circle damaging all nearby enemies with their equipped weapon.", "Juggernaut_Ability_Cyclone.png", new Cyclone(), 2, 1, List.of(Requirement.treePoints("Juggernaut", 10)), "1.0.0"));
        layout.put("Juggernaut_LearnAbility_Cyclone", new SkillTree.GridPosition(8, 0));

        // ---- Chain Pull Ability ---- //
        nodes.put("Juggernaut_LearnAbility_Chain_Pull", new SkillNode("Juggernaut_LearnAbility_Chain_Pull", "Learn 'Chain Pull': An activated ability that launches out chains that then pull in all nearby enemies.", "Juggernaut_Ability_ChainPull.png", new Chain_Pull(), 4, 1, List.of(Requirement.treePoints("Juggernaut", 10)), "1.0.0"));
        layout.put("Juggernaut_LearnAbility_Chain_Pull", new SkillTree.GridPosition(8, 2));

        // ---- Leap Slam Ability ---- //
        nodes.put("Juggernaut_LearnAbility_Leap_Slam", new SkillNode("Juggernaut_LearnAbility_Leap_Slam", "Learn 'Leap Slam': An activated ultimate ability that causes the user to leap into the air and then slam down with their weapon causing massive AoE damage and stun.", "Juggernaut_Ability_LeapSlam.png", new Leap_Slam(), 6, 1, List.of(Requirement.treePoints("Juggernaut", 10)), "1.0.0"));
        layout.put("Juggernaut_LearnAbility_Leap_Slam", new SkillTree.GridPosition(8, 4));

        // ---- Increased Physical Resistance Nodes ---- //
        nodes.put("Juggernaut_Physical_IncreasedResistance_1", new SkillNode("Juggernaut_Physical_IncreasedResistance_1", "Increase physical damage resistance by 1% per rank.", "Juggernaut_Physical_IncreasedResistance_1.png", StatType.PHYSICAL_RESIST_PERCENT, 1f, 1, 10, List.of(Requirement.treePoints("Juggernaut", 10)), "1.0.0"));
        layout.put("Juggernaut_Physical_IncreasedResistance_1", new SkillTree.GridPosition(0, 4));

        nodes.put("Juggernaut_Physical_IncreasedResistance_2", new SkillNode("Juggernaut_Physical_IncreasedResistance_2", "Increase physical damage resistance by 3% per rank.", "Juggernaut_Physical_IncreasedResistance_2.png", StatType.PHYSICAL_RESIST_PERCENT, 3f, 1, 5, List.of(Requirement.treePoints("Juggernaut", 15)), "1.0.0"));
        layout.put("Juggernaut_Physical_IncreasedResistance_2", new SkillTree.GridPosition(0, 5));

        nodes.put("Juggernaut_Physical_IncreasedResistance_3", new SkillNode("Juggernaut_Physical_IncreasedResistance_3", "Increase physical damage resistance by 25% per rank.", "Juggernaut_Physical_IncreasedResistance_3.png", StatType.PHYSICAL_RESIST_PERCENT, 25f, 1, 1, List.of(Requirement.treePoints("Juggernaut", 20)), "1.0.0"));
        layout.put("Juggernaut_Physical_IncreasedResistance_3", new SkillTree.GridPosition(0, 6));

        // ---- Increased Elemental Resistance Nodes ---- //
        nodes.put("Juggernaut_Elemental_IncreasedResistance_1", new SkillNode("Juggernaut_Elemental_IncreasedResistance_1", "Increase elemental damage resistance by 1% per rank.", "Juggernaut_Elemental_IncreasedResistance_1.png", StatType.ELEMENTAL_RESIST_PERCENT, 1f, 1, 10, List.of(Requirement.treePoints("Juggernaut", 10)), "1.0.0"));
        layout.put("Juggernaut_Elemental_IncreasedResistance_1", new SkillTree.GridPosition(2, 4));

        nodes.put("Juggernaut_Elemental_IncreasedResistance_2", new SkillNode("Juggernaut_Elemental_IncreasedResistance_2", "Increase elemental damage resistance by 3% per rank.", "Juggernaut_Elemental_IncreasedResistance_2.png", StatType.ELEMENTAL_RESIST_PERCENT, 3f, 1, 5, List.of(Requirement.treePoints("Juggernaut", 15)), "1.0.0"));
        layout.put("Juggernaut_Elemental_IncreasedResistance_2", new SkillTree.GridPosition(2, 5));

        nodes.put("Juggernaut_Elemental_IncreasedResistance_3", new SkillNode("Juggernaut_Elemental_IncreasedResistance_3", "Increase elemental damage resistance by 25% per rank.", "Juggernaut_Elemental_IncreasedResistance_3.png", StatType.ELEMENTAL_RESIST_PERCENT, 25f, 1, 1, List.of(Requirement.treePoints("Juggernaut", 20)), "1.0.0"));
        layout.put("Juggernaut_Elemental_IncreasedResistance_3", new SkillTree.GridPosition(2, 6));

        // ---- Register the Tree ---- //
        REGISTRY.put("Juggernaut", new SkillTree("Juggernaut", "Juggernaut", "Weapon", List.of(), List.of(), "1.0.0", nodes, layout, 8, 6));
    }
    public void registerRangerTree() {
        Map<String, SkillNode> nodes = new LinkedHashMap<>();
        Map<String, SkillTree.GridPosition> layout = new LinkedHashMap<>();

        // ---- Increased Shortbow Damage Nodes ---- //
        nodes.put("Ranger_Shortbow_IncreasedDamage_1", new SkillNode("Ranger_Shortbow_IncreasedDamage_1", "Increase shortbow damage by 1% per rank.", "Ranger_Shortbow_IncreasedDamage_1.png", StatType.SHORTBOW_DAMAGE_PERCENT, 1f, 1, 10, List.of(), "1.0.0"));
        layout.put("Ranger_Shortbow_IncreasedDamage_1", new SkillTree.GridPosition(0, 0));

        nodes.put("Ranger_Shortbow_IncreasedDamage_2", new SkillNode("Ranger_Shortbow_IncreasedDamage_2", "Increase shortbow damage by 3% per rank.", "Ranger_Shortbow_IncreasedDamage_2.png", StatType.SHORTBOW_DAMAGE_PERCENT, 3f, 1, 5, List.of(Requirement.nodeRank("Ranger_Shortbow_IncreasedDamage_1", 10)), "1.0.0"));
        layout.put("Ranger_Shortbow_IncreasedDamage_2", new SkillTree.GridPosition(0, 1));

        nodes.put("Ranger_Shortbow_IncreasedDamage_3", new SkillNode("Ranger_Shortbow_IncreasedDamage_3", "Increase shortbow damage by 25% per rank.", "Ranger_Shortbow_IncreasedDamage_3.png", StatType.SHORTBOW_DAMAGE_PERCENT, 25f, 1, 1, List.of(Requirement.nodeRank("Ranger_Shortbow_IncreasedDamage_2", 5)), "1.0.0"));
        layout.put("Ranger_Shortbow_IncreasedDamage_3", new SkillTree.GridPosition(0, 2));

        // ---- Increased Crossbow Damage Nodes ---- //
        nodes.put("Ranger_Crossbow_IncreasedDamage_1", new SkillNode("Ranger_Crossbow_IncreasedDamage_1", "Increase crossbow damage by 1% per rank.", "Ranger_Crossbow_IncreasedDamage_1.png", StatType.CROSSBOW_DAMAGE_PERCENT, 1f, 1, 10, List.of(), "1.0.0"));
        layout.put("Ranger_Crossbow_IncreasedDamage_1", new SkillTree.GridPosition(2, 0));

        nodes.put("Ranger_Crossbow_IncreasedDamage_2", new SkillNode("Ranger_Crossbow_IncreasedDamage_2", "Increase crossbow damage by 3% per rank.", "Ranger_Crossbow_IncreasedDamage_2.png", StatType.CROSSBOW_DAMAGE_PERCENT, 3f, 1, 5, List.of(Requirement.nodeRank("Ranger_Crossbow_IncreasedDamage_1", 10)), "1.0.0"));
        layout.put("Ranger_Crossbow_IncreasedDamage_2", new SkillTree.GridPosition(2, 1));

        nodes.put("Ranger_Crossbow_IncreasedDamage_3", new SkillNode("Ranger_Crossbow_IncreasedDamage_3", "Increase crossbow damage by 25% per rank.", "Ranger_Crossbow_IncreasedDamage_3.png", StatType.CROSSBOW_DAMAGE_PERCENT, 25f, 1, 1, List.of(Requirement.nodeRank("Ranger_Crossbow_IncreasedDamage_2", 5)), "1.0.0"));
        layout.put("Ranger_Crossbow_IncreasedDamage_3", new SkillTree.GridPosition(2, 2));

//        // ---- Increased Longbow Damage Nodes ---- //
//        nodes.put("Ranger_Longbow_IncreasedDamage_1", new SkillNode("Ranger_Longbow_IncreasedDamage_1", "Increase longbow damage by 1% per rank.", "Ranger_Longbow_IncreasedDamage_1.png", StatType.LONGBOW_DAMAGE_PERCENT, 1f, 1, 10, List.of(), "1.0.0"));
//        layout.put("Ranger_Longbow_IncreasedDamage_1", new SkillTree.GridPosition(4, 0));
//
//        nodes.put("Ranger_Longbow_IncreasedDamage_2", new SkillNode("Ranger_Longbow_IncreasedDamage_2", "Increase longbow damage by 3% per rank.", "Ranger_Longbow_IncreasedDamage_2.png", StatType.LONGBOW_DAMAGE_PERCENT, 3f, 1, 5, List.of(Requirement.nodeRank("Ranger_Longbow_IncreasedDamage_1", 10)), "1.0.0"));
//        layout.put("Ranger_Longbow_IncreasedDamage_2", new SkillTree.GridPosition(4, 1));
//
//        nodes.put("Ranger_Longbow_IncreasedDamage_3", new SkillNode("Ranger_Longbow_IncreasedDamage_3", "Increase longbow damage by 25% per rank.", "Ranger_Longbow_IncreasedDamage_3.png", StatType.LONGBOW_DAMAGE_PERCENT, 25f, 1, 1, List.of(Requirement.nodeRank("Ranger_Longbow_IncreasedDamage_2", 5)), "1.0.0"));
//        layout.put("Ranger_Longbow_IncreasedDamage_3", new SkillTree.GridPosition(4, 2));

        // ---- Increased Run Speed Nodes ---- //
        nodes.put("Ranger_RunSpeed_IncreasedAmount_1", new SkillNode("Ranger_RunSpeed_IncreasedAmount_1", "Increase run speed by 2% per rank.", "Ranger_RunSpeed_IncreasedAmount_1.png", StatType.RUN_SPEED_PERCENT, 2f, 4, 1, List.of(), "1.0.0"));
        layout.put("Ranger_RunSpeed_IncreasedAmount_1", new SkillTree.GridPosition(6, 0));

        nodes.put("Ranger_RunSpeed_IncreasedAmount_2", new SkillNode("Ranger_RunSpeed_IncreasedAmount_2", "Increase run speed by 3% per rank.", "Ranger_RunSpeed_IncreasedAmount_2.png", StatType.RUN_SPEED_PERCENT, 3f, 6, 1, List.of(Requirement.nodeRank("Ranger_RunSpeed_IncreasedAmount_1", 1)), "1.0.0"));
        layout.put("Ranger_RunSpeed_IncreasedAmount_2", new SkillTree.GridPosition(6, 1));

        nodes.put("Ranger_RunSpeed_IncreasedAmount_3", new SkillNode("Ranger_RunSpeed_IncreasedAmount_3", "Increase run speed by 5% per rank.", "Ranger_RunSpeed_IncreasedAmount_3.png", StatType.RUN_SPEED_PERCENT, 5f, 8, 1, List.of(Requirement.nodeRank("Ranger_RunSpeed_IncreasedAmount_2", 1)), "1.0.0"));
        layout.put("Ranger_RunSpeed_IncreasedAmount_3", new SkillTree.GridPosition(6, 2));

        // ---- Aerial Maneuver Ability ---- //
        nodes.put("Ranger_LearnAbility_AerialManeuver", new SkillNode("Ranger_LearnAbility_AerialManeuver", "Learn 'Aerial Maneuver': An activated ability causing the user to gain a surge of velocity.", "Ranger_LearnAbility_AerialManeuver.png", new Aerial_Maneuver(), 2, 1, List.of(Requirement.treePoints("Ranger", 10)), "1.0.0"));
        layout.put("Ranger_LearnAbility_AerialManeuver", new SkillTree.GridPosition(8, 0));

        // ---- Summon Crossbow Turret Ability ---- //
        nodes.put("Ranger_LearnAbility_SummonCrossbowTurret", new SkillNode("Ranger_LearnAbility_SummonCrossbowTurret", "Learn 'Summon Crossbow Turret': An activated ability that summons a crossbow turret that will fire at enemies for a period of time.", "Ranger_LearnAbility_SummonCrossbowTurret.png", new Summon_Crossbow_Turret(), 4, 1, List.of(Requirement.treePoints("Ranger", 10)), "1.0.0"));
        layout.put("Ranger_LearnAbility_SummonCrossbowTurret", new SkillTree.GridPosition(8, 2));

        // ---- Rain of Arrows Ability ---- //
        nodes.put("Ranger_LearnAbility_RainOfArrows", new SkillNode("Ranger_LearnAbility_RainOfArrows", "Learn 'Rain of Arrows': An activated ultimate ability that causes arrows to rain down on all nearby enemies damaging them and pinning them in place for the duration.", "Ranger_LearnAbility_RainOfArrows.png", new Rain_of_Arrows(), 6, 1, List.of(Requirement.treePoints("Ranger", 10)), "1.0.0"));
        layout.put("Ranger_LearnAbility_RainOfArrows", new SkillTree.GridPosition(8, 4));

        // ---- Increased Critical Strike Chance Nodes ---- //
        nodes.put("Ranger_CriticalStrikeChance_IncreasedAmount_1", new SkillNode("Ranger_CriticalStrikeChance_IncreasedAmount_1", "Increase critical strike chance by 1% per rank.", "Ranger_CriticalStrikeChance_IncreasedAmount_1.png", StatType.CRITICAL_STRIKE_CHANCE_PERCENT, 1f, 1, 5, List.of(Requirement.treePoints("Ranger", 10)), "1.0.0"));
        layout.put("Ranger_CriticalStrikeChance_IncreasedAmount_1", new SkillTree.GridPosition(0, 4));

        nodes.put("Ranger_CriticalStrikeChance_IncreasedAmount_2", new SkillNode("Ranger_CriticalStrikeChance_IncreasedAmount_2", "Increase critical strike chance by 2% per rank.", "Ranger_CriticalStrikeChance_IncreasedAmount_2.png", StatType.CRITICAL_STRIKE_CHANCE_PERCENT, 2f, 1, 5, List.of(Requirement.treePoints("Ranger", 15)), "1.0.0"));
        layout.put("Ranger_CriticalStrikeChance_IncreasedAmount_2", new SkillTree.GridPosition(0, 5));

        nodes.put("Ranger_CriticalStrikeChance_IncreasedAmount_3", new SkillNode("Ranger_CriticalStrikeChance_IncreasedAmount_3", "Increase critical strike chance by 25% per rank.", "Ranger_CriticalStrikeChance_IncreasedAmount_3.png", StatType.CRITICAL_STRIKE_CHANCE_PERCENT, 25f, 10, 1, List.of(Requirement.treePoints("Ranger", 20)), "1.0.0"));
        layout.put("Ranger_CriticalStrikeChance_IncreasedAmount_3", new SkillTree.GridPosition(0, 6));

        // ---- Increased Elemental Resistance Nodes ---- //
        nodes.put("Ranger_Elemental_IncreasedResistance_1", new SkillNode("Ranger_Elemental_IncreasedResistance_1", "Increase elemental damage resistance by 1% per rank.", "Ranger_Elemental_IncreasedResistance_1.png", StatType.ELEMENTAL_RESIST_PERCENT, 1f, 1, 10, List.of(Requirement.treePoints("Ranger", 10)), "1.0.0"));
        layout.put("Ranger_Elemental_IncreasedResistance_1", new SkillTree.GridPosition(2, 4));

        nodes.put("Ranger_Elemental_IncreasedResistance_2", new SkillNode("Ranger_Elemental_IncreasedResistance_2", "Increase elemental damage resistance by 3% per rank.", "Ranger_Elemental_IncreasedResistance_2.png", StatType.ELEMENTAL_RESIST_PERCENT, 3f, 1, 5, List.of(Requirement.treePoints("Ranger", 15)), "1.0.0"));
        layout.put("Ranger_Elemental_IncreasedResistance_2", new SkillTree.GridPosition(2, 5));

        nodes.put("Ranger_Elemental_IncreasedResistance_3", new SkillNode("Ranger_Elemental_IncreasedResistance_3", "Increase elemental damage resistance by 25% per rank.", "Ranger_Elemental_IncreasedResistance_3.png", StatType.ELEMENTAL_RESIST_PERCENT, 25f, 1, 1, List.of(Requirement.treePoints("Ranger", 20)), "1.0.0"));
        layout.put("Ranger_Elemental_IncreasedResistance_3", new SkillTree.GridPosition(2, 6));

        // ---- Register the Tree ---- //
        REGISTRY.put("Ranger", new SkillTree("Ranger", "Ranger", "Weapon", List.of(), List.of(), "1.0.0", nodes, layout, 8, 6));
    }

    // Check all requirements for a node or tree — pass the library so cross-tree, node lookups work for NODE_MIN_RANK requirements
    public boolean checkRequirements(List<Requirement> requirements) {
        for (Requirement req : requirements) {
            switch (req.type()) {
                case NODE_MIN_RANK -> {
                    // Find the node across all trees
                    SkillNode node = findNode(req.targetId());
                    if (node == null || node.getCurrentRank() < req.minValue()) return false;
                }
                case TREE_MIN_POINTS -> {
                    SkillTree tree = REGISTRY.get(req.targetId());
                    if (tree == null || tree.getPointsSpent() < req.minValue()) return false;
                }
            }
        }
        return true;
    }

    // Find a node by id across all trees
    public SkillNode findNode(String nodeId) {
        for (SkillTree tree : REGISTRY.values()) {
            SkillNode node = tree.getNodes().get(nodeId);
            if (node != null) return node;
        }
        return null;
    }

    // Rebuild skillStats and learnedAbilities from current node allocation state.
    public void recalculate() {
        skillStats = new EntityStats();

        // Pass 1 — rebuild stats and abilities from current allocation
        for (SkillTree tree : REGISTRY.values()) {
            for (SkillNode node : tree.getNodes().values()) {
                if (node.getCurrentRank() == 0) continue;

                if (node.statType != null) {
                    skillStats.add(node.statType, node.statValuePerRank * node.getCurrentRank());
                }
            }
        }

        // Pass 2 — update lock state on every node and tree based on requirements
        for (SkillTree tree : REGISTRY.values()) {
            // Update tree lock state
            if (checkRequirements(tree.requirements)) tree.unlock();
            else tree.lock();

            // Update each node's lock state
            for (SkillNode node : tree.getNodes().values()) {
                if (checkRequirements(node.requirements)) node.unlock();
                else node.lock();
            }
        }
    }

    // Refund all skill nodes from all trees
    public int refund(Component_RPG_Player comp) {
        int refundPoints = 0;
        for (SkillTree tree : REGISTRY.values()) refundPoints += tree.refund(comp);
        recalculate(); // rebuild derived state after full refund
        return refundPoints;
    }

    // Getters
    public String getVersion() { return version; }
    public Map<String, SkillTree> getRegistry() { return REGISTRY; }
    public EntityStats getSkillStats() { return skillStats; }
}