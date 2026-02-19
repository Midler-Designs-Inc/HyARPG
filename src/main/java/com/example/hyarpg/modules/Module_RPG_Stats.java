package com.example.hyarpg.modules;

// Hytale Imports
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

// Mod Imports
import com.example.hyarpg.events.*;
import com.example.hyarpg.HyARPGPlugin;
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.components.Component_RPG_Stats;
import com.example.hyarpg.components.Component_RPG_Enemy;

// Java Imports
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class Module_RPG_Stats {

    private final HyARPGPlugin plugin;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static ComponentType<EntityStore, Component_RPG_Stats> componentTypeRPGStats;
    public static ComponentType<EntityStore, Component_RPG_Enemy> componentTypeRPGEnemy;

    // properties that control enemy level as they get further from spawn
    private static final double LEVEL_DISTANCE_THRESHOLD = 500.0;
    private static final int LEVEL_VARIANCE = 5;
    private static final Random random = new Random();

    // Map<defender_ref, Map<attacker_ref, timestamp>>
    private final ConcurrentHashMap<Ref<EntityStore>, ConcurrentHashMap<Ref<EntityStore>, Long>> damageRegistry = new ConcurrentHashMap<>();

    // initialize this module
    public Module_RPG_Stats(HyARPGPlugin plugin) {
        this.plugin = plugin;

        // Register the component type using EntityStoreRegistry
        componentTypeRPGStats = plugin.getEntityStoreRegistry()
                .registerComponent(Component_RPG_Stats.class, "RPGStatsComponent", Component_RPG_Stats.CODEC);
        componentTypeRPGEnemy = plugin.getEntityStoreRegistry()
                .registerComponent(Component_RPG_Enemy.class, "RPGEnemyComponent", Component_RPG_Enemy.CODEC);

        // Listen to applicable events on the mods internal event bus
        ModEventBus.register(Event_PlayerReady.class, this::onPlayerReady);
        ModEventBus.register(Event_EntityDamaged.class, this::onEntityDamaged);
        ModEventBus.register(Event_NPCDeath.class, this::onEnemyKilled);
        ModEventBus.register(Event_NPCSpawn.class, this::onNPCSpawn);
        ModEventBus.register(Event_NPCPreSpawn.class, this::onNPCPreSpawn);
    }

    // This function runs whenever a PlayerReady event fires to add teh RPGStats component
    private void onPlayerReady(Event_PlayerReady event) {
        // get the joining player
        Player player = event.getPlayer();
        World world = event.getWorld();

        // get the player's Ref and the world entity store
        Ref<EntityStore> entityRef = player.getReference();
        Store<EntityStore> store = world.getEntityStore().getStore();
        if (entityRef == null) return;

        // ensure the component exists
        store.ensureAndGetComponent(entityRef, componentTypeRPGStats);
    }

    // This function runs whenever an NPCPreSpawn event is posted
    private void onNPCPreSpawn(Event_NPCPreSpawn event) {
        // get the entity holder Ref
        Holder<EntityStore> holder = event.getHolder();

        // Create an RPGEnemy component and assign a monster level
        int enemyLevel = calculateEnemyLevel(holder);
        Component_RPG_Enemy rpgEnemy = new Component_RPG_Enemy(enemyLevel);

        // Add the component
        holder.putComponent(componentTypeRPGEnemy, rpgEnemy);
    }

    // This function runs whenever an NPCSpawn event is posted
    private void onNPCSpawn(Event_NPCSpawn event) {
        // get the entity Ref
        Ref<EntityStore> ref = event.getRef();
        Store<EntityStore> store = event.getStore();
        CommandBuffer<EntityStore> commandBuffer = event.getCommandBuffer();

        // get the rpg enemy component
        Component_RPG_Enemy rpgEnemy = store.getComponent(ref, componentTypeRPGEnemy);
        if (rpgEnemy == null) return;
        int level = rpgEnemy.level;
        String rarityString = rpgEnemy.monsterRarity > 0 ? (rpgEnemy.getRarityString() + " ") : "";

        // get the NPC entity component
        NPCEntity npcEntity = store.getComponent(ref, NPCEntity.getComponentType());
        if (npcEntity == null) return;

        // Get the entity's role name and create the nameplate text
        String roleName = npcEntity.getRoleName().replace("_", " ");
        String nameplateText = rarityString + roleName + " (Lv. " + level + ")";

        // Nameplate is what actually shows above the head
        Nameplate nameplate = store.getComponent(ref, Nameplate.getComponentType());
        if (nameplate != null) nameplate.setText(nameplateText);
        else commandBuffer.addComponent(ref, Nameplate.getComponentType(), new Nameplate(nameplateText));

        // Add rarity effect if applicable
        if(rpgEnemy.monsterRarity > 0) {
            String entityEffectStr = rpgEnemy.getRarityString() + "_Glow";
            EntityEffect specialEffect = (EntityEffect) EntityEffect.getAssetMap().getAsset(entityEffectStr);
            if (specialEffect == null) return;

            EffectControllerComponent effectController = store.getComponent(ref, EffectControllerComponent.getComponentType());
            if (effectController != null) {
                effectController.addEffect(ref, specialEffect, commandBuffer);
                effectController.addEffect(ref, specialEffect, commandBuffer);
            }
        }
    }

    // This function adds/refreshes players/enemies to a registry when dealing damage/damages
    private void onEntityDamaged (Event_EntityDamaged event) {
        Ref<EntityStore> attacker = event.getAttacker();
        Ref<EntityStore> defender = event.getDefender();
        Store<EntityStore> store = event.getStore();
        Damage damage = event.getDamage();

        // check/register if a player is damaging an enemy
        Component_RPG_Stats rpgStats = store.getComponent(attacker, componentTypeRPGStats);
        if (rpgStats != null) {
            damage.setAmount(9999999999f);
            // register the player damage to the enemy in the damage registry
            damageRegistry
                .computeIfAbsent(defender, k -> new ConcurrentHashMap<>())
                .put(attacker, System.currentTimeMillis());
        }
    }

    // This function that fires when an enemy dies
    private void onEnemyKilled (Event_NPCDeath event) {
        awardXPToPlayers(event);
    }

    // check for deaths and award XP on repeat
    private void awardXPToPlayers(Event_NPCDeath event) {
        Ref<EntityStore> defender = event.getRef();
        Store<EntityStore> store = event.getStore();
        long cutoff = System.currentTimeMillis() - 30_000;

        // Pull and remove the attacker map for this specific enemy
        ConcurrentHashMap<Ref<EntityStore>, Long> attackers = damageRegistry.remove(defender);
        if (attackers == null) return;

        // get the world and then on it's next tick continue functionality
        World world = store.getExternalData().getWorld();
        world.execute(() -> {
            attackers.forEach((attacker, timestamp) -> {
                // Skip stale or invalid entries
                if (timestamp < cutoff || !attacker.isValid()) return;

                // get the playerRef that dealt damage and check they are still valid
                PlayerRef playerRef = store.getComponent(attacker, PlayerRef.getComponentType());
                if (playerRef == null) return;

                // get the killed enemies level or default to 1
                Component_RPG_Enemy rpgEnemy = store.getComponent(defender, componentTypeRPGEnemy);
                int enemyLevel = (rpgEnemy != null) ? rpgEnemy.level : 1;

                // award XP to the player
                Component_RPG_Stats attackerRPGStats = store.getComponent(attacker, componentTypeRPGStats);
                attackerRPGStats.awardXP(enemyLevel, playerRef);
            });
        });
    }

    // determine the distance in a straight line from 0,0 the entity is and set it's level accordingly
    private int calculateEnemyLevel(Holder<EntityStore> holder) {
        // get the entities transform component
        TransformComponent transform = holder.getComponent(TransformComponent.getComponentType());
        if (transform == null) return 1;

        // Extract the entities location
        Vector3d position = transform.getPosition();

        // Weight the y axis so things get stronger faster going down than they do going up
        double weightedY = position.y < 0 ? position.y * 1.5 : position.y;

        // 3D straight line distance from world origin 0,0,0
        double distance = Math.sqrt(
            position.x * position.x +
            weightedY * weightedY +
            position.z * position.z
        );

        // get level based on distance
        int baseLevel = Math.max(1, (int)(distance / LEVEL_DISTANCE_THRESHOLD) + 1);

        // roll for a random level within variance range of base level
        int variance = random.nextInt(LEVEL_VARIANCE * 2 + 1) - LEVEL_VARIANCE;

        // Minimum level 1 regardless of roll
        return Math.max(1, baseLevel + variance);
    }
}
