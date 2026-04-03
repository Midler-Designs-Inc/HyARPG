package com.example.hyarpg.modules;

// Hytale Imports
import com.example.hyarpg.components.Component_CraftingKnowledge;
import com.example.hyarpg.configs.ModConfig;
import com.example.hyarpg.events.Event_RemoveBlock;
import com.example.hyarpg.utils.HookedNotificationHandler;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;

// Mod Imports
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.events.Event_PlaceBlock;
import com.example.hyarpg.utils.rooms.RoomData;
import com.example.hyarpg.utils.rooms.RoomFloodFill;
import com.example.hyarpg.events.Event_WorldStart;
import com.example.hyarpg.utils.rooms.RoomType;
import com.example.hyarpg.utils.rooms.TerritoryData;
import com.example.hyarpg.utils.rooms.WorldRoomRegistry;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Java Imports
import java.awt.*;
import java.util.*;
import java.util.logging.Level;

public class Module_BuildSystem {

    private static final String LIGHT_WELL_KEY = "Bench_Light_Well";

    public Module_BuildSystem() {
        ModEventBus.register(Event_WorldStart.class, this::onWorldStart);
        ModEventBus.register(Event_PlaceBlock.class, this::onPlaceBlock);
        ModEventBus.register(Event_RemoveBlock.class, this::onRemoveBlock);
    }

    private void onWorldStart(Event_WorldStart event) {
        World world = event.world();

        // Inject hooked notification handler via reflection
        try {
            java.lang.reflect.Field field = World.class.getDeclaredField("notificationHandler");
            field.setAccessible(true);
            field.set(world, new HookedNotificationHandler(world));
        } catch (Exception e) {
            HytaleLogger.getLogger().at(Level.WARNING).log("Failed to inject HookedNotificationHandler: %s", e.getMessage());
        }

        try {
            WorldRoomRegistry.load(world).thenAccept(registry -> {
                WorldRoomRegistry.put(world.getName(), registry);
                HytaleLogger.getLogger().at(Level.INFO).log(
                        "[BuildSystem] Initialized registry for world '%s' with %d rooms, %d territories",
                        world.getName(), registry.getAllRooms().size(), registry.getAllTerritories().size()
                );
            });
        } catch (Exception e) {
            HytaleLogger.getLogger().at(Level.WARNING).log("Build System on world start failed: %s", e.getMessage());
        }
    }

    // --- Place Block / Remove Block --- //
    private void onPlaceBlock(Event_PlaceBlock event) {
        try {
            if (!ModConfig.get().building.allow_light_well_territory_claim) return;

            ItemStack stack = event.event().getItemInHand();
            if (stack == null || stack.getBlockKey() == null) return;

            String blockKey = stack.getBlockKey();
            BlockType placedBlockType = BlockType.getAssetMap().getAsset(blockKey);
            if (placedBlockType == null) return;

            Vector3i pos = event.event().getTargetBlock();
            World world = event.commandBuffer().getExternalData().getWorld();
            PlayerRef playerRef = event.store().getComponent(event.ref(), PlayerRef.getComponentType());

            // --- Territory terminal ---
            if (blockKey.equals(LIGHT_WELL_KEY)) {
                onLightWellPlaced(event, world, pos, playerRef);
                return;
            }

            WorldRoomRegistry registry = WorldRoomRegistry.get(world);
            if (registry == null) return;

            // --- Build restriction: benches and beds require a territory ---
            if (requiresTerritory(placedBlockType)) {
                if (registry.getTerritoryAt(pos.x, pos.y, pos.z) == null) {
                    event.event().setCancelled(true);
                    playerRef.sendMessage(Message.raw("You must be inside a territory to place this item. Place a Light Well first to claim one.").color(Color.RED));
                    return;
                }
            }

            // --- Only run room logic if inside a territory ---
            if (registry.getTerritoryAt(pos.x, pos.y, pos.z) == null) return;

            boolean isStructural = RoomFloodFill.isStructural(placedBlockType);
            if (isStructural) onStructuralBlockPlaced(world, pos, placedBlockType, registry, event.ref());
            else onDecorationBlockPlaced(world, pos, placedBlockType, registry, event.ref());

        } catch (Exception e) {
            HytaleLogger.getLogger().at(Level.WARNING).log("onPlaceBlock failed: %s", e.getMessage());
        }
    }
    private void onRemoveBlock(Event_RemoveBlock event) {
        try {
            BlockType blockType = event.blockType();
            if (blockType == null) return;

            World world = event.world();
            Vector3i pos = new Vector3i(event.x(), event.y(), event.z());

            // --- Territory terminal ---
            if (LIGHT_WELL_KEY.equals(blockType.getId())) {
                onLightWellBroken(world, pos);
                return;
            }

            WorldRoomRegistry registry = WorldRoomRegistry.get(world);
            if (registry == null) return;

            if (registry.getTerritoryAt(pos.x, pos.y, pos.z) == null) return;

            boolean isStructural = RoomFloodFill.isStructural(blockType);
            if (isStructural) onStructuralBlockBroken(world, pos, blockType, registry);
            else onDecorationBlockBroken(world, pos, blockType, registry);

        } catch (Exception e) {
            HytaleLogger.getLogger().at(Level.WARNING).log("onRemoveBlock failed: %s", e.getMessage());
        }
    }

