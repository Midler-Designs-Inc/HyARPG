package com.example.hyarpg.ui;

// Mod imports
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.modules.Module_RPG_System;
import com.example.hyarpg.utils.skills.Requirement;
import com.example.hyarpg.utils.skills.SkillLibrary;
import com.example.hyarpg.utils.skills.SkillNode;
import com.example.hyarpg.utils.skills.SkillTree;

// Hytale imports
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// HyUI imports
import au.ellie.hyui.builders.PageBuilder;

import java.util.Map;
import java.util.Objects;

public class Page_SkillTree {

    // Page dimensions
    private static final int CONTAINER_WIDTH  = 1100;
    private static final int CONTAINER_HEIGHT = 700;

    // The container chrome (title bar + border) eats ~50px vertically, ~30px horizontally
    private static final int INNER_WIDTH  = CONTAINER_WIDTH  - 30;
    private static final int INNER_HEIGHT = CONTAINER_HEIGHT - 50;

    // Column height — reduce further to stay inside the container chrome
    private static final int COL_HEIGHT   = INNER_HEIGHT - 20;

    // Left nav column
    private static final int NAV_WIDTH    = 200;
    // Nav scrollable area = col height minus the "Skill Trees" <p> header (~28px)
    private static final int NAV_SCROLL_H = COL_HEIGHT - 28;

    // Right grid column = remaining space after nav + 10px gutter
    private static final int GRID_WIDTH    = INNER_WIDTH - NAV_WIDTH - 10;
    // Grid scrollable area = col height minus the tree-name <p> header (~28px)
    private static final int GRID_SCROLL_H = COL_HEIGHT - 28;

    // Node cell sizing
    private static final int NODE_SIZE  = 64;
    private static final int CELL_SIZE  = 76;  // icon + small padding, sets cell width & height

    // -------------------------------------------------------------------------
    // Entry points
    // -------------------------------------------------------------------------

    public static void open(Ref<EntityStore> ref, Store<EntityStore> store) {
        open(ref, store, null);
    }

