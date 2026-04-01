package com.example.hyarpg.ui;

import com.example.hyarpg.components.Component_CraftingKnowledge;
import com.example.hyarpg.modules.Module_RPGSystem;
import com.example.hyarpg.utils.rooms.RoomType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import au.ellie.hyui.builders.PageBuilder;

import java.util.*;

public class Page_RecipeBook {

    public static void open(Ref<EntityStore> ref, Store<EntityStore> store) {
        Component_CraftingKnowledge craftingKnowledge = store.getComponent(ref, Module_RPGSystem.componentTypeCraftingKnowledge);

        String gearRecipesHTML = buildGearRecipesHTML(craftingKnowledge);
        String roomRecipesHTML = buildRoomRecipesHTML(craftingKnowledge);

        String html = """
        <div class="page-overlay">
            <button id="closeBtn" style="anchor-bottom: 10; anchor-width: 750; anchor-height: 40;">Close</button>

            <div class="container"
                 data-hyui-title="Recipe Book"
                 style="anchor-width: 750; anchor-height: 700;">

                <div class="container-contents" style="layout-mode: top; padding: 6;">

                    <nav id="recipe-tabs" class="tabs"
                         data-tabs="gear:Gear Recipes:gear-content,rooms:Room Recipes:rooms-content"
                         data-selected="gear">
                    </nav>

                    <!-- TAB 1: Gear Recipes -->
                    <div id="gear-content" class="tab-content"
                         data-hyui-tab-id="gear"
                         data-hyui-tab-nav="recipe-tabs">
                        <div style="layout-mode: topscrolling; anchor-width: 720; anchor-height: 600;"
                             data-hyui-scrollbar-style="&quot;Common.ui&quot; &quot;DefaultScrollbarStyle&quot;">
                            <div style="layout-mode: top; anchor-width: 700; margin-left: 15; margin-top: 10;">
                                ${GEAR_RECIPES_HTML}
                            </div>
                        </div>
                    </div>

                    <!-- TAB 2: Room Recipes -->
                    <div id="rooms-content" class="tab-content"
                         data-hyui-tab-id="rooms"
                         data-hyui-tab-nav="recipe-tabs">
                        <div style="layout-mode: topscrolling; anchor-width: 720; anchor-height: 600;"
                             data-hyui-scrollbar-style="&quot;Common.ui&quot; &quot;DefaultScrollbarStyle&quot;">
                            <div style="layout-mode: top; anchor-width: 700; margin-left: 10; margin-top: 10;">
                                ${ROOM_RECIPES_HTML}
                            </div>
                        </div>
                    </div>

                </div>
            </div>
        </div>
        """;

        html = html.replace("${GEAR_RECIPES_HTML}", gearRecipesHTML);
        html = html.replace("${ROOM_RECIPES_HTML}", roomRecipesHTML);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        PageBuilder.pageForPlayer(playerRef)
                .fromHtml(html)
                .addEventListener("closeBtn", CustomUIEventBindingType.Activating, (ctx) -> {
                    player.getPageManager().setPage(ref, store, Page.None);
                })
                .open(store);
    }