    // --- Territory Flow --- //
    private void onLightWellPlaced(Event_PlaceBlock event, World world, Vector3i pos, PlayerRef playerRef) {
        WorldRoomRegistry registry = WorldRoomRegistry.get(world);
        if (registry == null) return;

        UUID ownerUuid = playerRef.getUuid();

        boolean alreadyOwnsTerritory = registry.getAllTerritories().stream().anyMatch(t -> ownerUuid.equals(t.getOwnerUuid()));
        if (alreadyOwnsTerritory) {
            event.event().setCancelled(true);
            playerRef.sendMessage(Message.raw("You already have an active territory. Break your existing Light Well before placing a new one.").color(Color.RED));
            return;
        }

        if (registry.getTerritoryAt(pos.x, pos.y, pos.z) != null) return;

        TerritoryData candidate = new TerritoryData(pos, ownerUuid);
        boolean overlaps = registry.getAllTerritories().stream().anyMatch(existing ->
                candidate.getMaxX() >= existing.getMinX() && candidate.getMinX() <= existing.getMaxX() &&
                        candidate.getMaxY() >= existing.getMinY() && candidate.getMinY() <= existing.getMaxY() &&
                        candidate.getMaxZ() >= existing.getMinZ() && candidate.getMinZ() <= existing.getMaxZ()
        );
        if (overlaps) {
            event.event().setCancelled(true);
            playerRef.sendMessage(Message.raw("You cannot place a Light Well here — it would overlap with another player's territory.").color(Color.RED));
            return;
        }

        registry.addTerritory(candidate);
        registry.saveAsync(world);

        HytaleLogger.getLogger().at(Level.INFO).log(
                "[BuildSystem] Territory registered at (%d, %d, %d) by %s",
                pos.x, pos.y, pos.z, ownerUuid
        );
    }
    private void onLightWellBroken(World world, Vector3i pos) {
        WorldRoomRegistry registry = WorldRoomRegistry.get(world);
        if (registry == null) return;

        TerritoryData territory = null;
        for (int dy = 0; dy <= 2; dy++) {
            territory = registry.getTerritoryByLightWell(pos.x, pos.y - dy, pos.z);
            if (territory != null) break;
        }
        if (territory == null) return;

        registry.removeTerritory(territory);
        registry.saveAsync(world);

        HytaleLogger.getLogger().at(Level.INFO).log(
                "[BuildSystem] Territory removed at (%d, %d, %d), rooms de-registered",
                territory.getCenter().x, territory.getCenter().y, territory.getCenter().z
        );
    }

    // --- Structural Block Flow --- //
    private void onStructuralBlockPlaced(World world, Vector3i blockPos, BlockType placedBlockType, WorldRoomRegistry registry, Ref ref) {
        RoomData detected = RoomFloodFill.detectRoomFromPlacedBlock(world, blockPos, placedBlockType);
        RoomData existing = registry.getRoomAt(blockPos.x, blockPos.y, blockPos.z);

        if (detected != null && registry.findMatchingRoom(detected) == null) {
            if (existing != null) registry.removeRoom(existing);
            scanRoomContents(world, detected);
            detected.addBlockKey(placedBlockType.getId());
            registry.addRoom(detected);
            reevaluateRoomType(detected, registry, world, ref);
        } else if (detected != null && existing != null) {
            scanRoomContents(world, existing);
            existing.addBlockKey(placedBlockType.getId());
            reevaluateRoomType(existing, registry, world, ref);
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
            reevaluateRoomType(detected, registry, world, null);
        } else if (detected != null && existing != null) {
            scanRoomContents(world, existing);
            existing.removeBlockKey(removedBlockType.getId());
            reevaluateRoomType(existing, registry, world, null);
        } else if (detected == null && existing != null) {
            registry.removeRoom(existing);
            registry.saveAsync(world);
        }
    }

    // --- Decoration Block Flow --- //
    private void onDecorationBlockPlaced(World world, Vector3i blockPos, BlockType placedBlockType, WorldRoomRegistry registry, Ref ref) {
        RoomData room = registry.getRoomAt(blockPos.x, blockPos.y, blockPos.z);
        if (room != null) {
            scanRoomContents(world, room);
            room.addBlockKey(placedBlockType.getId());
            reevaluateRoomType(room, registry, world, ref);
        }
    }
    private void onDecorationBlockBroken(World world, Vector3i blockPos, BlockType removedBlockType, WorldRoomRegistry registry) {
        RoomData room = registry.getRoomAt(blockPos.x, blockPos.y, blockPos.z);
        if (room != null) {
            scanRoomContents(world, room);
            room.removeBlockKey(removedBlockType.getId());
            reevaluateRoomType(room, registry, world, null);
        }
    }

    // --- Helpers --- //
    private static boolean requiresTerritory(BlockType placedBlockType) {
        try {
            String[] categories = placedBlockType.getItem().getCategories();
            if (!ModConfig.get().building.allow_bed_placement_outside_territory && Arrays.stream(categories).anyMatch("Furniture.Beds"::equals))
                return true;
            if (!ModConfig.get().building.allow_bench_placement_outside_territory && Arrays.stream(categories).anyMatch("Furniture.Benches"::equals))
                return true;
        } catch (Exception e) {}
        return false;
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
    private void reevaluateRoomType(RoomData room, WorldRoomRegistry registry, World world, Ref ref) {
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

        // Only attempt recipe discovery if there's a new valid type and a player ref
        if (newTypeName == null || ref == null) return;
        try {
            Store<EntityStore> store = ref.getStore();
            Component_CraftingKnowledge knowledgeComp = store.getComponent(ref, Module_RPGSystem.componentTypeCraftingKnowledge);
            knowledgeComp.addDiscoveredRoomRecipe(ref, store, newType.name(), newTypeName);
        } catch (Exception e) {}
    }
}