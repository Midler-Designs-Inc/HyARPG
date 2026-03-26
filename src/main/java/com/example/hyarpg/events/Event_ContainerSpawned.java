package com.example.hyarpg.events;

// Hytale Imports
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;

public record Event_ContainerSpawned(ItemContainerBlock containerBlock, BlockModule.BlockStateInfo blockStateInfo) {}
