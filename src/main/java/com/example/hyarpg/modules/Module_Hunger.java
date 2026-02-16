package com.example.hyarpg.modules;

// Hytale Imports
import au.ellie.hyui.builders.*;
import com.example.hyarpg.HyARPGPlugin;
import com.example.hyarpg.components.Component_Hunger;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.PlayerRef;

// Mod Imports
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.events.Event_PlayerReady;

// Java Imports
import java.awt.*;

public class Module_Hunger {
    // Create a component type for the Hunger component
    public static ComponentType<EntityStore, Component_Hunger> componentTypeHunger;

    // initialize this module
    public Module_Hunger(HyARPGPlugin plugin) {
        // Register the component type using EntityStoreRegistry
        componentTypeHunger = plugin.getEntityStoreRegistry()
                .registerComponent(Component_Hunger.class, Component_Hunger::new);

        // Listen to applicable events on the mods internal event bus
        ModEventBus.register(Event_PlayerReady.class, this::onPlayerReady);
    }

    // This function runs whenever a PlayerReady event is posted
    private void onPlayerReady(Event_PlayerReady event) {
        // get the joining player
        Player player = event.getPlayer();
        World world = event.getWorld();

        // Initialize a new Hunger component
        Component_Hunger componentHunger = new Component_Hunger(
            120f,  // max hunger
            120f,  // initial hunger
            0.5f     // drain per second
        );

        // get the player's Ref and the world entity store
        Ref<EntityStore> entityRef = player.getReference();
        Store<EntityStore> store = world.getEntityStore().getStore();

        // add the component to the ref entity
        store.addComponent(entityRef, componentTypeHunger, componentHunger);

        // add the hunger hud to the player's hud
        createHungerHud(player, world, entityRef);
    }

    // function to show the hunger hud for a player
    private void createHungerHud(Player player, World world, Ref<EntityStore> entityRef) {
        // get the entity store and player ref
        Store<EntityStore> store = world.getEntityStore().getStore();
        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());

        // initialize the hud element with HyUI
        HudBuilder.hudForPlayer(playerRef)
            .addElement(new ImageBuilder()
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
                    .setWidth(309)
                    .setHeight(12)
                    .setLeft(-315)
                )
                .withValue(1f)
                .withBarTexturePath("FF9760.png")
                .withBackground(new HyUIPatchStyle().setColor("#222222"))
            )
            .withRefreshRate(2000)
            .onRefresh(hud -> {
                hud.getById("hungerBar", ProgressBarBuilder.class).ifPresent(bar -> {
                    // Schedule component access on the world thread
                    world.execute(() -> {
                        // get the players hunger component
                        Component_Hunger hunger = store.getComponent(entityRef, componentTypeHunger);

                        // get the players hunger percentage
                        float percentage = hunger.getPercentage();

                        // Update the bar value
                        bar.withValue(percentage);
                    });
                });
            })
            .show(playerRef);
    }
}