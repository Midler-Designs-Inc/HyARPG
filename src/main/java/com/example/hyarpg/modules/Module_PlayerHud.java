package com.example.hyarpg.modules;

// Hytale imports
import com.example.hyarpg.components.Component_Hunger;
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.configs.ModConfig;
import com.example.hyarpg.utils.skills.SkillNode;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
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

import static com.example.hyarpg.modules.Module_Hunger.componentTypeHunger;
import static com.example.hyarpg.modules.Module_RPGSystem.componentTypeRPGPlayer;
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
        createRoomHud(world, entityRef, store);
        createRaidHud(world, entityRef, store);
        createMarkHud(world, entityRef, store);

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
                try {
                    // Guard: bail out if the entity ref is no longer valid
                    if (entityRef == null || !entityRef.isValid()) return;

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

                    // marks
                    int assassinMarkCount = rpgPlayer.marks.count("ASSASSIN");

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
                            .withImage(rpgPlayer.ultimateAbilityIcon == null ? "" : "Skill_Icons/" + rpgPlayer.ultimateAbilityIcon)
                            .withVisible(rpgPlayer.ultimateAbilityIcon != null)
                    );
                    hudRef.getById("skillIcon_E", ImageBuilder.class).ifPresent(l -> l
                            .withImage(rpgPlayer.primaryAbilityIcon == null ? "" : "Skill_Icons/" + rpgPlayer.primaryAbilityIcon)
                            .withVisible(rpgPlayer.primaryAbilityIcon != null)
                    );
                    hudRef.getById("skillIcon_R", ImageBuilder.class).ifPresent(l -> l
                            .withImage(rpgPlayer.secondaryAbilityIcon == null ? "" : "Skill_Icons/" + rpgPlayer.secondaryAbilityIcon)
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

                    // Update Mark Icons
                    hudRef.getById("assassinMark_Icon", ImageBuilder.class).ifPresent(l -> l
                            .withVisible(assassinMarkCount > 0)
                    );
                    hudRef.getById("assassinMark_Count", LabelBuilder.class).ifPresent(l -> l
                            .withText(String.valueOf(assassinMarkCount))
                            .withVisible(assassinMarkCount > 0)
                    );

                    // update raid HUD — only visible during an active raid
                    Module_RaidSystem.RaidHudState raidState = rpgPlayer.activeRaidHudState;
                    boolean raidActive = raidState != null;
                    hudRef.getById("raidHud_Icon", ImageBuilder.class).ifPresent(b -> b.withVisible(raidActive));
                    hudRef.getById("raidHud_WaveStatus", LabelBuilder.class).ifPresent(b -> b.withVisible(raidActive));
                    hudRef.getById("raidHud_Countdown", LabelBuilder.class).ifPresent(b -> b.withVisible(raidActive));
                    hudRef.getById("raidHud_ExplosionWarning", LabelBuilder.class).ifPresent(b -> b.withVisible(raidActive && ModConfig.get().raids.unkilled_raid_enemies_explode));

                    if (raidActive) {
                        long nowMs = System.currentTimeMillis();

                        // determine wave status text and countdown text based on current phase
                        String waveStatusText;
                        String countdownText;

                        if (raidState.currentWave == 0) {
                            // pre-first-wave phase
                            long secondsUntilFirst = Math.max(0, (raidState.firstWaveSpawnAtMs - nowMs) / 1000L);
                            waveStatusText = "Incoming...";
                            countdownText = "First wave in " + secondsUntilFirst + "s";
                        } else if (raidState.currentWave < raidState.totalWaves) {
                            // mid-raid — a wave has spawned, more to come
                            long nextWaveAtMs = raidState.firstWaveSpawnAtMs + ((long) raidState.currentWave * raidState.secondsBetweenWaves * 1000L);
                            long secondsUntilNext = Math.max(0, (nextWaveAtMs - nowMs) / 1000L);
                            waveStatusText = "Wave " + raidState.currentWave + " / " + raidState.totalWaves;
                            countdownText = "Next wave in " + secondsUntilNext + "s";
                        } else {
                            // all waves spawned — counting down to raid end
                            long secondsUntilEnd = Math.max(0, (raidState.raidEndMs - nowMs) / 1000L);
                            waveStatusText = "Wave " + raidState.totalWaves + " / " + raidState.totalWaves;
                            countdownText = "Raid ends in " + secondsUntilEnd + "s";
                        }

                        final String waveStatusFinal = waveStatusText;
                        final String countdownFinal = countdownText;
                        hudRef.getById("raidHud_WaveStatus", LabelBuilder.class).ifPresent(b -> b.withText(waveStatusFinal));
                        hudRef.getById("raidHud_Countdown", LabelBuilder.class).ifPresent(b -> b.withText(countdownFinal));
                    }

                    // determine if we should show the room info or not
                    boolean showRoomInfo = ModConfig.get().building.allow_light_well_territory_claim && rpgPlayer.territory != null;
                    hudRef.getById("currentRoomBorder", ImageBuilder.class).ifPresent(l -> l
                            .withVisible(showRoomInfo)
                    );
                    hudRef.getById("currentRoom", LabelBuilder.class).ifPresent(l -> l
                            .withVisible(showRoomInfo)
                    );
                    if (!showRoomInfo || rpgPlayer.territory.getOwnerUuid() == null) return;

                    // get the viewing player's uuid
                    UUIDComponent viewerUuid = store.getComponent(entityRef, UUIDComponent.getComponentType());

                    // priority: room > outdoor space > territory label
                    PlayerRef territoryOwner = Universe.get().getPlayer(rpgPlayer.territory.getOwnerUuid());
                    String ownerName = territoryOwner != null ? territoryOwner.getUsername() : "Unknown";
                    String roomText;
                    if (rpgPlayer.room != null) roomText = rpgPlayer.room.getDesignatedRoomType();
                    else if (rpgPlayer.outdoorRoom != null) roomText = rpgPlayer.outdoorRoom.getDesignatedRoomType();
                    else {
                        boolean isOwnerOrCoOwner = viewerUuid != null && (viewerUuid.getUuid().equals(rpgPlayer.territory.getOwnerUuid()) || rpgPlayer.territory.isCoOwner(viewerUuid.getUuid()));
                        if (isOwnerOrCoOwner) {
                            PlayerRef viewerPlayerRef = Universe.get().getPlayer(viewerUuid.getUuid());
                            roomText = viewerPlayerRef != null ? viewerPlayerRef.getUsername() + "'s Territory" : ownerName + "'s Territory";
                        } else roomText = ownerName + "'s Territory";
                    }

                    // set the room text
                    hudRef.getById("currentRoom", LabelBuilder.class).ifPresent(l -> l
                            .withText(roomText)
                    );
                } catch (Exception e) {}
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
            .withPadding(new HyUIPadding().setLeft(-460))
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
            .withImage("HUD/HyARPG_Texture_EmptySkillSlot.png")
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
            .withImage("HUD/HyARPG_Texture_EmptySkillSlot.png")
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
            .withImage("HUD/HyARPG_Texture_EmptySkillSlot.png")
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

    // function to show the xp bar
    private void createRoomHud(World world, Ref<EntityStore> entityRef, Store<EntityStore> store) {
        // if the light well territory claim is disabled then we don't need to do any of this
        if (!ModConfig.get().building.allow_light_well_territory_claim) return;

        // Current Room Decoration
        hud.addElement(new ImageBuilder()
            .withId("currentRoomBorder")
            .withAnchor(new HyUIAnchor()
                .setWidth(250)
                .setHeight(15)
                .setTop(99)
            )
            .withVisible(false)
            .withImage("Common/ContainerDecorationTop@2x.png")
        );

        // Current Room Label
        hud.addElement(new LabelBuilder()
            .withId("currentRoom")
            .withAnchor(new HyUIAnchor()
                .setWidth(500)
                .setHeight(30)
                .setTop(75)
            )
            .withStyle(new HyUIStyle()
                .setFontSize(18)
                .setTextColor("#cccccc")
                .setRenderBold(true)
                .setAlignment(Alignment.Center)
            )
            .withVisible(false)
            .withText("Simple Kitchen")
        );
    }

    // function to create the raid notification hud elements
    private void createRaidHud(World world, Ref<EntityStore> entityRef, Store<EntityStore> store) {
        // Raid icon — middle left, padded ~100px from left
        hud.addElement(new ImageBuilder()
            .withId("raidHud_Icon")
            .withAnchor(new HyUIAnchor()
                .setWidth(120)
                .setHeight(50)
                .setLeft(75)
                .setTop(200)
            )
            .withVisible(false)
            .withImage("HyARPG_Raid_Notification.png")
        );

        // Wave status label — e.g. "Wave 1 / 3" or "Incoming..."
        hud.addElement(new LabelBuilder()
            .withId("raidHud_WaveStatus")
            .withAnchor(new HyUIAnchor()
                .setWidth(200)
                .setHeight(20)
                .setLeft(75)
                .setTop(250)
            )
            .withStyle(new HyUIStyle()
                .setFontSize(13)
                .setTextColor("#ffffff")
                .setRenderBold(true)
            )
            .withVisible(false)
            .withText("Wave 1 / 3")
        );

        // Countdown label — e.g. "First wave in 30s" or "Next wave in 45s" or "Raid ends in 60s"
        hud.addElement(new LabelBuilder()
            .withId("raidHud_Countdown")
            .withAnchor(new HyUIAnchor()
                .setWidth(200)
                .setHeight(20)
                .setLeft(75)
                .setTop(270)
            )
            .withStyle(new HyUIStyle()
                .setFontSize(13)
                .setTextColor("#ffaa00")
                .setRenderBold(true)
            )
            .withVisible(false)
            .withText("")
    );

        // Explosion warning label — only shown if unkilled_raid_enemies_explode is set
        hud.addElement(new LabelBuilder()
            .withId("raidHud_ExplosionWarning")
            .withAnchor(new HyUIAnchor()
                .setWidth(200)
                .setHeight(20)
                .setLeft(75)
                .setTop(290)
            )
            .withStyle(new HyUIStyle()
                .setFontSize(12)
                .setTextColor("#ff4400")
                .setRenderBold(true)
            )
            .withVisible(false)
            .withText("Survivors will explode!")
        );
    }

    // function to show the player marks stack count
    private void createMarkHud(World world, Ref<EntityStore> entityRef, Store<EntityStore> store) {
        hud.addElement(new ImageBuilder()
            .withId("assassinMark_Icon")
            .withAnchor(new HyUIAnchor()
                .setWidth(25)
                .setHeight(25)
                .setBottom(175)
            )
            .withPadding(new HyUIPadding().setRight(676))
            .withVisible(false)
            .withImage("Skill_Icons/Assassin_Ability_AssassinsMark.png")
        )
        .addElement(new LabelBuilder()
            .withId("assassinMark_Count")
            .withAnchor(new HyUIAnchor()
                .setWidth(25)
                .setHeight(25)
                .setBottom(183)
            )
            .withStyle(new HyUIStyle()
                .setFontSize(11)
                .setTextColor("#ffffff")
                .setRenderBold(true)
            )
            .withPadding(new HyUIPadding().setLeft(-317))
            .withVisible(false)
            .withText("0")
        );
    }
}
