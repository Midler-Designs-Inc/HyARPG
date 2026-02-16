package com.example.hyarpg.modules;

// Hytale Imports
import au.ellie.hyui.builders.LabelBuilder;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.PlayerRef;

// Mod Imports
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.events.Event_PlayerJoin;

// Dependency Imports
import au.ellie.hyui.builders.HudBuilder;
import au.ellie.hyui.html.TemplateProcessor;

// Java Imports
import java.awt.*;

public class PlayerHud {

    public PlayerHud () {
        // Listen to applicable events on the mods internal event bus
        ModEventBus.register(Event_PlayerJoin.class, this::onPlayerJoin);
    }

    // This function runs whenever a PlayerJoin event is posted
    private void onPlayerJoin(Event_PlayerJoin event) {
        // Get playerRef and world references
        PlayerRef playerRef = event.getPlayer();
        World world = event.getWorld();

        // get the joning player from the event and formulate our message
        Message systemMessage = Message.raw("Attempting to show HUD").color(Color.YELLOW);
        playerRef.sendMessage(systemMessage);

        // when the world is ready add the hud
        world.execute(new Runnable() {
            @Override
            public void run() {

                Ref<EntityStore> entityRef = playerRef.getReference();

                if (entityRef == null) {
                    world.execute(this); // retry next tick
                    return;
                }

                float currentHealth = 999.0f;

                Store<EntityStore> store = world.getEntityStore().getStore();
                ComponentType<EntityStore, EntityStatMap> statMapType =
                        EntityStatsModule.get().getEntityStatMapComponentType();

                EntityStatMap statMap = store.getComponent(entityRef, statMapType);
                if (statMap == null) return;

                int healthIndex = DefaultEntityStatTypes.getHealth();
                EntityStatValue healthStat = statMap.get(healthIndex);

                if (healthStat != null) {
                    currentHealth = healthStat.get();   // ✅ CURRENT health
                    // healthStat.getMax();              // max health
                }

                String html = "<div style='anchor-top: 10; anchor-left: 10;'>"
                    + "<p id='health'>Health: " + currentHealth + "</p>"
                    + "</div>";

                var tp = new TemplateProcessor();
                HudBuilder.detachedHud()
                    .fromTemplate(html, tp)
                    .withRefreshRate(1000)
                    .onRefresh(hud -> {
                        float hp = healthStat != null ? healthStat.get() : 0f;

                        hud.getById("health", LabelBuilder.class).ifPresent(label -> {
                            label.withText("Health: " + hp);
                        });
                    })
                    .show(playerRef);
            }
        });
    }
}