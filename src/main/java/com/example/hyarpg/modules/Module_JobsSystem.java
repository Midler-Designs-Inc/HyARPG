package com.example.hyarpg.modules;

// Hytale Imports
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.events.Event_PlayerReady;
import com.example.hyarpg.components.Component_JobSkills;
import com.example.hyarpg.events.Event_DamageBlock;

// Java Imports
import org.joml.Vector3i;
import java.util.Arrays;

public class Module_JobsSystem {

    // initialize this module
    public Module_JobsSystem() {
        // Listen to applicable events on the mods internal event bus
        ModEventBus.register(Event_PlayerReady.class, this::onPlayerReady);
        ModEventBus.register(Event_DamageBlock.class, this::onDamageBlock);
    }

    // This function runs whenever a PlayerReady event is posted
    private void onPlayerReady(Event_PlayerReady event) {
        // get the joining player
        Player player = event.getPlayer();
        World world = event.getWorld();

        // get the player's Ref and the world entity store
        Ref<EntityStore> ref = player.getReference();
        Store<EntityStore> store = world.getEntityStore().getStore();

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

        // look up the block type at the target location via chunk
        Vector3i blockLocation = event.event().getTargetBlock();
        World world = commandBuffer.getExternalData().getWorld();
        long chunkIndex = ChunkUtil.indexChunkFromBlock(blockLocation.x, blockLocation.z);
        WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);
        if (chunk == null) return;

        int blockId = chunk.getBlock(blockLocation.x, blockLocation.y, blockLocation.z);
        BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
        if (blockType == null) return;

        // route to the correct job handler based on tool
        if (usingHatchet)  onDamageBlockWithHatchet(jobSkills, blockType, event);
        if (usingPickaxe)  onDamageBlockWithPickaxe(jobSkills, blockType, event);
    }

    // fires when a player is damaging a block with a hatchet
    private void onDamageBlockWithHatchet(Component_JobSkills jobSkills, BlockType blockType, Event_DamageBlock event) {
        // bail if the targeted block is not a wood block
        Item blockItem = blockType.getItem();
        if (blockItem == null || !Arrays.toString(blockItem.getCategories()).contains("Blocks.Wood")) return;

        // award logging XP
        PlayerRef playerRef = event.ref().getStore().getComponent(event.ref(), PlayerRef.getComponentType());
        if (playerRef == null) return;
        jobSkills.awardXp(playerRef, "Logging", 10);
    }

    // fires when a player is damaging a block with a pickaxe
    private void onDamageBlockWithPickaxe(Component_JobSkills jobSkills, BlockType blockType, Event_DamageBlock event) {
        // bail if the targeted block is not an ore block
        Item blockItem = blockType.getItem();
        if (blockItem == null || !Arrays.toString(blockItem.getCategories()).contains("Blocks.Ores")) return;

        // award mining XP
        PlayerRef playerRef = event.ref().getStore().getComponent(event.ref(), PlayerRef.getComponentType());
        if (playerRef == null) return;
        jobSkills.awardXp(playerRef, "Mining", 10);
    }

}