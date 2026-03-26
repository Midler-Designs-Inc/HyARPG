package com.example.hyarpg.listeners;

// Hytale Imports
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.HolderSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

// Mod Imports
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.events.Event_ContainerSpawned;

// Checker annotations
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.jetbrains.annotations.NotNull;

public class Listeners_ContainerSpawn extends HolderSystem<ChunkStore> {

    @NullableDecl
    @Override
    public Query<ChunkStore> getQuery() {
        return ItemContainerBlock.getComponentType();
    }

    @Override
    public void onEntityAdd(@NonNullDecl Holder<ChunkStore> holder, @NonNullDecl AddReason reason, @NonNullDecl Store<ChunkStore> store) {
        // Get the ItemContainerBlock component or bail
        ItemContainerBlock containerBlock = (ItemContainerBlock) holder.getComponent(ItemContainerBlock.getComponentType());
        if (containerBlock == null) return;

        // Only fire for newly spawned containers, not loaded ones
        if (reason != AddReason.SPAWN) return;

        // Get BlockStateInfo
        BlockModule.BlockStateInfo blockStateInfo = holder.getComponent(BlockModule.BlockStateInfo.getComponentType());
        if (blockStateInfo == null) return;

        // fire off the event to the mod
        ModEventBus.post(new Event_ContainerSpawned(containerBlock, blockStateInfo));
    }

    @Override
    public void onEntityRemoved(@NotNull Holder<ChunkStore> holder, @NotNull RemoveReason removeReason, @NotNull Store<ChunkStore> store) {}
}