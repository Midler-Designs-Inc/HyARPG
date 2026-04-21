package com.example.hyarpg.ui;

// Hytale Imports
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.components.Component_CraftingKnowledge;
import com.example.hyarpg.modules.Module_RPGSystem;
import com.example.hyarpg.utils.rooms.RoomType;

// Java Imports
import javax.annotation.Nonnull;
import java.util.Set;
import java.util.Collections;

public class CustomPage_RecipeBookPage extends InteractiveCustomUIPage<CustomPage_RecipeBookPage.PageData> {

    public CustomPage_RecipeBookPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        // load the UI file
        cmd.append("CustomPage_RecipeBookPanel.ui");

        // apply room card visibility based on discovered state
        applyRoomCards(ref, store, cmd);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        // no interactive actions — page is read only, ESC to close
        sendUpdate((UICommandBuilder) null, false);
    }

    // show discovered room cards and fill remaining slots with hidden cards
    private void applyRoomCards(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder cmd) {
        // get discovered room recipes for this player
        Component_CraftingKnowledge craftingKnowledge = store.getComponent(ref, Module_RPGSystem.componentTypeCraftingKnowledge);
        Set<String> discovered = craftingKnowledge != null ? craftingKnowledge.discoveredRoomRecipes : Collections.emptySet();

        // count undiscovered rooms
        int undiscoveredCount = 0;
        for (RoomType roomType : RoomType.values()) {
            if (!discovered.contains(roomType.name())) undiscoveredCount++;
        }

        // show one hidden card per undiscovered room
        int totalRooms = RoomType.values().length;
        for (int i = 1; i <= totalRooms; i++) {
            cmd.set("#HiddenCard" + i + ".Visible", i <= undiscoveredCount);
        }

        // show each discovered room card
        for (RoomType roomType : RoomType.values()) {
            boolean isDiscovered = discovered.contains(roomType.name());
            String cardId = cardElementId(roomType);
            cmd.set(cardId + ".Visible", isDiscovered);
        }
    }

    // maps RoomType enum name to its UI card element id
    private static String cardElementId(RoomType roomType) {
        String enumName = roomType.name(); // e.g. SMALL_ROOM
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : enumName.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return "#RoomCard" + sb.toString();
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec
                .<PageData>builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action).add()
                .build();

        public String action;
    }
}
