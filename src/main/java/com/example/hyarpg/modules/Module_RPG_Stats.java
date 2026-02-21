package com.example.hyarpg.modules;

// Hytale Imports
import com.example.hyarpg.components.Component_CraftingKnowledge;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
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
import java.awt.*;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class Module_RPG_Stats {

    private final HyARPGPlugin plugin;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static ComponentType<EntityStore, Component_RPG_Stats> componentTypeRPGStats;
    public static ComponentType<EntityStore, Component_RPG_Enemy> componentTypeRPGEnemy;
    public static ComponentType<EntityStore, Component_CraftingKnowledge> componentTypeCraftingKnowledge;

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
        componentTypeCraftingKnowledge = plugin.getEntityStoreRegistry()
                .registerComponent(Component_CraftingKnowledge.class, "CraftingKnowledgeComponent", Component_CraftingKnowledge.CODEC);

        // Listen to applicable events on the mods internal event bus
        ModEventBus.register(Event_PlayerReady.class, this::onPlayerReady);
        ModEventBus.register(Event_EntityPreDamaged.class, this::onEntityPreDamage);
        ModEventBus.register(Event_NPCDeath.class, this::onEnemyKilled);
        ModEventBus.register(Event_NPCSpawn.class, this::onNPCSpawn);
        ModEventBus.register(Event_NPCPreSpawn.class, this::onNPCPreSpawn);
        ModEventBus.register(Event_PlayerInventoryChange.class, this::onPlayerInventoryChange);
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

        // ensure the RPG Stats component exists, add it if it doesn't
        store.ensureAndGetComponent(entityRef, componentTypeRPGStats);

        // Check if the Crafting Knowledge component exists and if not add it
        Component_CraftingKnowledge knowledge = store.getComponent(entityRef, componentTypeCraftingKnowledge);
        if (knowledge == null) {
            knowledge = new Component_CraftingKnowledge();
            store.putComponent(entityRef, componentTypeCraftingKnowledge, knowledge);
        }
    }

    // This function runs whenever an NPCPreSpawn event is posted
    private void onNPCPreSpawn(Event_NPCPreSpawn event) {
        // get the entity holder Ref
        Holder<EntityStore> holder = event.getHolder();
        Store<EntityStore> store = event.getStore();

        // If the RPG Enemy component doesn't exist, add it
        Component_RPG_Enemy rpgEnemy = holder.getComponent(componentTypeRPGEnemy);
        if (rpgEnemy == null) {
            // Create an RPGEnemy component and assign a monster level
            int enemyLevel = calculateEnemyLevel(holder);
            rpgEnemy = new Component_RPG_Enemy(enemyLevel);

            // Add the component to the NPC
            holder.putComponent(componentTypeRPGEnemy, rpgEnemy);
        }

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
    private void onEntityPreDamage (Event_EntityPreDamaged event) {
        // get event properties
        Ref<EntityStore> attacker = event.getAttacker();
        Ref<EntityStore> defender = event.getDefender();
        Store<EntityStore> store = event.getStore();
        Damage damage = event.getDamage();

        // get attacker components
        Component_RPG_Stats attackerRPGStats = store.getComponent(attacker, componentTypeRPGStats);
        Component_RPG_Enemy attackerRPGEnemy = store.getComponent(attacker, componentTypeRPGEnemy);

        // default attacker values
        int attackerLevel = 1;
        int attackerRarity = 0;

        // determine attacker values
        if(attackerRPGStats != null) attackerLevel = attackerRPGStats.level;
        else if(attackerRPGEnemy != null) {
            attackerLevel = attackerRPGEnemy.level;
            attackerRarity = attackerRPGEnemy.monsterRarity;
        };

        // check if the defender is a player or NPC
        Component_RPG_Stats defenderRPGStats = store.getComponent(defender, componentTypeRPGStats);
        Component_RPG_Enemy defenderRPGEnemy = store.getComponent(defender, componentTypeRPGEnemy);

        // default defender values
        int defenderLevel = 1;
        int defenderRarity = 0;

        // determine defender values
        if(defenderRPGStats != null) defenderLevel = defenderRPGStats.level;
        else if(defenderRPGEnemy != null) {
            defenderLevel = defenderRPGEnemy.level;
            defenderRarity = defenderRPGEnemy.monsterRarity;
        };

        // if attacker is a player and defender is an enemy register the damage and adjust based on gear score
        if(attackerRPGStats != null && defenderRPGEnemy != null) {
            // register the player damage to the enemy in the damage registry
            damageRegistry
                .computeIfAbsent(defender, k -> new ConcurrentHashMap<>())
                .put(attacker, System.currentTimeMillis());

            // adjust attack stats to gear score instead of level
            Player player = store.getComponent(attacker, Player.getComponentType());
            if(player != null) attackerLevel = attackerRPGStats.calculateGearScore(player);
        }
        else if(defenderRPGStats != null && attackerRPGEnemy != null) {
            // adjust attack stats to gear score instead of level
            Player player = store.getComponent(defender, Player.getComponentType());
            if(player != null) defenderLevel = defenderRPGStats.calculateGearScore(player);
        }

        // adjust damage based player/enemy level
        adjustDamageBasedOnLevel(attackerLevel, attackerRarity, defenderLevel, defenderRarity, damage);
    }

    // This function that fires when an enemy dies
    private void onEnemyKilled (Event_NPCDeath event) {
        awardXPToPlayers(event);
    }

    // This function runs whenever a players inventory is changed
    private void onPlayerInventoryChange(Event_PlayerInventoryChange event) {
        // get our entity and store refs
        Ref<EntityStore> ref = event.getRef();
        Store<EntityStore> store = event.getStore();
        ItemContainer container = event.getChangeEvent().container();

        // loop over slot transactions and determine overall changes made
        for (ItemStackSlotTransaction tx : event.getSlotTransactions()) {
            if (!tx.succeeded()) continue;

            // get the slot and check what it's state was before and after
            short slot = tx.getSlot();
            ItemStack before = tx.getSlotBefore();
            ItemStack after = tx.getSlotAfter();

            // some basic logic gates to determine what changed
            boolean wasAdded = ItemStack.isEmpty(before) && !ItemStack.isEmpty(after);
            boolean wasRemoved = !ItemStack.isEmpty(before) && ItemStack.isEmpty(after);
            boolean wasSwapped = !ItemStack.isEmpty(before) && !ItemStack.isEmpty(after);

            // check if an item was added or removed
            Item addedItem = wasAdded ? after.getItem() : null;
            Item removedItem = wasRemoved ? before.getItem() : null;

            if (addedItem != null) onPlayerInventoryItemAdded(ref, store, slot, addedItem, after, container);
            if (removedItem != null) onPlayerInventoryItemRemoved(ref, store, slot, removedItem, before, container);
        }
    }

    // capture when an item is added to a players inventory
    private void onPlayerInventoryItemAdded(Ref<EntityStore> ref, Store<EntityStore> store, short slot, Item item, ItemStack stack, ItemContainer container) {
        // register discovery for ALL items
        registerDiscoveredItem(ref, store, item);

        // gear score only for weapons/armor
        if (item.getWeapon() != null || item.getArmor() != null) {
            assignGearScore(ref, store, stack, container, slot);
        }
    }

    // capture when an item is removed from a players inventory
    private void onPlayerInventoryItemRemoved(Ref<EntityStore> ref, Store<EntityStore> store, short slot, Item item, ItemStack stack, ItemContainer container) {}

    // register an item a player picked up to their discovered list
    private void registerDiscoveredItem(Ref<EntityStore> ref, Store<EntityStore> store, Item query) {
        // get the crafting knowledge component
        Component_CraftingKnowledge craftingKnowledge = store.getComponent(ref, componentTypeCraftingKnowledge);
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (craftingKnowledge == null || playerRef == null) return;

        // get the item id string and try to discover it
        String itemId = query.getId();
        String itemName = query.getTranslationKey();
        craftingKnowledge.addDiscoveredItem(playerRef, itemId, itemName);
    }

    // assign a gear score to an item a player picked up
    private void assignGearScore(Ref<EntityStore> ref, Store<EntityStore> store, ItemStack stack, ItemContainer container, short slot) {
        // Already has a gear score
        if (stack.getFromMetadataOrNull("GearScore", Codec.INTEGER) != null) return;

        // Get the level of the player who picked up the item
        Component_RPG_Stats stats = store.getComponent(ref, componentTypeRPGStats);
        if (stats == null) return;

        // If the gear has a gear score already bail
        if (stack.getFromMetadataOrNull("GearScore", Codec.INTEGER) != null) return;

        ItemStack leveled = stack.withMetadata("GearScore", Codec.INTEGER, stats.level);
        container.replaceItemStackInSlot(slot, stack, leveled);
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
                int enemyRarity = (rpgEnemy != null) ? rpgEnemy.monsterRarity : 0;

                // award XP to the player
                Component_RPG_Stats attackerRPGStats = store.getComponent(attacker, componentTypeRPGStats);
                attackerRPGStats.awardXP(enemyLevel, enemyRarity, playerRef);
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

    // adjust damage packets based on the enemies level
    private void adjustDamageBasedOnLevel(int attackerLevel, int attackerRarity, int defenderLevel, int defenderRarity, Damage damage) {
        // Tunable constants (safe for infinite scaling)
        final double LEVEL_MULTIPLIER = 1.15;   // 15% per level
        final double RARITY_MULTIPLIER = 1.33;  // 33% per rarity tier

        // determine delta based on level/rarity difference
        int levelDelta = attackerLevel - defenderLevel;
        int rarityDelta = attackerRarity - defenderRarity;

        // use the deltas to determen a level/rarity scale
        double levelScale = Math.pow(LEVEL_MULTIPLIER, levelDelta);
        double rarityScale = Math.pow(RARITY_MULTIPLIER, rarityDelta);

        // apply the damage scales to the damage amount
        double scaledDamage = damage.getAmount();
        scaledDamage *= levelScale;
        scaledDamage *= rarityScale;

        // Clamp result to prevent degenerate damage
        scaledDamage = Math.max(1.0, scaledDamage);

        // update the value on the damage object
        damage.setAmount((int) scaledDamage);

        // loop over all players and broadcast the message
        for (PlayerRef player : Universe.get().getPlayers()) {
            player.sendMessage(Message.raw(
                "Initial Damage: " + damage.getInitialAmount() +
                " Final Damage: " + damage.getAmount()
            ).color(Color.YELLOW));
            player.sendMessage(Message.raw(
                "Level Delta: " + levelDelta +
                " Rarity Delta: " + rarityDelta
            ).color(Color.YELLOW));
            player.sendMessage(Message.raw(
                "Level Scale: " + levelScale +
                " Rarity Scale: " + rarityScale
            ).color(Color.YELLOW));
        }
    }
}
