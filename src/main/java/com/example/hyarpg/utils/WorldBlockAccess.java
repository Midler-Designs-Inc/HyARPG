package com.example.hyarpg.utils;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockOperations;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockComponentSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.ComponentAccessor;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import org.joml.Vector3i;

public final class WorldBlockAccess {

    private WorldBlockAccess() {
    }

    public static OptionalInt getBlockId(World world, int x, int y, int z) {
        if (y < ChunkUtil.MIN_Y || y >= ChunkUtil.MIN_Y + ChunkUtil.HEIGHT) {
            return OptionalInt.empty();
        }

        ChunkStore chunkStore = world.getChunkStore();
        Ref<ChunkStore> sectionRef = chunkStore.getChunkSectionReferenceAtBlock(x, y, z);
        if (sectionRef == null || !sectionRef.isValid()) {
            return OptionalInt.empty();
        }

        BlockSection blockSection = chunkStore.getStore().getComponent(sectionRef, BlockSection.getComponentType());
        if (blockSection == null) {
            return OptionalInt.empty();
        }

        return OptionalInt.of(blockSection.get(ChunkUtil.indexBlock(x & 31, y & 31, z & 31)));
    }

    public static Optional<BlockType> getBlockType(World world, int x, int y, int z) {
        OptionalInt blockId = getBlockId(world, x, y, z);
        return blockId.isEmpty()
            ? Optional.empty()
            : Optional.ofNullable(BlockType.getAssetMap().getAsset(blockId.getAsInt()));
    }

    public static Optional<Ref<ChunkStore>> getBlockEntityReference(World world, int x, int y, int z) {
        if (y < ChunkUtil.MIN_Y || y >= ChunkUtil.MIN_Y + ChunkUtil.HEIGHT) {
            return Optional.empty();
        }

        ChunkStore chunkStore = world.getChunkStore();
        Ref<ChunkStore> sectionRef = chunkStore.getChunkSectionReferenceAtBlock(x, y, z);
        if (sectionRef == null || !sectionRef.isValid()) {
            return Optional.empty();
        }

        BlockComponentSection blockComponents = chunkStore.getStore().getComponent(
            sectionRef,
            BlockComponentSection.getComponentType()
        );
        if (blockComponents == null) {
            return Optional.empty();
        }

        Ref<ChunkStore> blockRef = blockComponents.getBlockReference(ChunkUtil.indexBlock(x & 31, y & 31, z & 31));
        return blockRef == null || !blockRef.isValid() ? Optional.empty() : Optional.of(blockRef);
    }

    public static Optional<Ref<ChunkStore>> getLoadedChunkReference(World world, long chunkIndex) {
        Ref<ChunkStore> chunkRef = world.getChunkStore().getChunkReference(chunkIndex);
        return chunkRef == null || !chunkRef.isValid() ? Optional.empty() : Optional.of(chunkRef);
    }

    public static CompletableFuture<Ref<ChunkStore>> getChunkReferenceAsync(World world, long chunkIndex) {
        return world.getChunkStore().getChunkReferenceAsync(chunkIndex);
    }

    public static boolean setBlockIfLoaded(
        World world,
        int x,
        int y,
        int z,
        int blockId,
        BlockType blockType,
        int rotation,
        int filler,
        int settings
    ) {
        if (y < ChunkUtil.MIN_Y || y >= ChunkUtil.MIN_Y + ChunkUtil.HEIGHT) {
            return false;
        }

        Optional<Ref<ChunkStore>> chunkRef = getLoadedChunkReference(world, ChunkUtil.indexChunkFromBlock(x, z));
        return chunkRef.isPresent()
            && BlockOperations.setBlock(
                world.getChunkStore(),
                chunkRef.get(),
                x,
                y,
                z,
                blockId,
                blockType,
                rotation,
                filler,
                settings
            );
    }

    public static boolean breakBlockIfLoaded(
        World world,
        int x,
        int y,
        int z,
        int flags,
        Ref<EntityStore> actorRef
    ) {
        if (y < ChunkUtil.MIN_Y || y >= ChunkUtil.MIN_Y + ChunkUtil.HEIGHT) {
            return false;
        }

        Optional<Ref<ChunkStore>> chunkRef = getLoadedChunkReference(world, ChunkUtil.indexChunkFromBlock(x, z));
        if (chunkRef.isEmpty()) {
            return false;
        }

        BlockHarvestUtils.performBlockBreak(
            actorRef,
            null,
            new Vector3i(x, y, z),
            flags,
            chunkRef.get(),
            world.getEntityStore().getStore(),
            world.getChunkStore().getStore()
        );
        return true;
    }
}
