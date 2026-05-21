package com.example.hyarpg.modules;

// Hytale Imports
import com.hypixel.hytale.builtin.blockphysics.BlockPhysicsPlugin;
import com.hypixel.hytale.builtin.blockphysics.BlockPhysicsSystems;
import com.hypixel.hytale.builtin.blockphysics.BlockPhysicsUtil;
import com.hypixel.hytale.builtin.fallingblocks.BreakFallingBlockImpact;
import com.hypixel.hytale.builtin.fallingblocks.FallingBlock;
import com.hypixel.hytale.builtin.fallingblocks.FallingBlocksPlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.PhysicsDropType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.SupportDropType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.fallingblocks.FallingBlockSettings;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.blocktype.component.BlockPhysics;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.BreakBlockInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.events.Event_PlayerReady;
import com.example.hyarpg.components.Component_JobSkills;
import com.example.hyarpg.events.Event_DamageBlock;
import com.example.hyarpg.configs.ModConfig;
import com.example.hyarpg.events.Event_BreakBlock;
import com.example.hyarpg.utils.jobs.JobSkill;
import com.example.hyarpg.utils.jobs.JobSkill_Logging;
import com.hypixel.hytale.server.npc.systems.SpawnNPCInteractionFailureTrackerSystems;

// Java Imports
import java.util.Arrays;
import java.util.List;

public class Module_JobsSystem {

    // initialize this module
    public Module_JobsSystem() {
        // Listen to applicable events on the mods internal event bus
        ModEventBus.register(Event_PlayerReady.class, this::onPlayerReady);
        ModEventBus.register(Event_DamageBlock.class, this::onDamageBlock);
        ModEventBus.register(Event_BreakBlock.class, this::onBreakBlock);
//        ModEventBus.register(Event_BreakBlock.class, this::onBlockRemove);
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
        // get the player and their required components or bail
        Ref<EntityStore> ref = event.ref();
        CommandBuffer<EntityStore> commandBuffer = event.commandBuffer();
        Component_JobSkills jobSkills = commandBuffer.getComponent(ref, Component_JobSkills.getComponentType());
        ItemStack itemStack = event.event().getItemInHand();
        if (jobSkills == null || itemStack == null) return;

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

        // check for logging/mining flags
        boolean isWood = Arrays.asList(categories).contains("Blocks.Wood");
        boolean isOre = Arrays.asList(categories).contains("Blocks.Ores");

        // Get the player Ref
        PlayerRef playerRef = event.ref().getStore().getComponent(event.ref(), PlayerRef.getComponentType());
        if (playerRef == null) return;

        // award the appropriate XP
        if (usingHatchet && isWood) {
            // get skill level and unlocked skill perks
            int skillLevel = jobSkills.calculateLevelFromXp(jobSkills.loggingXp);
            List<JobSkill.JobPerk> unlockedPerks = JobSkill_Logging.INSTANCE.getUnlockedPerks(skillLevel);

            // award XP and apply the perks
            jobSkills.awardXp(playerRef, "Logging", ModConfig.get().experience.xp_increase_from_minor_activity);
            applyLoggingPerks(unlockedPerks, false);
        }
        if (usingPickaxe && isOre) {
            // get skill level and unlocked skill perks
            int skillLevel = jobSkills.calculateLevelFromXp(jobSkills.loggingXp);
            List<JobSkill.JobPerk> unlockedPerks = JobSkill_Logging.INSTANCE.getUnlockedPerks(skillLevel);

            // award XP and apply the perks
            jobSkills.awardXp(playerRef, "Mining", ModConfig.get().experience.xp_increase_from_minor_activity);
            applyMiningPerks(unlockedPerks, false);
        }
    }

    // this function fires whenever a player breaks a block
    private void onBreakBlock(Event_BreakBlock event) {
        // get the player and their required components or bail
        Ref<EntityStore> ref = event.ref();
        CommandBuffer<EntityStore> commandBuffer = event.commandBuffer();
        Component_JobSkills jobSkills = commandBuffer.getComponent(ref, Component_JobSkills.getComponentType());
        ItemStack itemStack = event.event().getItemInHand();
        if (jobSkills == null || itemStack == null) return;

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

        // check for logging/mining flags
        boolean isWood = Arrays.asList(categories).contains("Blocks.Wood");
        boolean isOre = Arrays.asList(categories).contains("Blocks.Ores");

        // Get the player Ref
        PlayerRef playerRef = event.ref().getStore().getComponent(event.ref(), PlayerRef.getComponentType());
        if (playerRef == null) return;

        // award the appropriate XP
        if (usingHatchet && isWood) {
            // get skill level and unlocked skill perks
            int skillLevel = jobSkills.calculateLevelFromXp(jobSkills.loggingXp);
            List<JobSkill.JobPerk> unlockedPerks = JobSkill_Logging.INSTANCE.getUnlockedPerks(skillLevel);

            // award XP and apply the perks
            jobSkills.awardXp(playerRef, "Logging", ModConfig.get().experience.xp_increase_from_major_activity);
            applyLoggingPerks(unlockedPerks, true);
        }
        if (usingPickaxe && isOre) {
            // get skill level and unlocked skill perks
            int skillLevel = jobSkills.calculateLevelFromXp(jobSkills.loggingXp);
            List<JobSkill.JobPerk> unlockedPerks = JobSkill_Logging.INSTANCE.getUnlockedPerks(skillLevel);

            // award XP and apply the perks
            jobSkills.awardXp(playerRef, "Mining", ModConfig.get().experience.xp_increase_from_major_activity);
            applyMiningPerks(unlockedPerks, true);
        }
    }

    // apply logging perks
    private void applyLoggingPerks (List<JobSkill.JobPerk> unlockedPerks, boolean breakEvent) {
        // apply perks applicable when damaging a block
        if (!breakEvent) {
            if (unlockedPerks.contains("Durability")) {
                // restore a slight amount of durability to the players pickaxe
            }

            if (unlockedPerks.contains("InstantFell")) {
                // roll a 10% chance and if success break the block
            }
        }

        // apply perks applicable when breaking a block
        else {

        }
    }

    // apply mining perks
    private void applyMiningPerks (List<JobSkill.JobPerk> unlockedPerks, boolean breakEvent) {

    }
}