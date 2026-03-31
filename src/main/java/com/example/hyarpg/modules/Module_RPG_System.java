package com.example.hyarpg.modules;

// Hytale Imports
import com.example.hyarpg.utils.affixes.ImplicitAffixPool;
import com.hypixel.hytale.builtin.adventure.objectives.config.triggercondition.ObjectiveLocationTriggerCondition;
import com.hypixel.hytale.builtin.hytalegenerator.scanners.deprecated.AreaScanner;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.component.spatial.SpatialStructure;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.protocol.PlaceBlockInteraction;
import com.hypixel.hytale.protocol.packets.player.SetBlockPlacementOverride;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockPlacementSettings;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.container.ChoiceItemDropContainer;
import com.hypixel.hytale.server.core.asset.type.item.config.container.ItemDropContainer;
import com.hypixel.hytale.server.core.asset.type.item.config.container.MultipleItemDropContainer;
import com.hypixel.hytale.server.core.asset.type.item.config.container.SingleItemDropContainer;
import com.hypixel.hytale.server.core.entity.Frozen;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.windows.*;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.inventory.InventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.block.BlockModule.BlockStateInfo;
import com.hypixel.hytale.server.core.modules.block.system.ItemContainerSystems;
import com.hypixel.hytale.server.core.modules.blockhealth.BlockHealthModule;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.system.PlayerSpatialSystem;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.modules.item.ItemModule;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;

// Mod Imports
import com.example.hyarpg.components.Component_CraftingKnowledge;
import com.example.hyarpg.events.*;
import com.example.hyarpg.HyARPGPlugin;
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.components.Component_RPG_Enemy;
import com.example.hyarpg.interactions.*;
import com.example.hyarpg.utils.affixes.Affix;
import com.example.hyarpg.utils.affixes.AffixPool;
import com.example.hyarpg.utils.affixes.EntityStats;
import com.example.hyarpg.utils.combat.SwingDamageGroup;
import com.example.hyarpg.configs.ModConfig;
import com.example.hyarpg.utils.skills.SkillLibrary;
import com.example.hyarpg.utils.skills.SkillLibraryMigration;

// Java Imports
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

