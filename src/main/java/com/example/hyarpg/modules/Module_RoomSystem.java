package com.example.hyarpg.modules;

// Hytale Imports
import com.hypixel.hytale.logger.HytaleLogger;
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
import com.example.hyarpg.utils.rooms.TerritoryData;
import com.example.hyarpg.utils.rooms.WorldRoomRegistry;

// Java Imports
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

public class Module_RoomSystem {

    private static final String LIGHT_WELL_KEY = "Bench_Light_Well";

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
                        "[RoomSystem] Initialized registry for world '%s' with %d rooms, %d territories",
                        world.getName(), registry.getAllRooms().size(), registry.getAllTerritories().size()
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

            Vector3i pos = event.event().getTargetBlock();
            World world = event.commandBuffer().getExternalData().getWorld();

            // --- Territory terminal ---
            if (blockKey.equals(LIGHT_WELL_KEY)) {
                onLightWellPlaced(world, pos);
                return;
            }

            WorldRoomRegistry registry = WorldRoomRegistry.get(world);
            if (registry == null) return;

            // --- Build restriction: benches and beds require a territory ---
            if (requiresTerritory(blockKey)) {
                if (registry.getTerritoryAt(pos.x, pos.y, pos.z) == null) {
                    event.event().setCancelled(true);
                    return;
                }
            }

            // --- Only run room logic if inside a territory ---
            if (registry.getTerritoryAt(pos.x, pos.y, pos.z) == null) return;