    public static void open(Ref<EntityStore> ref, Store<EntityStore> store, String selectedTreeId) {
        Player player            = store.getComponent(ref, Player.getComponentType());
        Component_RPG_Player rpg = store.getComponent(ref, Module_RPG_System.componentTypeRPGPlayer);

        // Use a fresh SkillLibrary for structure and layout — the Gson-deserialized
        // instance on rpg.skillLibrary has null layout/node maps due to bypassing
        // the public constructor. However we MUST read locked/rank state from the
        // player's own library since the fresh one always has default (unlocked, 0 rank) state.
        SkillLibrary structureLibrary = new SkillLibrary(rpg.skillLibrary.getVersion());
        SkillLibrary playerLibrary    = rpg.skillLibrary;

        // Resolve active tree — default to first unlocked entry in player library
        // (use playerLibrary for lock state, structureLibrary as fallback if player lib has null registry)
        String activeTreeId = selectedTreeId;
        if (activeTreeId == null) {
            for (Map.Entry<String, SkillTree> e : structureLibrary.getRegistry().entrySet()) {
                if (!e.getValue().getIsLocked()) {
                    activeTreeId = e.getKey();
                    break;
                }
            }
        }

        // Resolve active tree's display name
        String activeTreeName = "Select a Tree";
        if (activeTreeId != null && structureLibrary.getRegistry().containsKey(activeTreeId)) {
            activeTreeName = structureLibrary.getRegistry().get(activeTreeId).displayName;
        }

        // --- Build left nav HTML ---
        StringBuilder navHtml = new StringBuilder();
        for (Map.Entry<String, SkillTree> e : structureLibrary.getRegistry().entrySet()) {
            SkillTree tree = e.getValue();
            if (tree.getIsLocked()) continue;

            boolean isActive = tree.id.equals(activeTreeId);
            String  btnId    = "tree_btn_" + tree.id;
            String  activeBg = isActive ? "background-color: #1e3a5f;" : "";

            navHtml.append("<button id=\"").append(btnId).append("\"")
                    .append(" style=\"anchor-width: ").append(NAV_WIDTH - 16).append(";")
                    .append(" anchor-height: 36;")
                    .append(" margin-bottom: 4;")
                    .append(activeBg).append("\">")
                    .append(tree.displayName)
                    .append("</button>");
        }

        // --- Build right grid HTML ---
        String gridHtml = buildTreeGrid(structureLibrary, playerLibrary, activeTreeId);

        // --- Full page HTML ---
        // Layout: container-contents holds a single left-mode div that places
        // the nav column and grid column side by side.
        //
        // Left nav:  fixed anchor-width + anchor-height, topscrolling inside
        // Right grid: fixed anchor-width + anchor-height, the inner content div
        //             uses layout-mode: left so rows scroll horizontally.
        String html = """
        <div class="page-overlay">
            <button id="closeBtn"
                    style="anchor-bottom: 10; anchor-width: ${CONTAINER_WIDTH}; anchor-height: 40;">
                Close
            </button>

            <div class="container"
                 data-hyui-title="Skill Trees"
                 style="anchor-width: ${CONTAINER_WIDTH}; anchor-height: ${CONTAINER_HEIGHT};">

                <div class="container-contents">
                    <div style="layout-mode: left; anchor-width: ${INNER_WIDTH}; anchor-height: ${INNER_HEIGHT};">

                        <!-- ===== LEFT NAV ===== -->
                        <div style="layout-mode: top;
                                    anchor-width: ${NAV_WIDTH};
                                    anchor-height: ${COL_HEIGHT};
                                    background-color: #0d141c;">

                            <!-- Nav header -->
                            <p style="margin-left: 8; margin-top: 8; margin-bottom: 4;">
                                <span data-hyui-bold="true">Skill Trees</span>
                            </p>

                            <!-- Scrollable nav list — vertical scroll -->
                            <div style="layout-mode: topscrolling;
                                        anchor-width: ${NAV_WIDTH};
                                        anchor-height: ${NAV_SCROLL_H};"
                                    data-hyui-keep-scroll-position="true"
                                 data-hyui-scrollbar-style="&quot;Common.ui&quot; &quot;DefaultScrollbarStyle&quot;">
                                <div style="layout-mode: top; margin-left: 8; margin-top: 4;">
                                    ${NAV_HTML}
                                </div>
                            </div>
                        </div>

                        <!-- ===== RIGHT GRID ===== -->
                        <div style="layout-mode: top;
                                    anchor-width: ${GRID_WIDTH};
                                    anchor-height: ${COL_HEIGHT};
                                    margin-left: 10;">

                            <!-- Grid header -->
                            <p style="margin-left: 8; margin-top: 8; margin-bottom: 4;">
                                <span data-hyui-bold="true">${ACTIVE_TREE_NAME}</span>
                            </p>

                            <!-- Scrollable grid area — vertical scroll -->
                            <div style="layout-mode: topscrolling;
                                        anchor-width: ${GRID_WIDTH};
                                        anchor-height: ${GRID_SCROLL_H};"
                                        data-hyui-keep-scroll-position="true"
                                 data-hyui-scrollbar-style="&quot;Common.ui&quot; &quot;DefaultScrollbarStyle&quot;">
                                <div style="layout-mode: top; margin-left: 8; margin-top: 8;">
                                    ${GRID_HTML}
                                </div>
                            </div>
                        </div>

                    </div>
                </div>
            </div>
        </div>
        """;

        html = html
                .replace("${CONTAINER_WIDTH}",  String.valueOf(CONTAINER_WIDTH))
                .replace("${CONTAINER_HEIGHT}", String.valueOf(CONTAINER_HEIGHT))
                .replace("${INNER_WIDTH}",      String.valueOf(INNER_WIDTH))
                .replace("${INNER_HEIGHT}",     String.valueOf(INNER_HEIGHT))
                .replace("${COL_HEIGHT}",       String.valueOf(COL_HEIGHT))
                .replace("${NAV_WIDTH}",        String.valueOf(NAV_WIDTH))
                .replace("${NAV_SCROLL_H}",     String.valueOf(NAV_SCROLL_H))
                .replace("${GRID_WIDTH}",       String.valueOf(GRID_WIDTH))
                .replace("${GRID_SCROLL_H}",    String.valueOf(GRID_SCROLL_H))
                .replace("${NAV_HTML}",         navHtml.toString())
                .replace("${GRID_HTML}",        gridHtml)
                .replace("${ACTIVE_TREE_NAME}", activeTreeName);

        // --- Wire events ---
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        PageBuilder builder = PageBuilder.pageForPlayer(playerRef).fromHtml(html);

        // Close
        builder.addEventListener("closeBtn", CustomUIEventBindingType.Activating, (ctx) -> {
            player.getPageManager().setPage(ref, store, Page.None);
        });

        // Nav buttons — reopen page with selected tree
        for (Map.Entry<String, SkillTree> e : structureLibrary.getRegistry().entrySet()) {
            SkillTree tree = e.getValue();
            if (tree.getIsLocked()) continue;
            String btnId  = "tree_btn_" + tree.id;
            String treeId = tree.id;
            builder.addEventListener(btnId, CustomUIEventBindingType.Activating, (ctx) -> {
                player.getWorld().execute(() -> Page_SkillTree.open(ref, store, treeId));
            });
        }

        // Node buttons — wire up all interactive buttons in the active tree
        if (activeTreeId != null) {
            SkillTree activeStructureTree = structureLibrary.getRegistry().get(activeTreeId);
            SkillTree activePlayerTree    = (playerLibrary.getRegistry() != null)
                    ? playerLibrary.getRegistry().get(activeTreeId) : null;

            if (activeStructureTree != null) {
                for (SkillNode structureNode : activeStructureTree.getNodes().values()) {
                    SkillNode playerNode = (activePlayerTree != null && activePlayerTree.getNodes() != null)
                            ? activePlayerTree.getNodes().get(structureNode.id) : null;

                    boolean isLocked    = (playerNode != null) && playerNode.getIsLocked();
                    int     currentRank = (playerNode != null) ? playerNode.getCurrentRank() : 0;
                    boolean isMaxed     = currentRank >= structureNode.maxRanks;
                    boolean hasAbility  = structureNode.ability != null;
                    String  nodeId      = structureNode.id;
                    String  finalTreeId = activeTreeId;

                    if (isLocked) continue; // locked nodes have no interactive buttons

                    if (!isMaxed) {
                        // + button to allocate a rank
                        builder.addEventListener("node_" + nodeId, CustomUIEventBindingType.Activating, (ctx) -> {
                            player.getWorld().execute(() -> onNodeClicked(ref, store, player, rpg, finalTreeId, nodeId));
                        });
                    } else if (hasAbility && structureNode.ability.ultimateAbility) {
                        // Q button — equip ultimate
                        builder.addEventListener("equip_ult_" + nodeId, CustomUIEventBindingType.Activating, (ctx) -> {
                            player.getWorld().execute(() -> onEquipUltimateAbility(ref, store, player, rpg, finalTreeId, nodeId));
                        });
                    } else if (hasAbility) {
                        // E button — equip primary
                        builder.addEventListener("equip_pri_" + nodeId, CustomUIEventBindingType.Activating, (ctx) -> {
                            player.getWorld().execute(() -> onEquipPrimaryAbility(ref, store, player, rpg, finalTreeId, nodeId));
                        });
                        // R button — equip secondary
                        builder.addEventListener("equip_sec_" + nodeId, CustomUIEventBindingType.Activating, (ctx) -> {
                            player.getWorld().execute(() -> onEquipSecondaryAbility(ref, store, player, rpg, finalTreeId, nodeId));
                        });
                    }
                }
            }
        }

        builder.open(store);
    }

