package com.example.hyarpg.ui;

import com.example.hyarpg.components.Component_CraftingKnowledge;
import com.example.hyarpg.modules.Module_RPGSystem;
import com.example.hyarpg.utils.StatTypeInfo;
import com.example.hyarpg.utils.affixes.StatType;
import com.example.hyarpg.utils.items.ItemFactory;
import com.example.hyarpg.utils.rooms.RoomType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import au.ellie.hyui.builders.PageBuilder;
import org.bson.BsonDocument;
import org.bson.BsonValue;

import java.util.*;

public class Page_RecipeBook {

    private static final int CW  = 870;
    private static final int CH  = 700;
    private static final int IW  = CW - 20;
    private static final int IH  = CH - 110;
    private static final int NAV = 180;
    private static final int CON = IW - NAV - 10;

    private static final List<String> WEAPON_CATS = List.of("Heads_and_Blades", "Handles", "Shafts_and_Hilts", "Bow_Parts", "Shield_Parts", "Magic_Parts");
    private static final List<String> ARMOR_CATS  = List.of("Shells_and_Liners", "Straps_and_Buckles", "Leather_Panels", "Embellishments", "Cloth_Panels_and_Stitching");

    public static void open(Ref<EntityStore> ref, Store<EntityStore> store) { open(ref, store, "weapon", WEAPON_CATS.get(0), ARMOR_CATS.get(0)); }

