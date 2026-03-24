package com.example.hyarpg.utils.skills;

// Java Imports
import com.example.hyarpg.components.Component_RPG_Player;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SkillTree {

    // Grid position layout helper
    public record GridPosition(int col, int row) {}

    // Initial Class Properties
    public final String id;
    public final String displayName;
    public final String category;
    public final List<String> tags;
    public final List<Requirement> requirements;
    public final String version;

    // Internal Class Properties
    private final Map<String, SkillNode> nodes;
    private boolean isLocked;
    private final Map<String, GridPosition> layout;
    private final int gridColumns;
    private final int gridRows;

    public SkillTree(String id, String displayName, String category, List<String> tags, List<Requirement> requirements, String version, Map<String, SkillNode> nodes, Map<String, GridPosition> layout, int gridColumns, int gridRows) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.tags = tags;
        this.requirements = requirements;
        this.version = version;
        this.nodes = new LinkedHashMap<>(nodes);
        this.layout = new LinkedHashMap<>(layout);
        this.gridColumns = gridColumns;
        this.gridRows = gridRows;
    }

    // Getters
    public Map<String, SkillNode> getNodes() { return nodes; }
    public boolean getIsLocked() { return isLocked; }
    public String getVersion() { return version; }
    public Map<String, GridPosition> getLayout() { return layout; }
    public int getGridColumns() { return gridColumns; }
    public int getGridRows() { return gridRows; }

    // Sum of all points spent across all nodes in this tree — queried on the fly
    public int getPointsSpent() {
        int total = 0;
        for (SkillNode node : nodes.values()) total += node.getAllocatedPoints();
        return total;
    }

    // refund all nodes in this tree
    public int refund(Component_RPG_Player comp) {
        // check if this node is an equipped ability and if so, remove it
        int refundPoints = 0;
        for (SkillNode node : nodes.values()) refundPoints += node.refund(comp);
        return refundPoints;
    }

    public void lock() { this.isLocked = true; }
    public void unlock() { this.isLocked = false; }
}