    // -------------------------------------------------------------------------
    // Grid builder
    // -------------------------------------------------------------------------

    private static String buildTreeGrid(SkillLibrary structureLibrary, SkillLibrary playerLibrary, String treeId) {
        if (treeId == null) {
            return "<p style=\"margin-left: 10; margin-top: 10;\">No tree selected.</p>";
        }

        SkillTree tree = structureLibrary.getRegistry().get(treeId);
        if (tree == null) {
            return "<p style=\"margin-left: 10; margin-top: 10;\">Tree not found.</p>";
        }

        Map<String, SkillNode>              nodes  = tree.getNodes();
        Map<String, SkillTree.GridPosition> layout = tree.getLayout();

        if (layout == null || nodes == null || nodes.isEmpty()) {
            return "<p style=\"margin-left: 10; margin-top: 10;\">No nodes in this tree.</p>";
        }

        // Build a parallel lookup of player's live node state (rank, locked).
        // playerLibrary may have null maps if Gson-deserialized, so guard carefully.
        Map<String, SkillNode> playerNodes = null;
        SkillTree playerTree = playerLibrary.getRegistry() != null
                ? playerLibrary.getRegistry().get(treeId) : null;
        if (playerTree != null && playerTree.getNodes() != null) {
            playerNodes = playerTree.getNodes();
        }

        // Derive grid dimensions from actual layout positions (safe vs. Gson deserialization)
        int maxCol = 0;
        int maxRow = 0;
        for (SkillTree.GridPosition pos : layout.values()) {
            if (pos == null) continue;
            if (pos.col() > maxCol) maxCol = pos.col();
            if (pos.row() > maxRow) maxRow = pos.row();
        }
        int cols = maxCol + 1;
        int rows = maxRow + 1;

        // Place nodes into a 2D grid array
        SkillNode[][] grid = new SkillNode[rows][cols];
        for (Map.Entry<String, SkillNode> entry : nodes.entrySet()) {
            SkillTree.GridPosition pos = layout.get(entry.getKey());
            if (pos == null) continue;
            int r = pos.row();
            int c = pos.col();
            if (r >= 0 && r < rows && c >= 0 && c < cols) {
                grid[r][c] = entry.getValue();
            }
        }

        // Render row-by-row
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < rows; row++) {
            sb.append("<div style=\"layout-mode: left; margin-bottom: 6;\">");

            for (int col = 0; col < cols; col++) {
                SkillNode structureNode = grid[row][col];
                if (structureNode != null) {
                    // Look up live state from player's library; fall back to structure node
                    SkillNode playerNode = (playerNodes != null)
                            ? playerNodes.get(structureNode.id) : null;
                    sb.append(buildNodeCell(structureNode, playerNode, structureLibrary));
                } else {
                    sb.append("<div style=\"anchor-width: ").append(CELL_SIZE)
                            .append("; anchor-height: ").append(CELL_SIZE + 42)
                            .append("; margin-right: 6;\"></div>");
                }
            }

            sb.append("</div>");
        }

        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Node cell builder
    // -------------------------------------------------------------------------

