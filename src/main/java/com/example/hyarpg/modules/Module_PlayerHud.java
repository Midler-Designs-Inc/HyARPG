package com.example.hyarpg.modules;

// Hytale imports
import com.example.hyarpg.components.Component_Hunger;
import com.example.hyarpg.components.Component_RPG_Stats;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.events.Event_PlayerReady;
import com.example.hyarpg.HyARPGPlugin;
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.components.Component_Thirst;

// HyUI Imports
import au.ellie.hyui.builders.*;
import au.ellie.hyui.types.ProgressBarDirection;

import java.awt.*;

import static com.example.hyarpg.modules.Module_Hunger.componentTypeHunger;
import static com.example.hyarpg.modules.Module_RPG_Stats.componentTypeRPGStats;
import static com.example.hyarpg.modules.Module_Thirst.componentTypeThirst;

public class Module_PlayerHud {

    private final HyARPGPlugin plugin;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public HudBuilder hud;

    // initialize this module
    public Module_PlayerHud(HyARPGPlugin plugin) {
        this.plugin = plugin;

        // Listen to applicable events on the mods internal event bus
        ModEventBus.register(Event_PlayerReady.class, this::onPlayerReady);
    }

    // This function runs whenever a PlayerReady event is posted
    private void onPlayerReady(Event_PlayerReady event) {
        // get the joining player
        Player player = event.getPlayer();
        World world = event.getWorld();

        // get the player's Ref and the world entity store
        Ref<EntityStore> entityRef = player.getReference();
        Store<EntityStore> store = world.getEntityStore().getStore();
        if (entityRef == null) return;

        // create the hud logic
        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        if (playerRef == null) return;
        if (hud == null) hud = HudBuilder.hudForPlayer(playerRef);

        // add the applicable hud elements
        createThirstHud(world, entityRef, store);
        createHungerHud(world, entityRef, store);
        createXPHud(world, entityRef, store);

        // create the hud refresh logic
        startHUDRefresh(world, entityRef, store);

        // show the hud
        hud.show(playerRef);
    }

    // function to implment and start the hud refreshing
    private void startHUDRefresh(World world, Ref<EntityStore> entityRef, Store<EntityStore> store) {
        hud.withRefreshRate(250).onRefresh(hudRef -> {
            // Schedule component reads on the world thread
            world.execute(() -> {
                Component_Thirst thirst = store.getComponent(entityRef, componentTypeThirst);
                Component_Hunger hunger = store.getComponent(entityRef, componentTypeHunger);
                Component_RPG_Stats RPGStats = store.getComponent(entityRef, componentTypeRPGStats);
                Player player = store.getComponent(entityRef, Player.getComponentType());
                if (hunger == null || thirst == null || RPGStats == null || player == null);

                float thirstPercent = thirst.getPercentage();
                float hungerPercent = hunger.getPercentage();
                float levelPercent = RPGStats.calculateLevelProgress();
                int playerLevel = RPGStats.level;
                int gearScore = RPGStats.calculateGearScore(player);

                // Update UI back on the HyUI/render thread
                hudRef.getById("thirstBar", ProgressBarBuilder.class).ifPresent(b -> b.withValue(thirstPercent));
                hudRef.getById("hungerBar", ProgressBarBuilder.class).ifPresent(b -> b.withValue(hungerPercent));
                hudRef.getById("xpBar", ProgressBarBuilder.class).ifPresent(b -> b.withValue(levelPercent));
                hudRef.getById("xpLevelCurrent", LabelBuilder.class).ifPresent(l -> l.withText(
                    "Ip " + String.valueOf(gearScore)
                    + "  |  Lv " + String.valueOf(playerLevel)
                ));
            });
        });
    }

    // function to show the thirst bar
    private void createThirstHud(World world, Ref<EntityStore> entityRef, Store<EntityStore> store) {
        // initialize the hud element with HyUI
        hud.addElement(new ImageBuilder()
            .withId("thirstIcon")
            .withAnchor(new HyUIAnchor()
                    .setWidth(20)
                    .setHeight(22)
                    .setBottom(143)
            )
            .withPadding(new HyUIPadding().setLeft(676))
            .withImage("HyARPG_Texture_Thirst_Icon.png")
        )
        .addElement(new ProgressBarBuilder()
            .withId("thirstBar")
            .withDirection(ProgressBarDirection.Start)
            .withOuterAnchor(new HyUIAnchor()
                    .setWidth(0)
                    .setHeight(0)
                    .setBottom(153)
            )
            .withAnchor(new HyUIAnchor()
                    .setWidth(155)
                    .setHeight(12)
                    .setRight(-315)
            )
            .withValue(1f)
            .withBarTexturePath("0687cc.png")
            .withBackground(new HyUIPatchStyle().setColor("#222222"))
        );
    }

    // function to show the hunger bar
    private void createHungerHud(World world, Ref<EntityStore> entityRef, Store<EntityStore> store) {
        // initialize the hud element with HyUI
        hud.addElement(new ImageBuilder()
            .withId("hungerIcon")
            .withAnchor(new HyUIAnchor()
                    .setWidth(25)
                    .setHeight(25)
                    .setBottom(142)
            )
            .withPadding(new HyUIPadding().setRight(676))
            .withImage("HyARPG_Texture_Hunger_Icon.png")
        )
        .addElement(new ProgressBarBuilder()
            .withId("hungerBar")
            .withOuterAnchor(new HyUIAnchor()
                    .setWidth(0)
                    .setHeight(0)
                    .setBottom(153)
            )
            .withAnchor(new HyUIAnchor()
                    .setWidth(155) // 309
                    .setHeight(12)
                    .setLeft(-315)
            )
            .withValue(1f)
            .withBarTexturePath("FF9760.png")
            .withBackground(new HyUIPatchStyle().setColor("#222222"))
        );
    }

    // function to show the xp bar
    private void createXPHud(World world, Ref<EntityStore> entityRef, Store<EntityStore> store) {
        // XP Bar itself
        hud.addElement(new ProgressBarBuilder()
            .withId("xpBar")
            .withOuterAnchor(new HyUIAnchor()
                    .setWidth(0)
                    .setHeight(0)
                    .setBottom(12)
            )
            .withAnchor(new HyUIAnchor()
                    .setWidth(700)
                    .setHeight(10)
            )
            .withValue(0f)
            .withBarTexturePath("d3e582.png")
            .withBackground(new HyUIPatchStyle().setColor("#222222"))
        );

        // Current level label (left of bar)
        hud.addElement(new LabelBuilder()
            .withId("xpLevelCurrent")
            .withAnchor(new HyUIAnchor()
                .setWidth(75)
                .setHeight(15)
                .setBottom(5)
            )
            .withStyle(new HyUIStyle()
                .setFontSize(12)
                .setTextColor("#cccccc")
                .setRenderBold(true)
            )
            .withPadding(new HyUIPadding().setLeft(-417))
            .withText("IP 0  |  Lv 1")
        );
    }
}
