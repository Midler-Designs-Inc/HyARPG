package com.example.hyarpg.ui;

// Mod Imports
import com.example.hyarpg.utils.skills.Requirement;
import com.example.hyarpg.utils.skills.SkillLibrary;
import com.example.hyarpg.utils.skills.SkillNode;
import com.example.hyarpg.utils.skills.SkillTree;

// Hytale Imports
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

// Java Imports
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CustomPage_SkillTreeTooltip {

    private static final int MAX_LINES = 8;

    // a single colored line of tooltip content
    public record TooltipLine(String text, String color) {
        public static TooltipLine white(String text)  { return new TooltipLine(text, "#ffffff"); }
        public static TooltipLine grey(String text)   { return new TooltipLine(text, "#aaaaaa"); }
        public static TooltipLine gold(String text)   { return new TooltipLine(text, "#c8a84b"); }
        public static TooltipLine red(String text)    { return new TooltipLine(text, "#ff6666"); }
        public static TooltipLine orange(String text) { return new TooltipLine(text, "#ff9944"); }
    }

    // full tooltip content — title + up to MAX_LINES body lines
    public record TooltipContent(String title, List<TooltipLine> lines) {}

    // build tooltip content for a skill node based on its current state
    public static TooltipContent buildNodeTooltip(@Nonnull SkillNode structureNode, @Nullable SkillNode playerNode, @Nonnull SkillLibrary structureLibrary) {
        int     currentRank = (playerNode != null) ? playerNode.getCurrentRank() : 0;
        boolean isLocked    = (playerNode != null) && playerNode.getIsLocked();
        boolean isMaxed     = currentRank >= structureNode.maxRanks;

        List<TooltipLine> lines = new ArrayList<>();

        if (isLocked) {
            // show unmet requirements
            lines.add(TooltipLine.grey("Locked — Requirements:"));
            if (structureNode.requirements.isEmpty()) {
                lines.add(TooltipLine.red("  Unknown"));
            } else {
                for (Requirement req : structureNode.requirements) {
                    lines.add(TooltipLine.red("  " + buildRequirementText(req, structureLibrary)));
                }
            }
        } else if (!isMaxed) {
            // show rank, cost, weapon requirements
            lines.add(TooltipLine.white("Rank: " + currentRank + " / " + structureNode.maxRanks));
            lines.add(TooltipLine.gold("Cost: " + structureNode.cost + " point" + (structureNode.cost != 1 ? "s" : "")));
            String weaponReqs = buildWeaponRequirementsText(structureNode);
            if (weaponReqs != null) lines.add(TooltipLine.orange("Requires: " + weaponReqs));
        } else {
            // maxed
            lines.add(TooltipLine.gold("Maxed! " + structureNode.maxRanks + " / " + structureNode.maxRanks));
            String weaponReqs = buildWeaponRequirementsText(structureNode);
            if (weaponReqs != null) lines.add(TooltipLine.orange("Requires: " + weaponReqs));
        }

        return new TooltipContent(structureNode.displayName, lines);
    }

    // push tooltip content and make it visible
    public static void showTooltip(@Nonnull UICommandBuilder cmd, @Nonnull TooltipContent content) {
        cmd.set("#TooltipTitle.Text", content.title());

        List<TooltipLine> lines = content.lines();
        for (int i = 1; i <= MAX_LINES; i++) {
            if (i <= lines.size()) {
                TooltipLine line = lines.get(i - 1);
                cmd.set("#TooltipLine" + i + ".Text",            line.text());
                cmd.set("#TooltipLine" + i + ".Style.TextColor", line.color());
                cmd.set("#TooltipLine" + i + ".Visible",         true);
            } else {
                cmd.set("#TooltipLine" + i + ".Text",    "");
                cmd.set("#TooltipLine" + i + ".Visible", false);
            }
        }

        cmd.set("#TooltipPanel.Visible", true);
    }

    // hide the tooltip panel
    public static void hideTooltip(@Nonnull UICommandBuilder cmd) {
        cmd.set("#TooltipPanel.Visible", false);
    }

    private static String buildRequirementText(@Nonnull Requirement req, @Nonnull SkillLibrary structureLibrary) {
        return switch (req.type()) {
            case NODE_MIN_RANK -> {
                SkillNode reqNode = structureLibrary.findNode(req.targetId());
                String name = reqNode != null ? reqNode.displayName : req.targetId();
                yield "[Node] " + name + " — Rank " + req.minValue();
            }
            case TREE_MIN_POINTS -> {
                SkillTree reqTree = structureLibrary.getRegistry().get(req.targetId());
                String name = reqTree != null ? reqTree.displayName : req.targetId();
                yield "[Tree] " + name + " — " + req.minValue() + " points spent";
            }
        };
    }

    private static String buildWeaponRequirementsText(@Nonnull SkillNode node) {
        if (node.ability == null || node.ability.requiredWeapons == null || node.ability.requiredWeapons.isEmpty()) return null;
        return String.join(", ", node.ability.requiredWeapons);
    }
}