            boolean isStructural = isStructural(placedBlockType);
            if (isStructural) onStructuralBlockPlaced(world, pos, placedBlockType, registry);
            else onDecorationBlockPlaced(world, pos, placedBlockType, registry);

        } catch (Exception e) {
            HytaleLogger.getLogger().at(Level.WARNING).log("onPlaceBlock failed: %s", e.getMessage());
        }
    }
    private void onBreakBlock(Event_BreakBlock event) {
        try {
            BlockType brokenBlockType = event.event().getBlockType();
            Vector3i pos = event.event().getTargetBlock();
            World world = event.commandBuffer().getExternalData().getWorld();

            // --- Territory terminal ---
            if (brokenBlockType.getId().equals(LIGHT_WELL_KEY)) {
                onLightWellBroken(world, pos);
                return;
            }

            WorldRoomRegistry registry = WorldRoomRegistry.get(world);
            if (registry == null) return;

            // --- Only run room logic if inside a territory ---
            if (registry.getTerritoryAt(pos.x, pos.y, pos.z) == null) return;

            boolean isStructural = isStructural(brokenBlockType);
            if (isStructural) onStructuralBlockBroken(world, pos, brokenBlockType, registry);
            else onDecorationBlockBroken(world, pos, brokenBlockType, registry);

        } catch (Exception e) {
            HytaleLogger.getLogger().at(Level.WARNING).log("onBreakBlock failed: %s", e.getMessage());
        }
    }

    // --- Territory Flow --- //
    private void onLightWellPlaced(World world, Vector3i pos) {
        WorldRoomRegistry registry = WorldRoomRegistry.get(world);
        if (registry == null) return;

        // Prevent overlapping territories
        if (registry.getTerritoryAt(pos.x, pos.y, pos.z) != null) return;

        TerritoryData territory = new TerritoryData(pos);
        registry.addTerritory(territory);
        registry.saveAsync(world);

        HytaleLogger.getLogger().at(Level.INFO).log(
                "[RoomSystem] Territory registered at (%d, %d, %d)", pos.x, pos.y, pos.z
        );
    }
    private void onLightWellBroken(World world, Vector3i pos) {
        WorldRoomRegistry registry = WorldRoomRegistry.get(world);
        if (registry == null) return;

        // The light well is 1x3x1 — find the territory whose center matches any of those 3 blocks
        TerritoryData territory = null;
        for (int dy = 0; dy <= 2; dy++) {
            territory = registry.getTerritoryByLightWell(pos.x, pos.y - dy, pos.z);
            if (territory != null) break;
        }
        if (territory == null) return;

//        registry.removeRoomsInTerritory(territory);
        registry.removeTerritory(territory);
        registry.saveAsync(world);

        HytaleLogger.getLogger().at(Level.INFO).log(
                "[RoomSystem] Territory removed at (%d, %d, %d), rooms de-registered",
                territory.getCenter().x, territory.getCenter().y, territory.getCenter().z
        );
    }

    // --- Structural Block Flow --- //
    private void onStructuralBlockPlaced(World world, Vector3i blockPos, BlockType placedBlockType, WorldRoomRegistry registry) {
        RoomData detected = RoomFloodFill.detectRoomFromPlacedBlock(world, blockPos, placedBlockType);
        RoomData existing = registry.getRoomAt(blockPos.x, blockPos.y, blockPos.z);

        if (detected != null && registry.findMatchingRoom(detected) == null) {
            if (existing != null) registry.removeRoom(existing);
            scanRoomContents(world, detected);
            detected.addBlockKey(placedBlockType.getId());
            registry.addRoom(detected);
            reevaluateRoomType(detected, registry, world);
        } else if (detected != null && existing != null) {
            scanRoomContents(world, existing);
            existing.addBlockKey(placedBlockType.getId());
            reevaluateRoomType(existing, registry, world);
        } else if (detected == null && existing != null) {
            registry.removeRoom(existing);
            registry.saveAsync(world);
        }
    }
    private void onStructuralBlockBroken(World world, Vector3i blockPos, BlockType removedBlockType, WorldRoomRegistry registry) {
        RoomData detected = RoomFloodFill.detectRoomFromBrokenBlock(world, blockPos);
        RoomData existing = registry.getRoomsNear(blockPos.x, blockPos.y, blockPos.z).stream().findFirst().orElse(null);

        if (detected != null && registry.findMatchingRoom(detected) == null) {
            if (existing != null) registry.removeRoom(existing);
            scanRoomContents(world, detected);
            detected.removeBlockKey(removedBlockType.getId());
            registry.addRoom(detected);
            reevaluateRoomType(detected, registry, world);
        } else if (detected != null && existing != null) {
            scanRoomContents(world, existing);
            existing.removeBlockKey(removedBlockType.getId());
            reevaluateRoomType(existing, registry, world);
        } else if (detected == null && existing != null) {
            registry.removeRoom(existing);
            registry.saveAsync(world);
        }
    }

    // --- Decoration Block Flow --- //
    private void onDecorationBlockPlaced(World world, Vector3i blockPos, BlockType placedBlockType, WorldRoomRegistry registry) {
        RoomData room = registry.getRoomAt(blockPos.x, blockPos.y, blockPos.z);
        if (room != null) {
            scanRoomContents(world, room);
            room.addBlockKey(placedBlockType.getId());
            reevaluateRoomType(room, registry, world);
            for (PlayerRef player : Universe.get().getPlayers())
                player.sendMessage(Message.raw("Room contents were: " + room.getBlockCountsInside()));
        }
    }
    private void onDecorationBlockBroken(World world, Vector3i blockPos, BlockType removedBlockType, WorldRoomRegistry registry) {
        RoomData room = registry.getRoomAt(blockPos.x, blockPos.y, blockPos.z);
        if (room != null) {
            scanRoomContents(world, room);
            for (PlayerRef player : Universe.get().getPlayers())
                player.sendMessage(Message.raw("Removing: " + removedBlockType.getId()));
            room.removeBlockKey(removedBlockType.getId());
            reevaluateRoomType(room, registry, world);
            for (PlayerRef player : Universe.get().getPlayers())
                player.sendMessage(Message.raw("Room contents were: " + room.getBlockCountsInside()));
        }
    }

    // Blocks that require being inside a territory to be placed
    private static boolean requiresTerritory(String blockKey) {
        return blockKey.startsWith("Bench_") || blockKey.contains("Bed");
    }

    private void scanRoomContents(World world, RoomData room) {
        room.clearBlockKeys();
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
                    if (bt != null) rawCounts.merge(bt.getId(), 1, Integer::sum);
                }
            }
        }

        for (Map.Entry<String, Integer> entry : rawCounts.entrySet()) {
            String key = entry.getKey();
            int instances = getInstanceCount(key, entry.getValue());
            for (int i = 0; i < instances; i++) room.addBlockKey(key);
        }
    }

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
            if (volume < 1.0) return rawCount;
            return (int) Math.round(rawCount / volume);
        } catch (Exception e) {
            return rawCount;
        }
    }

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