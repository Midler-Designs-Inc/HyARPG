package com.example.hyarpg.modules;

// Hytale Imports
import com.example.hyarpg.components.Component_Hunger;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.HyARPGPlugin;
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.components.Component_RPG_Stats;
import com.example.hyarpg.events.Event_PlayerReady;

// HyUI Imports
import au.ellie.hyui.builders.*;

// Java Imports

public class Module_RPG_Stats {

    private final HyARPGPlugin plugin;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static ComponentType<EntityStore, Component_RPG_Stats> componentTypeRPGStats;

    // initialize this module
    public Module_RPG_Stats(HyARPGPlugin plugin) {
        this.plugin = plugin;

        // Register the component type using EntityStoreRegistry
        componentTypeRPGStats = plugin.getEntityStoreRegistry()
                .registerComponent(Component_RPG_Stats.class, "RPGStatsComponent", Component_RPG_Stats.CODEC);

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

        // add the hunger hud to the player's hud
        createXPHud(player, world, entityRef);
    }

    // function to show the hunger hud for a player
    private void createXPHud(Player player, World world, Ref<EntityStore> entityRef) {
        // get the entity store and player ref
        Store<EntityStore> store = world.getEntityStore().getStore();
        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());

        // initialize the hud element with HyUI
        HudBuilder.hudForPlayer(playerRef)
            .addElement(new ProgressBarBuilder()
                    .withId("hungerBar")
                    .withOuterAnchor(new HyUIAnchor()
                            .setWidth(0)
                            .setHeight(0)
                            .setBottom(50)
                    )
                    .withAnchor(new HyUIAnchor()
                            .setWidth(500)
                            .setHeight(10)
                    )
                    .withValue(1f)
                    .withBarTexturePath("d3e582.png")
                    .withBackground(new HyUIPatchStyle().setColor("#222222"))
            )
            .withRefreshRate(500)
            .onRefresh(hud -> {
                hud.getById("hungerBar", ProgressBarBuilder.class).ifPresent(bar -> {
                    // Schedule component access on the world thread
                    world.execute(() -> {
//                        // get the players hunger component
//                        Component_Hunger hunger = store.getComponent(entityRef, componentTypeHunger);
//
//                        // get the players hunger percentage
//                        float percentage = hunger.getPercentage();
//
//                        // Update the bar value
//                        bar.withValue(percentage);
                    });
                });
            })
            .show(playerRef);
    }

}
