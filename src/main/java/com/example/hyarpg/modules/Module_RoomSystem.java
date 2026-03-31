package com.example.hyarpg.modules;

// Hytale Imports
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.protocol.DrawType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;

// Mod Imports
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.events.Event_PlaceBlock;
import com.example.hyarpg.events.Event_BreakBlock;
import com.example.hyarpg.utils.rooms.RoomData;
import com.example.hyarpg.utils.rooms.RoomFloodFill;
import com.example.hyarpg.events.Event_WorldStart;
import com.example.hyarpg.utils.rooms.RoomType;
import com.example.hyarpg.utils.rooms.WorldRoomRegistry;

// Java Imports
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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

    // --- Event Handlers for Placing/Breaking --- //
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
            else onDecorationBlockPlaced(world, pos, placedBlockType);

        } catch (Exception e) {
            HytaleLogger.getLogger().at(Level.WARNING).log("onPlaceBlock failed: %s", e.getMessage());
        }
    }
    private void onBreakBlock(Event_BreakBlock event) {
        try {
            BlockType brokenBlockType = event.event().getBlockType();
            boolean isStructural = isStructural(brokenBlockType);
            Vector3i pos = event.event().getTargetBlock();
            World world = event.commandBuffer().getExternalData().getWorld();

            if (isStructural) onStructuralBlockBroken(world, pos, brokenBlockType);
            else onDecorationBlockBroken(world, pos, brokenBlockType);
        } catch (Exception e) {
            HytaleLogger.getLogger().at(Level.WARNING).log("onBreakBlock failed: %s", e.getMessage());
        }
    }

    // --- Structural Block Flow -- //
    private void onStructuralBlockPlaced(World world, Vector3i blockPos, BlockType placedBlockType) {
        WorldRoomRegistry registry = WorldRoomRegistry.get(world);
        if (registry == null) return;

        // Pass BlockType so flood fill can account for multi-block hitboxes (e.g. doors)
        RoomData detected = RoomFloodFill.detectRoomFromPlacedBlock(world, blockPos, placedBlockType);
        RoomData existing = registry.getRoomAt(blockPos.x, blockPos.y, blockPos.z);

        if (detected != null && registry.findMatchingRoom(detected) == null) {
            // New or resized room — scan interior contents then evaluate full designation
            if (existing != null) registry.removeRoom(existing);
            scanRoomContents(world, detected);
            detected.addBlockKey(placedBlockType.getId());
            registry.addRoom(detected);
            reevaluateRoomType(detected, registry, world);
        }
        else if (detected != null && existing != null) {
            // room unchanged structurally but a block was placed inside — update contents
            scanRoomContents(world, existing);
            existing.addBlockKey(placedBlockType.getId());
            reevaluateRoomType(existing, registry, world);
        }
        else if (detected == null && existing != null) {
            // No longer structurally a room — remove it
            registry.removeRoom(existing);
            registry.saveAsync(world);
        }
    }
    private void onStructuralBlockBroken(World world, Vector3i blockPos, BlockType removedBlockType) {
        WorldRoomRegistry registry = WorldRoomRegistry.get(world);
        if (registry == null) return;

        RoomData detected = RoomFloodFill.detectRoomFromBrokenBlock(world, blockPos);
        RoomData existing = registry.getRoomsNear(blockPos.x, blockPos.y, blockPos.z).stream().findFirst().orElse(null);

        if (detected != null && registry.findMatchingRoom(detected) == null) {
            // Room changed structurally — rescan and re-evaluate
            if (existing != null) registry.removeRoom(existing);
            scanRoomContents(world, detected);
            detected.removeBlockKey(removedBlockType.getId());
            registry.addRoom(detected);
            reevaluateRoomType(detected, registry, world);
        }
        else if (detected != null && existing != null) {
            // room unchanged structurally but a block was placed inside — update contents
            scanRoomContents(world, existing);
            existing.removeBlockKey(removedBlockType.getId());
            reevaluateRoomType(existing, registry, world);
        }
        else if (detected == null && existing != null) {
            // No longer structurally a room — remove it
            registry.removeRoom(existing);
            registry.saveAsync(world);
        }
    }

    // -- Decoration Block Flow -- //
    private void onDecorationBlockPlaced(World world, Vector3i blockPos, BlockType placedBlockType) {
        WorldRoomRegistry registry = WorldRoomRegistry.get(world);
        if (registry == null) return;

        RoomData room = registry.getRoomAt(blockPos.x, blockPos.y, blockPos.z);
        if (room != null) {
            scanRoomContents(world, room);
            room.addBlockKey(placedBlockType.getId());
            reevaluateRoomType(room, registry, world);
            for (PlayerRef player : Universe.get().getPlayers())
                player.sendMessage(Message.raw("Room contents were: " + room.getBlockCountsInside()));
        }
    }
    private void onDecorationBlockBroken(World world, Vector3i blockPos, BlockType removedBlockType) {
        WorldRoomRegistry registry = WorldRoomRegistry.get(world);
        if (registry == null) return;

        RoomData room = registry.getRoomAt(blockPos.x, blockPos.y, blockPos.z);
        if (room != null) {
            scanRoomContents(world, room);
            for (PlayerRef player : Universe.get().getPlayers()) player.sendMessage(Message.raw("Removing: " + removedBlockType.getId()));
            room.removeBlockKey(removedBlockType.getId());
            reevaluateRoomType(room, registry, world);
            for (PlayerRef player : Universe.get().getPlayers())
                player.sendMessage(Message.raw("Room contents were: " + room.getBlockCountsInside()));
        }
    }

    // Scans the interior of a newly detected room and populates blockKeysInside
    private void scanRoomContents(World world, RoomData room) {
        room.clearBlockKeys();

        // Accumulate raw block counts first
        Map<String, Integer> rawCounts = new HashMap<>();

        Vector3i min = room.getMinBound();
        Vector3i max = room.getMaxBound();

        for (int x = min.x; x <= max.x; x++) {
            for (int y = min.y; y <= max.y; y++) {
                for (int z = min.z; z <= max.z; z++) {
                    long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
                    WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);
                    if (chunk == null) continue;
                    int blockId = chunk.getBlock(x, y, z);
                    BlockType bt = BlockType.getAssetMap().getAsset(blockId);
                    if (bt != null) {
                        rawCounts.merge(bt.getId(), 1, Integer::sum);
                    }
                }
            }
        }

        // Normalize raw counts by block volume to get instance counts
        for (Map.Entry<String, Integer> entry : rawCounts.entrySet()) {
            String key = entry.getKey();
            int rawCount = entry.getValue();
            int instances = getInstanceCount(key, rawCount);
            for (int i = 0; i < instances; i++) room.addBlockKey(key);
        }
    }

    // get the true instance count of multi-block decorations
    private int getInstanceCount(String blockKey, int rawCount) {
        try {
            BlockType bt = BlockType.getAssetMap().getAsset(blockKey);
            if (bt == null) return rawCount;
            BlockBoundingBoxes hitbox = BlockBoundingBoxes.getAssetMap().getAsset(bt.getHitboxType());
            if (hitbox == null) return rawCount;
            BlockBoundingBoxes.RotatedVariantBoxes variant = hitbox.get(0);
            if (variant == null) return rawCount;
            double volume = variant.getBoundingBox().getVolume();
            if (volume <= 0) return rawCount;

            // If volume < 1.0 (e.g. torches, small decorations) each block is its own instance
            if (volume < 1.0) return rawCount;
            return (int) Math.round(rawCount / volume);
        } catch (Exception e) {
            return rawCount;
        }
    }

    // Classifies the room against size + decoration requirements, updates its designation
    private void reevaluateRoomType(RoomData room, WorldRoomRegistry registry, World world) {
        RoomType newType = RoomType.classify(
            room.getInteriorSizeX(),
            room.getInteriorSizeY(),
            room.getInteriorSizeZ(),
            room.getBlockCountsInside()
        );

        String newTypeName = newType != null ? newType.getDisplayName() : null;
        String oldTypeName = room.getDesignatedRoomType();

        if (Objects.equals(newTypeName, oldTypeName)) return;

        room.setDesignatedRoomType(newTypeName);
        registry.saveAsync(world);
    }

    // Determines if a block is structural (solid cube or specific exceptions identified by hitbox type)
    static boolean isStructural(BlockType bt) {
        return bt != null
                && bt.getMaterial() == BlockMaterial.Solid
                && (
                bt.getDrawType() == DrawType.Cube
                        || bt.getHitboxType().contains("Door")
                        || bt.getHitboxType().contains("Window")
        );
    }
}