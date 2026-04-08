package com.example.hyarpg.modules;

// Hytale Imports
import com.example.hyarpg.components.Component_CraftingKnowledge;
import com.example.hyarpg.configs.ModConfig;
import com.example.hyarpg.events.Event_RemoveBlock;
import com.example.hyarpg.utils.HookedNotificationHandler;
import com.example.hyarpg.utils.outdoor_rooms.OutdoorRoomData;
import com.example.hyarpg.utils.outdoor_rooms.OutdoorRoomFloodFill;
import com.example.hyarpg.utils.outdoor_rooms.OutdoorRoomType;
import com.hypixel.hytale.builtin.crafting.component.BenchBlock;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
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
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Java Imports
import java.awt.*;
import java.util.*;
import java.util.List;
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
                        "[BuildSystem] Initialized registry for world '%s' with %d rooms, %d outdoor rooms, %d territories",
                        world.getName(), registry.getAllRooms().size(), registry.getAllOutdoorRooms().size(), registry.getAllTerritories().size()
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

            // --- Skip entirely if not in a territory and no existing room covers this position ---
            boolean inTerritory = registry.getTerritoryAt(pos.x, pos.y, pos.z) != null;
            boolean inExistingRoom = !registry.getRoomsNear(pos.x, pos.y, pos.z).isEmpty();
            boolean inExistingOutdoorRoom = !registry.getOutdoorRoomsNear(pos.x, pos.y, pos.z).isEmpty();
            if (!inTerritory && !inExistingRoom && !inExistingOutdoorRoom) return;

            // get structural flag for indoor rooms and boundary check for outdoor rooms
            boolean isStructural = RoomFloodFill.isStructural(placedBlockType);
            boolean isBoundary = OutdoorRoomFloodFill.isBoundary(placedBlockType);

            // --- Indoor room flow: always run if in territory, or if existing room covers this position ---
            if (inTerritory || inExistingRoom) {
                if (isStructural) onStructuralBlockPlaced(world, pos, placedBlockType, registry, event.ref());
                else onDecorationBlockPlaced(world, pos, placedBlockType, registry, event.ref());
            }

            // --- Outdoor space flow: always run if in territory, or if existing outdoor room covers this position ---
//            if (inTerritory || inExistingOutdoorRoom) {
//                if (isBoundary) onBoundaryBlockPlaced(world, pos, placedBlockType, registry, event.ref());
//                else onOutdoorDecorationBlockPlaced(world, pos, placedBlockType, registry, event.ref());
//            }
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

            // --- Skip entirely if not in a territory and no existing room covers this position ---
            boolean inTerritory = registry.getTerritoryAt(pos.x, pos.y, pos.z) != null;
            boolean inExistingRoom = !registry.getRoomsNear(pos.x, pos.y, pos.z).isEmpty();
            boolean inExistingOutdoorRoom = !registry.getOutdoorRoomsNear(pos.x, pos.y, pos.z).isEmpty();
            if (!inTerritory && !inExistingRoom && !inExistingOutdoorRoom) return;

            boolean isStructural = RoomFloodFill.isStructural(blockType);
            boolean isBoundary = OutdoorRoomFloodFill.isBoundary(blockType);

            // --- Indoor room flow: always run if in territory, or if existing room covers this position ---
            if (inTerritory || inExistingRoom) {
                if (isStructural) onStructuralBlockBroken(world, pos, blockType, registry);
                else onDecorationBlockBroken(world, pos, blockType, registry);
            }

            // --- Outdoor space flow: always run if in territory, or if existing outdoor room covers this position ---
