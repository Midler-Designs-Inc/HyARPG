package com.example.hyarpg.listeners;

// Hytale Imports
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.HolderSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

// Mod Imports
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.events.Event_ContainerSpawned;

// Checker annotations
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.checkerframework.checker.nullness.qual.NonNull;

public class Listeners_ContainerSpawn extends HolderSystem<ChunkStore> {

    @NullableDecl
    @Override
    public Query<ChunkStore> getQuery() {
        return ItemContainerBlock.getComponentType();
    }

    @Override
    public void onEntityAdd(@NonNullDecl Holder<ChunkStore> holder, @NonNullDecl AddReason reason, @NonNullDecl Store<ChunkStore> store) {
        // Get the ItemContainerBlock component or bail
        ItemContainerBlock containerBlock = holder.getComponent(ItemContainerBlock.getComponentType());
        if (containerBlock == null) return;

        // player-placed containers have null droplist on SPAWN — mark them so they get no loot
        if (reason == AddReason.SPAWN && containerBlock.getDroplist() == null) {
            containerBlock.setDroplist("Empty");
            return;
        }

        // Get BlockStateInfo
        BlockModule.BlockStateInfo blockStateInfo = holder.getComponent(BlockModule.BlockStateInfo.getComponentType());
        if (blockStateInfo == null) return;

        // Get the block's local location
        int index = blockStateInfo.getIndex();
        int localX = ChunkUtil.xFromBlockInColumn(index);
        int localY = ChunkUtil.yFromBlockInColumn(index);
        int localZ = ChunkUtil.zFromBlockInColumn(index);

        // get the chunk
        Ref<ChunkStore> chunkRef = blockStateInfo.getChunkRef();
        WorldChunk worldChunk = (WorldChunk) store.getComponent(chunkRef, WorldChunk.getComponentType());
        if(worldChunk == null) return;

        int chunkX = worldChunk.getX();
        int chunkZ = worldChunk.getZ();

        // get the blocks world location from the chunk
        int worldX = ChunkUtil.worldCoordFromLocalCoord(chunkX, localX);
        int worldZ = ChunkUtil.worldCoordFromLocalCoord(chunkZ, localZ);

        // fire off the event to the mod
        ModEventBus.post(new Event_ContainerSpawned(containerBlock, blockStateInfo, worldX, localY, worldZ));
    }

    @Override
    public void onEntityRemoved(@NonNull Holder<ChunkStore> holder, @NonNull RemoveReason removeReason, @NonNull Store<ChunkStore> store) {}
}