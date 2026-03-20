package com.example.hyarpg.modules;

// Hytale imports
import com.example.hyarpg.components.Component_Hunger;
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.configs.ModConfig;
import com.example.hyarpg.utils.skills.SkillNode;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
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
import org.jline.jansi.Ansi;

import java.awt.*;

import static com.example.hyarpg.modules.Module_Hunger.componentTypeHunger;
import static com.example.hyarpg.modules.Module_RPG_System.componentTypeRPGPlayer;
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
        if(ModConfig.get().thirst.enabled) createThirstHud(world, entityRef, store);
        if(ModConfig.get().hunger.enabled) createHungerHud(world, entityRef, store);
        createXPHud(world, entityRef, store);
        createBarrierBar(world, entityRef, store);
        createSkillsBar(world, entityRef, store);

        // create the hud refresh logic
        startHUDRefresh(world, entityRef, store);

        // show the hud
        hud.show(playerRef);
    }

    // function to implement and start the hud refreshing
    private void startHUDRefresh(World world, Ref<EntityStore> entityRef, Store<EntityStore> store) {
        hud.withRefreshRate(250).onRefresh(hudRef -> {
            // Schedule component reads on the world thread
            world.execute(() -> {
                Component_Thirst thirst = store.getComponent(entityRef, componentTypeThirst);
                Component_Hunger hunger = store.getComponent(entityRef, componentTypeHunger);
                Component_RPG_Player rpgPlayer = store.getComponent(entityRef, componentTypeRPGPlayer);
                EntityStatMap statMap = store.getComponent(entityRef, EntityStatsModule.get().getEntityStatMapComponentType());
                Player player = store.getComponent(entityRef, Player.getComponentType());
                if (hunger == null || thirst == null || rpgPlayer == null || player == null || statMap == null) return;

                float thirstPercent = thirst.getPercentage();
                float hungerPercent = hunger.getPercentage();
                float levelPercent = rpgPlayer.calculateLevelProgress();
                float barrierOnBlockPercent;
                int playerLevel = rpgPlayer.level;
                int gearScore = rpgPlayer.gearScore;

                // check for health and barrier stats
                int barrierStatIndex = EntityStatType.getAssetMap().getIndex("BarrierOnBlock");
                int healthIndex = DefaultEntityStatTypes.getHealth();
                EntityStatValue barrierStat = statMap.get(barrierStatIndex);
                EntityStatValue healthStat = statMap.get(healthIndex);

                // barrier and health stats found
                if (barrierStat != null && healthStat != null && barrierStat.getMax() > 0)
                    barrierOnBlockPercent = (float) barrierStat.get() / (float) healthStat.getMax();
                else barrierOnBlockPercent = 0f;

                // get abilities cooldowns if applicable //skillSlotOverlay_Label_R
                int secondsLeft_E = 0;
                if(rpgPlayer.primaryAbility != null) {
                    SkillNode node = rpgPlayer.skillLibrary.findNode(rpgPlayer.primaryAbility);

                    // check if the node exists and has an ability still
                    if(node !=null && node.ability != null) {
                        // get the time now and the last use of the ability in nano time
                        long now = System.nanoTime();
                        long lastUse = node.ability.getLastUse();

                        // if lastUse was ever set (not zero) do comparison logic
                        if(lastUse != 0) {
                            // determine how much nano seconds have passed
                            long elapsedNanos = now - lastUse;

                            // get the remaining cooldown time in nanos
                            long cooldownNanos = node.ability.cooldownSeconds * 1_000_000_000L;
                            long remainingNanos = cooldownNanos - elapsedNanos;

                            // set the remaining nanos into a clamped seconds left value
                            secondsLeft_E = (int) Math.max(0, (remainingNanos + 999_999_999L) / 1_000_000_000L);
                        }
                    }
                }

                // get abilities cooldowns if applicable //skillSlotOverlay_Label_R
                int secondsLeft_R = 0;
                if(rpgPlayer.secondaryAbility != null) {
                    SkillNode node = rpgPlayer.skillLibrary.findNode(rpgPlayer.secondaryAbility);

                    // check if the node exists and has an ability still
                    if(node !=null && node.ability != null) {
                        // get the time now and the last use of the ability in nano time
                        long now = System.nanoTime();
                        long lastUse = node.ability.getLastUse();

                        // if lastUse was ever set (not zero) do comparison logic
                        if(lastUse != 0) {
                            // determine how much nano seconds have passed
                            long elapsedNanos = now - lastUse;

                            // get the remaining cooldown time in nanos
                            long cooldownNanos = node.ability.cooldownSeconds * 1_000_000_000L;
                            long remainingNanos = cooldownNanos - elapsedNanos;

                            // set the remaining nanos into a clamped seconds left value
                            secondsLeft_R = (int) Math.max(0, (remainingNanos + 999_999_999L) / 1_000_000_000L);
                        }
                    }
                }

                // convert seconds left to final to shut up lambda error (java is so stupid)
                int secondsLeft_E_final = secondsLeft_E;
                int secondsLeft_R_final = secondsLeft_R;

                // Update UI back on the HyUI/render thread
                hudRef.getById("thirstBar", ProgressBarBuilder.class).ifPresent(b -> b.withValue(thirstPercent));
                hudRef.getById("hungerBar", ProgressBarBuilder.class).ifPresent(b -> b.withValue(hungerPercent));
                hudRef.getById("barrierBar", ProgressBarBuilder.class).ifPresent(b -> b.withValue(barrierOnBlockPercent));
                hudRef.getById("xpBar", ProgressBarBuilder.class).ifPresent(b -> b.withValue(levelPercent));
                hudRef.getById("xpLevelCurrent", LabelBuilder.class).ifPresent(l -> l.withText(
                    "SP " + rpgPlayer.skillPoints + "  |  GS " + String.valueOf(gearScore) + "  |  Lv " + String.valueOf(playerLevel)
                ));
                hudRef.getById("skillIcon_Q", ImageBuilder.class).ifPresent(l -> l
                    .withImage(rpgPlayer.ultimateAbilityIcon == null ? "" : rpgPlayer.ultimateAbilityIcon)
                    .withVisible(rpgPlayer.ultimateAbilityIcon != null)
                );
                hudRef.getById("skillIcon_E", ImageBuilder.class).ifPresent(l -> l
                    .withImage(rpgPlayer.primaryAbilityIcon == null ? "" : rpgPlayer.primaryAbilityIcon)
                    .withVisible(rpgPlayer.primaryAbilityIcon != null)
                );
                hudRef.getById("skillIcon_R", ImageBuilder.class).ifPresent(l -> l
                    .withImage(rpgPlayer.secondaryAbilityIcon == null ? "" : rpgPlayer.secondaryAbilityIcon)
                    .withVisible(rpgPlayer.secondaryAbilityIcon != null)
                );
                hudRef.getById("skillSlotOverlay_Label_E", LabelBuilder.class).ifPresent(l -> l
                    .withText(String.valueOf(secondsLeft_E_final))
                    .withVisible(secondsLeft_E_final > 0)
                );
                hudRef.getById("skillIconOverlay_E", ImageBuilder.class).ifPresent(l -> l
                    .withVisible(secondsLeft_E_final > 0)
                );
                hudRef.getById("skillSlotOverlay_Label_R", LabelBuilder.class).ifPresent(l -> l
                    .withText(String.valueOf(secondsLeft_R_final))
                    .withVisible(secondsLeft_R_final > 0)
                );
                hudRef.getById("skillIconOverlay_R", ImageBuilder.class).ifPresent(l -> l
                        .withVisible(secondsLeft_R_final > 0)
                );
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
            .withPadding(new HyUIPadding().setLeft(-440))
            .withText("GS 0  |  Lv 1")
        );
    }

    // function to show the barrier bar
    private void createBarrierBar(World world, Ref<EntityStore> entityRef, Store<EntityStore> store) {
        // initialize the hud element with HyUI
        hud.addElement(new ProgressBarBuilder()
            .withId("barrierBar")
            .withOuterAnchor(new HyUIAnchor()
                .setWidth(0)
                .setHeight(0)
                .setBottom(136)
            )
            .withAnchor(new HyUIAnchor()
                .setWidth(309)
                .setHeight(6)
                .setLeft(-315)
            )
            .withValue(0f)
            .withBarTexturePath("abd4f9.png")
        );
    }

    // function to show the skills bar/slots
    private void createSkillsBar(World world, Ref<EntityStore> entityRef, Store<EntityStore> store) {
        // Skill Slot E
        hud.addElement(new ImageBuilder()
            .withId("skillSlot_E")
            .withAnchor(new HyUIAnchor()
                .setWidth(56)
                .setHeight(56)
                .setBottom(135)
                .setRight(165)
            )
            .withImage("HyARPG_Texture_EmptySkillSlot.png")
        )
        .addElement(new ImageBuilder()
            .withId("skillIcon_E")
            .withAnchor(new HyUIAnchor()
                .setWidth(48)
                .setHeight(48)
                .setBottom(139)
                .setRight(169)
            )
            .withVisible(false)
            .withImage("")
        )
        .addElement(new ImageBuilder()
            .withId("skillIconOverlay_E")
            .withAnchor(new HyUIAnchor()
                .setWidth(48)
                .setHeight(48)
                .setBottom(139)
                .setRight(169)
            )
            .withVisible(false)
            .withImage("Black_Alpha_75.png")
        )
        .addElement(new LabelBuilder()
            .withId("skillSlotOverlay_Label_E")
            .withAnchor(new HyUIAnchor()
                .setWidth(48)
                .setHeight(48)
                .setBottom(139)
                .setRight(169)
            )
            .withStyle(new HyUIStyle()
                .setFontSize(18)
                .setTextColor("#FFFFFF")
                .setRenderBold(true)
                .setAlignment(Alignment.Center)
                .setVerticalAlignment(Alignment.Center)
            )
            .withVisible(false)
            .withText("0")
        )
        .addElement(new LabelBuilder()
            .withId("skillSlot_Label_E")
            .withAnchor(new HyUIAnchor()
                .setWidth(5)
                .setHeight(5)
                .setBottom(143)
                .setRight(192)
            )
            .withStyle(new HyUIStyle()
                .setFontSize(16)
                .setTextColor("#FFFFFF")
                .setRenderBold(true)
            )
            .withText("E")
        )

        // Skill Slot R
        .addElement(new ImageBuilder()
            .withId("skillSlot_R")
            .withAnchor(new HyUIAnchor()
                .setWidth(56)
                .setHeight(56)
                .setBottom(135)
                .setRight(80)
            )
            .withImage("HyARPG_Texture_EmptySkillSlot.png")
        )
        .addElement(new ImageBuilder()
            .withId("skillIcon_R")
            .withAnchor(new HyUIAnchor()
                .setWidth(48)
                .setHeight(48)
                .setBottom(139)
                .setRight(84)
            )
            .withVisible(false)
            .withImage("")
        )
        .addElement(new ImageBuilder()
            .withId("skillIconOverlay_R")
            .withAnchor(new HyUIAnchor()
                .setWidth(48)
                .setHeight(48)
                .setBottom(139)
                .setRight(84)
            )
            .withVisible(false)
            .withImage("Black_Alpha_75.png")
        )
        .addElement(new LabelBuilder()
            .withId("skillSlotOverlay_Label_R")
            .withAnchor(new HyUIAnchor()
                .setWidth(48)
                .setHeight(48)
                .setBottom(139)
                .setRight(84)
            )
            .withStyle(new HyUIStyle()
                .setFontSize(18)
                .setTextColor("#FFFFFF")
                .setRenderBold(true)
                .setAlignment(Alignment.Center)
                .setVerticalAlignment(Alignment.Center)
            )
            .withVisible(false)
            .withText("0")
        )
        .addElement(new LabelBuilder()
            .withId("skillSlot_Label_R")
            .withAnchor(new HyUIAnchor()
                .setWidth(5)
                .setHeight(5)
                .setBottom(143)
                .setRight(107)
            )
            .withStyle(new HyUIStyle()
                .setFontSize(16)
                .setTextColor("#FFFFFF")
                .setRenderBold(true)
            )
            .withText("R")
        )

        // Skill Slot Q
        .addElement(new ImageBuilder()
            .withId("skillSlot_Q")
            .withAnchor(new HyUIAnchor()
                .setWidth(37)
                .setHeight(37)
                .setBottom(153)
                .setRight(269)
            )
            .withImage("HyARPG_Texture_EmptySkillSlot.png")
        )
        .addElement(new ImageBuilder()
            .withId("skillIcon_Q")
            .withAnchor(new HyUIAnchor()
                .setWidth(31)
                .setHeight(31)
                .setBottom(156)
                .setRight(271)
            )
            .withVisible(false)
            .withImage("")
        );;
    }
}
