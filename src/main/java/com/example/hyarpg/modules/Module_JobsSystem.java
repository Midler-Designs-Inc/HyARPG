package com.example.hyarpg.modules;

// Hytale Imports
import com.example.hyarpg.events.Event_PlayerInventoryItemAdded;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.ExplosionUtils;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.events.Event_PlayerReady;
import com.example.hyarpg.components.Component_JobSkills;
import com.example.hyarpg.events.Event_DamageBlock;
import com.example.hyarpg.configs.ModConfig;
import com.example.hyarpg.events.Event_BreakBlock;
import com.example.hyarpg.utils.jobs.JobSkill;
import com.example.hyarpg.utils.jobs.JobSkill.*;
import com.example.hyarpg.utils.jobs.JobSkill_Logging;

import org.joml.Vector3d;
import org.joml.Vector3i;

// Java Imports
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Module_JobsSystem {

    // initialize this module
    public Module_JobsSystem() {
        // Listen to applicable events on the mods internal event bus
        ModEventBus.register(Event_PlayerReady.class, this::onPlayerReady);
        ModEventBus.register(Event_DamageBlock.class, this::onDamageBlock);
        ModEventBus.register(Event_BreakBlock.class, this::onBreakBlock);
        ModEventBus.register(Event_PlayerInventoryItemAdded.class, this::onItemPickup);
    }

    // This function runs whenever a PlayerReady event is posted
    private void onPlayerReady(Event_PlayerReady event) {
        // get the joining player
        Player player = event.getPlayer();
        World world = event.getWorld();

        // get the player's Ref and the world entity store
        Ref<EntityStore> ref = player.getReference();
        Store<EntityStore> store = world.getEntityStore().getStore();
        if(ref == null || !ref.isValid()) return;

        // ensure the component exists (supposedly this will putComponent internally if not??)
        store.ensureAndGetComponent(ref, Component_JobSkills.getComponentType());
    }

    // this function fires whenever a player damages a block
    private void onDamageBlock(Event_DamageBlock event) {
        // get the hand item
        ItemStack itemStack = event.event().getItemInHand();
        if (itemStack == null) return;

        // check if the player has a hatchet or pickaxe in their main hand, if not bail
        boolean usingHatchet  = itemStack.getItemId().contains("Tool_Hatchet");
        boolean usingPickaxe  = itemStack.getItemId().contains("Tool_Pickaxe");
        if (!usingHatchet && !usingPickaxe) return;

        // get the blocktype from the event and then get its asset configuration
        BlockType blockType = event.event().getBlockType();
        Item blockItem = blockType.getItem();
        if(blockItem == null) return;

        // get the categories from the asset config
        String[] categories = blockItem.getCategories();

        // check for logging/mining flags, return if neither
        boolean isWood = Arrays.asList(categories).contains("Blocks.Wood");
        boolean isOre = Arrays.asList(categories).contains("Blocks.Ores");
        if (!isWood && !isOre) return;

        // get the ref, store, command buffer and applicable components or bail
        Ref<EntityStore> ref = event.ref();
        Store<EntityStore> store = event.store();
        CommandBuffer<EntityStore> commandBuffer = event.commandBuffer();
        Component_JobSkills jobSkills = commandBuffer.getComponent(ref, Component_JobSkills.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (jobSkills == null || playerRef == null) return;

        // using hatchet and was wood
        if (usingHatchet && isWood) {
            // get skill level and unlocked skill perks
            int skillLevel = jobSkills.calculateLevelFromXP(jobSkills.loggingXp);
            Map<String, JobSkill.JobPerk> unlockedPerks = JobSkill_Logging.INSTANCE.getUnlockedPerks(skillLevel);

            // award XP and apply the perks
            jobSkills.awardXP(playerRef, "Logging", ModConfig.get().experience.xp_increase_from_minor_activity);
            applyLoggingDamagePerks(ref, store, event.event().getTargetBlock(), blockItem.getId(), unlockedPerks, jobSkills, playerRef);
        }

        // using pickaxe and was ore
        else if (usingPickaxe && isOre) {
            // get skill level and unlocked skill perks
            int skillLevel = jobSkills.calculateLevelFromXP(jobSkills.loggingXp);
            Map<String, JobSkill.JobPerk> unlockedPerks = JobSkill_Logging.INSTANCE.getUnlockedPerks(skillLevel);

            // award XP and apply the perks
            jobSkills.awardXP(playerRef, "Mining", ModConfig.get().experience.xp_increase_from_minor_activity);
            applyMiningPerks(ref, store, playerRef, unlockedPerks, false);
        }
    }

    // this function fires whenever a player breaks a block
    private void onBreakBlock(Event_BreakBlock event) {
        // get the hand item
        ItemStack itemStack = event.event().getItemInHand();
        if (itemStack == null) return;

        // check if the player has a hatchet or pickaxe in their main hand, if not bail
        boolean usingHatchet  = itemStack.getItemId().contains("Tool_Hatchet");
        boolean usingPickaxe  = itemStack.getItemId().contains("Tool_Pickaxe");
        if (!usingHatchet && !usingPickaxe) return;

        // get the blocktype from the event and then get its asset configuration
        BlockType blockType = event.event().getBlockType();
        Item blockItem = blockType.getItem();
        if(blockItem == null) return;

        // get the categories from the asset config
        String[] categories = blockItem.getCategories();

        // check for logging/mining flags, return if neither
        boolean isWood = Arrays.asList(categories).contains("Blocks.Wood");
        boolean isOre = Arrays.asList(categories).contains("Blocks.Ores");
        if (!isWood && !isOre) return;

        // get the ref, store, command buffer and applicable components or bail
        Ref<EntityStore> ref = event.ref();
        Store<EntityStore> store = event.store();
        CommandBuffer<EntityStore> commandBuffer = event.commandBuffer();
        Component_JobSkills jobSkills = commandBuffer.getComponent(ref, Component_JobSkills.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (jobSkills == null || playerRef == null) return;

        // using hatchet and was wood
        if (usingHatchet && isWood) {
            // get the loggers boon status effect (just an empty entity effect with a 2 second timer)
            EntityEffect loggersBoon = EntityEffect.getAssetMap().getAsset("LoggersBoon");
            if (loggersBoon == null) return;

            // award XP for breaking a wood block
            jobSkills.awardXP(playerRef, "Logging", ModConfig.get().experience.xp_increase_from_major_activity);

            // apply the loggers boon to the player
            EffectControllerComponent effectController = store.getComponent(ref, EffectControllerComponent.getComponentType());
            if (effectController != null) effectController.addEffect(ref, loggersBoon, commandBuffer);
        }

        // using pickaxe and was ore
        else if (usingPickaxe && isOre) {
            // get skill level and unlocked skill perks
            int skillLevel = jobSkills.calculateLevelFromXP(jobSkills.loggingXp);
            Map<String, JobSkill.JobPerk> unlockedPerks = JobSkill_Logging.INSTANCE.getUnlockedPerks(skillLevel);

            // award XP and apply the perks
            jobSkills.awardXP(playerRef, "Mining", ModConfig.get().experience.xp_increase_from_major_activity);
            applyMiningPerks(ref, store, playerRef, unlockedPerks, true);
        }
    }

    // this function fires whenever a player pickups an item
    private void onItemPickup(Event_PlayerInventoryItemAdded event) {
        // Get the item stack and make sure it's not marked with loggers boon
        ItemStack itemStack = event.getStack();
        if(itemStack.getFromMetadataOrNull("LoggersBoon", Codec.BOOLEAN) != null) return;

        // ensure the item id is a wood trunk item or bail
        String itemId = event.getStack().getItemId();
        if (!itemId.startsWith("Wood_") || !itemId.endsWith("_Trunk")) return;

        // get the ref, store and item id string
        Ref<EntityStore> ref = event.getRef();
        Store<EntityStore> store = event.getStore();

        // check that the ref/player has the loggers boon effect or bail
        EntityEffect loggersBoon = EntityEffect.getAssetMap().getAsset("LoggersBoon");
        EffectControllerComponent effectController = store.getComponent(ref, EffectControllerComponent.getComponentType());
        if (effectController == null || loggersBoon == null || !effectController.hasEffect(loggersBoon)) return;

        // get the player ref and job skills component from the ref/player
        Component_JobSkills jobSkills = store.getComponent(ref, Component_JobSkills.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (jobSkills == null) return;

        // get skill level and unlocked skill perks
        int skillLevel = jobSkills.calculateLevelFromXP(jobSkills.loggingXp);
        Map<String, JobSkill.JobPerk> unlockedPerks = JobSkill_Logging.INSTANCE.getUnlockedPerks(skillLevel);

        // award XP and apply the perks
        jobSkills.awardXP(playerRef, "Logging", ModConfig.get().experience.xp_increase_from_major_activity);
        applyLoggingBreakPerks(ref, store, itemId, unlockedPerks);
    }

    // apply logging perks when damaging a block
    private void applyLoggingDamagePerks (Ref<EntityStore> ref, Store<EntityStore> store, Vector3i blockPos, String blockId,  Map<String, JobPerk> unlockedPerks, Component_JobSkills jobSkills, PlayerRef playerRef) {
        // get the durability perk
        JobPerk durability = unlockedPerks.get("Durability");
        if (durability != null) {
            // wait for next world execute so the pickaxe damage is already done
            store.getExternalData().getWorld().execute(() -> {
                // get the players hotbar
                InventoryComponent.Hotbar hotbar = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
                if(hotbar == null) return;

                // get the players active item
                ItemStack heldItem = hotbar.getActiveItem();
                if (heldItem == null || heldItem.isEmpty()) return;

                // restore durability to the player's hatchet equal to .05f x perk tier (each swing vanilla takes off .25 durability)
                double restoreAmount = .05d * (double) durability.tier();
                ItemUtils.updateItemStackDurability(ref, heldItem, hotbar.getInventory(), hotbar.getActiveSlot(), restoreAmount, store);
            });
        }

        // get the instant fell perk
        JobPerk instantFell = unlockedPerks.get("InstantFell");
        if (instantFell != null) {
            // roll a 25% x perk tier chance and if success break the block on the player's behalf
            float chance = 0.25f * instantFell.tier();
            if (Math.random() < chance) {
                // create the item drop for the log we are going to manually break
                Holder<EntityStore>[] drops = ItemComponent.generateItemDrops(store, List.of(new ItemStack(blockId, 1)), new Vector3d(blockPos), Rotation3f.IDENTITY);

                // get the world and fire logic on next tick
                World world = store.getExternalData().getWorld();
                world.execute(() -> {
                    // get the block location, and neccessary accessors
                    Vector3i blockLocation = new Vector3i(blockPos.x, blockPos.y, blockPos.z);
                    long chunkIndex = ChunkUtil.indexChunkFromBlock(blockLocation.x, blockLocation.z);
                    ComponentAccessor<ChunkStore> chunkStore = world.getChunkStore().getStore();
                    Ref<ChunkStore> chunkReference = chunkStore.getExternalData().getChunkReference(chunkIndex);
                    if (chunkReference == null || !chunkReference.isValid()) return;

                    // perform block damage on the players behalf so the break can flow through the normal routes
                    BlockHarvestUtils.performBlockDamage(ref, blockLocation, null, null, null, false, 999f, 0, chunkReference, store, chunkStore);
                });
            }
        }
    }

    // apply logging perks when damaging a block
    private void applyLoggingBreakPerks (Ref<EntityStore> ref, Store<EntityStore> store, String itemId, Map<String, JobPerk> unlockedPerks) {
        // get the world and ref/player transform component or bail
        World world = store.getExternalData().getWorld();
        TransformComponent transformComp = store.getComponent(ref, TransformComponent.getComponentType());
        if (transformComp == null) return;

        // get the players current position for item drops
        Vector3d dropPos = new Vector3d(transformComp.getPosition());

        // apply the leaf finder perk
        JobSkill.JobPerk leafFinder = unlockedPerks.get("LeafFinder");
        if (leafFinder != null) {
            // drop plant fiber equal to perk tier
            int quantity = leafFinder.tier();
            Holder<EntityStore>[] drops = ItemComponent.generateItemDrops(store, List.of(new ItemStack("Ingredient_Fibre", quantity)), dropPos, Rotation3f.IDENTITY);
            world.execute(() -> store.addEntities(drops, AddReason.SPAWN));
        }

        // apply the stick bundler perk
        JobSkill.JobPerk stickBundler = unlockedPerks.get("StickBundler");
        if (stickBundler != null) {
            // drop sticks equal to perk tier
            int quantity = stickBundler.tier();
            Holder<EntityStore>[] drops = ItemComponent.generateItemDrops(store, List.of(new ItemStack("Ingredient_Stick", quantity)), dropPos, Rotation3f.IDENTITY);
            world.execute(() -> store.addEntities(drops, AddReason.SPAWN));
        }

        // apply the yield perk
        JobSkill.JobPerk yield = unlockedPerks.get("Yield");
        if (yield != null) {
            // roll a 10% x perk tier chance and if success drop one extra of the picked up item
            float chance = 0.10f * yield.tier();
            if (Math.random() < chance) {
                // create a new item stack with meta data to mark this log as coming from the loggers boon
                ItemStack itemStack = new ItemStack(itemId, 1).withMetadata("LoggersBoon", Codec.BOOLEAN, true);

                // create a holder entity for the drops and on next world tick spawn them
                Holder<EntityStore>[] drops = ItemComponent.generateItemDrops(store, List.of(itemStack), dropPos, Rotation3f.IDENTITY);
                world.execute(() -> store.addEntities(drops, AddReason.SPAWN));
            }
        }
    }

    // apply mining perks
    private void applyMiningPerks (Ref<EntityStore> ref, Store<EntityStore> store, PlayerRef playerRef, Map<String, JobPerk> unlockedPerks, boolean breakEvent) {

    }
}