package com.example.hyarpg.modules;

// Hytale Imports
import com.example.hyarpg.events.Event_WorldStart;
import com.example.hyarpg.utils.rooms.WorldRoomRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.protocol.DrawType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

// Mod Imports
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.events.Event_PlaceBlock;
import com.example.hyarpg.events.Event_BreakBlock;
import com.example.hyarpg.utils.rooms.RoomData;
import com.example.hyarpg.utils.rooms.RoomFloodFill;

// Java Imports
import java.util.logging.Level;


public class Module_RoomSystem {

    public Module_RoomSystem() {
        ModEventBus.register(Event_WorldStart.class, this::onWorldStart);
        ModEventBus.register(Event_PlaceBlock.class, this::onPlaceBlock);
        ModEventBus.register(Event_BreakBlock.class, this::onBreakBlock);
    }

    private void onWorldStart(Event_WorldStart event) {
        try {
            World world = event.world();
            WorldRoomRegistry.load(world).thenAccept(registry -> {
                WorldRoomRegistry.put(world.getName(), registry);
                HytaleLogger.getLogger().at(Level.INFO).log(
                        "[RoomSystem] Initialized registry for world '%s' with %d rooms",
                        world.getName(), registry.getAllRooms().size()
                );
            });
        } catch (Exception e) {
            HytaleLogger.getLogger().at(Level.WARNING).log("Room System on world start failed: %s", e.getMessage());
        }
    }

    private void onPlaceBlock(Event_PlaceBlock event) {
        try {
            ItemStack stack = event.event().getItemInHand();
            if (stack == null || stack.getBlockKey() == null) return;

            String blockKey = stack.getBlockKey();
            BlockType placedBlockType = BlockType.getAssetMap().getAsset(blockKey);
            if (placedBlockType == null) return;

            boolean isStructural = isStructural(placedBlockType);
            Vector3i pos = event.event().getTargetBlock();
            World world = event.commandBuffer().getExternalData().getWorld();

            // Pass the BlockType so the flood fill knows the full hitbox extent (e.g. doors)
            if (isStructural) onStructuralBlockPlaced(world, pos, placedBlockType);
            else onDecorationBlockPlaced(world, pos, blockKey);

        } catch (Exception e) {
            HytaleLogger.getLogger().at(Level.WARNING).log("onPlaceBlock failed: %s", e.getMessage());
        }
    }

    private void onBreakBlock(Event_BreakBlock event) {
        try {
            BlockType brokenBlockType = event.event().getBlockType();
            String blockKey = brokenBlockType.getId();
            boolean isStructural = isStructural(brokenBlockType);
            Vector3i pos = event.event().getTargetBlock();
            World world = event.commandBuffer().getExternalData().getWorld();

            if (isStructural) onStructuralBlockBroken(world, pos);
            else onDecorationBlockBroken(world, pos, blockKey);
        } catch (Exception e) {
            HytaleLogger.getLogger().at(Level.WARNING).log("onBreakBlock failed: %s", e.getMessage());
        }
    }

    // Determines if a block is structural (solid cube or door hitbox type)
    private static boolean isStructural(BlockType bt) {
        return bt != null
            && bt.getMaterial() == BlockMaterial.Solid
            && (
                bt.getDrawType() == DrawType.Cube
                || bt.getHitboxType().contains("Door")
                || bt.getHitboxType().contains("Window")
            );
    }

    private void onStructuralBlockPlaced(World world, Vector3i blockPos, BlockType placedBlockType) {
        WorldRoomRegistry registry = WorldRoomRegistry.get(world);
        if (registry == null) return;

        // Pass BlockType so flood fill can account for multi-block hitboxes (e.g. doors)
        RoomData detected = RoomFloodFill.detectRoomFromPlacedBlock(world, blockPos, placedBlockType);
        RoomData existing = registry.getRoomAt(blockPos.x, blockPos.y, blockPos.z);

        if (detected != null && registry.findMatchingRoom(detected) == null) {
            if (existing != null) registry.removeRoom(existing);
            registry.addRoom(detected);
            registry.saveAsync(world);
            for (PlayerRef player : Universe.get().getPlayers())
                player.sendMessage(Message.raw("Room created: " + detected.getDesignatedRoomType()));
        } else if (detected == null && existing != null) {
            registry.removeRoom(existing);
            registry.saveAsync(world);
            for (PlayerRef player : Universe.get().getPlayers())
                player.sendMessage(Message.raw("Room removed: " + existing.getDesignatedRoomType()));
        }
    }

    private void onStructuralBlockBroken(World world, Vector3i blockPos) {
        WorldRoomRegistry registry = WorldRoomRegistry.get(world);
        if (registry == null) return;

        RoomData detected = RoomFloodFill.detectRoomFromBrokenBlock(world, blockPos);
        RoomData existing = registry
            .getRoomsNear(blockPos.x, blockPos.y, blockPos.z)
            .stream().findFirst().orElse(null);

        if (detected != null && registry.findMatchingRoom(detected) == null) {
            if (existing != null) registry.removeRoom(existing);
            registry.addRoom(detected);
            registry.saveAsync(world);
        } else if (detected == null && existing != null) {
            registry.removeRoom(existing);
            registry.saveAsync(world);
        }
    }

    private void onDecorationBlockPlaced(World world, Vector3i blockPos, String blockKey) {
        WorldRoomRegistry registry = WorldRoomRegistry.get(world);
        if (registry == null) return;

        RoomData room = registry.getRoomAt(blockPos.x, blockPos.y, blockPos.z);
        if (room != null) {
            room.getBlockKeysInside().add(blockKey);
            // TODO: reevaluate room recipe
            registry.saveAsync(world);
        }
    }

    private void onDecorationBlockBroken(World world, Vector3i blockPos, String blockKey) {
        WorldRoomRegistry registry = WorldRoomRegistry.get(world);
        if (registry == null) return;

        RoomData room = registry.getRoomAt(blockPos.x, blockPos.y, blockPos.z);
        if (room != null) {
            room.getBlockKeysInside().remove(blockKey);
            // TODO: reevaluate room recipe
            registry.saveAsync(world);
        }
    }
}