package com.example.hyarpg.modules;

// Hytale imports
import com.example.hyarpg.configs.ModConfig;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesSystems;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod imports
import com.example.hyarpg.components.*;
import com.example.hyarpg.HyARPGPlugin;

// Java imports
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Module_ModTickLoop {

    private final HyARPGPlugin plugin;

    // set the interval at which this tick loop executes
    private static final long TICK_INTERVAL_MS = 200;
    private static final long TICK_INTERVALS_PER_SECOND = (1000 / TICK_INTERVAL_MS);

    // setup some scheduler props
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledFuture<?> future;

    // constructor
    public Module_ModTickLoop(HyARPGPlugin plugin, ScheduledExecutorService scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    // function to start the ticking (tick, tick, boom!)
    public void start() {
        // check if the scheduler is already running and if so bail
        if (!running.compareAndSet(false, true)) return;

        // schedule a repeating task
        future = scheduler.scheduleAtFixedRate(() -> {
            long start = System.nanoTime();

            // loop over all worlds, and queue up our player loop logic
            for (World world : Universe.get().getWorlds().values().toArray(new World[0])) {
                world.execute(() -> {
                    // get the store for this world
                    Store<EntityStore> store = world.getEntityStore().getStore();

                    // flush ready damage groups
                    plugin.rpgSystem.tickDamageGroups(store);

                    // loop over all players in the world
                    for (PlayerRef playerRef : Universe.get().getPlayers()) {
                       try {
                           // validate teh player ref
                           if (!playerRef.isValid()) continue;

                           // get the entity ref
                           Ref<EntityStore> ref = playerRef.getReference();
                           if (ref == null) continue;

                           // get the player
                           Player player = store.getComponent(ref, Player.getComponentType());
                           if (player == null) continue;

                           // do our individual tick concerns
                           tickHunger(ref, store, player);
                           tickThirst(ref, store, player);
                           tickGearRefresh(ref, store, player);
                           tickResourceRegens(ref, store, player);
                       } catch (Exception e) {}
                    }
                });
            }

            long elapsed = System.nanoTime() - start;

            // Optional: log slow ticks
            if (elapsed > TimeUnit.MILLISECONDS.toNanos(TICK_INTERVAL_MS)) {
                System.err.println("Tick overran interval: " +
                        TimeUnit.NANOSECONDS.toMillis(elapsed) + "ms");
            }

        }, 0, TICK_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    // function to stop the ticking (tick, ti...., what no boom?)
    public void stop() {
        if (!running.compareAndSet(true, false)) return;
        if (future != null) future.cancel(false);
    }

    // function to tick down hunger
    private void tickHunger(Ref<EntityStore> ref, Store<EntityStore> store, Player player){
        try {
            // if it's creative mode don't interact with hunger and bail
            if (player.getGameMode() == GameMode.Creative || !ModConfig.get().hunger.enabled) return;

            // get the hunger component
            Component_Hunger hunger = store.getComponent(ref, Module_Hunger.componentTypeHunger);

            // do the drain
            hunger.drain((float) ModConfig.get().hunger.drain_rate);

            // Apply starvation damage if starving
            if (hunger.isStarving()) {
                // get the stat map component from the player
                ComponentType<EntityStore, EntityStatMap> statMapType =
                EntityStatsModule.get().getEntityStatMapComponentType();
                EntityStatMap statMap = store.getComponent(ref, statMapType);
                if (statMap == null) return;

                // Get the health stat from the stat map
                int healthIndex = DefaultEntityStatTypes.getHealth();
                EntityStatValue healthStat = statMap.get(healthIndex);
                if (healthStat == null) return;

                // get teh current and max values from health stat
                float currentHealth = healthStat.get();
                float maxHealth = healthStat.getMax();

                // Only damage if health > 1 (don't kill the player)
                if (currentHealth > 1.0f) {
                    float healthDMG = maxHealth / ((float)ModConfig.get().hunger.seconds_till_death * TICK_INTERVALS_PER_SECOND); // full drain over 60 seconds
                    float newHealth = Math.max(1.0f, currentHealth - healthDMG);
                    statMap.setStatValue(healthIndex, newHealth);
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    // function to tick down thirst
    private void tickThirst(Ref<EntityStore> ref, Store<EntityStore> store, Player player){
        try {
            // if it's creative mode don't interact with thirst and bail
            if (player.getGameMode() == GameMode.Creative || !ModConfig.get().thirst.enabled) return;

            // get the thirst component
            Component_Thirst thirst = store.getComponent(ref, Module_Thirst.componentTypeThirst);

            // do the drain
            thirst.drain((float) ModConfig.get().thirst.drain_rate);

            // Apply starvation damage if starving
            if (thirst.isStarving()) {
                // get the stat map component from the player
                ComponentType<EntityStore, EntityStatMap> statMapType = EntityStatsModule.get().getEntityStatMapComponentType();
                EntityStatMap statMap = store.getComponent(ref, statMapType);
                if (statMap == null) return;

                // Get the health stat from the stat map
                int healthIndex = DefaultEntityStatTypes.getHealth();
                EntityStatValue healthStat = statMap.get(healthIndex);
                if (healthStat == null) return;

                // get teh current and max values from health stat
                float currentHealth = healthStat.get();
                float maxHealth = healthStat.getMax();

                // Only damage if health > 1 (don't kill the player)
                if (currentHealth > 1.0f) {
                    float healthDMG = maxHealth / ((float)ModConfig.get().thirst.seconds_till_death * TICK_INTERVALS_PER_SECOND); // full drain over 60 seconds
                    float newHealth = Math.max(1.0f, currentHealth - healthDMG);
                    statMap.setStatValue(healthIndex, newHealth);
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    // function to update gear score
    private void tickGearRefresh(Ref<EntityStore> ref, Store<EntityStore> store, Player player){
        try {
            // get the RPG Player component
            Component_RPG_Player rpgPlayer = store.getComponent(ref, Module_RPG_System.componentTypeRPGPlayer);
            if(rpgPlayer == null) return;

            // get the players current mainhand/offhand item and see if it's any different, if not we can skip
            // because we already do this when they equip/unequip something, we just can't fire those for mainhad/offhand changes
            ItemStack mainHand = player.getInventory().getItemInHand();
            ItemStack offHand = player.getInventory().getUtilityItem();
            if(Objects.equals(rpgPlayer.mainHandItem, mainHand) && Objects.equals(rpgPlayer.offHandItem, offHand)) return;

            // calculate the gear score
            rpgPlayer.calculateGearScore(player);
            rpgPlayer.calculateAffixStats(ref, store);

            // mark the offhand/utility item so they aren't dirty anymore
            rpgPlayer.mainHandItem = mainHand;
            rpgPlayer.offHandItem = offHand;
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    // function to tick resource regen
    private void tickResourceRegens(Ref<EntityStore> ref, Store<EntityStore> store, Player player) {
        try {
            // get the rpg player component
            Component_RPG_Player rpgPlayer = store.getComponent(ref, Module_RPG_System.componentTypeRPGPlayer);
            if (rpgPlayer == null) return;

            // get the stat map component from the player
            ComponentType<EntityStore, EntityStatMap> statMapType = EntityStatsModule.get().getEntityStatMapComponentType();
            EntityStatMap statMap = store.getComponent(ref, statMapType);
            if (statMap == null) return;

            // get stat indices
            int healthIndex = DefaultEntityStatTypes.getHealth();
            int staminaIndex = DefaultEntityStatTypes.getStamina();
            int manaIndex = DefaultEntityStatTypes.getMana();
            int ammoIndex = DefaultEntityStatTypes.getAmmo();

            // get current stat values
            EntityStatValue healthStat = statMap.get(healthIndex);
            EntityStatValue staminaStat = statMap.get(staminaIndex);
            EntityStatValue manaStat = statMap.get(manaIndex);
            EntityStatValue ammoStat = statMap.get(ammoIndex);
            if (healthStat == null || staminaStat == null || manaStat == null || ammoStat == null) return;

            // get movement states to check regen conditions
            MovementStatesComponent movementStateComp = store.getComponent(ref, MovementStatesComponent.getComponentType());
            MovementStates movementStates = movementStateComp != null ? movementStateComp.getMovementStates() : null;
            boolean isSprinting = movementStates != null && movementStates.sprinting;
            boolean isGliding = movementStates != null && movementStates.gliding;

            // check if stamina regen is delayed
            int staminaRegenDelayIndex = EntityStatType.getAssetMap().getIndex("StaminaRegenDelay");
            EntityStatValue regenDelayStat = statMap.get(staminaRegenDelayIndex);
            boolean staminaRegenDelayed = regenDelayStat != null && regenDelayStat.get() != 0;

            // compute per-tick scalar (regen values are per second, divide by ticks per second)
            float tickScalar = 1f / TICK_INTERVALS_PER_SECOND;

            // get base values
            float healthBaseRegen = ModConfig.get().players.base_health_regen_per_second;
            float manaBaseRegen = ModConfig.get().players.base_mana_regen_per_second;
            float staminaBaseRegen = ModConfig.get().players.base_stamina_regen_per_second;
            float ammoBaseRegen = ModConfig.get().players.base_ammo_regen_per_second;

            // --- HEALTH REGEN ---
            float healthRegenPerTick = (healthBaseRegen + rpgPlayer.stats.getFlatResourceRegen("Life")) * (1f + rpgPlayer.stats.getIncreasedResourceRegen("Life") / 100f) * tickScalar;
            if (healthRegenPerTick != 0) statMap.addStatValue(healthIndex, healthRegenPerTick);

            // --- MANA REGEN ---
            float manaRegenPerTick = (manaBaseRegen + rpgPlayer.stats.getFlatResourceRegen("Mana")) * (1f + rpgPlayer.stats.getIncreasedResourceRegen("Mana") / 100f) * tickScalar;
            if (manaRegenPerTick != 0) statMap.addStatValue(manaIndex, manaRegenPerTick);

            // --- STAMINA REGEN ---
            boolean staminaCanRegen = !isSprinting && !isGliding && !staminaRegenDelayed;
            if (staminaCanRegen) {
                float staminaRegenPerTick = (staminaBaseRegen + rpgPlayer.stats.getFlatResourceRegen("Stamina")) * (1f + rpgPlayer.stats.getIncreasedResourceRegen("Stamina") / 100f) * tickScalar;
                if (staminaRegenPerTick != 0) statMap.addStatValue(staminaIndex, staminaRegenPerTick);
            }

            // --- AMMO REGEN ---
            float ammoRegenPerTick = ammoBaseRegen * (1f + rpgPlayer.stats.getAmmoRegenPercent() / 100f) * tickScalar;
            if (ammoRegenPerTick != 0) statMap.addStatValue(ammoIndex, ammoRegenPerTick);

        } catch (Exception e) {}
    }
}