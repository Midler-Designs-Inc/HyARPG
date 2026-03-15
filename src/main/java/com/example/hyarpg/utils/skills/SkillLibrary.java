package com.example.hyarpg.utils.skills;

// Mod Imports
import com.example.hyarpg.utils.affixes.EntityStats;
import com.example.hyarpg.utils.affixes.StatType;

// Java Imports
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SkillLibrary {

    // Internal Properties
    private final Map<String, SkillTree> REGISTRY = new LinkedHashMap<>();
    private final String version;

    // Derived state — not serialized, recalculated on load and after any allocation/refund
    private transient EntityStats skillStats = new EntityStats();
    private transient Set<String> learnedAbilities = new HashSet<>();

    public SkillLibrary(String version) {
        this.version = version;
        registerKnightTree();
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
        nodes.put("Knight_Axes_IncreasedDamage_1", new SkillNode("Axes_IncreasedDamage_1", "Increase axe damage by 1% per rank.", "Skills_Axes_IncreasedDamage_1.png", null, StatType.AXE_DAMAGE_PERCENT, 1f, 1, 10, List.of(), "1.0.0"));
        layout.put("Knight_Axes_IncreasedDamage_1", new SkillTree.GridPosition(0, 0));

        nodes.put("Knight_Axes_IncreasedDamage_2", new SkillNode("Knight_Axes_IncreasedDamage_2", "Increase axe damage by 3% per rank.", "Skills_Axes_IncreasedDamage_2.png", null, StatType.AXE_DAMAGE_PERCENT, 3f, 1, 5, List.of(Requirement.nodeRank("Knight_Axes_IncreasedDamage_1", 10)), "1.0.0"));
        layout.put("Knight_Axes_IncreasedDamage_2", new SkillTree.GridPosition(0, 1));

        nodes.put("Knight_Axes_IncreasedDamage_3", new SkillNode("Knight_Axes_IncreasedDamage_3", "Increase axe damage by 25% per rank.", "Skills_Axes_IncreasedDamage_3.png", null, StatType.AXE_DAMAGE_PERCENT, 25f, 1, 1, List.of(Requirement.nodeRank("Knight_Axes_IncreasedDamage_2", 5)), "1.0.0"));
        layout.put("Knight_Axes_IncreasedDamage_3", new SkillTree.GridPosition(0, 2));

        // ---- Increased Club Damage Nodes ---- //
        nodes.put("Knight_Clubs_IncreasedDamage_1", new SkillNode("Knight_Clubs_IncreasedDamage_1", "Increase club damage by 1% per rank.", "Skills_Clubs_IncreasedDamage_1.png", null, StatType.CLUB_DAMAGE_PERCENT, 1f, 1, 10, List.of(), "1.0.0"));
        layout.put("Knight_Clubs_IncreasedDamage_1", new SkillTree.GridPosition(2, 0));

        nodes.put("Knight_Clubs_IncreasedDamage_2", new SkillNode("Knight_Clubs_IncreasedDamage_2", "Increase club damage by 3% per rank.", "Skills_Clubs_IncreasedDamage_2.png", null, StatType.CLUB_DAMAGE_PERCENT, 3f, 1, 5, List.of(Requirement.nodeRank("Knight_Clubs_IncreasedDamage_1", 10)), "1.0.0"));
        layout.put("Knight_Clubs_IncreasedDamage_2", new SkillTree.GridPosition(2, 1));

        nodes.put("Knight_Clubs_IncreasedDamage_3", new SkillNode("Knight_Clubs_IncreasedDamage_3", "Increase club damage by 25% per rank.", "Skills_Clubs_IncreasedDamage_3.png", null, StatType.CLUB_DAMAGE_PERCENT, 25f, 1, 1, List.of(Requirement.nodeRank("Knight_Clubs_IncreasedDamage_2", 5)), "1.0.0"));
        layout.put("Knight_Clubs_IncreasedDamage_3", new SkillTree.GridPosition(2, 2));

        // ---- Increased Sword Damage Nodes ---- //
        nodes.put("Knight_Swords_IncreasedDamage_1", new SkillNode("Knight_Swords_IncreasedDamage_1", "Increase sword damage by 1% per rank.", "Skills_Swords_IncreasedDamage_1.png", null, StatType.SWORD_DAMAGE_PERCENT, 1f, 1, 10, List.of(), "1.0.0"));
        layout.put("Knight_Swords_IncreasedDamage_1", new SkillTree.GridPosition(4, 0));

        nodes.put("Knight_Swords_IncreasedDamage_2", new SkillNode("Knight_Swords_IncreasedDamage_2", "Increase sword damage by 3% per rank.", "Skills_Swords_IncreasedDamage_2.png", null, StatType.SWORD_DAMAGE_PERCENT, 3f, 1, 5, List.of(Requirement.nodeRank("Knight_Swords_IncreasedDamage_1", 10)), "1.0.0"));
        layout.put("Knight_Swords_IncreasedDamage_2", new SkillTree.GridPosition(4, 1));

        nodes.put("Knight_Swords_IncreasedDamage_3", new SkillNode("Knight_Swords_IncreasedDamage_3", "Increase sword damage by 25% per rank.", "Skills_Swords_IncreasedDamage_3.png", null, StatType.SWORD_DAMAGE_PERCENT, 25f, 1, 1, List.of(Requirement.nodeRank("Knight_Swords_IncreasedDamage_2", 5)), "1.0.0"));
        layout.put("Knight_Swords_IncreasedDamage_3", new SkillTree.GridPosition(4, 2));

        // ---- Increased Stability Nodes ---- //
        nodes.put("Knight_Shields_IncreasedStability_1", new SkillNode("Knight_Shields_IncreasedStability_1", "Increase stability while wielding a shield by 1% per rank.", "Skills_Shields_IncreasedStability_1.png", null, StatType.SHIELD_STABILITY_PERCENT, 1f, 1, 10, List.of(), "1.0.0"));
        layout.put("Knight_Shields_IncreasedStability_1", new SkillTree.GridPosition(6, 0));

        nodes.put("Knight_Shields_IncreasedStability_2", new SkillNode("Knight_Shields_IncreasedStability_2", "Increase stability while wielding a shield by 3% per rank.", "Skills_Shields_IncreasedStability_2.png", null, StatType.SHIELD_STABILITY_PERCENT, 3f, 1, 5, List.of(Requirement.nodeRank("Knight_Shields_IncreasedStability_1", 10)), "1.0.0"));
        layout.put("Knight_Shields_IncreasedStability_2", new SkillTree.GridPosition(6, 1));

        nodes.put("Knight_Shields_IncreasedStability_3", new SkillNode("Knight_Shields_IncreasedStability_3", "Increase stability while wielding a shield by 25% per rank.", "Skills_Shields_IncreasedStability_3.png", null, StatType.SHIELD_STABILITY_PERCENT, 25f, 1, 1, List.of(Requirement.nodeRank("Knight_Shields_IncreasedStability_2", 3)), "1.0.0"));
        layout.put("Knight_Shields_IncreasedStability_3", new SkillTree.GridPosition(6, 2));

        // ---- Taunt Ability ---- //
        nodes.put("Knight_LearnAbility_Taunt", new SkillNode("Knight_LearnAbility_Taunt", "Learn 'Taunt': An activated ability that taunts all nearby enemies to target you.", "Skills_LearnAbility_AoETaunt.png", "Ability_Taunt", null, 0, 1, 1, List.of(Requirement.treePoints("Knight", 10)), "1.0.0"));
        layout.put("Knight_LearnAbility_Taunt", new SkillTree.GridPosition(8, 0));

        // ---- Fortify Ability ---- //
        nodes.put("Knight_IncreasedBarrierOnBlock", new SkillNode("Knight_IncreasedBarrierOnBlock", "Learn 'Fortify': A passive ability that causes a barrier equal to 1% of your max life (per rank) to generate while blocking. Damage will be applied to the barrier before life.", "Skills_IncreasedBarrierOnBlock.png", null, StatType.BARRIER_ON_BLOCK, 2, 1, 5, List.of(Requirement.treePoints("Knight", 20)), "1.0.0"));
        layout.put("Knight_IncreasedBarrierOnBlock", new SkillTree.GridPosition(8, 2));

        // ---- Rallying Cry Ability ---- //
        nodes.put("Knight_LearnAbility_RallyingCry", new SkillNode("Knight_LearnAbility_RallyingCry", "Learn 'Rallying Cry': An activated ultimate ability that dramatically increases the physical attack and defense of all nearby players for a short period of time.", "Skills_LearnAbility_RallyingCry.png", "Ability_Rallying_Cry", null, 0, 1, 1, List.of(Requirement.treePoints("Knight", 30)), "1.0.0"));
        layout.put("Knight_LearnAbility_RallyingCry", new SkillTree.GridPosition(8, 4));

        // ---- Increased Physical Resistance Nodes ---- //
        nodes.put("Knight_Physical_IncreasedResistance_1", new SkillNode("Knight_Physical_IncreasedResistance_1", "Increase physical damage resistance by 1% per rank.", "Skills_Physical_IncreasedResistance_1.png", null, StatType.PHYSICAL_RESIST_PERCENT, 1f, 1, 10, List.of(Requirement.treePoints("Knight", 10)), "1.0.0"));
        layout.put("Knight_Physical_IncreasedResistance_1", new SkillTree.GridPosition(0, 4));

        nodes.put("Knight_Physical_IncreasedResistance_2", new SkillNode("Knight_Physical_IncreasedResistance_2", "Increase physical damage resistance by 3% per rank.", "Skills_Physical_IncreasedResistance_2.png", null, StatType.PHYSICAL_RESIST_PERCENT, 3f, 1, 5, List.of(Requirement.treePoints("Knight", 15)), "1.0.0"));
        layout.put("Knight_Physical_IncreasedResistance_2", new SkillTree.GridPosition(0, 5));

        nodes.put("Knight_Physical_IncreasedResistance_3", new SkillNode("Knight_Physical_IncreasedResistance_3", "Increase physical damage resistance by 25% per rank.", "Skills_Physical_IncreasedResistance_3.png", null, StatType.PHYSICAL_RESIST_PERCENT, 25f, 1, 1, List.of(Requirement.treePoints("Knight", 20)), "1.0.0"));
        layout.put("Knight_Physical_IncreasedResistance_3", new SkillTree.GridPosition(0, 6));

        // ---- Parry Window Nodes ---- //
        nodes.put("Knight_Parry_IncreasedChance_1", new SkillNode("Knight_Parry_IncreasedChance_1", "Increase parry window by 0.05 seconds per rank.", "Skills_Parry_IncreasedChance_1.png", null, StatType.PARRY_WINDOW_FLAT, .05f, 1, 10, List.of(Requirement.treePoints("Knight", 10)), "1.0.0"));
        layout.put("Knight_Parry_IncreasedChance_1", new SkillTree.GridPosition(2, 4));

        nodes.put("Knight_Parry_IncreasedChance_2", new SkillNode("Knight_Parry_IncreasedChance_2", "Increase parry window by 0.10 seconds per rank.", "Skills_Parry_IncreasedChance_2.png", null, StatType.PARRY_WINDOW_FLAT, .1f, 1, 5, List.of(Requirement.treePoints("Knight", 15)), "1.0.0"));
        layout.put("Knight_Parry_IncreasedChance_2", new SkillTree.GridPosition(2, 5));

        // ---- Register the Tree ---- //
        REGISTRY.put("Knight", new SkillTree("Knight", "Knight", "Weapon", List.of(), List.of(), "1.0.0", nodes, layout, 8, 6));
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
        learnedAbilities = new HashSet<>();

        // Pass 1 — rebuild stats and abilities from current allocation
        for (SkillTree tree : REGISTRY.values()) {
            for (SkillNode node : tree.getNodes().values()) {
                if (node.getCurrentRank() == 0) continue;

                if (node.statType != null) {
                    skillStats.add(node.statType, node.statValuePerRank * node.getCurrentRank());
                }

                if (node.abilityId != null) {
                    learnedAbilities.add(node.abilityId);
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
    public int refund() {
        int refundPoints = 0;
        for (SkillTree tree : REGISTRY.values()) refundPoints += tree.refund();
        recalculate(); // rebuild derived state after full refund
        return refundPoints;
    }

    // Getters
    public String getVersion() { return version; }
    public Map<String, SkillTree> getRegistry() { return REGISTRY; }
    public EntityStats getSkillStats() { return skillStats; }
    public Set<String> getLearnedAbilities() { return learnedAbilities; }
    public boolean hasLearnedAbility(String abilityId) { return learnedAbilities.contains(abilityId); }
}