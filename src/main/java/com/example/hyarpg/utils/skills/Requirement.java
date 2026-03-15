package com.example.hyarpg.utils.skills;

public record Requirement(RequirementType type, String targetId, int minValue) {

    public enum RequirementType {
        NODE_MIN_RANK,    // targetId = nodeId,  minValue = minimum rank required
        TREE_MIN_POINTS   // targetId = treeId,  minValue = minimum points spent in tree
    }

    // Convenience factory methods for readability in tree registration
    public static Requirement nodeRank(String nodeId, int minRank) {
        return new Requirement(RequirementType.NODE_MIN_RANK, nodeId, minRank);
    }

    public static Requirement treePoints(String treeId, int minPoints) {
        return new Requirement(RequirementType.TREE_MIN_POINTS, treeId, minPoints);
    }
}