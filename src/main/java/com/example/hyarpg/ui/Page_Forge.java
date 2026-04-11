package com.example.hyarpg.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import au.ellie.hyui.builders.PageBuilder;

public class Page_Forge {

    // ── Overall panel dimensions ──────────────────────────────────────────────
    private static final int PANEL_W = 700;
    private static final int PANEL_H = 570;

    // ── Top category tabs (5 icons) ───────────────────────────────────────────
    private static final int CAT_TAB_SIZE   = 52;
    private static final int CAT_TAB_GAP    = 4;

    // ── Sub-category item-type icon strip ─────────────────────────────────────
    private static final int SUB_ICON_SIZE  = 44;

    // ── Preview area (weapon silhouette) ──────────────────────────────────────
    // Ratio is approx 900:500 → 450:250 to fit in the panel
    private static final int PREVIEW_W      = 450;
    private static final int PREVIEW_H      = 250;

    // ── Output slot (top-right of preview area) ────────────────────────────────
    private static final int OUTPUT_SLOT_SIZE = 64;

    // ── Quality-tier / action button strip ────────────────────────────────────
    private static final int ACTION_BTN_SIZE = 52;
    private static final int ACTION_BTN_GAP  = 6;

    // ── CRAFT button ──────────────────────────────────────────────────────────
    private static final int CRAFT_BTN_W    = 130;
    private static final int CRAFT_BTN_H    = 52;

