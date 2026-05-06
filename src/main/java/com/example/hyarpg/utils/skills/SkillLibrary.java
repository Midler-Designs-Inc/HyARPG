package com.example.hyarpg.utils.skills;

// Mod Imports
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.utils.abilities.juggernaut.*;
import com.example.hyarpg.utils.abilities.mage.Arcane_Meteor;
import com.example.hyarpg.utils.abilities.mage.Arcane_Missiles;
import com.example.hyarpg.utils.abilities.mage.Simulacrum;
import com.example.hyarpg.utils.abilities.ranger.*;
import com.example.hyarpg.utils.abilities.knight.*;
import com.example.hyarpg.utils.abilities.assassin.*;
import com.example.hyarpg.utils.affixes.EntityStats;
import com.example.hyarpg.utils.affixes.StatType;

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
        registerAssassinTree();
        registerJuggernautTree();
        registerKnightTree();
        registerMageTree();
        registerRangerTree();
    }

    // Gson deserialization only — no tree registration, recalculate() called after load
    private SkillLibrary() {
        this.version = null;
    }

    // Tree Registrations
    public void registerAssassinTree() {
        Map<String, SkillNode> nodes = new LinkedHashMap<>();
        Map<String, SkillTree.GridPosition> layout = new LinkedHashMap<>();

        // ---- Increased Daggers Damage Nodes ---- //
        nodes.put("Assassin_Daggers_IncreasedDamage_1", new SkillNode("Assassin_Daggers_IncreasedDamage_1", "Increase daggers damage by 1% per rank.", "Assassin_Daggers_IncreasedDamage_1.png", StatType.DAGGERS_DAMAGE_PERCENT, 1f, 1, 10, List.of(), "1.0.0"));
        layout.put("Assassin_Daggers_IncreasedDamage_1", new SkillTree.GridPosition(0, 0));

        nodes.put("Assassin_Daggers_IncreasedDamage_2", new SkillNode("Assassin_Daggers_IncreasedDamage_2", "Increase daggers damage by 3% per rank.", "Assassin_Daggers_IncreasedDamage_2.png", StatType.DAGGERS_DAMAGE_PERCENT, 3f, 1, 5, List.of(Requirement.nodeRank("Assassin_Daggers_IncreasedDamage_1", 10)), "1.0.0"));
        layout.put("Assassin_Daggers_IncreasedDamage_2", new SkillTree.GridPosition(0, 1));

        nodes.put("Assassin_Daggers_IncreasedDamage_3", new SkillNode("Assassin_Daggers_IncreasedDamage_3", "Increase daggers damage by 25% per rank.", "Assassin_Daggers_IncreasedDamage_3.png", StatType.DAGGERS_DAMAGE_PERCENT, 25f, 1, 1, List.of(Requirement.nodeRank("Assassin_Daggers_IncreasedDamage_2", 5)), "1.0.0"));
        layout.put("Assassin_Daggers_IncreasedDamage_3", new SkillTree.GridPosition(0, 2));

        // ---- Increased Kunai Damage Nodes ---- //
        nodes.put("Assassin_Kunai_IncreasedDamage_1", new SkillNode("Assassin_Kunai_IncreasedDamage_1", "Increase kunai damage by 1% per rank.", "Assassin_Kunai_IncreasedDamage_1.png", StatType.KUNAI_DAMAGE_PERCENT, 1f, 1, 10, List.of(), "1.0.0"));
        layout.put("Assassin_Kunai_IncreasedDamage_1", new SkillTree.GridPosition(2, 0));

        nodes.put("Assassin_Kunai_IncreasedDamage_2", new SkillNode("Assassin_Kunai_IncreasedDamage_2", "Increase kunai damage by 3% per rank.", "Assassin_Kunai_IncreasedDamage_2.png", StatType.KUNAI_DAMAGE_PERCENT, 3f, 1, 5, List.of(Requirement.nodeRank("Assassin_Kunai_IncreasedDamage_1", 10)), "1.0.0"));
        layout.put("Assassin_Kunai_IncreasedDamage_2", new SkillTree.GridPosition(2, 1));

        nodes.put("Assassin_Kunai_IncreasedDamage_3", new SkillNode("Assassin_Kunai_IncreasedDamage_3", "Increase kunai damage by 25% per rank.", "Assassin_Kunai_IncreasedDamage_3.png", StatType.KUNAI_DAMAGE_PERCENT, 25f, 1, 1, List.of(Requirement.nodeRank("Assassin_Kunai_IncreasedDamage_2", 5)), "1.0.0"));
        layout.put("Assassin_Kunai_IncreasedDamage_3", new SkillTree.GridPosition(2, 2));

        // ---- Added Poison Damage Nodes ---- //
        nodes.put("Assassin_Poison_AddedDamage_1", new SkillNode("Assassin_Poison_AddedDamage_1", "Add +0.25 flat poison damage per rank.", "Assassin_Poison_IncreasedDamage_1.png", StatType.POISON_DAMAGE_FLAT, 0.25f, 1, 8, List.of(), "1.0.0"));
        layout.put("Assassin_Poison_AddedDamage_1", new SkillTree.GridPosition(4, 0));

        nodes.put("Assassin_Poison_AddedDamage_2", new SkillNode("Assassin_Poison_AddedDamage_2", "Add +0.50 flat poison damage per rank.", "Assassin_Poison_IncreasedDamage_2.png", StatType.POISON_DAMAGE_FLAT, 0.50f, 1, 4, List.of(Requirement.nodeRank("Assassin_Poison_AddedDamage_1", 8)), "1.0.0"));
        layout.put("Assassin_Poison_AddedDamage_2", new SkillTree.GridPosition(4, 1));

        // ---- Increased Dodge Chance Nodes ---- //
        nodes.put("Assassin_Dodge_IncreasedChance_1", new SkillNode("Assassin_Dodge_IncreasedChance_1", "Increase dodge chance by 1% per rank.", "Assassin_Dodge_IncreasedChance_1.png", StatType.DODGE_CHANCE_PERCENT, 1f, 1, 10, List.of(), "1.0.0"));
        layout.put("Assassin_Dodge_IncreasedChance_1", new SkillTree.GridPosition(6, 0));

        nodes.put("Assassin_Dodge_IncreasedChance_2", new SkillNode("Assassin_Dodge_IncreasedChance_2", "Increase dodge chance by 3% per rank.", "Assassin_Dodge_IncreasedChance_2.png", StatType.DODGE_CHANCE_PERCENT, 3f, 1, 5, List.of(Requirement.nodeRank("Assassin_Dodge_IncreasedChance_1", 10)), "1.0.0"));
        layout.put("Assassin_Dodge_IncreasedChance_2", new SkillTree.GridPosition(6, 1));

        // ---- Assassins Mark Ability ---- //
        nodes.put("Assassin_LearnAbility_AssassinsMark", new SkillNode("Assassin_LearnAbility_AssassinsMark", "Learn 'Assassins Mark': A passive ability that marks the last enemy hit increasing damage you do against that target by 2% per Assassin Mark.\n\n* Assassin Marks can stack up to 10x and last for 10 seconds.\n* Hitting a new target clears all marks from prior targets.", "Assassin_Ability_AssassinsMark.png", StatType.APPLY_ASSASSIN_MARK_FLAT, 1f, 4, 1, List.of(Requirement.treePoints("Assassin", 10)), "1.0.0"));
        layout.put("Assassin_LearnAbility_AssassinsMark", new SkillTree.GridPosition(8, 0));

        // ---- Shadow Strike Ability ---- //
        nodes.put("Assassin_LearnAbility_ShadowStrike", new SkillNode("Assassin_LearnAbility_ShadowStrike", "Learn 'Shadow Strike': An activated ability that teleports you behind the last enemy you hit, striking with your main hand weapon for a guaranteed critical strike.\n\n* Will consume up to 5 Assassin Marks on the target to increase critical damage of the strike by 40% per mark consumed.", "Assassin_Ability_ShadowStrike.png", new Shadow_Strike(), 4, 1, List.of(Requirement.treePoints("Assassin", 10)), "1.0.0"));
        layout.put("Assassin_LearnAbility_ShadowStrike", new SkillTree.GridPosition(8, 2));

        // ---- Reaper Death Seal Ability ---- //
        nodes.put("Assassin_LearnAbility_ReaperDeathSeal", new SkillNode("Assassin_LearnAbility_ReaperDeathSeal", "Learn 'Reaper Death Seal': An activated ability that summons a death god to claim the soul of the last enemy you hit.\n\n* Will consume all Assassin Marks on the target to deal damage equal to 10% of the targets max health per Assassin Mark consumed.", "Assassin_Ability_ReaperDeathSeal.png", new Reaper_Death_Seal(), 10, 1, List.of(Requirement.treePoints("Assassin", 10)), "1.0.0"));
        layout.put("Assassin_LearnAbility_ReaperDeathSeal", new SkillTree.GridPosition(8, 4));

        // ---- Increased Critical Strike Chance Nodes ---- //
        nodes.put("Assassin_CriticalStrike_IncreasedChance_1", new SkillNode("Assassin_CriticalStrike_IncreasedChance_1", "Increase critical strike chance by 1% per rank.", "Assassin_CriticalStrikeChance_IncreasedAmount_1.png", StatType.CRITICAL_STRIKE_CHANCE_PERCENT, 1f, 1, 10, List.of(Requirement.treePoints("Assassin", 10)), "1.0.0"));
        layout.put("Assassin_CriticalStrike_IncreasedChance_1", new SkillTree.GridPosition(0, 4));

        nodes.put("Assassin_CriticalStrike_IncreasedChance_2", new SkillNode("Assassin_CriticalStrike_IncreasedChance_2", "Increase critical strike chance by 3% per rank.", "Assassin_CriticalStrikeChance_IncreasedAmount_2.png", StatType.CRITICAL_STRIKE_CHANCE_PERCENT, 3f, 1, 5, List.of(Requirement.treePoints("Assassin", 15)), "1.0.0"));
        layout.put("Assassin_CriticalStrike_IncreasedChance_2", new SkillTree.GridPosition(0, 5));

        // ---- Increased Critical Strike Damage Nodes ---- //
        nodes.put("Assassin_CriticalStrike_IncreasedDamage_1", new SkillNode("Assassin_CriticalStrike_IncreasedDamage_1", "Increase critical strike damage by 1% per rank.", "Assassin_CriticalStrikeDamage_IncreasedAmount_1.png", StatType.CRITICAL_STRIKE_DAMAGE_PERCENT, 1f, 1, 10, List.of(Requirement.treePoints("Assassin", 10)), "1.0.0"));
        layout.put("Assassin_CriticalStrike_IncreasedDamage_1", new SkillTree.GridPosition(2, 4));

        nodes.put("Assassin_CriticalStrike_IncreasedDamage_2", new SkillNode("Assassin_CriticalStrike_IncreasedDamage_2", "Increase critical strike damage by 3% per rank.", "Assassin_CriticalStrikeDamage_IncreasedAmount_2.png", StatType.CRITICAL_STRIKE_DAMAGE_PERCENT, 3f, 1, 5, List.of(Requirement.treePoints("Assassin", 15)), "1.0.0"));
        layout.put("Assassin_CriticalStrike_IncreasedDamage_2", new SkillTree.GridPosition(2, 5));

        // ---- Register the Tree ---- //
        REGISTRY.put("Assassin", new SkillTree("Assassin", "Assassin", "Weapon", List.of(), List.of(), "1.0.0", nodes, layout, 8, 6));
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
    public void registerMageTree() {
        Map<String, SkillNode> nodes = new LinkedHashMap<>();
        Map<String, SkillTree.GridPosition> layout = new LinkedHashMap<>();

        // ---- Increased Staff Damage Nodes ---- //
        nodes.put("Mage_Staff_IncreasedDamage_1", new SkillNode("Mage_Staff_IncreasedDamage_1", "Increase staff damage by 1% per rank.", "Mage_Staff_IncreasedDamage_1.png", StatType.STAFF_DAMAGE_PERCENT, 1f, 1, 10, List.of(), "1.0.0"));
        layout.put("Mage_Staff_IncreasedDamage_1", new SkillTree.GridPosition(0, 0));

        nodes.put("Mage_Staff_IncreasedDamage_2", new SkillNode("Mage_Staff_IncreasedDamage_2", "Increase staff damage by 3% per rank.", "Mage_Staff_IncreasedDamage_2.png", StatType.STAFF_DAMAGE_PERCENT, 3f, 1, 5, List.of(Requirement.nodeRank("Mage_Staff_IncreasedDamage_1", 10)), "1.0.0"));
        layout.put("Mage_Staff_IncreasedDamage_2", new SkillTree.GridPosition(0, 1));

        nodes.put("Mage_Staff_IncreasedDamage_3", new SkillNode("Mage_Staff_IncreasedDamage_3", "Increase staff damage by 25% per rank.", "Mage_Staff_IncreasedDamage_3.png", StatType.STAFF_DAMAGE_PERCENT, 25f, 1, 1, List.of(Requirement.nodeRank("Mage_Staff_IncreasedDamage_2", 5)), "1.0.0"));
        layout.put("Mage_Staff_IncreasedDamage_3", new SkillTree.GridPosition(0, 2));

        // ---- Increased Wand Damage Nodes ---- //
        nodes.put("Mage_Wand_IncreasedDamage_1", new SkillNode("Mage_Wand_IncreasedDamage_1", "Increase wand damage by 1% per rank.", "Mage_Wand_IncreasedDamage_1.png", StatType.WAND_DAMAGE_PERCENT, 1f, 1, 10, List.of(), "1.0.0"));
        layout.put("Mage_Wand_IncreasedDamage_1", new SkillTree.GridPosition(2, 0));

        nodes.put("Mage_Wand_IncreasedDamage_2", new SkillNode("Mage_Wand_IncreasedDamage_2", "Increase wand damage by 3% per rank.", "Mage_Wand_IncreasedDamage_2.png", StatType.WAND_DAMAGE_PERCENT, 3f, 1, 5, List.of(Requirement.nodeRank("Mage_Wand_IncreasedDamage_1", 10)), "1.0.0"));
        layout.put("Mage_Wand_IncreasedDamage_2", new SkillTree.GridPosition(2, 1));

        nodes.put("Mage_Wand_IncreasedDamage_3", new SkillNode("Mage_Wand_IncreasedDamage_3", "Increase wand damage by 25% per rank.", "Mage_Wand_IncreasedDamage_3.png", StatType.WAND_DAMAGE_PERCENT, 25f, 1, 1, List.of(Requirement.nodeRank("Mage_Wand_IncreasedDamage_2", 5)), "1.0.0"));
        layout.put("Mage_Wand_IncreasedDamage_3", new SkillTree.GridPosition(2, 2));

        // ---- Added Magic Damage Nodes ---- //
        nodes.put("Mage_Magic_AddedDamage_1", new SkillNode("Mage_Magic_AddedDamage_1", "Add +0.25 flat magic damage per rank.", "Mage_Magic_AddedDamage_1.png", StatType.MAGIC_DAMAGE_FLAT, 0.25f, 1, 8, List.of(), "1.0.0"));
        layout.put("Mage_Magic_AddedDamage_1", new SkillTree.GridPosition(4, 0));

        nodes.put("Mage_Magic_AddedDamage_2", new SkillNode("Mage_Magic_AddedDamage_2", "Add +0.50 flat magic damage per rank.", "Mage_Magic_AddedDamage_2.png", StatType.MAGIC_DAMAGE_FLAT, 0.50f, 1, 4, List.of(Requirement.nodeRank("Mage_Magic_AddedDamage_1", 8)), "1.0.0"));
        layout.put("Mage_Magic_AddedDamage_2", new SkillTree.GridPosition(4, 1));

        // ---- Arcane Missiles Ability ---- //
        nodes.put("Mage_LearnAbility_Arcane_Missiles", new SkillNode("Mage_LearnAbility_Arcane_Missiles", "Learn 'Arcane Missiles': An activated ability that fires off 5 magic based homing projectiles at the last target you hit.", "Mage_Ability_Arcane_Missiles.png", new Arcane_Missiles(), 4, 1, List.of(Requirement.treePoints("Mage", 10)), "1.0.0"));
        layout.put("Mage_LearnAbility_Arcane_Missiles", new SkillTree.GridPosition(8, 0));

        // ---- Simulacrum Ability ---- //
        nodes.put("Mage_LearnAbility_Simulacrum", new SkillNode("Mage_LearnAbility_Simulacrum", "Learn 'Simulacrum': An activated ability that teleports you to a nearby location, leaving a simulacrum of you in your place.\n\n* Your simulacrum has HP equal to your max mana and will persist for up to 30 seconds or until it dies.\n* Your simulacrum will occasionally cast arcane missiles and draw the aggro of nearby enemies.", "Mage_Ability_Simulacrum.png", new Simulacrum(), 4, 1, List.of(Requirement.treePoints("Mage", 10)), "1.0.0"));
        layout.put("Mage_LearnAbility_Simulacrum", new SkillTree.GridPosition(8, 2));

        // ---- Arcane Meteor Ability ---- //
        nodes.put("Mage_LearnAbility_Arcane_Meteor", new SkillNode("Mage_LearnAbility_Arcane_Meteor", "Learn 'Arcane Meteor': An activated ability that summons an arcane meteor to rain from the sky on all nearby enemies.", "Mage_Ability_Arcane_Meteor.png", new Arcane_Meteor(), 10, 1, List.of(Requirement.treePoints("Mage", 10)), "1.0.0"));
        layout.put("Mage_LearnAbility_Arcane_Meteor", new SkillTree.GridPosition(8, 4));

        // ---- Added Flat Mana / Increased Mana Regen ---- //
        nodes.put("Mage_Mana_AddedAmount_1", new SkillNode("Mage_Mana_AddedAmount_1", "Add +5 flat mana per rank.", "Mage_Mana_AddedAmount_1.png", StatType.MANA_FLAT, 5f, 1, 10, List.of(Requirement.treePoints("Mage", 10)), "1.0.0"));
        layout.put("Mage_Mana_AddedAmount_1", new SkillTree.GridPosition(0, 4));

        nodes.put("Mage_ManaRegen_IncreasedAmount_1", new SkillNode("Mage_ManaRegen_IncreasedAmount_1", "Increase mana regeneration rate by 10% per rank.", "Mage_ManaRegen_IncreasedAmount_1.png", StatType.MANA_REGEN_PERCENT, 10f, 2, 5, List.of(Requirement.treePoints("Mage", 10)), "1.0.0"));
        layout.put("Mage_ManaRegen_IncreasedAmount_1", new SkillTree.GridPosition(0, 5));

        // ---- Increased Barrier on Block / Increased Damage Taken from Mana ---- //
        nodes.put("Mage_BarrierOnBlock_IncreasedAmount_1", new SkillNode("Mage_BarrierOnBlock_IncreasedAmount_1", "Increase max barrier on block by 2% per rank.", "Mage_BarrierOnBlock_IncreasedAmount_1.png", StatType.BARRIER_ON_BLOCK, 2f, 1, 10, List.of(Requirement.treePoints("Mage", 10)), "1.0.0"));
        layout.put("Mage_BarrierOnBlock_IncreasedAmount_1", new SkillTree.GridPosition(2, 4));

        nodes.put("Mage_DamageTakenFromMana_IncreasedAmount_1", new SkillNode("Mage_DamageTakenFromMana_IncreasedAmount_1", "Increase damage taken from mana by 5% per rank.", "Mage_DamageTakenFromMana_IncreasedAmount_1.png", StatType.DAMAGE_TAKEN_FROM_MANA_PERCENT, 5f, 5, 5, List.of(Requirement.treePoints("Mage", 15)), "1.0.0"));
        layout.put("Mage_DamageTakenFromMana_IncreasedAmount_1", new SkillTree.GridPosition(2, 5));

        // ---- Register the Tree ---- //
        REGISTRY.put("Mage", new SkillTree("Mage", "Mage", "Weapon", List.of(), List.of(), "1.0.0", nodes, layout, 8, 6));
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

        // ---- Increased Ammo Nodes ---- //
        nodes.put("Ranger_Ammo_IncreasedFlat_1", new SkillNode("Ranger_Ammo_IncreasedFlat_1", "Increases ammo charges by +1 per rank.", "Ranger_Ammo_IncreasedFlat_1.png", StatType.AMMO_FLAT, 1f, 5, 2, List.of(), "1.0.0"));
        layout.put("Ranger_Ammo_IncreasedFlat_1", new SkillTree.GridPosition(4, 0));

        nodes.put("Ranger_AmmoRegen_IncreasedAmount_1", new SkillNode("Ranger_AmmoRegen_IncreasedAmount_1", "Increase ammo regeneration speed by 10% per rank.", "Ranger_AmmoRegen_IncreasedAmount_1.png", StatType.AMMO_REGEN_PERCENT, 10f, 2, 5, List.of(Requirement.nodeRank("Ranger_Ammo_IncreasedFlat_1", 2)), "1.0.0"));
        layout.put("Ranger_AmmoRegen_IncreasedAmount_1", new SkillTree.GridPosition(4, 1));

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

        // ---- Reduced Fall Damage Nodes ---- //
        nodes.put("Ranger_FallDamage_IncreasedResistance_1", new SkillNode("Ranger_FallDamage_IncreasedResistance_1", "Increase fall damage resistance by 25% per rank.", "Ranger_FallDamage_IncreasedResistance_1.png", StatType.FALL_RESIST_PERCENT, 25f, 2, 2, List.of(), "1.0.0"));
        layout.put("Ranger_FallDamage_IncreasedResistance_1", new SkillTree.GridPosition(8, 2));

        nodes.put("Ranger_FallDamage_IncreasedResistance_2", new SkillNode("Ranger_FallDamage_IncreasedResistance_2", "Increase fall damage resistance by 50% per rank.", "Ranger_FallDamage_IncreasedResistance_2.png", StatType.FALL_RESIST_PERCENT, 50f, 4, 1, List.of(Requirement.nodeRank("Ranger_FallDamage_IncreasedResistance_1", 2)), "1.0.0"));
        layout.put("Ranger_FallDamage_IncreasedResistance_2", new SkillTree.GridPosition(8, 3));

//        // ---- Summon Crossbow Turret Ability ---- //
//        nodes.put("Ranger_LearnAbility_SummonCrossbowTurret", new SkillNode("Ranger_LearnAbility_SummonCrossbowTurret", "Learn 'Summon Crossbow Turret': An activated ability that summons a crossbow turret that will fire at enemies for a period of time.", "Ranger_LearnAbility_SummonCrossbowTurret.png", new Summon_Crossbow_Turret(), 4, 1, List.of(Requirement.treePoints("Ranger", 10)), "1.0.0"));
//        layout.put("Ranger_LearnAbility_SummonCrossbowTurret", new SkillTree.GridPosition(8, 2));

        // ---- Rain of Arrows Ability ---- //
        nodes.put("Ranger_LearnAbility_RainOfArrows", new SkillNode("Ranger_LearnAbility_RainOfArrows", "Learn 'Rain of Arrows': An activated ultimate ability that causes arrows to rain down on all nearby enemies damaging them and pinning them in place for the duration.", "Ranger_LearnAbility_RainOfArrows.png", new Rain_Of_Arrows(), 6, 1, List.of(Requirement.treePoints("Ranger", 10)), "1.0.0"));
        layout.put("Ranger_LearnAbility_RainOfArrows", new SkillTree.GridPosition(8, 5));

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