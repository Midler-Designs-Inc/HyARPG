package com.example.hyarpg.modules;

// Hytale Imports
import com.example.hyarpg.components.Component_Grave;
import com.example.hyarpg.utils.items.ItemFactory;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule.BlockStateInfo;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.item.ItemModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
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
import com.example.hyarpg.configs.ModConfig;
import com.example.hyarpg.utils.skills.SkillLibrary;
import com.example.hyarpg.utils.skills.SkillLibraryMigration;

// Java Imports
import java.awt.*;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;


public class Module_RPGSystem {

    private final HyARPGPlugin plugin;

    // Component Type references
    public static ComponentType<EntityStore, Component_RPG_Player> componentTypeRPGPlayer;
    public static ComponentType<EntityStore, Component_RPG_Enemy> componentTypeRPGEnemy;
    public static ComponentType<EntityStore, Component_CraftingKnowledge> componentTypeCraftingKnowledge;
    public static ComponentType<ChunkStore, Component_Grave> componentTypeGrave;

    // properties that control enemy level as they get further from spawn
    private static final Random random = new Random();

    // Skill Tree Version Constant
    private static final String SKILL_TREE_VERSION = "1.11.0"; // 1.6.0

    // initialize this module
    public Module_RPGSystem(HyARPGPlugin plugin) {
        this.plugin = plugin;

        // Register the component type using EntityStoreRegistry
        componentTypeRPGPlayer = plugin.getEntityStoreRegistry().registerComponent(Component_RPG_Player.class, "RPGStatsComponent", Component_RPG_Player.CODEC);
        componentTypeRPGEnemy = plugin.getEntityStoreRegistry().registerComponent(Component_RPG_Enemy.class, "RPGEnemyComponent", Component_RPG_Enemy.CODEC);
        componentTypeCraftingKnowledge = plugin.getEntityStoreRegistry().registerComponent(Component_CraftingKnowledge.class, "CraftingKnowledgeComponent", Component_CraftingKnowledge.CODEC);
        componentTypeGrave = plugin.getChunkStoreRegistry().registerComponent(Component_Grave.class, "PlayerGraveComponent", Component_Grave.CODEC);

        // Get the interaction registry and register the custom interactions
        final var interactionRegistry = plugin.getCodecRegistry(Interaction.CODEC);
        interactionRegistry.register("ShowHowToPlay", Interaction_ShowHowToPlay.class, Interaction_ShowHowToPlay.CODEC);
        interactionRegistry.register("ChangeItemState", Interaction_ChangeItemStateInteraction.class, Interaction_ChangeItemStateInteraction.CODEC);
        interactionRegistry.register("Use_Ability_1", Interaction_UseAbility1.class, Interaction_UseAbility1.CODEC);
        interactionRegistry.register("Use_Ability_2", Interaction_UseAbility2.class, Interaction_UseAbility2.CODEC);
        interactionRegistry.register("Use_Ability_3", Interaction_UseAbility3.class, Interaction_UseAbility3.CODEC);
        interactionRegistry.register("SpawnDeployableAtHitLocationFixed", Interaction_SpawnDeployableAtHitLocation.class, Interaction_SpawnDeployableAtHitLocation.CODEC);
        interactionRegistry.register("Bench_Forge_Open_Crafting", Interaction_Bench_Forge_Open_Crafting.class, Interaction_Bench_Forge_Open_Crafting.CODEC);
        interactionRegistry.register("Bench_Forge_Open_Salvaging", Interaction_Bench_Forge_Open_Salvaging.class, Interaction_Bench_Forge_Open_Salvaging.CODEC);
        interactionRegistry.register("Open_Territory_Panel", Interaction_Open_Territory_Panel.class, Interaction_Open_Territory_Panel.CODEC);
        interactionRegistry.register("Resurrect_Player_At_Grave", Interaction_RezPlayer.class, Interaction_RezPlayer.CODEC);

        // Listen to applicable events on the mods internal event bus
        ModEventBus.register(Event_PlayerReady.class, this::onPlayerReady);
        ModEventBus.register(Event_PlayerDisconnect.class, this::onPlayerDisconnect);
        ModEventBus.register(Event_NPCSpawn.class, this::onNPCSpawn);
        ModEventBus.register(Event_NPCPreSpawn.class, this::onNPCPreSpawn);
        ModEventBus.register(Event_PlayerInventoryItemAdded.class, this::onPlayerInventoryItemAdded);
        ModEventBus.register(Event_PlayerInventoryItemEquip.class, this::onPlayerInventoryItemEquip);
        ModEventBus.register(Event_PlayerInventoryItemUnEquip.class, this::onPlayerInventoryItemUnEquip);
        ModEventBus.register(Event_ContainerSpawned.class, this::onContainerSpawned);
        ModEventBus.register(Event_PlayerDeath.class, this::onPlayerDeath);
        ModEventBus.register(Event_PlayerRespawn.class, this::onPlayerRespawn);

        // Replace the inventory open with our own window
//        Window.CLIENT_REQUESTABLE_WINDOW_TYPES.put(WindowType.PocketCrafting, () -> new InterceptPocketCraftingWindow());
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

        // check if the player should be included in any raid
        plugin.raidSystem.onPlayerJoinedWhileRaidActive(entityRef);
    }