    // structureNode  — from fresh library: has iconId, displayName, maxRanks (layout/display data)
    // playerNode     — from player library: has currentRank, isLocked (live state); may be null
    // structureLibrary — needed to look up node display names for requirement tooltips
    private static String buildNodeCell(SkillNode structureNode, SkillNode playerNode, SkillLibrary structureLibrary) {
        int     currentRank = (playerNode != null) ? playerNode.getCurrentRank() : 0;
        boolean isLocked    = (playerNode != null) ? playerNode.getIsLocked() : false;
        boolean isMaxed     = currentRank >= structureNode.maxRanks;

        String bgColor   = isLocked ? "#0d0d0d" : (isMaxed ? "#2a2000" : "#111a24");
        String rankColor = isLocked ? "#444444"  : (isMaxed ? "#c8a84b" : "#aaaaaa");
        String rankLabel = currentRank + "/" + structureNode.maxRanks;

        StringBuilder sb = new StringBuilder();

        // Outer cell div
        sb.append("<div style=\"layout-mode: top;")
                .append(" anchor-width: ").append(CELL_SIZE).append(";")
                .append(" anchor-height: ").append(CELL_SIZE + 42).append(";")
                .append(" margin-right: 6;")
                .append(" background-color: ").append(bgColor).append(";\">");

        // Plain icon — no tooltip
        sb.append("<img src=\"").append(structureNode.iconId).append("\"")
                .append(" width=\"").append(NODE_SIZE).append("\"")
                .append(" height=\"").append(NODE_SIZE).append("\" />");

        if (isLocked) {
            sb.append("<img src=\"Skills_Locked_Overlay.png\"")
                    .append(" width=\"").append(NODE_SIZE).append("\"")
                    .append(" height=\"").append(NODE_SIZE).append("\"")
                    .append(" style=\"margin-top: -").append(NODE_SIZE).append(";\" />");
        }

        // Rank label
        sb.append("<p style=\"color: ").append(rankColor).append("; margin-top: 2; text-align: center;\">")
                .append(rankLabel).append("</p>");

        boolean hasAbility = structureNode.ability != null;

        if (isLocked || !isMaxed) {
            // + button — locked or not yet maxed
            String rankBtnId = "node_" + structureNode.id;
            if (!isLocked) {
                sb.append("<button id=\"").append(rankBtnId).append("\" class=\"small-tertiary-button\">");
            } else {
                sb.append("<button disabled class=\"small-tertiary-button\">");
            }
            sb.append("<tooltip>");
            sb.append("<span data-hyui-bold=\"true\">").append(structureNode.displayName).append("\n</span>");
            if (isLocked) {
                sb.append("<span>\nRequires:\n</span>");
                if (structureNode.requirements.isEmpty()) {
                    sb.append("<span data-hyui-color=\"#ff6666\">  Unknown</span>");
                } else {
                    for (Requirement req : structureNode.requirements) {
                        sb.append("<span data-hyui-color=\"#ff6666\">  ")
                                .append(buildRequirementText(req, structureLibrary))
                                .append("\n</span>");
                    }
                }
            } else {
                sb.append("<span data-hyui-color=\"#aaaaaa\">").append(currentRank).append("/").append(structureNode.maxRanks).append("</span>");
            }
            sb.append("</tooltip>");
            sb.append("+</button>");

        } else if (hasAbility && structureNode.ability.ultimateAbility) {
            // Q button — equip ultimate
            sb.append("<button id=\"equip_ult_").append(structureNode.id).append("\" class=\"small-tertiary-button\"")
                    .append(" style=\"anchor-width: ").append(CELL_SIZE).append("; anchor-height: 30;\">");
            sb.append("<tooltip>");
            sb.append("<span data-hyui-bold=\"true\">Equip as Ultimate Ability (Q)\n</span>");
            sb.append("<span>\n</span>");
            sb.append("<span>").append(structureNode.displayName).append("\n</span>");
            sb.append("<span data-hyui-color=\"#c8a84b\">Maxed! ").append(structureNode.maxRanks).append("/").append(structureNode.maxRanks).append("</span>");
            sb.append("</tooltip>");
            sb.append("Q</button>");

        } else if (hasAbility) {
            // E and R buttons — equip primary/secondary
            String eBtnId = "equip_pri_" + structureNode.id;
            String rBtnId = "equip_sec_" + structureNode.id;
            sb.append("<div style=\"layout-mode: left; anchor-width: ").append(CELL_SIZE).append(";\">");

            sb.append("<button id=\"").append(eBtnId).append("\" class=\"small-tertiary-button\"")
                    .append(" style=\"anchor-width: ").append(CELL_SIZE / 2).append("; anchor-height: 30;\">");
            sb.append("<tooltip>");
            sb.append("<span data-hyui-bold=\"true\">Equip as Primary Ability (E)\n</span>");
            sb.append("<span>\n</span>");
            sb.append("<span>").append(structureNode.displayName).append("\n</span>");
            sb.append("<span data-hyui-color=\"#c8a84b\">Maxed! ").append(structureNode.maxRanks).append("/").append(structureNode.maxRanks).append("</span>");
            sb.append("</tooltip>");
            sb.append("E</button>");

            sb.append("<button id=\"").append(rBtnId).append("\" class=\"small-tertiary-button\"")
                    .append(" style=\"anchor-width: ").append(CELL_SIZE / 2).append("; anchor-height: 30;\">");
            sb.append("<tooltip>");
            sb.append("<span data-hyui-bold=\"true\">Equip as Secondary Ability (R)\n</span>");
            sb.append("<span>\n</span>");
            sb.append("<span>").append(structureNode.displayName).append("\n</span>");
            sb.append("<span data-hyui-color=\"#c8a84b\">Maxed! ").append(structureNode.maxRanks).append("/").append(structureNode.maxRanks).append("</span>");
            sb.append("</tooltip>");
            sb.append("R</button>");

            sb.append("</div>");

        } else {
            // Maxed stat node — disabled + with tooltip
            sb.append("<button disabled class=\"small-tertiary-button\">");
            sb.append("<tooltip>");
            sb.append("<span data-hyui-bold=\"true\">").append(structureNode.displayName).append("\n</span>");
            sb.append("<span data-hyui-color=\"#c8a84b\">Maxed! ").append(structureNode.maxRanks).append("/").append(structureNode.maxRanks).append("</span>");
            sb.append("</tooltip>");
            sb.append("+</button>");
        }

        sb.append("</div>");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Tooltip builder
    // -------------------------------------------------------------------------

    private static String buildRequirementText(Requirement req, SkillLibrary structureLibrary) {
        switch (req.type()) {
            case NODE_MIN_RANK -> {
                SkillNode reqNode = structureLibrary.findNode(req.targetId());
                String nodeName = (reqNode != null) ? reqNode.displayName : req.targetId();
                return "[Node] " + nodeName + " - Rank " + req.minValue();
            }
            case TREE_MIN_POINTS -> {
                SkillTree reqTree = structureLibrary.getRegistry().get(req.targetId());
                String treeName = (reqTree != null) ? reqTree.displayName : req.targetId();
                return "[Tree] " + treeName + " - " + req.minValue() + " points spent";
            }
            default -> { return req.targetId() + " (" + req.minValue() + ")"; }
        }
    }

    // -------------------------------------------------------------------------
    // Node interaction
    // -------------------------------------------------------------------------

    private static void onNodeClicked(Ref<EntityStore> ref, Store<EntityStore> store, Player player, Component_RPG_Player rpg, String treeId, String nodeId) {
        try {
            SkillNode node = rpg.skillLibrary.findNode(nodeId);
            rpg.skillPoints = node.allocatePoints(rpg.skillPoints);
            rpg.skillLibrary.recalculate();

            // refresh gear score
            rpg.calculateGearScore(player);
            rpg.calculateAffixStats(player.getReference(), store);

            Page_SkillTree.open(ref, store, treeId);
        } catch (Exception e) {
            player.sendMessage(Message.raw("You got error trying to skill up: " + e));
        }
    }

    private static void onEquipUltimateAbility(Ref<EntityStore> ref, Store<EntityStore> store, Player player, Component_RPG_Player rpg, String treeId, String nodeId) {
        SkillNode node = rpg.skillLibrary.findNode(nodeId);
        if(node == null || node.ability == null || !node.ability.ultimateAbility) return;

        rpg.ultimateAbility = node.id;
        rpg.ultimateAbilityIcon = node.iconId;
    }

    private static void onEquipPrimaryAbility(Ref<EntityStore> ref, Store<EntityStore> store, Player player, Component_RPG_Player rpg, String treeId, String nodeId) {
        SkillNode node = rpg.skillLibrary.findNode(nodeId);
        if(node == null || node.ability == null || node.ability.ultimateAbility) return;

        rpg.primaryAbility = node.id;
        rpg.primaryAbilityIcon = node.iconId;

        // if this ability was in the other slot remove it
        if(rpg.secondaryAbility != null && Objects.equals(rpg.secondaryAbility, node.id)) {
            rpg.secondaryAbility = null;
            rpg.secondaryAbilityIcon = null;
        }
    }

    private static void onEquipSecondaryAbility(Ref<EntityStore> ref, Store<EntityStore> store, Player player, Component_RPG_Player rpg, String treeId, String nodeId) {
        SkillNode node = rpg.skillLibrary.findNode(nodeId);
        if(node == null || node.ability == null || node.ability.ultimateAbility) return;

        rpg.secondaryAbility = node.id;
        rpg.secondaryAbilityIcon = node.iconId;

        // if this ability was in the other slot remove it
        if(rpg.primaryAbility != null && Objects.equals(rpg.primaryAbility, node.id)) {
            rpg.primaryAbility = null;
            rpg.primaryAbilityIcon = null;
        }
    }
}