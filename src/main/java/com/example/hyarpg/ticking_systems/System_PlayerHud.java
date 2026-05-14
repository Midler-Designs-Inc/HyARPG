package com.example.hyarpg.ticking_systems;

// Hytale Imports
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemGroupDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.components.Component_Hunger;
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.components.Component_Thirst;
import com.example.hyarpg.configs.ModConfig;
import com.example.hyarpg.modules.Module_Hunger;
import com.example.hyarpg.modules.Module_RaidSystem;
import com.example.hyarpg.modules.Module_RPGSystem;
import com.example.hyarpg.modules.Module_Thirst;
import com.example.hyarpg.ui.CustomHUD_Player;
import com.example.hyarpg.utils.skills.SkillNode;

// Java Imports
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

public class System_PlayerHud extends EntityTickingSystem<EntityStore> {

    // how often the HUD is refreshed — accumulates dt in nanos and fires when this threshold is crossed
    private static final long REFRESH_INTERVAL_NS = 100 * 1_000_000L;

    private final ComponentType<EntityStore, Component_RPG_Player> componentType;

    public System_PlayerHud(ComponentType<EntityStore, Component_RPG_Player> componentType) {
        this.componentType = componentType;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() { return Query.and(this.componentType); }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        Component_RPG_Player rpgPlayer = chunk.getComponent(index, Module_RPGSystem.componentTypeRPGPlayer);
        if (rpgPlayer == null) return;

        // accumulate elapsed time and bail early if we haven't hit the refresh threshold yet
        rpgPlayer.hudAccumulatorNs += (long)(dt * 1_000_000_000L);
        if (rpgPlayer.hudAccumulatorNs < REFRESH_INTERVAL_NS) return;
        rpgPlayer.hudAccumulatorNs = 0;

        // skip dead players and any entity missing the core player component
        DeathComponent death = store.getComponent(ref, DeathComponent.getComponentType());
        if (death != null) return;
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        // only push updates if our HUD is currently the registered one — another mod could have claimed the slot
        HudManager hudManager = player.getHudManager();
        CustomUIHud registeredHud = hudManager.getCustomHud();
        if (!(registeredHud instanceof CustomHUD_Player playerHUD)) return;

        // bail if any of the core components we depend on are missing
        Component_Thirst thirst = store.getComponent(ref, Module_Thirst.componentTypeThirst);
        Component_Hunger hunger = store.getComponent(ref, Module_Hunger.componentTypeHunger);
        EntityStatMap statMap = store.getComponent(ref, EntityStatsModule.get().getEntityStatMapComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (thirst == null || hunger == null || statMap == null || playerRef == null) return;

        // build a fresh state snapshot from the current world-thread component values
        CustomHUD_Player.HudState state = new CustomHUD_Player.HudState();

        // resource bar percentages
        state.thirstPercent = thirst.getPercentage();
        state.hungerPercent = hunger.getPercentage();
        state.levelPercent = rpgPlayer.calculateLevelProgress();
        state.playerLevel = rpgPlayer.level;
        state.gearScore = rpgPlayer.gearScore;
        state.skillPoints = rpgPlayer.skillPoints;

        // barrier is expressed as a fraction of max health so it maps directly onto the health bar width
        int barrierStatIndex = EntityStatType.getAssetMap().getIndex("BarrierOnBlock");
        int healthIndex = DefaultEntityStatTypes.getHealth();
        EntityStatValue barrierStat = statMap.get(barrierStatIndex);
        EntityStatValue healthStat = statMap.get(healthIndex);
        if (barrierStat != null && healthStat != null && barrierStat.getMax() > 0) state.barrierOnBlockPercent = (float) barrierStat.get() / (float) healthStat.getMax();

        // ability icon paths and cooldown countdowns for E and R slots
        state.ultimateAbilityIcon = rpgPlayer.ultimateAbilityIcon;
        state.primaryAbilityIcon = rpgPlayer.primaryAbilityIcon;
        state.secondaryAbilityIcon = rpgPlayer.secondaryAbilityIcon;
        state.secondsLeft_E = resolveCooldownSeconds(rpgPlayer.primaryAbility, rpgPlayer);
        state.secondsLeft_R = resolveCooldownSeconds(rpgPlayer.secondaryAbility, rpgPlayer);

        // mark stacks applied to enemies by the assassin class
        state.assassinMarkCount = rpgPlayer.marks.count("ASSASSIN");

        // world tier and estimated enemy level derived from the player's distance from origin
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform != null) {
            double dx = transform.getPosition().x - ModConfig.get().world.origin_spawn_point_x;
            double dy = transform.getPosition().y - ModConfig.get().world.origin_spawn_point_y;
            double dz = transform.getPosition().z - ModConfig.get().world.origin_spawn_point_z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            // determine world tier based on the metal configs
            if (dist >= ModConfig.get().world.min_distance_for_mithril_spawn) state.worldTier = 6;
            else if (dist >= ModConfig.get().world.min_distance_for_adamantite_spawn) state.worldTier = 5;
            else if (dist >= ModConfig.get().world.min_distance_for_cobalt_spawn) state.worldTier = 4;
            else if (dist >= ModConfig.get().world.min_distance_for_thorium_spawn) state.worldTier = 3;
            else if (dist >= ModConfig.get().world.min_distance_for_iron_spawn) state.worldTier = 2;
            else if (dist >= ModConfig.get().world.min_distance_for_copper_spawn) state.worldTier = 1;

            // calculate the average enemy level
            state.avgEnemyLevel = Math.max(1, (int)(dist / ModConfig.get().enemies.blocks_per_level_threshold) + 1);
        }

        // room and territory label — only shown when light well claiming is enabled and the player is on claimed land
        state.showRoomInfo = ModConfig.get().building.allow_light_well_territory_claim && rpgPlayer.territory != null;
        if (state.showRoomInfo && rpgPlayer.territory.getOwnerUuid() != null) {
            UUIDComponent viewerUuid = store.getComponent(ref, UUIDComponent.getComponentType());
            PlayerRef territoryOwner = Universe.get().getPlayer(rpgPlayer.territory.getOwnerUuid());
            String ownerName = territoryOwner != null ? territoryOwner.getUsername() : "Unknown";

            if (rpgPlayer.room != null) state.roomText = rpgPlayer.room.getDesignatedRoomType();
            else if (rpgPlayer.outdoorRoom != null) state.roomText = rpgPlayer.outdoorRoom.getDesignatedRoomType();
            else {
                boolean isOwnerOrCoOwner = viewerUuid != null && (viewerUuid.getUuid().equals(rpgPlayer.territory.getOwnerUuid()) || rpgPlayer.territory.isCoOwner(viewerUuid.getUuid()));
                PlayerRef viewerPlayerRef = isOwnerOrCoOwner ? Universe.get().getPlayer(viewerUuid.getUuid()) : null;
                state.roomText = viewerPlayerRef != null ? viewerPlayerRef.getUsername() + "'s Territory" : ownerName + "'s Territory";
            }
        }

        // raid panel — tracks wave progress, enemy count, and countdown timers for the active raid
        Module_RaidSystem.RaidHudState raidState = rpgPlayer.activeRaidHudState;
        state.raidActive = raidState != null;
        state.showExplosionWarning = ModConfig.get().raids.unkilled_raid_enemies_explode;
        if (state.raidActive) {
            long nowMs = System.currentTimeMillis();
            if (raidState.currentWave == 0) {
                state.raidWaveStatus = "Incoming...";
                state.raidCountdown = "First wave in " + Math.max(0, (raidState.firstWaveSpawnAtMs - nowMs) / 1000L) + "s";
            } else if (raidState.currentWave < raidState.totalWaves) {
                long nextWaveAtMs = raidState.firstWaveSpawnAtMs + ((long) raidState.currentWave * raidState.secondsBetweenWaves * 1000L);
                state.raidWaveStatus = "Wave " + raidState.currentWave + " / " + raidState.totalWaves;
                state.raidCountdown = "Next wave in " + Math.max(0, (nextWaveAtMs - nowMs) / 1000L) + "s";
            } else {
                state.raidWaveStatus = "Wave " + raidState.totalWaves + " / " + raidState.totalWaves;
                state.raidCountdown = "Raid ends in " + Math.max(0, (raidState.raidEndMs - nowMs) / 1000L) + "s";
            }
            state.raidEnemiesRemaining = raidState.raidGroup != null ? raidState.getLiveEnemyCount(player.getWorld()) : 0;
        }

        // all state is ready — hand it off to the HUD to push the delta packet
        playerHUD.pushUpdate(state);
    }

    // walk back from now to the ability's last use and return how many whole seconds remain on the cooldown
    private int resolveCooldownSeconds(@Nullable String abilityId, @Nonnull Component_RPG_Player rpgPlayer) {
        if (abilityId == null) return 0;
        SkillNode node = rpgPlayer.skillLibrary.findNode(abilityId);
        if (node == null || node.ability == null) return 0;
        long lastUse = node.ability.getLastUse();
        if (lastUse == 0) return 0;
        long remainingNanos = (node.ability.cooldownSeconds * 1_000_000_000L) - (System.nanoTime() - lastUse);
        return (int) Math.max(0, (remainingNanos + 999_999_999L) / 1_000_000_000L);
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(new SystemGroupDependency(Order.BEFORE, EntityStore.SEND_PACKET_GROUP));
    }
}