    // This function runs whenever a PlayerDisconnect event is posted
    private void onPlayerDisconnect(Event_PlayerDisconnect event) {
        try {
            PlayerRef playerRef = event.getPlayer();
            assert playerRef.getWorldUuid() != null;
            World world = Universe.get().getWorld(playerRef.getWorldUuid());
            if (world == null) return;

            world.execute(() -> {
                try {
                    Ref<EntityStore> ref = playerRef.getReference();
                    if (ref == null) return;
                    Store<EntityStore> store = world.getEntityStore().getStore();
                    Component_RPG_Player rpgPlayer = store.getComponent(ref, componentTypeRPGPlayer);
                    if (rpgPlayer == null) return;
                    rpgPlayer.lastLogoutTime = Instant.now().getEpochSecond();
                } catch (Exception e) {
                    HytaleLogger.getLogger().at(Level.WARNING).log("HyARPG: Failed to store logout time for: %s. Error: %s", event.getPlayer().getUsername(), e.getMessage());
                }
            });
        } catch (Exception e) {
            HytaleLogger.getLogger().at(Level.WARNING).log("HyARPG: Failed to perform player disconnect logic for: %s. Error: %s", event.getPlayer().getUsername(), e.getMessage());
        }
    }

    // This function runs whenever an NPCPreSpawn event is posted
    private void onNPCPreSpawn(Event_NPCPreSpawn event) {
        // get the entity holder Ref
        Holder<EntityStore> holder = event.getHolder();

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

    // capture when an item is added to a players inventory
    private void onPlayerInventoryItemAdded(Event_PlayerInventoryItemAdded event) {
        // entity and store refs
        Ref<EntityStore> ref = event.getRef();
        Store<EntityStore> store = event.getStore();

        // get item stack, item, item slot and item container
        ItemStack stack = event.getStack();

        // register discovery for ALL items
        registerDiscoveredItem(ref, store, stack.getItem());
    }

    // capture when a container spawns
    private void onContainerSpawned(Event_ContainerSpawned event) {
        ItemContainerBlock containerBlock = event.containerBlock();
        BlockStateInfo blockStateInfo = event.blockStateInfo();

        // if droplist is already null this chest was previously looted — leave it empty
        if (containerBlock.getDroplist() == null) return;

        // getIndex() returns indexBlockInColumn — full column-relative coords
        int index = blockStateInfo.getIndex();
        int localX = ChunkUtil.xFromBlockInColumn(index);
        int localY = ChunkUtil.yFromBlockInColumn(index);
        int localZ = ChunkUtil.zFromBlockInColumn(index);

        // Get chunk X/Z from the WorldChunk component on the chunk ref
        Ref<ChunkStore> chunkRef = blockStateInfo.getChunkRef();
        Store<ChunkStore> store = chunkRef.getStore();
        WorldChunk worldChunk = (WorldChunk) store.getComponent(chunkRef, WorldChunk.getComponentType());
        int chunkX = worldChunk.getX();
        int chunkZ = worldChunk.getZ();

        // Reconstruct world coordinates — no sectionY needed, localY is already full column Y
        int worldX = ChunkUtil.worldCoordFromLocalCoord(chunkX, localX);
        int worldY = localY;
        int worldZ = ChunkUtil.worldCoordFromLocalCoord(chunkZ, localZ);

        // Compute distance from config origin
        double dx = worldX - ModConfig.get().world.origin_spawn_point_x;
        double dy = worldY - ModConfig.get().world.origin_spawn_point_y;
        double dz = worldZ - ModConfig.get().world.origin_spawn_point_z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Determine tier based on ore spawn distance ranges
        int tier = 0;
        if (distance >= ModConfig.get().world.min_distance_for_mithril_spawn)          tier = 6;
        else if (distance >= ModConfig.get().world.min_distance_for_adamantite_spawn)   tier = 5;
        else if (distance >= ModConfig.get().world.min_distance_for_cobalt_spawn)       tier = 4;
        else if (distance >= ModConfig.get().world.min_distance_for_thorium_spawn)      tier = 3;
        else if (distance >= ModConfig.get().world.min_distance_for_iron_spawn)         tier = 2;
        else if (distance >= ModConfig.get().world.min_distance_for_copper_spawn)       tier = 1;

        // resolve the droplist into raw stacks then null it out so the container can't re-populate on open or break
        List<ItemStack> rawDrops = ItemModule.get().getRandomItemDrops("HyARPG_Container_Tier" + tier);
        containerBlock.setDroplist(null);

        // loop over raw drops and replace any mod gear with a factory-generated equivalent
        List<ItemStack> finalItems = new ArrayList<>();
        int gearLevel = Math.max(1, (int)(distance / ModConfig.get().enemies.blocks_per_level_threshold) + 1);
        for (ItemStack drop : rawDrops) {
            Item item = drop.getItem();
            if (item == null) continue;

            String[] categories = item.getCategories();
            boolean isModGear = categories != null && Arrays.asList(categories).contains("Items.HyARPG.Gear");

            // pass through non-gear drops as-is, replace gear drops via ItemFactory
            if (!isModGear) { finalItems.add(drop); continue; }
            ItemStack generated = ItemFactory.createItem(drop.getItemId(), gearLevel, null, null, null);
            if (generated != null) finalItems.add(generated);
        }

        // populate the container with the final item list
        containerBlock.getItemContainer().addItemStacks(finalItems);
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

    // method for when a player dies
    public void onPlayerDeath(Event_PlayerDeath event) {
        Ref<EntityStore> ref = event.getRef();
        Store<EntityStore> store = event.getStore();

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return;

        Component_RPG_Player rpg = store.getComponent(ref, componentTypeRPGPlayer);
        if (rpg == null) return;

        UUID deadUuid = playerRef.getUuid();
        Vector3d pos = transform.getPosition();
        int x = (int) Math.floor(pos.x);
        int y = (int) Math.floor(pos.y);
        int z = (int) Math.floor(pos.z);

        World world = store.getExternalData().getWorld();
        world.execute(() -> {
            // remove any existing grave for this player first
            if (rpg.gravePosition != null) {
                String[] parts = rpg.gravePosition.split(",");
                world.breakBlock(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), 0);
            }

            long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
            WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);
            if (chunk == null) return;

            BlockType blockType = BlockType.getAssetMap().getAsset("HyARPG_Player_Grave");
            if (blockType == null) return;

            int blockIndex = BlockType.getAssetMap().getIndex("HyARPG_Player_Grave");

            Holder<ChunkStore> graveHolder = ChunkStore.REGISTRY.newHolder();
            graveHolder.addComponent(componentTypeGrave, new Component_Grave(deadUuid, ref));

            chunk.setState(x, y, z, blockType, 0, graveHolder);
            chunk.setBlock(x, y, z, blockIndex, blockType, 0, 0, 2);

            // store grave position on player component
            rpg.gravePosition = x + "," + y + "," + z;
        });
    }

    // method for when a player respawns
    public void onPlayerRespawn(Event_PlayerRespawn event) {
        Ref<EntityStore> ref = event.ref();
        Store<EntityStore> store = event.store();

        Component_RPG_Player rpg = store.getComponent(ref, componentTypeRPGPlayer);
        if (rpg == null || rpg.gravePosition == null) return;

        String[] parts = rpg.gravePosition.split(",");
        int x = Integer.parseInt(parts[0]);
        int y = Integer.parseInt(parts[1]);
        int z = Integer.parseInt(parts[2]);
        rpg.gravePosition = null;

        World world = store.getExternalData().getWorld();
        world.execute(() -> world.breakBlock(x, y, z, 0));
    }

    // register an item a player picked up to their discovered list
    private void registerDiscoveredItem(Ref<EntityStore> ref, Store<EntityStore> store, Item query) {
        try {
            Component_CraftingKnowledge craftingKnowledge = store.getComponent(ref, componentTypeCraftingKnowledge);
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (craftingKnowledge == null || playerRef == null) return;

            // Discover the item, then discover any new recipes
            craftingKnowledge.addDiscoveredItem(playerRef, query);
            craftingKnowledge.discoverRecipes(ref, store, query);
        } catch (Exception _) {}
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
}