    // -------------------------------------------------------------------------
    // Gear Recipes Tab — unchanged
    // -------------------------------------------------------------------------
    private static String buildGearRecipesHTML(Component_CraftingKnowledge craftingKnowledge) {
        if (craftingKnowledge == null || craftingKnowledge.discoveredDroppableRecipes.isEmpty()) {
            return "<p style=\"margin-left: 10; margin-top: 10;\">No recipes discovered yet.</p>";
        }

        List<String> qualities = List.of("Common", "Uncommon", "Rare", "Epic", "Legendary");

        Map<String, String> baseItemToFirstDiscoveredId = new LinkedHashMap<>();
        for (String recipeId : craftingKnowledge.discoveredDroppableRecipes) {
            String baseId = recipeId;
            for (String quality : qualities) {
                if (recipeId.endsWith("_" + quality)) {
                    baseId = recipeId.substring(0, recipeId.length() - quality.length() - 1);
                    break;
                }
            }
            baseItemToFirstDiscoveredId.putIfAbsent(baseId, recipeId);
        }

        Map<String, List<String>> levelGroups = new LinkedHashMap<>();
        levelGroups.put("Level 0-19", new ArrayList<>());
        levelGroups.put("Level 20-29", new ArrayList<>());
        levelGroups.put("Level 30-39", new ArrayList<>());
        levelGroups.put("Level 40-49", new ArrayList<>());
        levelGroups.put("Level 50-59", new ArrayList<>());

        for (String baseId : baseItemToFirstDiscoveredId.keySet()) {
            int itemLevel = 0;
            for (String quality : qualities) {
                String variantId = baseId + "_" + quality;
                Item item = Item.getAssetMap().getAsset(variantId);
                if (item != null) {
                    itemLevel = item.getItemLevel();
                    break;
                }
            }

            String group;
            if (itemLevel < 20)       group = "Level 0-19";
            else if (itemLevel < 30)  group = "Level 20-29";
            else if (itemLevel < 40)  group = "Level 30-39";
            else if (itemLevel < 50)  group = "Level 40-49";
            else                      group = "Level 50-59";

            levelGroups.get(group).add(baseId);
        }

        for (List<String> group : levelGroups.values()) {
            Collections.sort(group);
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> groupEntry : levelGroups.entrySet()) {
            List<String> baseItems = groupEntry.getValue();
            if (baseItems.isEmpty()) continue;

            sb.append("<div style=\"layout-mode: top; anchor-width: 700; margin-top: 15; margin-bottom: 5;\">")
                    .append("<p style=\"font-size: 20; margin-left: 5; margin-top: 3; color: #888888;\">")
                    .append("<span data-hyui-bold=\"true\">").append(groupEntry.getKey()).append("</span>")
                    .append("</p></div>");

            for (String baseId : baseItems) {
                String displayName = baseId
                        .replace("Weapon_", "")
                        .replace("Armor_", "")
                        .replace("_", " ");

                sb.append("<div style=\"layout-mode: left; anchor-width: 700; margin-bottom: 4; margin-left: 10;\">")
                        .append("<p style=\"anchor-width: 200; vertical-align: center;\">").append(displayName).append("</p>");

                for (String quality : qualities) {
                    String variantId = baseId + "_" + quality;
                    boolean discovered = craftingKnowledge.discoveredDroppableRecipes.contains(variantId);

                    if (discovered) {
                        sb.append("<span class=\"item-slot\"")
                                .append(" data-hyui-item-id=\"").append(variantId).append("\"")
                                .append(" data-hyui-show-quality-background=\"true\"")
                                .append(" data-hyui-show-quantity=\"false\"")
                                .append(" data-hyui-tooltiptext=\"").append(displayName).append(" ").append(quality).append("\"")
                                .append(" style=\"anchor-width: 60; anchor-height: 60; margin-left: 3; margin-right: 3;\">")
                                .append("</span>");
                    } else {
                        sb.append("<div style=\"layout-mode: top; anchor-width: 60; anchor-height: 60; margin-left: 3; margin-right: 3;\">")
                                .append("<span class=\"item-slot\"")
                                .append(" data-hyui-tooltiptext=\"").append(quality).append(" (Undiscovered)\"")
                                .append(" style=\"anchor-width: 60; anchor-height: 60;\">")
                                .append("</span>")
                                .append("<div style=\"anchor-width: 60; anchor-height: 60; margin-top: -60; background-color: rgba(0,0,0,0.5);\">")
                                .append("</div>")
                                .append("<img src=\"Common/UnknownItemIcon@2x.png\"")
                                .append(" style=\"anchor-width: 40; anchor-height: 40; margin-top: -50; margin-left: 10;\"/>")
                                .append("</div>");
                    }
                }
                sb.append("</div>");
            }
        }

        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Room Recipes Tab — card layout
    // -------------------------------------------------------------------------
    private static String buildRoomRecipesHTML(Component_CraftingKnowledge craftingKnowledge) {
        StringBuilder sb = new StringBuilder();

        for (RoomType roomType : RoomType.values()) {
            boolean discovered = craftingKnowledge != null
                    && craftingKnowledge.discoveredRoomRecipes.contains(roomType.name());

            // Outer card
            sb.append("<div style=\"layout-mode: top; anchor-width: 690; margin-bottom: 10; background-color: #111a24;\">");

            // Top margin spacer
            sb.append("<div style=\"anchor-width: 666; anchor-height: 12; margin-left: 12;\"></div>");

            // --- Header row: name (left) + tier (right) ---
            sb.append("<div style=\"layout-mode: left; anchor-width: 666; margin-left: 12; margin-right: 12; margin-bottom: 2;\">");
            if (discovered) {
                sb.append("<p style=\"font-size: 18; anchor-width: 546;\">")
                        .append("<span data-hyui-bold=\"true\">").append(roomType.getDisplayName()).append("</span>")
                        .append("</p>");
                sb.append("<p style=\"font-size: 13; anchor-width: 120; text-align: right; color: #888888;\">")
                        .append("Tier ").append(roomType.getTier())
                        .append("</p>");
            } else {
                sb.append("<p style=\"font-size: 18; anchor-width: 546; color: #555555;\">")
                        .append("<span data-hyui-bold=\"true\">???????</span>")
                        .append("</p>");
                sb.append("<p style=\"font-size: 13; anchor-width: 120; text-align: right; color: #555555;\">Tier ?</p>");
            }
            sb.append("</div>"); // end header row

            // --- Size subtitle ---
            if (discovered) {
                sb.append("<p style=\"font-size: 12; color: #777777; margin-left: 12; margin-bottom: 10;\">")
                        .append("Size: ").append(roomType.getSizeDescription())
                        .append("</p>");
            } else {
                sb.append("<p style=\"font-size: 12; color: #555555; margin-left: 12; margin-bottom: 10;\">Size: ???????</p>");
            }

            // --- Divider ---
            sb.append("<div style=\"anchor-width: 666; anchor-height: 1; margin-left: 12; margin-bottom: 10; background-color: #2a3a4a;\"></div>");

            // --- Body: two columns (Buffs | Requirements) ---
            sb.append("<div style=\"layout-mode: left; anchor-width: 666; margin-left: 12; margin-right: 12;\">");

            // Left column — Buffs
            sb.append("<div style=\"layout-mode: top; anchor-width: 323;\">");
            sb.append("<p style=\"font-size: 13; color: #aaaaaa; margin-bottom: 6;\"><span data-hyui-bold=\"true\">Buffs</span></p>");
            if (discovered) {
                sb.append("<p style=\"font-size: 12; color: #cccccc;\">Coming Soon!</p>");
            } else {
                sb.append("<p style=\"font-size: 12; color: #555555;\">???????</p>");
            }
            sb.append("</div>"); // end buffs column

            // Right column — Requirements
            sb.append("<div style=\"layout-mode: top; anchor-width: 323; margin-left: 10;\">");
            sb.append("<p style=\"font-size: 13; color: #aaaaaa; margin-bottom: 6;\"><span data-hyui-bold=\"true\">Requirements</span></p>");
            if (discovered) {
                List<RoomType.RequirementLine> lines = roomType.getRequirementLines();
                if (lines.isEmpty()) {
                    sb.append("<p style=\"font-size: 12; color: #cccccc;\">None</p>");
                } else {
                    for (RoomType.RequirementLine line : lines) {
                        switch (line.type) {
                            case ITEM ->
                                    sb.append("<p style=\"font-size: 12; color: #cccccc;\">• ").append(line.label).append("</p>");
                            case ANY_START ->
                                    sb.append("<p style=\"font-size: 12; color: #aaaaaa; margin-top: 2;\">Any: [</p>");
                            case ANY_ITEM ->
                                    sb.append("<p style=\"font-size: 12; color: #cccccc; margin-left: 10;\">• ").append(line.label).append("</p>");
                            case ANY_END ->
                                    sb.append("<p style=\"font-size: 12; color: #aaaaaa;\">]</p>");
                        }
                    }
                }
            } else {
                sb.append("<p style=\"font-size: 12; color: #555555;\">???????</p>");
            }
            sb.append("</div>"); // end requirements column

            sb.append("</div>"); // end body row

            // Bottom margin spacer
            sb.append("<div style=\"anchor-width: 666; anchor-height: 12; margin-left: 12;\"></div>");

            sb.append("</div>"); // end card
        }

        return sb.toString();
    }
}