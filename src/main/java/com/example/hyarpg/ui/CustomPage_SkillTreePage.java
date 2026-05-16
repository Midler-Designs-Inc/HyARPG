package com.example.hyarpg.ui;

// Mod Imports
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.modules.Module_RPGSystem;
import com.example.hyarpg.utils.skills.SkillLibrary;
import com.example.hyarpg.utils.skills.SkillNode;
import com.example.hyarpg.utils.skills.SkillTree;

// Hytale Imports
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPage;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

// Java Imports
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CustomPage_SkillTreePage extends InteractiveCustomUIPage<CustomPage_SkillTreePage.PageData> {

    // grid dimensions — fixed to match SkillLibrary tree definitions
    private static final int GRID_ROWS = 7;
    private static final int GRID_COLS = 9;
    private static final int MAX_NAV   = 30;

    // ordered list of unlocked tree ids — populated on first build, drives nav slot assignment
    private final List<String> navTreeIds = new ArrayList<>();

    // active tree shown in the grid — null only before first build
    private String activeTreeId = null;

    public CustomPage_SkillTreePage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        // load the UI file
        cmd.append("CustomPage_SkillTreePage.ui");

        // bind nav buttons
        for (int i = 0; i < MAX_NAV; i++) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#NavBtn" + i, EventData.of("Action", "nav:" + i));
        }

        // bind all cell buttons and hover events for every grid position
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                String cid = "R" + row + "C" + col;
                events.addEventBinding(CustomUIEventBindingType.Activating,   "#NodePlusBtn" + cid, EventData.of("Action", "invest:" + row + ":" + col));
                events.addEventBinding(CustomUIEventBindingType.Activating,   "#NodeQBtn"    + cid, EventData.of("Action", "equip_q:" + row + ":" + col));
                events.addEventBinding(CustomUIEventBindingType.Activating,   "#NodeEBtn"    + cid, EventData.of("Action", "equip_e:" + row + ":" + col));
                events.addEventBinding(CustomUIEventBindingType.Activating,   "#NodeRBtn"    + cid, EventData.of("Action", "equip_r:" + row + ":" + col));
                events.addEventBinding(CustomUIEventBindingType.MouseEntered, "#NodeCell" + cid, EventData.of("Action", "hover:"   + row + ":" + col), false);
                events.addEventBinding(CustomUIEventBindingType.MouseExited,  "#NodeCell" + cid, EventData.of("Action", "unhover:" + row + ":" + col), false);
            }
        }

        // on first open resolve nav tree list and default active tree
        if (activeTreeId == null) {
            Component_RPG_Player rpg      = getRpg(ref, store);
            SkillLibrary structureLibrary = getStructureLibrary(rpg);
            for (Map.Entry<String, SkillTree> e : structureLibrary.getRegistry().entrySet()) {
                if (!e.getValue().getIsLocked()) navTreeIds.add(e.getKey());
            }
            if (!navTreeIds.isEmpty()) activeTreeId = navTreeIds.get(0);
        }

        // push full initial state
        pushNavState(cmd, ref, store);
        pushGridState(cmd, ref, store);
        pushSkillPoints(cmd, ref, store);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        // route incoming actions
        if (data.action == null)                      { sendUpdate((UICommandBuilder) null, false); return; }
        if (data.action.startsWith("nav:"))           { handleNav(Integer.parseInt(data.action.substring("nav:".length())), ref, store); }
        else if (data.action.startsWith("invest:"))   { handleInvest(data.action.substring("invest:".length()), ref, store); }
        else if (data.action.startsWith("equip_q:"))  { handleEquip(data.action.substring("equip_q:".length()), "q", ref, store); }
        else if (data.action.startsWith("equip_e:"))  { handleEquip(data.action.substring("equip_e:".length()), "e", ref, store); }
        else if (data.action.startsWith("equip_r:"))  { handleEquip(data.action.substring("equip_r:".length()), "r", ref, store); }
        else if (data.action.startsWith("hover:"))    { handleHover(data.action.substring("hover:".length()), ref, store); }
        else if (data.action.startsWith("unhover:"))  { handleUnhover(); }
        else                                          { sendUpdate((UICommandBuilder) null, false); }
    }

    // switch active tree and push the new grid without rebuilding the page
    private void handleNav(int navIndex, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        if (navIndex < 0 || navIndex >= navTreeIds.size()) { sendUpdate((UICommandBuilder) null, false); return; }
        UICommandBuilder cmd = new UICommandBuilder();

        clearGridState(cmd);
        this.activeTreeId = navTreeIds.get(navIndex);
        pushNavState(cmd, ref, store);
        pushGridState(cmd, ref, store);
        sendUpdate(cmd, false);
    }

    // invest one rank into the node at this grid position
    private void handleInvest(@Nonnull String rowCol, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        UICommandBuilder cmd        = new UICommandBuilder();
        Component_RPG_Player rpg   = getRpg(ref, store);
        Player player              = store.getComponent(ref, Player.getComponentType());
        SkillNode node             = resolveNodeFromRowCol(rowCol, rpg);
        if (node == null) { sendUpdate((UICommandBuilder) null, false); return; }

        try {
            rpg.skillPoints = node.allocatePoints(rpg.skillPoints);
            rpg.skillLibrary.recalculate();
            rpg.calculateGearScore(player.getReference(), store);
            rpg.calculateAffixStats(player.getReference(), store);
        } catch (Exception e) {
            sendUpdate((UICommandBuilder) null, false);
            return;
        }

        // push the full grid so any newly unlocked nodes reflect immediately
        pushGridState(cmd, ref, store);
        pushSkillPoints(cmd, ref, store);
        sendUpdate(cmd, false);
    }

    // equip a maxed ability node to the given slot
    private void handleEquip(@Nonnull String rowCol, @Nonnull String slot, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        UICommandBuilder cmd      = new UICommandBuilder();
        Component_RPG_Player rpg  = getRpg(ref, store);
        SkillNode node            = resolveNodeFromRowCol(rowCol, rpg);
        if (node == null || node.ability == null) { sendUpdate((UICommandBuilder) null, false); return; }

        switch (slot) {
            case "q" -> {
                if (!node.ability.ultimateAbility) { sendUpdate((UICommandBuilder) null, false); return; }
                rpg.ultimateAbility     = node.id;
                rpg.ultimateAbilityIcon = node.iconId;
            }
            case "e" -> {
                if (node.ability.ultimateAbility) { sendUpdate((UICommandBuilder) null, false); return; }
                rpg.primaryAbility     = node.id;
                rpg.primaryAbilityIcon = node.iconId;
                if (Objects.equals(rpg.secondaryAbility, node.id)) { rpg.secondaryAbility = null; rpg.secondaryAbilityIcon = null; }
            }
            case "r" -> {
                if (node.ability.ultimateAbility) { sendUpdate((UICommandBuilder) null, false); return; }
                rpg.secondaryAbility     = node.id;
                rpg.secondaryAbilityIcon = node.iconId;
                if (Objects.equals(rpg.primaryAbility, node.id)) { rpg.primaryAbility = null; rpg.primaryAbilityIcon = null; }
            }
        }

        sendUpdate(cmd, false);
    }

    // show the tooltip for the hovered cell — bypasses acknowledgment queue for reliable hover response
    private void handleHover(@Nonnull String rowCol, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        UICommandBuilder cmd      = new UICommandBuilder();
        Component_RPG_Player rpg  = getRpg(ref, store);
        SkillLibrary structure    = getStructureLibrary(rpg);
        SkillNode structureNode   = resolveNodeFromRowCol(rowCol, structure);
        if (structureNode == null) { sendHoverUpdate(new UICommandBuilder()); return; }

        SkillTree playerTree = getPlayerTree(rpg, activeTreeId);
        SkillNode playerNode = (playerTree != null && playerTree.getNodes() != null) ? playerTree.getNodes().get(structureNode.id) : null;

        CustomPage_SkillTreeTooltip.TooltipContent content = CustomPage_SkillTreeTooltip.buildNodeTooltip(structureNode, playerNode, structure);
        CustomPage_SkillTreeTooltip.showTooltip(cmd, content);
        sendHoverUpdate(cmd);
    }

    // hide tooltip when mouse exits a cell — bypasses acknowledgment queue for reliable hover response
    private void handleUnhover() {
        UICommandBuilder cmd = new UICommandBuilder();
        CustomPage_SkillTreeTooltip.hideTooltip(cmd);
        sendHoverUpdate(cmd);
    }

    // bypasses PageManager acknowledgment counter so hover events are never blocked by queued updates
    private void sendHoverUpdate(@Nonnull UICommandBuilder cmd) {
        Ref<EntityStore> ref = this.playerRef.getReference();
        if (ref == null) return;
        this.playerRef.getPacketHandler().writeNoCache(new CustomPage(
                this.getClass().getName(), false, false, this.lifetime,
                cmd.getCommands(), UIEventBuilder.EMPTY_EVENT_BINDING_ARRAY
        ));
    }

    // push nav button labels, visibility, and active highlight
    private void pushNavState(@Nonnull UICommandBuilder cmd, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        Component_RPG_Player rpg  = getRpg(ref, store);
        SkillLibrary structure    = getStructureLibrary(rpg);

        for (int i = 0; i < MAX_NAV; i++) {
            if (i < navTreeIds.size()) {
                String treeId    = navTreeIds.get(i);
                SkillTree tree   = structure.getRegistry().get(treeId);
                boolean isActive = treeId.equals(activeTreeId);
                cmd.set("#NavSlot" + i + ".Visible", true);
                cmd.set("#NavBtnLabel" + i + ".Text", tree != null ? tree.displayName : treeId);
                cmd.set("#NavActiveHighlight" + i + ".Visible", isActive);
            } else {
                cmd.set("#NavSlot" + i + ".Visible", false);
            }
        }

        // update active tree name in title bar
        SkillTree activeTree = activeTreeId != null ? structure.getRegistry().get(activeTreeId) : null;
        cmd.set("#ActiveTreeNameLabel.Text", activeTree != null ? "— " + activeTree.displayName : "");
    }

    // push all cell states for the active tree
    private void pushGridState(@Nonnull UICommandBuilder cmd, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        if (activeTreeId == null) return;
        Component_RPG_Player rpg  = getRpg(ref, store);
        SkillLibrary structure    = getStructureLibrary(rpg);
        SkillTree structureTree   = structure.getRegistry().get(activeTreeId);
        SkillTree playerTree      = getPlayerTree(rpg, activeTreeId);
        if (structureTree == null) return;

        Map<String, SkillNode>              nodes  = structureTree.getNodes();
        Map<String, SkillTree.GridPosition> layout = structureTree.getLayout();
        if (nodes == null || layout == null) return;

        // build reverse lookup: (row, col) -> structureNode
        SkillNode[][] grid = new SkillNode[GRID_ROWS][GRID_COLS];
        for (Map.Entry<String, SkillNode> e : nodes.entrySet()) {
            SkillTree.GridPosition pos = layout.get(e.getKey());
            if (pos == null || pos.row() >= GRID_ROWS || pos.col() >= GRID_COLS) continue;
            grid[pos.row()][pos.col()] = e.getValue();
        }

        // push each cell — hide empty ones
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                SkillNode structureNode = grid[row][col];
                if (structureNode != null) {
                    SkillNode playerNode = (playerTree != null && playerTree.getNodes() != null) ? playerTree.getNodes().get(structureNode.id) : null;
                    pushCellState(cmd, structureNode, playerNode, row, col, rpg);
                } else {
                    // empty cell — hide all contents so the button shell holds spacing only
                    String cid = "R" + row + "C" + col;
                    cmd.set("#NodeIconArea"  + cid + ".Visible", false);
                    cmd.set("#NodeRankLabel" + cid + ".Visible", false);
                    cmd.set("#NodePlusBtn"   + cid + ".Visible", false);
                    cmd.set("#NodeQBtn"      + cid + ".Visible", false);
                    cmd.set("#NodeERBtns"    + cid + ".Visible", false);
                }
            }
        }
    }

    // hide all cell contents — called before switching trees
    private void clearGridState(@Nonnull UICommandBuilder cmd) {
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                String cid = "R" + row + "C" + col;
                cmd.set("#NodeIconArea"  + cid + ".Visible", false);
                cmd.set("#NodeRankLabel" + cid + ".Visible", false);
                cmd.set("#NodePlusBtn"   + cid + ".Visible", false);
                cmd.set("#NodeQBtn"      + cid + ".Visible", false);
                cmd.set("#NodeERBtns"    + cid + ".Visible", false);
            }
        }
        CustomPage_SkillTreeTooltip.hideTooltip(cmd);
    }

    // push the full visual state of a single node cell
    private void pushCellState(@Nonnull UICommandBuilder cmd, @Nonnull SkillNode structureNode, @Nullable SkillNode playerNode, int row, int col, @Nonnull Component_RPG_Player rpg) {
        String  cid         = "R" + row + "C" + col;
        int     currentRank = (playerNode != null) ? playerNode.getCurrentRank() : 0;
        boolean isLocked    = (playerNode != null) && playerNode.getIsLocked();
        boolean isMaxed     = currentRank >= structureNode.maxRanks;
        boolean hasAbility  = structureNode.ability != null;
        boolean isUltimate  = hasAbility && structureNode.ability.ultimateAbility;

        // show icon area and rank label
        cmd.set("#NodeIconArea" + cid + ".Visible", true);
        cmd.set("#NodeRankLabel" + cid + ".Visible", true);

        // icon background image
        cmd.set("#NodeIcon" + cid + ".Background", "Skills/Icons/" + structureNode.iconId);

        // lock overlay
        cmd.set("#NodeLockOverlay" + cid + ".Visible", isLocked);

        // rank label — gold when maxed, dim when locked, grey otherwise
        String rankColor = isLocked ? "#444444" : (isMaxed ? "#c8a84b" : "#aaaaaa");
        cmd.set("#NodeRankLabel" + cid + ".Text", currentRank + "/" + structureNode.maxRanks);
        cmd.set("#NodeRankLabel" + cid + ".Style.TextColor", rankColor);

        // hide all button variants first, then show the correct one
        cmd.set("#NodePlusBtn" + cid + ".Visible", false);
        cmd.set("#NodeQBtn"    + cid + ".Visible", false);
        cmd.set("#NodeERBtns"  + cid + ".Visible", false);

        if (!isMaxed) {
            cmd.set("#NodePlusBtn" + cid + ".Visible",  true);
            cmd.set("#NodePlusBtn" + cid + ".Disabled", isLocked || rpg.skillPoints < structureNode.cost);
        } else if (hasAbility && isUltimate) {
            cmd.set("#NodeQBtn"   + cid + ".Visible", true);
        } else if (hasAbility) {
            cmd.set("#NodeERBtns" + cid + ".Visible", true);
        }
        // maxed stat node — no button
    }

    // push available skill points to the nav label
    private void pushSkillPoints(@Nonnull UICommandBuilder cmd, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        cmd.set("#SkillPointsLabel.Text", String.valueOf(getRpg(ref, store).skillPoints));
    }

    // resolve from the player's live library — used for invest and equip where we need the actual mutable node
    @Nullable
    private SkillNode resolveNodeFromRowCol(@Nonnull String rowCol, @Nonnull Component_RPG_Player rpg) {
        return resolveNodeFromRowCol(rowCol, rpg.skillLibrary);
    }

    @Nullable
    private SkillNode resolveNodeFromRowCol(@Nonnull String rowCol, @Nonnull SkillLibrary structureLibrary) {
        int[] rc = parseRowCol(rowCol);
        if (rc == null || activeTreeId == null) return null;
        SkillTree structureTree = structureLibrary.getRegistry().get(activeTreeId);
        if (structureTree == null || structureTree.getNodes() == null || structureTree.getLayout() == null) return null;
        for (Map.Entry<String, SkillNode> e : structureTree.getNodes().entrySet()) {
            SkillTree.GridPosition pos = structureTree.getLayout().get(e.getKey());
            if (pos != null && pos.row() == rc[0] && pos.col() == rc[1]) return e.getValue();
        }
        return null;
    }

    // get the player's live tree for the given tree id
    @Nullable
    private SkillTree getPlayerTree(@Nonnull Component_RPG_Player rpg, @Nullable String treeId) {
        if (treeId == null || rpg.skillLibrary.getRegistry() == null) return null;
        return rpg.skillLibrary.getRegistry().get(treeId);
    }

    // fresh structure library built from the player's current version
    private SkillLibrary getStructureLibrary(@Nonnull Component_RPG_Player rpg) {
        return new SkillLibrary(rpg.skillLibrary.getVersion());
    }

    private Component_RPG_Player getRpg(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        return store.getComponent(ref, Module_RPGSystem.componentTypeRPGPlayer);
    }

    // parse "row:col" into [row, col] — returns null on bad input
    @Nullable
    private int[] parseRowCol(@Nonnull String rowCol) {
        String[] parts = rowCol.split(":");
        if (parts.length != 2) return null;
        try { return new int[]{ Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) }; }
        catch (NumberFormatException e) { return null; }
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec
                .<PageData>builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action).add()
                .build();

        public String action;
    }
}