    public static void open(Ref<EntityStore> ref, Store<EntityStore> store) {

        // ── Category tabs (5 icons) ───────────────────────────────────────────
        // Each is a raw-button with an icon image inside so we can use the
        // game's own Tab / TabSelectedOverlay textures for active state.
        String[] catIds     = { "cat_1h", "cat_2h", "cat_metal", "cat_leather", "cat_cloth" };
        String[] catIcons   = {
                "Common/UI/Custom/Placeholder_Icon.png",  // 1H Weapons
                "Common/UI/Custom/Placeholder_Icon.png",  // 2H Weapons
                "Common/UI/Custom/Placeholder_Icon.png",  // Metal Gear
                "Common/UI/Custom/Placeholder_Icon.png",  // Leather Gear
                "Common/UI/Custom/Placeholder_Icon.png"   // Cloth Gear
        };
        String[] catLabels  = { "1H Weapons", "2H Weapons", "Metal Gear", "Leather Gear", "Cloth Gear" };

        StringBuilder catTabsHtml = new StringBuilder();
        for (int i = 0; i < catIds.length; i++) {
            boolean isActive = (i == 0); // first tab active by default
            String bgImg = isActive
                    ? "background-image: url('Tab@2x.png') 4; background-color: #1a2a3a;"
                    : "background-color: #111a24;";

            catTabsHtml
                    .append("<button id=\"").append(catIds[i]).append("\"")
                    .append(" class=\"raw-button\"")
                    .append(" style=\"anchor-width: ").append(CAT_TAB_SIZE).append(";")
                    .append(" anchor-height: ").append(CAT_TAB_SIZE).append(";")
                    .append(" margin-right: ").append(CAT_TAB_GAP).append(";")
                    .append(bgImg)
                    .append("\"")
                    .append(" data-hyui-tooltiptext=\"").append(catLabels[i]).append("\"")
                    .append(">")
                    .append("<img src=\"Common/UI/Custom/Placeholder_Icon.png\"")
                    .append(" width=\"32\" height=\"32\" />")
                    .append("</button>");
        }

        // ── Sub-category icon strip (one placeholder for now) ─────────────────
        String subIconsHtml =
                "<button id=\"sub_daggers\" class=\"raw-button\""
                        + " style=\"anchor-width: " + SUB_ICON_SIZE + "; anchor-height: " + SUB_ICON_SIZE + ";"
                        + " margin-right: 4; background-color: #1e3a5f;\""
                        + " data-hyui-tooltiptext=\"Daggers\">"
                        + "<img src=\"Common/UI/Custom/Placeholder_Icon.png\" width=\"32\" height=\"32\" />"
                        + "</button>";

        // ── Quality / action buttons (3 shown: download + 2 locked) ───────────
        // Matching the original: left download-icon btn is active, the two lock-icon btns are disabled
        String actionBtnsHtml =
                // Download / material-select button (active)
                "<button id=\"action_main\" class=\"raw-button\""
                        + " style=\"anchor-width: " + ACTION_BTN_SIZE + "; anchor-height: " + ACTION_BTN_SIZE + ";"
                        + " margin-right: " + ACTION_BTN_GAP + "; background-color: #1e3a5f;\""
                        + " data-hyui-tooltiptext=\"Select Material\">"
                        + "<img src=\"Common/UI/Custom/Placeholder_Icon.png\" width=\"28\" height=\"28\" />"
                        + "</button>"
                        // Locked tier 1
                        + "<button disabled class=\"raw-button\""
                        + " style=\"anchor-width: " + ACTION_BTN_SIZE + "; anchor-height: " + ACTION_BTN_SIZE + ";"
                        + " margin-right: " + ACTION_BTN_GAP + "; background-color: #0d141c;\""
                        + " data-hyui-tooltiptext=\"Locked\">"
                        + "<img src=\"Common/UI/Custom/Placeholder_Icon.png\" width=\"28\" height=\"28\" />"
                        + "</button>"
                        // Locked tier 2
                        + "<button disabled class=\"raw-button\""
                        + " style=\"anchor-width: " + ACTION_BTN_SIZE + "; anchor-height: " + ACTION_BTN_SIZE + ";"
                        + " margin-right: " + ACTION_BTN_GAP + "; background-color: #0d141c;\""
                        + " data-hyui-tooltiptext=\"Locked\">"
                        + "<img src=\"Common/UI/Custom/Placeholder_Icon.png\" width=\"28\" height=\"28\" />"
                        + "</button>";

        // ── Full page HTML ─────────────────────────────────────────────────────
        String html = """
        <style>
            .forge-panel {
                background-image: url('Common/ContainerBackground@2x.png') 8;
                anchor-width: ${PANEL_W};
                anchor-height: ${PANEL_H};
            }

            .forge-header {
                background-image: url('Common/ContainerHeader@2x.png') 8;
                anchor-width: ${PANEL_W};
                anchor-height: 50;
            }

            .forge-sub-header {
                background-image: url('Common/ContainerPanelSeparatorFancyLine@2x.png') 4;
                anchor-width: ${PANEL_W};
                anchor-height: 4;
            }

            .preview-area {
                background-color: #0a1420;
                background-image: url('Common/ContainerBackground@2x.png') 6;
                anchor-width: ${PREVIEW_W};
                anchor-height: ${PREVIEW_H};
            }

            .output-slot {
                background-image: url('Common/ContainerBackground@2x.png') 4;
                anchor-width: ${OUTPUT_SLOT_SIZE};
                anchor-height: ${OUTPUT_SLOT_SIZE};
            }

            .separator-h {
                background-image: url('Common/ContainerPanelSeparatorFancyLine@2x.png') 4;
                anchor-width: ${PANEL_W};
                anchor-height: 4;
            }

            .craft-button {
                anchor-width: ${CRAFT_BTN_W};
                anchor-height: ${CRAFT_BTN_H};
            }

            .section-label {
                font-size: 11;
                color: #8899aa;
                text-transform: uppercase;
                letter-spacing: 2;
            }
        </style>

        <div class="page-overlay">
            <!-- ── Inventory panel below (rendered behind forge so it shows up) -->
            <!-- We keep the forge panel as the primary focus; inventory is separate. -->

            <!-- ── FORGE PANEL ──────────────────────────────────────────── -->
            <div class="forge-panel"
                 style="layout-mode: top;">

                <!-- ── HEADER ROW: breadcrumb only ── -->
                <div class="forge-header"
                     style="layout-mode: left; padding: 0 8;">

                    <!-- Breadcrumb: FORGE > WEAPONS -->
                    <div style="layout-mode: left; flex-weight: 1; vertical-align: center; padding-left: 10;">
                        <p style="font-size: 12; color: #8899aa; letter-spacing: 1; text-transform: uppercase; vertical-align: center;">FORGE</p>
                        <p style="font-size: 12; color: #8899aa; vertical-align: center; margin-left: 4; margin-right: 4;"> ▶ </p>
                        <p style="font-size: 12; color: #ffffff; letter-spacing: 1; text-transform: uppercase; vertical-align: center;"><span data-hyui-bold="true">WEAPONS</span></p>
                    </div>
                </div>

                <!-- ── CATEGORY TABS (5 icons) ── -->
                <div style="layout-mode: left; padding-left: 10; padding-top: 8; padding-bottom: 6;
                            anchor-width: ${PANEL_W}; anchor-height: 68;">
                    ${CAT_TABS}
                </div>

                <!-- Thin separator line -->
                <div class="separator-h"></div>

                <!-- ── SUB-CATEGORY ICON STRIP ── -->
                <div style="layout-mode: left; padding-left: 10; padding-top: 6; padding-bottom: 6;
                            anchor-width: ${PANEL_W}; anchor-height: 60;">
                    ${SUB_ICONS}
                </div>

                <!-- Thin separator line -->
                <div class="separator-h"></div>

                <!-- ── MAIN BODY: preview + output slot side-by-side ── -->
                <div style="layout-mode: left; anchor-width: ${PANEL_W}; anchor-height: ${PREVIEW_H};
                            padding-left: 10; padding-top: 10;">

                    <!-- Preview / weapon silhouette area -->
                    <div class="preview-area">
                        <img src="Common/UI/Custom/Placeholder_Weapon_Preview.png"
                             width="${PREVIEW_W}" height="${PREVIEW_H}" />
                    </div>

                    <!-- Output slot (top-right of preview, slight overlap inset) -->
                    <div style="layout-mode: top; margin-left: 10; margin-top: 0;">
                        <div class="output-slot"
                             style="margin-bottom: 8;">
                            <!-- Empty output slot – item icon will be placed here when crafted -->
                        </div>
                    </div>

                </div>

                <!-- Thin separator line -->
                <div class="separator-h" style="margin-top: 10;"></div>

                <!-- ── BOTTOM ROW: quality/action buttons + CRAFT ── -->
                <div style="layout-mode: left; anchor-width: ${PANEL_W}; anchor-height: 72;
                            padding-left: 10; padding-top: 8;">

                    <!-- Left: quality tier / action buttons -->
                    <div style="layout-mode: left; flex-weight: 1; vertical-align: center;">
                        ${ACTION_BTNS}
                    </div>

                    <!-- Right: CRAFT button -->
                    <button id="craftBtn" class="secondary-button"
                            style="anchor-width: ${CRAFT_BTN_W}; anchor-height: ${CRAFT_BTN_H}; margin-right: 10;"
                            disabled>
                        CRAFT
                    </button>

                </div>

            </div>
        </div>
        """;

        html = html
                .replace("${PANEL_W}",          String.valueOf(PANEL_W))
                .replace("${PANEL_H}",          String.valueOf(PANEL_H))
                .replace("${PREVIEW_W}",        String.valueOf(PREVIEW_W))
                .replace("${PREVIEW_H}",        String.valueOf(PREVIEW_H))
                .replace("${OUTPUT_SLOT_SIZE}", String.valueOf(OUTPUT_SLOT_SIZE))
                .replace("${CRAFT_BTN_W}",      String.valueOf(CRAFT_BTN_W))
                .replace("${CRAFT_BTN_H}",      String.valueOf(CRAFT_BTN_H))
                .replace("${CAT_TABS}",         catTabsHtml.toString())
                .replace("${SUB_ICONS}",        subIconsHtml)
                .replace("${ACTION_BTNS}",      actionBtnsHtml);

        Player    player    = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        PageBuilder builder = PageBuilder.pageForPlayer(playerRef).fromHtml(html);

        builder.addEventListener("craftBtn", CustomUIEventBindingType.Activating, (ctx) -> {
            // TODO: trigger craft logic
        });

        // ── Category tab events ───────────────────────────────────────────────
        for (String catId : catIds) {
            builder.addEventListener(catId, CustomUIEventBindingType.Activating, (ctx) -> {
                // TODO: switch active category, reopen page with new selection
            });
        }

        // ── Sub-icon events ───────────────────────────────────────────────────
        builder.addEventListener("sub_daggers", CustomUIEventBindingType.Activating, (ctx) -> {
            // TODO: switch weapon sub-type
        });

        // ── Action button events ──────────────────────────────────────────────
        builder.addEventListener("action_main", CustomUIEventBindingType.Activating, (ctx) -> {
            // TODO: open material selection
        });

        builder.open(store);
    }
}