//            if (inTerritory || inExistingOutdoorRoom) {
//                if (isBoundary) onBoundaryBlockBroken(world, pos, registry);
//                else onOutdoorDecorationBlockBroken(world, pos, registry);
//            }

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

        final TerritoryData finalTerritory = territory;

        // Defer block breaking to next tick to avoid calling store methods
        // from within the current event processing pipeline
        world.execute(() -> {
            breakRestrictedBlocksInTerritory(world, finalTerritory);
            registry.removeTerritory(finalTerritory);
            registry.saveAsync(world);

            HytaleLogger.getLogger().at(Level.INFO).log(
                    "[BuildSystem] Territory removed at (%d, %d, %d), rooms de-registered",
                    finalTerritory.getCenter().x, finalTerritory.getCenter().y, finalTerritory.getCenter().z
            );
        });
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
            reevaluateRoomType(room, registry, world, null);
        }
    }

    // --- Boundary Block Flow --- //
    private void onBoundaryBlockPlaced(World world, Vector3i blockPos, BlockType placedBlockType, WorldRoomRegistry registry, Ref ref) {
        List<OutdoorRoomData> detected = OutdoorRoomFloodFill.detectOutdoorSpacesFromPlacedBlock(world, blockPos, placedBlockType);

        // Register or update each detected space
        for (OutdoorRoomData space : detected) {
            OutdoorRoomData existing = registry.findMatchingOutdoorRoom(space);
            if (existing != null) {
                // Existing room — increment the placed block key and reevaluate, no full rescan needed
                existing.addBlockKey(placedBlockType.getId());
                reevaluateOutdoorRoomType(existing, registry, world, ref);
            } else {
                // New room — full scan to establish baseline counts, then register
                OutdoorRoomFloodFill.scanOutdoorContents(world, space);
                space.addBlockKey(placedBlockType.getId());
                registry.addOutdoorRoom(space);
                reevaluateOutdoorRoomType(space, registry, world, ref);
            }
        }

        // Remove any previously registered spaces near this block that are no longer valid
        List<OutdoorRoomData> nearby = registry.getOutdoorRoomsNear(blockPos.x, blockPos.y, blockPos.z);
        for (OutdoorRoomData room : new ArrayList<>(nearby)) {
            boolean stillValid = detected.stream().anyMatch(d ->
                    d.getCenterX() == room.getCenterX() &&
                            d.getCenterZ() == room.getCenterZ() &&
                            d.getInteriorSizeX() == room.getInteriorSizeX() &&
                            d.getInteriorSizeZ() == room.getInteriorSizeZ()
            );
            if (!stillValid) {
                registry.removeOutdoorRoom(room);
                registry.saveAsync(world);
            }
        }
    }
    private void onBoundaryBlockBroken(World world, Vector3i blockPos, WorldRoomRegistry registry) {
        List<OutdoorRoomData> detected = OutdoorRoomFloodFill.detectOutdoorSpacesFromBrokenBlock(world, blockPos);

        // Register or update each detected space
        for (OutdoorRoomData space : detected) {
            OutdoorRoomData existing = registry.findMatchingOutdoorRoom(space);
            if (existing != null) {
                // Existing room — reevaluate as-is, block counts already reflect the removal
                reevaluateOutdoorRoomType(existing, registry, world, null);
            } else {
                // New room detected after a break — full scan to establish baseline counts, then register
                OutdoorRoomFloodFill.scanOutdoorContents(world, space);
                registry.addOutdoorRoom(space);
                reevaluateOutdoorRoomType(space, registry, world, null);
            }
        }

        // Remove any previously registered spaces near this block that are no longer valid
        List<OutdoorRoomData> nearby = registry.getOutdoorRoomsNear(blockPos.x, blockPos.y, blockPos.z);
        for (OutdoorRoomData room : new ArrayList<>(nearby)) {
            boolean stillValid = detected.stream().anyMatch(d ->
                    d.getCenterX() == room.getCenterX() &&
                            d.getCenterZ() == room.getCenterZ() &&
                            d.getInteriorSizeX() == room.getInteriorSizeX() &&
                            d.getInteriorSizeZ() == room.getInteriorSizeZ()
            );
            if (!stillValid) {
                registry.removeOutdoorRoom(room);
                registry.saveAsync(world);
            }
        }
    }

    // --- Outdoor Decoration Block Flow --- //
    private void onOutdoorDecorationBlockPlaced(World world, Vector3i blockPos, BlockType placedBlockType, WorldRoomRegistry registry, Ref ref) {
        // Also rescan any existing space this block falls inside for decoration counts
        OutdoorRoomData room = registry.getOutdoorRoomAt(blockPos.x, blockPos.y, blockPos.z);
        if (room != null) {
            OutdoorRoomFloodFill.scanOutdoorContents(world, room);
            room.addBlockKey(placedBlockType.getId());
            reevaluateOutdoorRoomType(room, registry, world, ref);
        }
    }
    private void onOutdoorDecorationBlockBroken(World world, Vector3i blockPos, WorldRoomRegistry registry) {
        // Also rescan any existing space this block falls inside for decoration counts
        OutdoorRoomData room = registry.getOutdoorRoomAt(blockPos.x, blockPos.y, blockPos.z);
        if (room != null) {
            OutdoorRoomFloodFill.scanOutdoorContents(world, room);
            reevaluateOutdoorRoomType(room, registry, world, null);
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
        if (newType == null) return;
        room.setDesignatedRoomType(newTypeName);
        registry.saveAsync(world);

        // Only attempt recipe discovery if there's a new valid type and a player ref
        if (ref == null) return;
        try {
            Store<EntityStore> store = ref.getStore();
            Component_CraftingKnowledge knowledgeComp = store.getComponent(ref, Module_RPGSystem.componentTypeCraftingKnowledge);
            knowledgeComp.addDiscoveredRoomRecipe(ref, store, newType.name(), newTypeName);
        } catch (Exception e) {}
    }
    private void reevaluateOutdoorRoomType(OutdoorRoomData room, WorldRoomRegistry registry, World world, Ref ref) {
        OutdoorRoomType newType = OutdoorRoomType.classify(
                room.getInteriorSizeX(),
                room.getInteriorSizeZ(),
                room.getBlockCountsInside()
        );

        String newTypeName = newType != null ? newType.getDisplayName() : null;
        if (newType == null) return;
        room.setDesignatedRoomType(newTypeName);
        registry.saveAsync(world);

        // Only attempt recipe discovery if there's a new valid type and a player ref
        if (ref == null) return;
        try {
            Store<EntityStore> store = ref.getStore();
            Component_CraftingKnowledge knowledgeComp = store.getComponent(ref, Module_RPGSystem.componentTypeCraftingKnowledge);
            knowledgeComp.addDiscoveredRoomRecipe(ref, store, newType.name(), newTypeName);
        } catch (Exception e) {}
    }
    private void breakRestrictedBlocksInTerritory(World world, TerritoryData territory) {
        Store<EntityStore> entityStore = world.getEntityStore().getStore();

        for (int x = territory.getMinX(); x <= territory.getMaxX(); x++) {
            for (int y = territory.getMinY(); y <= territory.getMaxY(); y++) {
                for (int z = territory.getMinZ(); z <= territory.getMaxZ(); z++) {
                    try {
                        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
                        WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);
                        if (chunk == null) continue;

                        int blockId = chunk.getBlock(x, y, z);
                        BlockType bt = BlockType.getAssetMap().getAsset(blockId);
                        if (bt == null || !requiresTerritory(bt)) continue;
                        if (LIGHT_WELL_KEY.equals(bt.getId())) continue;

                        // Determine the item key to drop — use base block type to strip tier state
                        BlockType baseType = BenchBlock.getBaseBlockType(bt);
                        String dropKey = baseType.getId();

                        // Check for a bench tier component to drop the correct tiered item
                        // BenchBlock tierLevel is 1-based; tier 1 = base block key, tier 2+ = key with tier state
                        try {
                            Ref<ChunkStore> chunkRef = ((ChunkStore) world.getChunkStore().getStore().getExternalData()).getChunkReference(chunkIndex);
                            if (chunkRef != null && chunkRef.isValid()) {
                                BenchBlock benchBlock = world.getChunkStore().getStore().getComponent(chunkRef, BenchBlock.getComponentType());
                                if (benchBlock != null && benchBlock.getTierLevel() > 1) {
                                    // Tier 2+ benches use a state key — getTierStateName() returns e.g. "Tier2"
                                    String tieredKey = bt.getBlockKeyForState(benchBlock.getTierStateName());
                                    if (tieredKey != null) dropKey = tieredKey;
                                }
                            }
                        } catch (Exception ignored) {}

                        // Spawn the item drop at block center
                        Vector3d dropPos = new Vector3d(x + 0.5, y + 0.5, z + 0.5);
                        Holder<EntityStore> holder = ItemComponent.generateItemDrop(entityStore, new ItemStack(dropKey, 1), dropPos, Vector3f.ZERO, 0f, 0f, 0f);
                        if (holder != null) {
                            entityStore.addEntities(new Holder[]{holder}, AddReason.SPAWN);
                        }

                        world.breakBlock(x, y, z, 0);
                    } catch (Exception e) {
                        for (PlayerRef player : Universe.get().getPlayers()) {
                            player.sendMessage(Message.raw(e.getMessage()));
                        }
                        HytaleLogger.getLogger().at(Level.WARNING).log(
                                "[BuildSystem] Failed to eject restricted block at (%d, %d, %d): %s", x, y, z, e.getMessage()
                        );
                    }
                }
            }
        }
    }
}