import com.hypixel.hytale.server.worldgen.loader.prefab.BlockPlacementMaskRegistry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Module_RPG_System {

    private final HyARPGPlugin plugin;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // Component Type references
    public static ComponentType<EntityStore, Component_RPG_Player> componentTypeRPGPlayer;
    public static ComponentType<EntityStore, Component_RPG_Enemy> componentTypeRPGEnemy;
    public static ComponentType<EntityStore, Component_CraftingKnowledge> componentTypeCraftingKnowledge;

    // properties that control enemy level as they get further from spawn
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

    // Skill Tree Version Constant
    private final String SKILL_TREE_VERSION = "1.4.0"; // 1.4.0

    // Create a map for damage message colors
    public static final Map<String, Color> DAMAGE_COLORS = Map.of(
        "Fire", new Color(249, 146, 32),
        "Ice", new Color(219, 241, 253),
        "Lightning", new Color(0, 92, 165),
        "Poison",  new Color(0, 255, 0),
        "Magic", new Color(242, 89, 255),
        "Physical",  new Color(175, 175, 175)
    );

    // create some maps to use for filtering
    private static final Set<String> MOD_DAMAGE_TYPES = Set.of(
        "Physical", "Magic", "Poison", "Fire", "Ice", "Lightning"
    );

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

    // Map<defender_ref, Map<attacker_ref, timestamp>, damage registry to track when a player damages an enemy for xp/loot awards
    private final ConcurrentHashMap<Ref<EntityStore>, ConcurrentHashMap<Ref<EntityStore>, Long>> damageRegistry = new ConcurrentHashMap<>();

    // Properties to pool damage ticks that happen in quick succession from the same source
    private final ConcurrentHashMap<String, SwingDamageGroup> swingGroups = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final long BUCKET_MS = 10;
    private static final long FLUSH_DELAY_MS = 5;
    private String swingKey(Ref<EntityStore> attacker, Ref<EntityStore> defender) {
        return attacker + "->" + defender + "@" + (System.currentTimeMillis() / BUCKET_MS);
    }

    // Used for weapon damage swap
    private static class ResolvedDamage {
        final DamageCause cause;
        final float amount;
        ResolvedDamage(DamageCause cause, float amount) {
            this.cause = cause;
            this.amount = amount;
        }
    }

    // initialize this module
    public Module_RPG_System(HyARPGPlugin plugin) {
        this.plugin = plugin;

        // Register the component type using EntityStoreRegistry
        componentTypeRPGPlayer = plugin.getEntityStoreRegistry().registerComponent(Component_RPG_Player.class, "RPGStatsComponent", Component_RPG_Player.CODEC);
        componentTypeRPGEnemy = plugin.getEntityStoreRegistry().registerComponent(Component_RPG_Enemy.class, "RPGEnemyComponent", Component_RPG_Enemy.CODEC);
        componentTypeCraftingKnowledge = plugin.getEntityStoreRegistry().registerComponent(Component_CraftingKnowledge.class, "CraftingKnowledgeComponent", Component_CraftingKnowledge.CODEC);

        // Get the interaction registry and register the custom interactions
        final var interactionRegistry = plugin.getCodecRegistry(Interaction.CODEC);
        interactionRegistry.register("LearnRandomGearRecipe_Uncommon", Interaction_LearnRandomGearRecipe_Uncommon.class, Interaction_LearnRandomGearRecipe_Uncommon.CODEC);
        interactionRegistry.register("LearnRandomGearRecipe_Rare", Interaction_LearnRandomGearRecipe_Rare.class, Interaction_LearnRandomGearRecipe_Rare.CODEC);
        interactionRegistry.register("LearnRandomGearRecipe_Epic", Interaction_LearnRandomGearRecipe_Epic.class, Interaction_LearnRandomGearRecipe_Epic.CODEC);
        interactionRegistry.register("LearnRandomGearRecipe_Legendary", Interaction_LearnRandomGearRecipe_Legendary.class, Interaction_LearnRandomGearRecipe_Legendary.CODEC);
        interactionRegistry.register("ShowRPGStats", Interaction_ShowRPGStats.class, Interaction_ShowRPGStats.CODEC);
        interactionRegistry.register("ChangeItemState", Interaction_ChangeItemStateInteraction.class, Interaction_ChangeItemStateInteraction.CODEC);
        interactionRegistry.register("Use_Ability_1", Interaction_UseAbility1.class, Interaction_UseAbility1.CODEC);
        interactionRegistry.register("Use_Ability_2", Interaction_UseAbility2.class, Interaction_UseAbility2.CODEC);
        interactionRegistry.register("Use_Ability_3", Interaction_UseAbility3.class, Interaction_UseAbility3.CODEC);
        interactionRegistry.register("Beam_Particle", Interaction_BeamParticle.class, Interaction_BeamParticle.CODEC);
        interactionRegistry.register("Beam_Particle", Interaction_BeamParticle.class, Interaction_BeamParticle.CODEC);
        interactionRegistry.register("SpawnDeployableAtHitLocationFixed", Interaction_SpawnDeployableAtHitLocation.class, Interaction_SpawnDeployableAtHitLocation.CODEC);

        // Listen to applicable events on the mods internal event bus
        ModEventBus.register(Event_EntityPreDamaged.class, this::onEntityPreDamage);
        ModEventBus.register(Event_NPCDeath.class, this::onEnemyKilled);
        ModEventBus.register(Event_NPCSpawn.class, this::onNPCSpawn);
        ModEventBus.register(Event_NPCPreSpawn.class, this::onNPCPreSpawn);
        ModEventBus.register(Event_PlayerInventoryItemAdded.class, this::onPlayerInventoryItemAdded);
        ModEventBus.register(Event_PlayerInventoryItemRemoved.class, this::onPlayerInventoryItemRemoved);
        ModEventBus.register(Event_PlayerInventoryItemEquip.class, this::onPlayerInventoryItemEquip);
        ModEventBus.register(Event_PlayerInventoryItemUnEquip.class, this::onPlayerInventoryItemUnEquip);
        ModEventBus.register(Event_PlayerInteraction.class, this::onPlayerInteraction);
        ModEventBus.register(Event_PlayerReady.class, this::onPlayerReady);
        ModEventBus.register(Event_ContainerSpawned.class, this::onContainerSpawned);
        ModEventBus.register(Event_PlaceBlock.class, this::onPlaceBlock);
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

        // ensure the player components exists, add them if they don't
        Component_RPG_Player rpgPlayer = store.ensureAndGetComponent(entityRef, componentTypeRPGPlayer);
        store.ensureAndGetComponent(entityRef, componentTypeCraftingKnowledge);

        // load the latest skill library and migrate the player if needed
        SkillLibrary currentLibrary = new SkillLibrary(SKILL_TREE_VERSION); // fresh instance with latest trees
        SkillLibraryMigration.migrate(rpgPlayer, currentLibrary);

        // update the skill library state based on current allocations
        rpgPlayer.skillLibrary.recalculate();

        // refresh gear score
        rpgPlayer.calculateGearScore(entityRef, store);
        rpgPlayer.calculateAffixStats(player.getReference(), store);
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

        // prep a list of affixes for the enemy
        List<Affix> affixes = new ArrayList<>();

        // add critical strike chance affix no matter what
        Affix affixCritChance = AffixPool.getAffixByStatName("Stat_Increased_Critical_Strike_Chance");
        affixCritChance.rollTier(rpgEnemy.level);
        affixes.add(affixCritChance);

        // if monster is at least rarity 1 get a damage stat also
        if(rpgEnemy.monsterRarity >= 1) {
            Affix affixFlatDamage = AffixPool.randomFlatDamageAffix();
            affixFlatDamage.rollTier(rpgEnemy.level);
            affixes.add(affixFlatDamage);
        };

        // if monster is at least rarity 2 get a resistance stat also
        if(rpgEnemy.monsterRarity >= 2) {
            Affix affixFlatDamage = AffixPool.randomResistanceAffix();
            affixFlatDamage.rollTier(rpgEnemy.level);
            affixes.add(affixFlatDamage);
        };

        // if monster is at least rarity 3 increase their crit damage
        if(rpgEnemy.monsterRarity >= 3) {
            Affix affixCritDamage = AffixPool.getAffixByStatName("Stat_Increased_Critical_Strike_Damage");
            affixCritDamage.rollTier(rpgEnemy.level);
            affixes.add(affixCritDamage);
        };

        // apply the affixes to the NPC
        rpgEnemy.applyAffixes(affixes.toArray(new Affix[0]));
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

        // Get the entity's role name and create or clear the nameplate depending on config settings
        if (ModConfig.get().enemies.clear_enemy_nameplates) {
            // Nameplate is what actually shows above the head
            Nameplate nameplate = store.getComponent(ref, Nameplate.getComponentType());
            if (nameplate != null) nameplate.setText("");
            else commandBuffer.addComponent(ref, Nameplate.getComponentType(), new Nameplate());
        }
        else if (ModConfig.get().enemies.show_enemy_nameplates) {
            String roleName = npcEntity.getRoleName().replace("_", " ");
            String nameplateText = rarityString + roleName + " (Lv. " + level + ")";

            // Nameplate is what actually shows above the head
            Nameplate nameplate = store.getComponent(ref, Nameplate.getComponentType());
            if (nameplate != null) nameplate.setText(nameplateText);
            else commandBuffer.addComponent(ref, Nameplate.getComponentType(), new Nameplate(nameplateText));
        }

        // Add rarity effect if applicable
        if (rpgEnemy.monsterRarity > 0) {
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

    // This function fires right before an entity takes damage
    private void onEntityPreDamage(Event_EntityPreDamaged event) {
        Ref<EntityStore> attacker = event.getAttacker();
        Ref<EntityStore> defender = event.getDefender();
        Store<EntityStore> store = event.getStore();
        Damage damage = event.getDamage();

        // get the damage type
        int causeIndex = damage.getDamageCauseIndex();
        DamageCause cause = DamageCause.getAssetMap().getAsset(causeIndex);

        // handle fall damage
        if(Objects.equals(cause.getId(), "Fall")) {
            Component_RPG_Player defenderRPGStats = store.getComponent(defender, componentTypeRPGPlayer);
            if(defenderRPGStats == null) return;

            // Get fall resist multiplier
            float fallResist = defenderRPGStats.stats.getResistance("Fall");
            float multiplier = 1f - (Math.max(0f, Math.min(100f, fallResist)) / 100f);

            // adjust the damage amount
            damage.setAmount(damage.getAmount() * multiplier);
            return;
        }

        // if the damage type is command or null bail
        if (attacker == null || cause == null || Objects.equals(cause.getId(), "Command")) return;

        // check that at least one party was a player, otherwise bail
        Component_RPG_Player attackerRPGStats = store.getComponent(attacker, componentTypeRPGPlayer);
        Component_RPG_Player defenderRPGStats = store.getComponent(defender, componentTypeRPGPlayer);
        if(attackerRPGStats == null && defenderRPGStats == null) return;

        // if damage type is "Weapon", resolve the real damage type from the attacker's main hand
        float damageAmount = damage.getInitialAmount();
        if (Objects.equals(cause.getId(), "Weapon")) {
            ResolvedDamage resolved = resolveWeaponDamageType(attacker, store, cause, damageAmount, damage);
            cause = resolved.cause;
            damageAmount = resolved.amount;
        }

        // check if the damage cause has a parent (if it does, it was a weapon type)
        String weaponType;
        if (cause.getInherits() != null) weaponType = cause.getId();
        else weaponType = null;

        // if the damage type is not recognized by the mod (or is a weapon type) just set it to physical
        if (!MOD_DAMAGE_TYPES.contains(cause.getId())) {
            cause = DamageCause.getAssetMap().getAsset("Physical");
            damage.setDamageCauseIndex(DamageCause.getAssetMap().getIndex("Physical"));
        }

        // Intercept and buffer this packet into its swing group
        String key = swingKey(attacker, defender);
        boolean blocked = Boolean.TRUE.equals(damage.getMetaStore().getMetaObject(Damage.BLOCKED));
        boolean isProjectile = damage.getSource() instanceof Damage.ProjectileSource;
        SwingDamageGroup group = swingGroups.computeIfAbsent(key, k -> {
            SwingDamageGroup g = new SwingDamageGroup(attacker, defender, blocked, isProjectile, weaponType);
            scheduler.schedule(() -> {
                SwingDamageGroup pending = swingGroups.get(key);
                if (pending != null) pending.readyToApply = true;
            }, FLUSH_DELAY_MS, TimeUnit.MILLISECONDS);
            return g;
        });
        group.add(cause, damageAmount);

        // Zero out the packet — real damage applied during consumeSwingGroup
        damage.setAmount(0.001f);
    }

    // Consume grouped damage into a single hit and apply it to the target
    private void applyDamageGroup(SwingDamageGroup damageGroup) {
        if (damageGroup == null) return;

        // get event properties
        Ref<EntityStore> attacker = damageGroup.attacker;
        Ref<EntityStore> defender = damageGroup.defender;
        Store<EntityStore> store = attacker.getStore();

        // get attacker components
        Component_RPG_Player attackerRPGStats = store.getComponent(attacker, componentTypeRPGPlayer);
        Component_RPG_Enemy attackerRPGEnemy = store.getComponent(attacker, componentTypeRPGEnemy);

        // default attacker values
        int attackerLevel = 1;
        int attackerRarity = 0;
        String attackerName = "Entity";
        EntityStats attackerStats = new EntityStats();

        // determine attacker values
        if (attackerRPGStats != null) {
            attackerLevel = attackerRPGStats.gearScore;
            attackerStats = attackerRPGStats.stats;
            attackerName = store.getComponent(attacker, Player.getComponentType()).getDisplayName();
        }
        else if(attackerRPGEnemy != null) {
            attackerLevel = attackerRPGEnemy.level;
            attackerRarity = attackerRPGEnemy.monsterRarity;
            attackerStats = attackerRPGEnemy.stats;
            attackerName = store.getComponent(attacker, NPCEntity.getComponentType()).getRoleName().replace("_", " ");
        };

        // check if the defender is a player or NPC
        Component_RPG_Player defenderRPGStats = store.getComponent(defender, componentTypeRPGPlayer);
        Component_RPG_Enemy defenderRPGEnemy = store.getComponent(defender, componentTypeRPGEnemy);

        // default defender values
        int defenderLevel = 1;
        int defenderRarity = 0;
        boolean defenderUsingShield = false;
        EntityStatMap defenderStatMap = null;
        String defenderName = "Entity";
        EntityStats defenderStats = new EntityStats();

        // determine defender values
        if(defenderRPGStats != null) {
            defenderLevel = defenderRPGStats.gearScore;
            defenderStats = defenderRPGStats.stats;
            defenderName = store.getComponent(defender, Player.getComponentType()).getDisplayName();

            // defender is a player, set some things
            defenderStatMap = store.getComponent(defender, EntityStatsModule.get().getEntityStatMapComponentType());

            // player had a utility item equipped, check if it was a shield
            if(defenderRPGStats.offHandItem != null && defenderRPGStats.offHandItem.getItemId().contains("Weapon_Shield"))
                defenderUsingShield = true;
        }
        else if(defenderRPGEnemy != null) {
            defenderLevel = defenderRPGEnemy.level;
            defenderRarity = defenderRPGEnemy.monsterRarity;
            defenderStats = defenderRPGEnemy.stats;
            defenderName = store.getComponent(defender, NPCEntity.getComponentType()).getRoleName().replace("_", " ");
        };

        // if attacker is a player and defender is an enemy register the damage for XP awarding
        if(attackerRPGStats != null && defenderRPGEnemy != null)
            damageRegistry.computeIfAbsent(defender, k -> new ConcurrentHashMap<>()).put(attacker, System.currentTimeMillis());

        // apply added flat damage
        if (attackerStats.getFlatDamage("Fire") > 0)
            damageGroup.add(DamageCause.getAssetMap().getAsset("Fire"), attackerStats.getFlatDamage("Fire"));
        if (attackerStats.getFlatDamage("Ice") > 0)
            damageGroup.add(DamageCause.getAssetMap().getAsset("Ice"), attackerStats.getFlatDamage("Ice"));
        if (attackerStats.getFlatDamage("Lightning") > 0)
            damageGroup.add(DamageCause.getAssetMap().getAsset("Lightning"), attackerStats.getFlatDamage("Lightning"));
        if (attackerStats.getFlatDamage("Physical") > 0)
            damageGroup.add(DamageCause.getAssetMap().getAsset("Physical"), attackerStats.getFlatDamage("Physical"));
        if (attackerStats.getFlatDamage("Elemental") > 0)
            damageGroup.add(DamageCause.getAssetMap().getAsset("Elemental"), attackerStats.getFlatDamage("Elemental"));
        if (attackerStats.getFlatDamage("Magic") > 0)
            damageGroup.add(DamageCause.getAssetMap().getAsset("Magic"), attackerStats.getFlatDamage("Magic"));
        if (attackerStats.getFlatDamage("Poison") > 0)
            damageGroup.add(DamageCause.getAssetMap().getAsset("Poison"), attackerStats.getFlatDamage("Poison"));

        // roll for crit which applies to all packets
        float critChance = attackerStats.getCriticalStrikeChance();
        float critRoll = (float) (Math.random() * 100.0) ;
        boolean crit = critRoll < critChance;

        // loop over the damage packets for each type and adjust them accordingly
        double finalDamage = 0;
        for (var entry : damageGroup.packets()) {
            // get the damage cause and total value
            DamageCause cause = entry.getKey();
            double totalAmount = entry.getValue();

            // adjust based on config settings for damage multipliers
            if(attackerRPGStats != null) totalAmount *= ModConfig.get().combat.damage_from_player_multiplier;
            if(defenderRPGStats != null) totalAmount *= ModConfig.get().combat.damage_to_player_multiplier;

            // adjust damage based on player/enemy level
            totalAmount = adjustDamageBasedOnLevel(attackerLevel, attackerRarity, defenderLevel, defenderRarity, totalAmount);

            // apply increased damages
            totalAmount += totalAmount * (attackerStats.getIncreasedDamage(cause.getId()) / 100);

            // apply increased weapon damage if applicable
            if (damageGroup.weaponType != null) totalAmount += totalAmount * (attackerStats.getIncreasedDamage(damageGroup.weaponType) / 100);

            // adjust the damage based on crit
            if (crit) totalAmount *= attackerStats.getCriticalStrikeDamage();

            // reduce the damage based on the defenders resistance stat
            totalAmount -= totalAmount * (defenderStats.getResistance(cause.getId()) / 100);

            // total this damage into the final amount
            finalDamage += totalAmount;

            // don't process the message if no damage was dealt
            if(totalAmount <= 0) continue;

            // ping damage packet to chat for combat analysis
            if(ModConfig.get().combat.broadcast_combat_logs_in_chat && (attackerRPGStats != null || defenderRPGStats != null)) {
                PlayerRef playerRef = null;

                // try to get a player ref
                if (attackerRPGStats != null && attackerRPGStats.showCombatText)
                    playerRef = store.getComponent(attacker, PlayerRef.getComponentType());
                else if (defenderRPGStats != null && defenderRPGStats.showCombatText)
                    playerRef = store.getComponent(defender, PlayerRef.getComponentType());

                // if we found a player ref and they have the setting enabled, broadcast the message
                if (playerRef != null) {
                    float truncated = (float) (Math.floor(totalAmount * 10.0) / 10.0);
                    playerRef.sendMessage(Message.join(
                        Message.raw(attackerName).color("#ffffff").bold(true),
                        Message.raw(" dealt ").color(Color.GRAY),
                        Message.raw(truncated + " (" + cause.getId() + ")").color(DAMAGE_COLORS.getOrDefault(cause.getId(), Color.WHITE)).bold(true),
                        Message.raw(" damage to ").color(Color.GRAY),
                        Message.raw(defenderName).color("#ffffff").bold(true))
                    );
                }
            }
        }

        // check if the conditions were met to try to parry
        if (!damageGroup.isProjectile && damageGroup.blocked && defenderRPGStats != null) {
            try {
                // check if was within timing window
                long blockStart = defenderRPGStats.blockStart;
                long parryWindowModified = ModConfig.get().combat.base_parry_window_in_seconds + (long) (defenderStats.getParryWindow() * 1_000_000_000L);

                if(System.nanoTime() - blockStart <= parryWindowModified) {
                    // set the damage threshold for parrying based on stability
                    double damageThreshold = finalDamage * (1f - (defenderStats.getStabilityPercent(defenderUsingShield) / 100));

                    // get the stat map component from the player
                    ComponentType<EntityStore, EntityStatMap> statMapType = EntityStatsModule.get().getEntityStatMapComponentType();
                    EntityStatMap statMap = store.getComponent(defender, statMapType);

                    // Get the stamina stat from the stat map
                    int staminaIndex = DefaultEntityStatTypes.getStamina();

                    // get the stats by index
                    EntityStatValue staminaStat = statMap.get(staminaIndex);

                    // apply the stun and knockback to NPCs
                    if (damageThreshold <= staminaStat.get() && attackerRPGEnemy != null) {
                        // get the effect controller for the attacker to set stun effect
                        EffectControllerComponent effectController = store.getComponent(attacker, EffectControllerComponent.getComponentType());
                        EntityEffect effect = EntityEffect.getAssetMap().getAsset("ParryStun");
                        effectController.addEffect(attacker, effect, 2, OverlapBehavior.OVERWRITE, store);

                        // reduce the damage to 0
                        finalDamage = 0;

                        // freeze the entity and set a scheduler to remove the freeze
                        World world = attacker.getStore().getExternalData().getWorld();
                        world.execute(() -> {
                            // stun via frozen component
                            store.addComponent(attacker, Frozen.getComponentType(), Frozen.get());

                            Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                                world.execute(() -> {
                                    if (attacker.isValid()) {
                                        store.removeComponent(attacker, Frozen.getComponentType());
                                    }
                                });
                            }, (long)(3 * 1000), TimeUnit.MILLISECONDS);
                        });
                    }
                }
            } catch (Exception e) {}
        }

        // now handle blocking
        if(damageGroup.blocked) {
            // set the damage threshold for blocking based on stability
            double damageThreshold = finalDamage * (1f - (defenderStats.getStabilityPercent(defenderUsingShield) / 100));

            // get the stat map component from the player
            ComponentType<EntityStore, EntityStatMap> statMapType = EntityStatsModule.get().getEntityStatMapComponentType();
            EntityStatMap statMap = store.getComponent(defender, statMapType);

            // Get the stamina stat from the stat map
            int staminaIndex = DefaultEntityStatTypes.getStamina();

            // get the defenders current stamina
            EntityStatValue staminaStat = statMap.get(staminaIndex);
            float staminaValue = staminaStat.get();

            // subtract the stamina from the damage
            double remainingDmg = damageThreshold - staminaValue;
            double remainingStamina = staminaValue - damageThreshold;

            // set stamina to the new value ceiled to 0
            statMap.setStatValue(staminaIndex, Math.max(0, (float) remainingStamina));

            // set remaining amount of damage ceiled to 0
            finalDamage = Math.max(0, (float) remainingDmg);
        }

        // now handle barrier which absorbs damage after parry/block, before dodge
        if(defenderStatMap != null && finalDamage > 0) {
            int barrierOnBlockStatIndex = EntityStatType.getAssetMap().getIndex("BarrierOnBlock");
            EntityStatValue barrierOnBlockStat = defenderStatMap.get(barrierOnBlockStatIndex);

            if (barrierOnBlockStat != null) {
                double barrier = barrierOnBlockStat.get();

                if (barrier > 0) {
                    double absorbed = Math.min(barrier, finalDamage);

                    finalDamage -= absorbed;
                    barrier -= absorbed;

                    defenderStatMap.setStatValue(barrierOnBlockStatIndex, (float) barrier);
                }
            }
        }

        // now handle dodging
        if(finalDamage > 0f && defenderStats.getDodgeChance() > 0f) {
            // get teh players dodge chance and roll a random number between 0 and 100
            float dodgeChance = defenderStats.getDodgeChance();
            float dodgeRoll = (float) (Math.random() * 100.0) ;

            // if the roll is under the dodge chance the player dodged so negate the damage
            if (dodgeRoll < dodgeChance) {
                // successful dodge
                finalDamage = 0f;
            }
        }

        // apply the final damage to the player
        DamageSystems.executeDamage(defender, store,
            new Damage(
                new Damage.EntitySource(attacker),
                DamageCause.getAssetMap().getAsset("Command"),
                (float) finalDamage
            )
        );
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

        // filter out vanilla weapons and armor and ingots
        List<ItemStack> filteredDrops = new ObjectArrayList();
        for (ItemStack drop : drops) {
            Item item = drop.getItem();
            if (item.getWeapon() != null || item.getArmor() != null || item.getId().contains("Ingredient_Bar") || item.getId().contains("Ore_") || item.getId().contains("Weapon_") || item.getId().contains("Armor_")) {
//                alertPlayers("Filtering out " + item.getId(), Color.DARK_GRAY);
                continue;
            };
//            alertPlayers("Not Filtering out " + item.getId(), Color.DARK_GRAY);
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
        InventoryChangeEvent changeEvent = event.getChangeEvent();
        ItemContainer container = changeEvent.getItemContainer();
        alertPlayers("I am here", Color.YELLOW);

        // gear score only for weapons/armor
        String[] categories = item.getCategories();
        if (categories != null && Arrays.asList(categories).contains("Items.HyARPG.Gear")) {
            // get the players level
            Component_RPG_Player rpgPlayer = store.getComponent(ref, componentTypeRPGPlayer);
            int level = rpgPlayer == null ? 1 : rpgPlayer.level;
            alertPlayers("I am here 2", Color.YELLOW);

            // assign a gear score to the item
            ItemStack newStack = assignGearScoreAndAffixes(stack, level);
            if (newStack == null || newStack.isEmpty()) return;
            alertPlayers("I am here 3", Color.YELLOW);

            // swap out the old stack for the new stack, then update reference for down stream
            container.replaceItemStackInSlot(slot, stack, newStack);
            stack = newStack;

            // refresh gear score
            rpgPlayer.calculateGearScore(ref, store);
            rpgPlayer.calculateAffixStats(ref, store);
            alertPlayers("I am here 4", Color.YELLOW);
        }

        // register discovery for ALL items
        registerDiscoveredItem(ref, store, stack.getItem());
    }

    // capture when an item is removed from a players inventory
    private void onPlayerInventoryItemRemoved(Event_PlayerInventoryItemRemoved event) {}

    // capture when a container spawns
    private void onContainerSpawned(Event_ContainerSpawned event) {
        ItemContainerBlock containerBlock = event.containerBlock();
        BlockStateInfo blockStateInfo = event.blockStateInfo();

        // getIndex() returns indexBlockInColumn — full column-relative coords
        int index = blockStateInfo.getIndex();
        int localX = ChunkUtil.xFromBlockInColumn(index);
        int localY = ChunkUtil.yFromBlockInColumn(index); // full Y within column, not section-local
        int localZ = ChunkUtil.zFromBlockInColumn(index);

        // Get chunk X/Z from the WorldChunk component on the chunk ref
        Ref<ChunkStore> chunkRef = blockStateInfo.getChunkRef();
        Store<ChunkStore> store = chunkRef.getStore();
        WorldChunk worldChunk = (WorldChunk) store.getComponent(chunkRef, WorldChunk.getComponentType());
        int chunkX = worldChunk.getX();
        int chunkZ = worldChunk.getZ();

        // Reconstruct world coordinates — no sectionY needed, localY is already full column Y
        int worldX = ChunkUtil.worldCoordFromLocalCoord(chunkX, localX);
        int worldY = localY; // Y is absolute within the column (MIN_Y = 0)
        int worldZ = ChunkUtil.worldCoordFromLocalCoord(chunkZ, localZ);

        // Compute distance from config origin
        double dx = worldX - ModConfig.get().world.origin_spawn_point_x;
        double dy = worldY - ModConfig.get().world.origin_spawn_point_y;
        double dz = worldZ - ModConfig.get().world.origin_spawn_point_z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Determine tier based on ore spawn distance ranges
        int tier = 1;
        if (distance >= ModConfig.get().world.min_distance_for_mithril_spawn) tier = 5;
        else if (distance >= ModConfig.get().world.min_distance_for_adamantite_spawn) tier = 4;
        else if (distance >= ModConfig.get().world.min_distance_for_thorium_spawn) tier = 3;
        else if (distance >= ModConfig.get().world.min_distance_for_iron_spawn) tier = 2;

        containerBlock.setDroplist("HyARPG_Container_Tier" + tier);
    }

    // method for when a player equips an item
    private void onPlayerInventoryItemEquip(Event_PlayerInventoryItemEquip event) {
        // get entity ref and entity store
        Ref<EntityStore> ref = event.getRef();
        Store<EntityStore> store = event.getStore();

        //get the rpg player comp and player comp
        Player player = store.getComponent(ref, Player.getComponentType());
        Component_RPG_Player rpgPlayer = store.getComponent(ref, componentTypeRPGPlayer);
        if(rpgPlayer == null || player == null) return;

        // refresh gear score
        rpgPlayer.calculateGearScore(ref, store);
        rpgPlayer.calculateAffixStats(ref, store);
    }

    // method for when a player unequips an item
    private void onPlayerInventoryItemUnEquip(Event_PlayerInventoryItemUnEquip event) {
        // get entity ref and entity store
        Ref<EntityStore> ref = event.getRef();
        Store<EntityStore> store = event.getStore();

        //get the rpg player comp and player comp
        Player player = store.getComponent(ref, Player.getComponentType());
        Component_RPG_Player rpgPlayer = store.getComponent(ref, componentTypeRPGPlayer);
        if(rpgPlayer == null || player == null) return;

        // refresh gear score and affix stats
        rpgPlayer.calculateGearScore(ref, store);
        rpgPlayer.calculateAffixStats(ref, store);
    }

    // method for when the player performs an interaction
    private void onPlayerInteraction(Event_PlayerInteraction event){
        // get ref and store reference
        Ref<EntityStore> ref = event.getRef();
        Store<EntityStore> store = event.getStore();

        // validate the ref has an rpg player comp
        Component_RPG_Player rpgPlayer = store.getComponent(ref, componentTypeRPGPlayer);
        if(rpgPlayer == null) return;

        // if secondary action possibly blocking so timestamp it (we will check for this in damage pipeline if there is blocked damage)
        if(Objects.equals(event.getInteractionID(), "Secondary")) {
            // set the block start
            rpgPlayer.blockStart = System.nanoTime();
        }
    }

    // method for when a player tries to place a block
    private void onPlaceBlock(Event_PlaceBlock event) {
       try {
           PlaceBlockEvent origEvent = event.event();

           ItemStack stack = origEvent.getItemInHand();
           Item item = stack.getItem();
           String[] categories = item.getCategories();

//           if (Arrays.asList(categories).contains("Furniture.Benches"))
//               origEvent.setCancelled(true);
       } catch (Exception e) {
           HytaleLogger.getLogger().at(Level.WARNING).log("onPlaceBlock interception failed: %s", e.getMessage());
       }
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
    private ItemStack assignGearScoreAndAffixes(ItemStack stack, int gearScore) {
        // If it already has a gear score, bail
        if (stack.getFromMetadataOrNull("GearScore", Codec.INTEGER) != null) return null;

        // If we can't get an item id bail
        Item item = stack.getItem();
        String itemId = item.getId();
        if(itemId == null) return null;

        // determine the item rarity and type from its id ex: Weapon_Sword_Copper_Uncommon -> Uncommon / Weapon_Sword
        String rarity = itemId.substring(itemId.lastIndexOf('_') + 1);
        String itemType = itemId.substring(0, itemId.indexOf('_', itemId.indexOf('_') + 1));
        int itemLevel = item.getItemLevel();
        int itemTier = Math.clamp(6 - (itemLevel / 10), 1, 5);

        // String fix for pure armors
        if(itemType.contains("Armor") && !itemType.contains("Cloth") && !itemType.contains("Leather")) itemType = "Armor";

        // apply implicits
        List<Affix> implicits = ImplicitAffixPool.getImplicits(itemType, itemTier);
        List<String> implicitStrings = new ArrayList<>();
        for (Affix implicit : implicits) {
            String implicitEncoded = implicit.stat() + "|" + implicit.value() + "|" + implicit.display();
            implicitStrings.add(implicitEncoded);
        }
        stack = stack.withMetadata("implicits", Codec.STRING_ARRAY, implicitStrings.toArray(new String[0]));

        // get affixes
        int affixCount = rarityToAffixMap.getAffixCount(rarity);
        List<Affix> affixes = new AffixPool().randomAffixes(affixCount);

        // loop over affixes
        List<String> affixStrings = new ArrayList<>();
        for (Affix affix : affixes) {
            // roll the affix tier which also adjusts the value
            affix.rollTier(gearScore);

            // convert the affix to a string value
            String affixEncoded = affix.stat() + "|" + affix.value() + "|" + affix.tier();
            affixStrings.add(affixEncoded);
        }
        stack = stack.withMetadata("affixes", Codec.STRING_ARRAY, affixStrings.toArray(new String[0]));

        // assign the gear store and replace the old stack with the new one
        stack = stack.withMetadata("GearScore", Codec.INTEGER, gearScore);

        // return the new item stack
        return stack;
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

        // Get the configured origin point
        double originX = ModConfig.get().world.origin_spawn_point_x;
        double originY = ModConfig.get().world.origin_spawn_point_y;
        double originZ = ModConfig.get().world.origin_spawn_point_z;

        // Calculate deltas from origin
        double dx = position.x - originX;
        double dy = position.y - originY;
        double dz = position.z - originZ;

        // Weight the y delta so things get stronger faster going down than they do going up
        double weightedDy = dy < 0 ? dy * 1.5 : dy;

        // 3D straight line distance from origin
        double distance = Math.sqrt(dx * dx + weightedDy * weightedDy + dz * dz);

        // get level based on distance
        int baseLevel = Math.max(1, (int)(distance / ModConfig.get().enemies.blocks_per_level_threshold) + 1);

        // roll for a random level within variance range of base level
        int variance = random.nextInt(ModConfig.get().enemies.random_level_offset * 2 + 1) - ModConfig.get().enemies.random_level_offset;

        // Minimum level 1 regardless of roll
        return Math.max(1, baseLevel + variance);
    }

    // adjust damage packets based on the enemies level
    private double adjustDamageBasedOnLevel(int attackerLevel, int attackerRarity, int defenderLevel, int defenderRarity, double damage) {
        // Tunable constants (safe for infinite scaling)
        final double LEVEL_MULTIPLIER = ModConfig.get().combat.level_diff_damage_multiplier;
        final double RARITY_MULTIPLIER = ModConfig.get().combat.rarity_diff_damage_multiplier;

        // determine delta based on level/rarity difference
        int levelDelta = attackerLevel - defenderLevel;
        int rarityDelta = attackerRarity - defenderRarity;

        // use the deltas to determine a level/rarity scale
        double levelScale = Math.pow(LEVEL_MULTIPLIER, levelDelta);
        double rarityScale = Math.pow(RARITY_MULTIPLIER, rarityDelta);

        // apply the damage scales to the damage amount
        damage *= levelScale;
        damage *= rarityScale;

        // Clamp result to prevent degenerate damage
        damage = Math.max(0, damage);

        // return the scaled damage number
        return damage;
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
        boolean shouldLootDrop = shouldLootDrop(rarity, ModConfig.get().loot.loot_drop_chance_modifier);
        boolean shouldRecipeDrop = shouldLootDrop(rarity, ModConfig.get().loot.recipe_drop_chance_modifier);

        // loop over all players who damaged the defender in the last 30 seconds
        if (ModConfig.get().loot.broadcast_drops_in_global_chat) {

        }
        for (Ref<EntityStore> ref : players) {
            // resolve the player component
            Player player = store.getComponent(ref, Player.getComponentType());

            // get the player's applicable components or bail
            Component_CraftingKnowledge craftingKnowledge = store.getComponent(ref, componentTypeCraftingKnowledge);
            Component_RPG_Player rpgPlayer = store.getComponent(ref, componentTypeRPGPlayer);
            if (craftingKnowledge == null || rpgPlayer == null) continue;

            // add a recipe if applicable
            if(shouldRecipeDrop && ModConfig.get().loot.broadcast_drops_in_global_chat && rpgPlayer.showLootDrops) {
                // roll recipe rarity and add it to the drop pool
                String recipeRarity = rollRarity();
                String recipeID = "Recipe_Page_" + recipeRarity;
                dropPool.add(new ItemStack(recipeID, 1));

                // get item color based on rarity
                Color color = colorUtils.getRarityColor(recipeRarity);

                // notify players of the loot roll
                alertPlayers(new Message[]{
                    Message.raw(player.getDisplayName()).color("#ffffff").bold(true),
                    Message.raw(" rolled a ").color(Color.GRAY),
                    Message.raw(recipeID.replace("_", " ")).color(color).bold(true)
                });
            }

            // bail now if loot should not drop
            if(!shouldLootDrop) continue;

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
            if (player != null && ModConfig.get().loot.broadcast_drops_in_global_chat && rpgPlayer.showLootDrops) {
                // get rarity of the item actually returned
                String[] parts = randomItemId.split("_");
                String actualRarity = parts[parts.length - 1];

                // format item name for display
                String itemName = randomItemId
                    .replace("Weapon_", "")
                    .replace("Armor_", "")
                    .replace("_", " ");

                // get the item rarity color
                Color color = colorUtils.getRarityColor(actualRarity);

                // notify players of the loot roll
                alertPlayers(new Message[]{
                    Message.raw(player.getDisplayName()).color("#ffffff").bold(true),
                    Message.raw(" rolled a ").color(Color.GRAY),
                    Message.raw(itemName).color(color).bold(true)
                });
            }
        }
    }

    // Returns true if the modifier application succeeds
    private boolean shouldLootDrop(String rarity, float modifier) {
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

        // get the success chance and apply the passed modifier
        double successChance = 1.0 - failChance;
        successChance *= modifier;

        // Clamp to [0, 1]
        successChance = Math.max(0.0, Math.min(1.0, successChance));


        // Success occurs when roll exceeds failure probability
        return roll < successChance;
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

    // helper function to apply a knockback to an entity
    public void applyKnockbackFromEntity(Ref<EntityStore> originRef, Ref<EntityStore> targetRef, int strength){
        Store<EntityStore> store = originRef.getStore();

        // get transform components
        TransformComponent targetTransform = store.getComponent(targetRef, TransformComponent.getComponentType());
        TransformComponent originTransform = store.getComponent(originRef, TransformComponent.getComponentType());

        // get attacker/defender positions
        Vector3d originPos = originTransform.getPosition();
        Vector3d targetPos = targetTransform.getPosition();

        // Direction from origin to target
        Vector3d direction = new Vector3d(targetPos.x - originPos.x, 0, targetPos.z - originPos.z);

        // Normalize and scale the direction
        direction.normalize();
        direction.scale(strength);

        // get velocity and transform components from attacker
        Velocity vc = store.getComponent(targetRef, Velocity.getComponentType());

        // set the instruction
        vc.addInstruction(direction, new VelocityConfig(), ChangeVelocityType.Add);
    }

    // function to flush ready damage groups
    public void tickDamageGroups(Store<EntityStore> store) {
        swingGroups.entrySet().removeIf(entry -> {
            SwingDamageGroup group = entry.getValue();
            if (!group.readyToApply) return false;
            applyDamageGroup(group);
            return true;
        });
    }

    // helper function for console logging
    public void alertPlayers(String msg, Color color) {
        color = color != null ? color : Color.WHITE;
        // loop over all players and broadcast the message
        for (PlayerRef player : Universe.get().getPlayers()) {
            player.sendMessage(Message.raw(msg).color(color));
        }
    }

    // helper function for console logging
    public void alertPlayers(Message[] messages) {
        // loop over all players and broadcast the message
        for (PlayerRef player : Universe.get().getPlayers()) {
            player.sendMessage(Message.join(messages));
        }
    }

    // helper function to swap damage type "Weapon" for a damage interaction var from the main hand weapon item
    private ResolvedDamage resolveWeaponDamageType(Ref<EntityStore> attacker, Store<EntityStore> store, DamageCause fallback, float fallbackAmount, Damage damage) {
        try {
            Component_RPG_Player rpgPlayer = store.getComponent(attacker, componentTypeRPGPlayer);
            if (rpgPlayer == null) return new ResolvedDamage(fallback, fallbackAmount);

            ItemStack mainHand = rpgPlayer.mainHandItem;
            if (mainHand == null) return new ResolvedDamage(fallback, fallbackAmount);

            Item item = mainHand.getItem();
            if (item.getData() == null) return new ResolvedDamage(fallback, fallbackAmount);

            Path itemPath = Item.getAssetStore().getAssetMap().getPath(item.getId());
            if (itemPath == null) return new ResolvedDamage(fallback, fallbackAmount);

            String rawJson = Files.readString(itemPath);
            JsonObject itemJson = JsonParser.parseString(rawJson).getAsJsonObject();

            if (!itemJson.has("InteractionVars")) return new ResolvedDamage(fallback, fallbackAmount);
            JsonObject interactionVars = itemJson.getAsJsonObject("InteractionVars");

            JsonObject damageVar = null;
            for (Map.Entry<String, JsonElement> entry : interactionVars.entrySet()) {
                if (entry.getKey().endsWith("_Damage")) {
                    damageVar = entry.getValue().getAsJsonObject();
                    break;
                }
            }
            if (damageVar == null) return new ResolvedDamage(fallback, fallbackAmount);

            JsonObject interaction = damageVar
                    .getAsJsonArray("Interactions")
                    .get(0).getAsJsonObject();
            if (!interaction.has("DamageCalculator")) return new ResolvedDamage(fallback, fallbackAmount);

            JsonObject baseDamage = interaction
                    .getAsJsonObject("DamageCalculator")
                    .getAsJsonObject("BaseDamage");

            Map.Entry<String, JsonElement> damageEntry = baseDamage.entrySet().iterator().next();
            String damageType = damageEntry.getKey();
            float damageValue = damageEntry.getValue().getAsFloat();

            DamageCause resolvedCause = DamageCause.getAssetMap().getAsset(damageType);
            if (resolvedCause == null) return new ResolvedDamage(fallback, fallbackAmount);

            damage.setDamageCauseIndex(DamageCause.getAssetMap().getIndex(damageType));
            return new ResolvedDamage(resolvedCause, damageValue);

        } catch (Exception e) {
            HytaleLogger.getLogger().at(Level.WARNING).log("resolveWeaponDamageType error: %s", e.getMessage());
            return new ResolvedDamage(fallback, fallbackAmount);
        }
    }
}