    public static void open(Ref<EntityStore> ref, Store<EntityStore> store, String tab, String weaponCat, String armorCat) {
        Component_CraftingKnowledge craftingKnowledge = store.getComponent(ref, Module_RPGSystem.componentTypeCraftingKnowledge);
        Player player       = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        Set<String> discovered = craftingKnowledge != null ? craftingKnowledge.discoveredDroppableRecipes : Collections.emptySet();

        String activeWeaponCat = WEAPON_CATS.contains(weaponCat) ? weaponCat : WEAPON_CATS.get(0);
        String activeArmorCat  = ARMOR_CATS.contains(armorCat)   ? armorCat  : ARMOR_CATS.get(0);

        // build weapon nav
        StringBuilder weaponNav = new StringBuilder();
        for (String c : WEAPON_CATS) {
            String active = c.equals(activeWeaponCat) ? " background-color: #1e3a5f;" : "";
            weaponNav.append("<button id=\"w_").append(catBtnId(c)).append("\" style=\"anchor-width: ").append(NAV - 10)
                    .append("; margin-bottom: 3; white-space: normal;").append(active).append("\">")
                    .append(catDisplay(c)).append("</button>");
        }

        // build armor nav
        StringBuilder armorNav = new StringBuilder();
        for (String c : ARMOR_CATS) {
            String active = c.equals(activeArmorCat) ? " background-color: #1e3a5f;" : "";
            armorNav.append("<button id=\"a_").append(catBtnId(c)).append("\" style=\"anchor-width: ").append(NAV - 10)
                    .append("; margin-bottom: 3; white-space: normal;").append(active).append("\">")
                    .append(catDisplay(c)).append("</button>");
        }

        String weaponContent = buildComponentContent(discovered, activeWeaponCat, "weapon", CON - 20);
        String armorContent  = buildComponentContent(discovered, activeArmorCat,  "armor",  CON - 20);
        String roomContent   = buildRoomHTML(craftingKnowledge);

        String html = """
            <div class="page-overlay">
                <button id="closeBtn" style="anchor-bottom: 10; anchor-width: ${CW}; anchor-height: 36;">Close</button>
                <div class="container" data-hyui-title="Recipe Book" style="anchor-width: ${CW}; anchor-height: ${CH};">
                    <div class="container-contents" style="layout-mode: top; padding: 6;">

                        <nav id="primary-tabs" class="tabs"
                             data-tabs="weapon:Weapon Components:weapon-content,armor:Armor Components:armor-content,rooms:Rooms:rooms-content"
                             data-selected="${SELECTED_TAB}">
                        </nav>

                        <!-- Weapon Components Tab -->
                        <div id="weapon-content" class="tab-content"
                             data-hyui-tab-id="weapon"
                             data-hyui-tab-nav="primary-tabs">
                            <div style="layout-mode: left; anchor-width: ${IW}; anchor-height: ${IH};">
                                <div style="layout-mode: top; anchor-width: ${NAV}; anchor-height: ${IH}; background-color: #0d141c;">
                                    <div style="layout-mode: topscrolling; anchor-width: ${NAV}; anchor-height: ${IH};" data-hyui-scrollbar-style="&quot;Common.ui&quot; &quot;DefaultScrollbarStyle&quot;" data-hyui-keep-scroll-position="true">
                                        <div style="layout-mode: top; margin-top: 4;">
                                            ${WEAPON_NAV}
                                        </div>
                                    </div>
                                </div>
                                <div style="anchor-width: 10;"></div>
                                <div style="layout-mode: topscrolling; anchor-width: ${CON}; anchor-height: ${IH};" data-hyui-scrollbar-style="&quot;Common.ui&quot; &quot;DefaultScrollbarStyle&quot;">
                                    <div style="layout-mode: top; anchor-width: ${CON2}; margin-left: 8; margin-top: 6;">
                                        ${WEAPON_CONTENT}
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Armor Components Tab -->
                        <div id="armor-content" class="tab-content"
                             data-hyui-tab-id="armor"
                             data-hyui-tab-nav="primary-tabs">
                            <div style="layout-mode: left; anchor-width: ${IW}; anchor-height: ${IH};">
                                <div style="layout-mode: top; anchor-width: ${NAV}; anchor-height: ${IH}; background-color: #0d141c;">
                                    <div style="layout-mode: topscrolling; anchor-width: ${NAV}; anchor-height: ${IH};" data-hyui-scrollbar-style="&quot;Common.ui&quot; &quot;DefaultScrollbarStyle&quot;" data-hyui-keep-scroll-position="true">
                                        <div style="layout-mode: top; margin-top: 4;">
                                            ${ARMOR_NAV}
                                        </div>
                                    </div>
                                </div>
                                <div style="anchor-width: 10;"></div>
                                <div style="layout-mode: topscrolling; anchor-width: ${CON}; anchor-height: ${IH};" data-hyui-scrollbar-style="&quot;Common.ui&quot; &quot;DefaultScrollbarStyle&quot;">
                                    <div style="layout-mode: top; anchor-width: ${CON2}; margin-left: 8; margin-top: 6;">
                                        ${ARMOR_CONTENT}
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Rooms Tab -->
                        <div id="rooms-content" class="tab-content"
                             data-hyui-tab-id="rooms"
                             data-hyui-tab-nav="primary-tabs">
                            <div style="layout-mode: topscrolling; anchor-width: ${IW}; anchor-height: ${IH};" data-hyui-scrollbar-style="&quot;Common.ui&quot; &quot;DefaultScrollbarStyle&quot;">
                                <div style="layout-mode: top; anchor-width: ${IW2}; margin-left: 10; margin-top: 6;">
                                    ${ROOM_CONTENT}
                                </div>
                            </div>
                        </div>

                    </div>
                </div>
            </div>
            """;

        html = html
                .replace("${CW}",             String.valueOf(CW))
                .replace("${CH}",             String.valueOf(CH))
                .replace("${IW}",             String.valueOf(IW))
                .replace("${IW2}",            String.valueOf(IW - 20))
                .replace("${IH}",             String.valueOf(IH))
                .replace("${NAV}",            String.valueOf(NAV))
                .replace("${CON}",            String.valueOf(CON))
                .replace("${CON2}",           String.valueOf(CON - 20))
                .replace("${SELECTED_TAB}",   tab)
                .replace("${WEAPON_NAV}",     weaponNav.toString())
                .replace("${ARMOR_NAV}",      armorNav.toString())
                .replace("${WEAPON_CONTENT}", weaponContent)
                .replace("${ARMOR_CONTENT}",  armorContent)
                .replace("${ROOM_CONTENT}",   roomContent);

        PageBuilder builder = PageBuilder.pageForPlayer(playerRef).fromHtml(html);
        builder.addEventListener("closeBtn", CustomUIEventBindingType.Activating, ctx -> player.getPageManager().setPage(ref, store, Page.None));

        // weapon category listeners — prefix w_
        for (String c : WEAPON_CATS) {
            final String fc = c;
            builder.addEventListener("w_" + catBtnId(c), CustomUIEventBindingType.Activating,
                    ctx -> player.getWorld().execute(() -> open(ref, store, "weapon", fc, activeArmorCat)));
        }

        // armor category listeners — prefix a_
        for (String c : ARMOR_CATS) {
            final String fc = c;
            builder.addEventListener("a_" + catBtnId(c), CustomUIEventBindingType.Activating,
                    ctx -> player.getWorld().execute(() -> open(ref, store, "armor", activeWeaponCat, fc)));
        }

        builder.open(store);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String catDisplay(String raw) {
        return raw.replace("_and_", " & ").replace("_", " ");
    }

    private static String catBtnId(String cat) {
        return "cat_" + cat;
    }

    // -------------------------------------------------------------------------
    // Component grid
    // -------------------------------------------------------------------------

    private static String buildComponentContent(Set<String> discovered, String category, String tab, int width) {
        StringBuilder sb = new StringBuilder();

        for (int tier = 1; tier <= 6; tier++) {
            List<String> tierComponents = new ArrayList<>();
            for (Map.Entry<String, Map<Integer, List<String>>> typeEntry : ItemFactory.COMPONENT_INDEX.entrySet()) {
                List<String> ids = typeEntry.getValue().get(tier);
                if (ids == null) continue;
                for (String id : ids) {
                    if (tab.equals("weapon") && !id.startsWith("Weapon_Component_")) continue;
                    if (tab.equals("armor")  && !id.startsWith("Armor_Component_"))  continue;
                    String idCat = ItemFactory.COMPONENT_CATEGORY_INDEX.get(id);
                    if (category.equals(idCat)) tierComponents.add(id);
                }
            }

            if (tierComponents.isEmpty()) continue;
            Collections.sort(tierComponents);

            sb.append("<p style=\"margin-top: 10; margin-bottom: 4;\"><span data-hyui-bold=\"true\" data-hyui-color=\"#c8a84b\">Tier ").append(tier).append("</span></p>");
            sb.append("<div style=\"anchor-height: 1; background-color: #2a3a4a; margin-bottom: 8;\"></div>");
            sb.append("<div style=\"layout-mode: left; anchor-width: ").append(width).append("; margin-bottom: 14; flex-wrap: wrap;\">");

            for (String id : tierComponents) {
                boolean isDisc = discovered.contains(id);
                BsonDocument comp = ItemFactory.readCraftingComponent(id);
                String type = comp != null && comp.get("type") != null ? comp.get("type").asString().getValue() : "Unknown";
                String tooltip = escapeTooltip(buildTooltip(id, type, tier, isDisc));

                if (isDisc) {
                    sb.append("<span class=\"item-slot\"")
                            .append(" data-hyui-item-id=\"").append(id).append("\"")
                            .append(" data-hyui-show-quality-background=\"true\"")
                            .append(" data-hyui-show-quantity=\"false\"")
                            .append(" data-hyui-tooltiptext=\"").append(tooltip).append("\"")
                            .append(" style=\"anchor-width: 64; anchor-height: 64; margin-right: 6; margin-bottom: 6;\">")
                            .append("</span>");
                } else {
                    sb.append("<div style=\"layout-mode: top; anchor-width: 64; anchor-height: 64; margin-right: 6; margin-bottom: 6;\"")
                            .append(" data-hyui-tooltiptext=\"").append(tooltip).append("\">")
                            .append("<span class=\"item-slot\" style=\"anchor-width: 64; anchor-height: 64;\"></span>")
                            .append("<div style=\"anchor-width: 64; anchor-height: 64; margin-top: -64; background-color: rgba(0,0,0,0.5);\"></div>")
                            .append("<img src=\"Common/UnknownItemIcon@2x.png\" style=\"anchor-width: 40; anchor-height: 40; margin-top: -52; margin-left: 12;\"/>")
                            .append("</div>");
                }
            }

            sb.append("</div>");
        }

        if (sb.length() == 0) sb.append("<p style=\"color: #555555; margin-top: 20;\">No components indexed for this category.</p>");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Tooltip
    // -------------------------------------------------------------------------

    private static String buildTooltip(String id, String type, int tier, boolean discovered) {
        if (!discovered) return "T" + tier + " " + type + " (Undiscovered)";

        StringBuilder sb = new StringBuilder();
        String name = id.replace("Weapon_Component_", "").replace("Armor_Component_", "").replace("_", " ");
        sb.append(name).append("\n");

        List<String> inputs = ItemFactory.COMPONENT_RECIPE_INDEX.get(id);
        if (inputs != null && !inputs.isEmpty()) {
            sb.append("\nCrafting:\n");
            for (String input : inputs) sb.append("  ").append(input).append("\n");
        }

        BsonDocument comp = ItemFactory.readCraftingComponent(id);
        if (comp != null) {
            BsonValue implicitsVal = comp.get("implicits");
            if (implicitsVal != null && implicitsVal.isArray() && !implicitsVal.asArray().isEmpty()) {
                sb.append("\nImplicits:\n");
                for (BsonValue entry : implicitsVal.asArray()) {
                    if (!entry.isDocument()) continue;
                    BsonDocument implicit = entry.asDocument();
                    BsonValue statVal = implicit.get("stat");
                    BsonValue minVal  = implicit.get("min");
                    BsonValue maxVal  = implicit.get("max");
                    if (statVal == null || minVal == null || maxVal == null) continue;
                    float min = minVal.isDouble() ? (float) minVal.asDouble().getValue() : (float) minVal.asInt32().getValue();
                    float max = maxVal.isDouble() ? (float) maxVal.asDouble().getValue() : (float) maxVal.asInt32().getValue();
                    try {
                        StatType stat = StatType.valueOf(statVal.asString().getValue());
                        if (StatTypeInfo.isWeaponDamageStat(stat)) {
                            sb.append("  ").append(fmt(min)).append("-").append(fmt(max)).append(" ").append(getDamageTypeName(stat)).append(" (Base Weapon Damage)\n");
                        } else {
                            sb.append("  ").append(StatTypeInfo.getDisplay(stat, min, max)).append("\n");
                        }
                    } catch (Exception e) {
                        sb.append("  ").append(statVal.asString().getValue()).append("\n");
                    }
                }
            }
        }

        return sb.toString().trim();
    }

    private static String getDamageTypeName(StatType stat) {
        if (stat == StatType.MAIN_HAND_FIRE_DAMAGE_FLAT      || stat == StatType.OFF_HAND_FIRE_DAMAGE_FLAT)      return "Fire";
        if (stat == StatType.MAIN_HAND_LIGHTNING_DAMAGE_FLAT || stat == StatType.OFF_HAND_LIGHTNING_DAMAGE_FLAT) return "Lightning";
        if (stat == StatType.MAIN_HAND_ICE_DAMAGE_FLAT       || stat == StatType.OFF_HAND_ICE_DAMAGE_FLAT)       return "Ice";
        if (stat == StatType.MAIN_HAND_POISON_DAMAGE_FLAT    || stat == StatType.OFF_HAND_POISON_DAMAGE_FLAT)    return "Poison";
        if (stat == StatType.MAIN_HAND_MAGIC_DAMAGE_FLAT     || stat == StatType.OFF_HAND_MAGIC_DAMAGE_FLAT)     return "Magic";
        return "Physical";
    }

    private static String fmt(float value) {
        float rounded = Math.round(value * 10) / 10f;
        return rounded == (int) rounded ? String.valueOf((int) rounded) : String.valueOf(rounded);
    }

    private static String escapeTooltip(String text) {
        return text.replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // -------------------------------------------------------------------------
    // Rooms
    // -------------------------------------------------------------------------

    private static String buildRoomHTML(Component_CraftingKnowledge craftingKnowledge) {
        StringBuilder sb = new StringBuilder();

        for (RoomType roomType : RoomType.values()) {
            boolean disc = craftingKnowledge != null && craftingKnowledge.discoveredRoomRecipes.contains(roomType.name());

            sb.append("<div style=\"layout-mode: top; anchor-width: ").append(IW - 20).append("; margin-bottom: 10; background-color: #111a24;\">");
            sb.append("<div style=\"anchor-height: 10;\"></div>");

            sb.append("<div style=\"layout-mode: left; anchor-width: ").append(IW - 40).append("; margin-left: 12; margin-bottom: 2;\">");
            if (disc) {
                sb.append("<p style=\"font-size: 16; anchor-width: ").append(IW - 180).append(";\"><span data-hyui-bold=\"true\">").append(roomType.getDisplayName()).append("</span></p>");
                sb.append("<p style=\"font-size: 12; anchor-width: 120; text-align: right; color: #888888;\">Tier ").append(roomType.getTier()).append("</p>");
            } else {
                sb.append("<p style=\"font-size: 16; anchor-width: ").append(IW - 180).append("; color: #555555;\"><span data-hyui-bold=\"true\">???????</span></p>");
                sb.append("<p style=\"font-size: 12; anchor-width: 120; text-align: right; color: #555555;\">Tier ?</p>");
            }
            sb.append("</div>");

            if (disc) {
                sb.append("<p style=\"font-size: 11; color: #777777; margin-left: 12; margin-bottom: 8;\">Size: ").append(roomType.getSizeDescription()).append("</p>");
            } else {
                sb.append("<p style=\"font-size: 11; color: #555555; margin-left: 12; margin-bottom: 8;\">Size: ???????</p>");
            }

            sb.append("<div style=\"anchor-width: ").append(IW - 40).append("; anchor-height: 1; margin-left: 12; margin-bottom: 8; background-color: #2a3a4a;\"></div>");

            int colW = (IW - 40) / 2;
            sb.append("<div style=\"layout-mode: left; anchor-width: ").append(IW - 40).append("; margin-left: 12; margin-right: 12;\">");

            sb.append("<div style=\"layout-mode: top; anchor-width: ").append(colW).append(";\">");
            sb.append("<p style=\"font-size: 12; color: #aaaaaa; margin-bottom: 4;\"><span data-hyui-bold=\"true\">Buffs</span></p>");
            sb.append(disc ? "<p style=\"font-size: 11; color: #cccccc;\">Coming Soon!</p>" : "<p style=\"font-size: 11; color: #555555;\">???????</p>");
            sb.append("</div>");

            sb.append("<div style=\"layout-mode: top; anchor-width: ").append(colW).append("; margin-left: 10;\">");
            sb.append("<p style=\"font-size: 12; color: #aaaaaa; margin-bottom: 4;\"><span data-hyui-bold=\"true\">Requirements</span></p>");
            if (disc) {
                List<RoomType.RequirementLine> lines = roomType.getRequirementLines();
                if (lines.isEmpty()) {
                    sb.append("<p style=\"font-size: 11; color: #cccccc;\">None</p>");
                } else {
                    for (RoomType.RequirementLine line : lines) {
                        switch (line.type) {
                            case ITEM      -> sb.append("<p style=\"font-size: 11; color: #cccccc;\">• ").append(line.label).append("</p>");
                            case ANY_START -> sb.append("<p style=\"font-size: 11; color: #aaaaaa; margin-top: 2;\">Any: [</p>");
                            case ANY_ITEM  -> sb.append("<p style=\"font-size: 11; color: #cccccc; margin-left: 10;\">• ").append(line.label).append("</p>");
                            case ANY_END   -> sb.append("<p style=\"font-size: 11; color: #aaaaaa;\">]</p>");
                        }
                    }
                }
            } else {
                sb.append("<p style=\"font-size: 11; color: #555555;\">???????</p>");
            }
            sb.append("</div>");

            sb.append("</div>");
            sb.append("<div style=\"anchor-height: 10;\"></div>");
            sb.append("</div>");
        }

        return sb.toString();
    }
}