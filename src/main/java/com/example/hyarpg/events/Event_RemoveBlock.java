package com.example.hyarpg.events;

// Hytale Imports
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;

public record Event_RemoveBlock(World world, int x, int y, int z, BlockType blockType) {}