package com.example.hyarpg.modules;

// Hytale Imports
import com.example.hyarpg.interactions.*;
import com.example.hyarpg.utils.Affix;
import com.example.hyarpg.utils.AffixPool;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.ItemQuality;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.item.ItemModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;

// Mod Imports
import com.example.hyarpg.components.Component_CraftingKnowledge;
import com.example.hyarpg.events.*;
import com.example.hyarpg.HyARPGPlugin;
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.components.Component_RPG_Enemy;

// Java Imports
import java.awt.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class Module_RPG_System {

    private final HyARPGPlugin plugin;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static ComponentType<EntityStore, Component_RPG_Player> componentTypeRPGPlayer;
    public static ComponentType<EntityStore, Component_RPG_Enemy> componentTypeRPGEnemy;
    public static ComponentType<EntityStore, Component_CraftingKnowledge> componentTypeCraftingKnowledge;

    // properties that control enemy level as they get further from spawn
    private static final double LEVEL_DISTANCE_THRESHOLD = 500.0;
    private static final int LEVEL_VARIANCE = 5;
    private static final Random random = new Random();

    // Create a map to match a rarity string to a java utils color
    public class colorUtils {
        // simple JS-style lookup map
        private static final Map<String, Color> RARITY_COLORS = Map.of(
            "Common", Color.WHITE,
            "Uncommon", Color.GREEN,
            "Rare", Color.BLUE,
            "Epic", new Color(255, 0, 255),        // magenta
            "Legendary", new Color(255, 180, 0)   // gold-orange
        );

        // usage
        public static Color getRarityColor(String rarity) {
            return RARITY_COLORS.getOrDefault(rarity, Color.WHITE);
        }
    }

    // Create a map to match a rarity string to number of affixes
    public class rarityToAffixMap {
        // simple JS-style lookup map
        private static final Map<String, Integer> AFFIX_COUNT = Map.of(
            "Common", 0,
            "Uncommon", 1,
            "Rare", 2,
            "Epic", 3,
            "Legendary", 4
        );

        // usage
        public static int getAffixCount(String rarity) {
            if (rarity == null) return 0;
            return AFFIX_COUNT.getOrDefault(rarity, 0);
        }
    }

    // Map<defender_ref, Map<attacker_ref, timestamp>
    private final ConcurrentHashMap<Ref<EntityStore>, ConcurrentHashMap<Ref<EntityStore>, Long>> damageRegistry = new ConcurrentHashMap<>();

    // initialize this module
    public Module_RPG_System(HyARPGPlugin plugin) {
        this.plugin = plugin;

        // Register the component type using EntityStoreRegistry
        componentTypeRPGPlayer = plugin.getEntityStoreRegistry()
                .registerComponent(Component_RPG_Player.class, "RPGStatsComponent", Component_RPG_Player.CODEC);
        componentTypeRPGEnemy = plugin.getEntityStoreRegistry()
                .registerComponent(Component_RPG_Enemy.class, "RPGEnemyComponent", Component_RPG_Enemy.CODEC);
        componentTypeCraftingKnowledge = plugin.getEntityStoreRegistry()
                .registerComponent(Component_CraftingKnowledge.class, "CraftingKnowledgeComponent", Component_CraftingKnowledge.CODEC);

        // Get the interaction registry and register the custom interactions
        final var interactionRegistry = plugin.getCodecRegistry(Interaction.CODEC);
        interactionRegistry.register("LearnRandomGearRecipe_Uncommon", Interaction_LearnRandomGearRecipe_Uncommon.class, Interaction_LearnRandomGearRecipe_Uncommon.CODEC);
        interactionRegistry.register("LearnRandomGearRecipe_Rare", Interaction_LearnRandomGearRecipe_Rare.class, Interaction_LearnRandomGearRecipe_Rare.CODEC);
        interactionRegistry.register("LearnRandomGearRecipe_Epic", Interaction_LearnRandomGearRecipe_Epic.class, Interaction_LearnRandomGearRecipe_Epic.CODEC);
        interactionRegistry.register("LearnRandomGearRecipe_Legendary", Interaction_LearnRandomGearRecipe_Legendary.class, Interaction_LearnRandomGearRecipe_Legendary.CODEC);
        interactionRegistry.register("ShowRPGStats", Interaction_ShowRPGStats.class, Interaction_ShowRPGStats.CODEC);

        // Listen to applicable events on the mods internal event bus
        ModEventBus.register(Event_PlayerReady.class, this::onPlayerReady);
        ModEventBus.register(Event_EntityPreDamaged.class, this::onEntityPreDamage);
        ModEventBus.register(Event_NPCDeath.class, this::onEnemyKilled);
        ModEventBus.register(Event_NPCSpawn.class, this::onNPCSpawn);
        ModEventBus.register(Event_NPCPreSpawn.class, this::onNPCPreSpawn);
        ModEventBus.register(Event_PlayerInventoryItemAdded.class, this::onPlayerInventoryItemAdded);
        ModEventBus.register(Event_PlayerInventoryItemRemoved.class, this::onPlayerInventoryItemRemoved);
        ModEventBus.register(Event_PlayerInventoryItemEquip.class, this::onPlayerInventoryItemEquip);
        ModEventBus.register(Event_PlayerInventoryItemUnEquip.class, this::onPlayerInventoryItemUnEquip);
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
        store.ensureAndGetComponent(entityRef, componentTypeRPGPlayer);
        store.ensureAndGetComponent(entityRef, componentTypeCraftingKnowledge);
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
    private void onEntityPreDamage(Event_EntityPreDamaged event) {
        // get event properties
        Ref<EntityStore> attacker = event.getAttacker();
        Ref<EntityStore> defender = event.getDefender();
        Store<EntityStore> store = event.getStore();
        Damage damage = event.getDamage();

        // get attacker components
        Component_RPG_Player attackerRPGStats = store.getComponent(attacker, componentTypeRPGPlayer);
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
        Component_RPG_Player defenderRPGStats = store.getComponent(defender, componentTypeRPGPlayer);
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

//        // debug info about damage source
//        Damage.Source dmgSource = damage.getSource();
//        if (dmgSource instanceof Damage.ProjectileSource projectileSource) {
//            alertPlayers("Projectile from: " + projectileSource.getRef(), Color.BLUE);
//        } else if (dmgSource instanceof Damage.EntitySource entitySource) {
//            Ref<EntityStore> sourceRef = entitySource.getRef();
//
//            // check if it has an ItemComponent (thrown items have this)
//            ItemComponent itemComponent = store.getComponent(sourceRef, ItemComponent.getComponentType());
//            if (itemComponent != null) {
//                ItemStack itemStack = itemComponent.getItemStack();
//                alertPlayers("Source item: " + itemStack.getItemId(), Color.BLUE);
//            }
//        } else if (dmgSource instanceof Damage.EnvironmentSource envSource) {
//            alertPlayers("Environment: " + envSource.getType(), Color.BLUE);
//        }
//
//        DamageCause.getAssetMap().getAssetMap().forEach((id, cause) -> {
//            alertPlayers("Cause: " + cause.getId() + " index: " + DamageCause.getAssetMap().getIndex(cause.getId()), Color.BLUE);
//        });
//
//        // Damage cause — index 6 maps to a DamageCause asset, get its ID:
//        DamageCause cause = damage.getCause();
//        if (cause != null) alertPlayers("Cause ID: " + cause.getId(), Color.BLUE);
//        boolean isProjectile = damage.getSource() instanceof Damage.ProjectileSource;
//        alertPlayers("Was a projectile: " + (isProjectile ? "Yes!" : "No!"), Color.BLUE);
//
//        // other stuff
//        Boolean blocked = damage.getMetaStore().getMetaObject(Damage.BLOCKED);
//        KnockbackComponent kb = damage.getMetaStore().getMetaObject(Damage.KNOCKBACK_COMPONENT);
//        alertPlayers("Blocked: " + blocked, Color.BLUE);

        // adjust damage based player/enemy level
        adjustDamageBasedOnLevel(attackerLevel, attackerRarity, defenderLevel, defenderRarity, damage);
    }

    // This function fires when an enemy dies to award XP and modify loot
    private void onEnemyKilled(Event_NPCDeath event) {
        // get event props
        CommandBuffer<EntityStore> commandBuffer = event.getCommandBuffer();
        Ref<EntityStore> ref = event.getRef();
        Store<EntityStore> store = event.getStore();

        // get the NPC component
        NPCEntity npcComponent = commandBuffer.getComponent(ref, NPCEntity.getComponentType());
        if (npcComponent == null) return;

        // get the role component
        Role role = npcComponent.getRole();
        if (role == null) return;

        // get the drop list from the role!?
        String dropListId = role.getDropListId();
        if (dropListId == null) return;

        // get the item module from teh drop list!?
        ItemModule itemModule = ItemModule.get();
        if (!itemModule.isEnabled()) return;

        // get the items that are to be dropped
        var drops = itemModule.getRandomItemDrops(dropListId);

        // filter out vanilla weapons and armor
        List<ItemStack> filteredDrops = new ObjectArrayList();
        for (ItemStack drop : drops) {
            Item item = drop.getItem();
            if (item.getWeapon() != null || item.getArmor() != null) {
                alertPlayers("Filtered out: " + item.getId(), Color.GRAY);
            };
            filteredDrops.add(drop);
        }

        // get a list of players that attacked the enemy in the last 30 seconds
        List<Ref<EntityStore>> players = getAttackingPlayers(ref, store);

        // add drops to the pool
        rollLoot(ref, store, players, filteredDrops);

        // spawn filtered drops
        if (!filteredDrops.isEmpty()) {
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            HeadRotation headRotation = store.getComponent(ref, HeadRotation.getComponentType());
            assert transform != null && headRotation != null;

            Vector3d dropPosition = transform.getPosition().clone().add(0.0, 1.0, 0.0);
            Holder<EntityStore>[] dropEntities = ItemComponent.generateItemDrops(store, filteredDrops, dropPosition, headRotation.getRotation().clone());
            commandBuffer.addEntities(dropEntities, AddReason.SPAWN);
        }

        // do this last because it removes the enemy from the damage registry
        awardXPToPlayers(event);
    }

    // capture when an item is added to a players inventory
    private void onPlayerInventoryItemAdded(Event_PlayerInventoryItemAdded event) {
        // entity and store refs
        Ref<EntityStore> ref = event.getRef();
        Store<EntityStore> store = event.getStore();

        // get item stack, item, item slot and item container
        ItemStack stack = event.getStack();
        Item item = stack.getItem();
        short slot = event.getSlot();
        ItemContainer.ItemContainerChangeEvent changeEvent = event.getChangeEvent();
        ItemContainer container = changeEvent.container();

        // gear score only for weapons/armor
        if ((item.getWeapon() != null || item.getArmor() != null)) {
            // swap vanilla items if applicable, this will replace the itemstack or empty it
            swapVanillaItem(ref, store, stack, container, slot);
            stack = container.getItemStack(slot);
            if(stack == null || stack.isEmpty()) return;

            // get the players level
            Component_RPG_Player rpgPlayer = store.getComponent(ref, componentTypeRPGPlayer);
            int level = rpgPlayer == null ? 1 : rpgPlayer.level;

            // assign a gear score to the item
            ItemStack newStack = assignGearScoreAndAffixes(stack, level);
            if(newStack == null || newStack.isEmpty()) return;

            // swap out the old stack for the new stack, then update refernce for down stream
            container.replaceItemStackInSlot(slot, stack, newStack);
            stack = newStack;
        }

        // register discovery for ALL items
        registerDiscoveredItem(ref, store, stack.getItem());
    }

    // capture when an item is removed from a players inventory
    private void onPlayerInventoryItemRemoved(Event_PlayerInventoryItemRemoved event) {}

    // method for when a player equips an item
    private void onPlayerInventoryItemEquip(Event_PlayerInventoryItemEquip event) {
//        alertPlayer("You equipped something");
    }

    // method for when a player unequips an item
    private void onPlayerInventoryItemUnEquip(Event_PlayerInventoryItemUnEquip event) {
//        alertPlayer("You unequipped something");
    }

    // register an item a player picked up to their discovered list
    private void registerDiscoveredItem(Ref<EntityStore> ref, Store<EntityStore> store, Item query) {
        try {
            Component_CraftingKnowledge craftingKnowledge = store.getComponent(ref, componentTypeCraftingKnowledge);
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (craftingKnowledge == null || playerRef == null) return;

            // Discover the item, then discover any new recipes
            boolean discoveredNew = craftingKnowledge.addDiscoveredItem(playerRef, query);
            if (discoveredNew) craftingKnowledge.discoverRecipes(ref, store, query);
        } catch (Exception e) {}
    }

    // assign a gear score to an item a player picked up
    private void swapVanillaItem(Ref<EntityStore> ref, Store<EntityStore> store, ItemStack stack, ItemContainer container, short slot) {
        // if in creative mode let it happen
//        Player player = store.getComponent(ref, Player.getComponentType());
//        if (player.getGameMode() == GameMode.Creative) return;

        // get the item from the stack
        Item item = stack.getItem();
        if (Arrays.asList(item.getCategories()).contains("Items.HyARPG.Gear")) return;

        // get the players crafting knowledge or remove the item and bail
        Component_CraftingKnowledge craftingKnowledge = store.getComponent(ref, componentTypeCraftingKnowledge);
        if (craftingKnowledge == null) {
            container.removeItemStackFromSlot(slot);
            return;
        }

        // get teh players known recipes, roll a rarity and then roll an item or empty container and bail if none returned
        Set<String> recipes = craftingKnowledge.discoveredDroppableRecipes;
        String rarityRoll = rollRarity();
        String randomItemId = rollItemForRarity(recipes, rarityRoll);
        if (randomItemId == null ) {
            container.removeItemStackFromSlot(slot);
            return;
        }

        // create a new item stack and swap it in
        ItemStack newStack = new ItemStack(randomItemId, 1);
        container.replaceItemStackInSlot(slot, stack, newStack);
    }

    // assign a gear score to an item a player picked up
    private ItemStack assignGearScoreAndAffixes(ItemStack stack, int gearScore) {
        // If it already has a gear score, bail
        if (stack.getFromMetadataOrNull("GearScore", Codec.INTEGER) != null) return null;

        // If we can't get an item id bail
        Item item = stack.getItem();
        String itemId = item.getId();
        if(itemId == null) return null;

        // create a new item stack
        ItemStack returnStack = new ItemStack(itemId, 1);

        // determine the item rarity from its id ex: Weapon_Copper_Sword_Uncommon
        String rarity = itemId.substring(itemId.lastIndexOf('_') + 1);

        // get affixes
        int affixCount = rarityToAffixMap.getAffixCount(rarity);
        List<Affix> affixes = new AffixPool().randomAffixes(affixCount);

        // loop over affixes
        List<String> affixStrings = new ArrayList<>();
        for (Affix affix : affixes) {
            String affixEncoded = affix.stat() + "|" + affix.value();
            affixStrings.add(affixEncoded);
        }
        returnStack = returnStack.withMetadata("affixes", Codec.STRING_ARRAY, affixStrings.toArray(new String[0]));

        // assign the gear store and replace the old stack with the new one
        returnStack = returnStack.withMetadata("GearScore", Codec.INTEGER, gearScore);

        // return the new item stack
        return returnStack;
    }

    // get valid attackers from the damage registry
    private List<Ref<EntityStore>> getAttackingPlayers(Ref<EntityStore> defender, Store<EntityStore> store) {
        ConcurrentHashMap<Ref<EntityStore>, Long> attackers = damageRegistry.get(defender);
        long cutoff = System.currentTimeMillis() - 30_000;
        List<Ref<EntityStore>> attackingRefs = new ArrayList<Ref<EntityStore>>();
        if(attackers == null) return Collections.emptyList();

        // loop over attackers
        attackers.forEach((attacker, timestamp) -> {
                // Skip stale or invalid entries
                if (timestamp < cutoff || !attacker.isValid()) return;

                // get the playerRef that dealt damage and check they are still valid
                PlayerRef playerRef = store.getComponent(attacker, PlayerRef.getComponentType());
                if (playerRef == null) return;

                // add ref to attacking refs
                attackingRefs.add(attacker);
            });

        // return the attacking refs
        return attackingRefs;
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
                Component_RPG_Player attackerRPGStats = store.getComponent(attacker, componentTypeRPGPlayer);
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
//        for (PlayerRef player : Universe.get().getPlayers()) {
//            player.sendMessage(Message.raw(
//                "Initial Damage: " + damage.getInitialAmount() +
//                " Final Damage: " + damage.getAmount()
//            ).color(Color.GRAY));
//            player.sendMessage(Message.raw(
//                "Level Delta: " + levelDelta +
//                " Rarity Delta: " + rarityDelta
//            ).color(Color.GRAY));
//            player.sendMessage(Message.raw(
//                "Level Scale: " + levelScale +
//                " Rarity Scale: " + rarityScale
//            ).color(Color.GRAY));
//        }
    }

    // function for spawning loot drops
    private void rollLoot(Ref<EntityStore> defender, Store<EntityStore> store, List<Ref<EntityStore>> players, List<ItemStack> dropPool) {
        // Get the killed enemies rpg component
        Component_RPG_Enemy rpgEnemy = store.getComponent(defender, componentTypeRPGEnemy);
        if(rpgEnemy == null) return;

        // get the enemies level and rarity
        String rarity = rpgEnemy.getRarityString();
        int level = rpgEnemy.level;

        // roll to see if gear or recipes should drop
        boolean shouldLootDrop = shouldLootDrop(rarity);
        boolean shouldRecipeDrop = shouldLootDrop(rarity);

        // loop over all players who damaged the defender in the last 30 seconds
        for (Ref<EntityStore> ref : players) {
            // resolve the player component
            Player player = store.getComponent(ref, Player.getComponentType());

            // add a recipe if applicable
            if(shouldRecipeDrop) {
                // roll recipe rarity and add it to the drop pool
                String recipeRarity = rollRarity();
                String recipeID = "Recipe_Page_" + recipeRarity;
                dropPool.add(new ItemStack(recipeID, 1));

                // notify players of the roll
                Color color = colorUtils.getRarityColor(recipeRarity);
                alertPlayers(player.getDisplayName() + " rolled a " + recipeID.replace("_", " "), color);
            }

            // bail now if loot should not drop
            if(!shouldLootDrop) continue;

            // get the player's crafting knowledge component or bail
            Component_CraftingKnowledge craftingKnowledge = store.getComponent(ref, componentTypeCraftingKnowledge);
            if (craftingKnowledge == null) continue;

            // bail if the player's recipes are empty
            Set<String> recipes = craftingKnowledge.discoveredDroppableRecipes;
            int recipeCount = recipes.size();
            if (recipeCount == 0) continue;

            // get the rarity the random item should be
            String rolledRarity = rollRarity();

            // get a random item based on the rarity rolled (falling down rarity order otherwise)
            String randomItemId = rollItemForRarity(recipes, rolledRarity);
            if (randomItemId == null) continue;

            // assign gear score based on enemy level and then add the item stack to the drop pool
            ItemStack newStack = new ItemStack(randomItemId, 1);
            newStack = assignGearScoreAndAffixes(newStack, level);
            dropPool.add(newStack);

            // resolve the player component for messaging and alert players of the roll
            if (player != null) {
                // get rarity of the item actually returned
                String[] parts = randomItemId.split("_");
                String actualRarity = parts[parts.length - 1];

                // format item name for display
                String itemName = randomItemId
                    .replace("Weapon_", "")
                    .replace("Armor_", "")
                    .replace("_", " ");

                // notify players of the roll
                Color color = colorUtils.getRarityColor(actualRarity);
                alertPlayers(player.getDisplayName() + " rolled a " + itemName, color);
            }
        }
    }

    // Returns true if the modifier application succeeds
    private boolean shouldLootDrop(String rarity) {
        // Defensive default (treat unknown rarity as worst case)
        if (rarity == null) return false;

        // make a random roll between 0 and 1
        double roll = ThreadLocalRandom.current().nextDouble(); // 0.0 <= roll < 1.0
        double failChance;

        switch (rarity) {
            case "Common":
                failChance = 0.95; // 95% fail → 5% success
                break;

            case "Uncommon":
                failChance = 0.85; // 85% fail → 15% success
                break;

            case "Rare":
                failChance = 0.65; // 65% fail → 35% success
                break;

            case "Epic":
                failChance = 0.40; // 40% fail → 60% success
                break;

            case "Legendary":
                failChance = 0.10; // 10% fail → 90% success
                break;

            default:
                failChance = 0.95; // Safe fallback
                break;
        }

        // Success occurs when roll exceeds failure probability
        return roll > failChance;
    }

    // Rolls a rarity tier based on fixed probabilities
    private String rollRarity() {
        double roll = ThreadLocalRandom.current().nextDouble(); // 0.0 <= roll < 1.0

        // Cumulative probability bands (must sum to 1.0)
        if (roll < 0.03) return "Legendary";   // 3%
        else if (roll < 0.10) return "Epic";   // 7% (0.10 total)
        else if (roll < 0.25) return "Rare";   // 15% (0.25 total)
        else return "Uncommon";                // 75% (0.70 total)
    }

    // Rolls a random item based on the passed rarity/recipes
    private String rollItemForRarity(Set<String> recipes, String rolledRarity) {
        // use ThreadLocalRandom for efficient RNG in game loops
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        // Define fallback order (highest → lowest)
        String[] rarityOrder = { "Legendary", "Epic", "Rare", "Uncommon", "Common" };

        // Find starting rarity order index based on rolled rarity
        int startIndex = -1;
        for (int i = 0; i < rarityOrder.length; i++) {
            if (rarityOrder[i].equals(rolledRarity)) {
                startIndex = i;
                break;
            }
        }
        if (startIndex == -1) return null;

        // loop over rarity order and try to get a random item or fall down the chain
        for (int r = startIndex; r < rarityOrder.length; r++) {
            String rarity = rarityOrder[r];

            // Collect matching recipes
            List<String> matches = new ArrayList<>();
            for (String id : recipes) {
                if (id.endsWith("_" + rarity)) {
                    matches.add(id);
                }
            }

            // If matches exist → roll one
            if (!matches.isEmpty()) {
                return matches.get(rng.nextInt(matches.size()));
            }
        }

        // Should never occur if Common recipes exist
        return null;
    }

    // function to calculate stats on tick
    private void onTickCalculateStats() {
//        // cache the last known state
//        ItemStack lastMainHand = null;
//        ItemStack[] lastArmor = new ItemStack[armorCapacity];
//
//        // in your ticking system
//        ItemStack currentMainHand = player.getInventory().getHotbar().getItemStackForSlot(activeSlot);
//        ItemStack[] currentArmor = // get all armor slots
//
//        boolean changed = !ItemStack.equals(currentMainHand, lastMainHand);
//        if (!changed) {
//            for (int i = 0; i < currentArmor.length; i++) {
//                if (!ItemStack.equals(currentArmor[i], lastArmor[i])) {
//                    changed = true;
//                    break;
//                }
//            }
//        }
//
//        if (changed) {
//            recalculateGearScore();
//            lastMainHand = currentMainHand;
//            lastArmor = currentArmor;
//        }
    }

    // helper function for console logging
    public void alertPlayers(String msg, Color color) {
        color = color != null ? color : Color.WHITE;
        // loop over all players and broadcast the message
        for (PlayerRef player : Universe.get().getPlayers()) {
            player.sendMessage(Message.raw(msg).color(color));
        }
    }
}
