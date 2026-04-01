package com.example.hyarpg.modules;

// Hytale Imports
import com.example.hyarpg.components.Component_CraftingKnowledge;
import com.example.hyarpg.configs.ModConfig;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.protocol.DrawType;
import com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Java Imports
import java.util.*;
import java.util.logging.Level;

public class Module_BuildSystem {

    private static final String LIGHT_WELL_KEY = "Bench_Light_Well";

    public Module_BuildSystem() {
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
            HytaleLogger.getLogger().at(Level.WARNING).log("Build System on world start failed: %s", e.getMessage());
        }
    }

    // --- Event Handlers for Placing/Breaking --- //
    private void onPlaceBlock(Event_PlaceBlock event) {
        try {
            // if the light well territory claim is disabled then we don't need to do any of this
            if (!ModConfig.get().building.allow_light_well_territory_claim) return;

            // get the item in the main hand that the player is trying to place
            ItemStack stack = event.event().getItemInHand();
            if (stack == null || stack.getBlockKey() == null) return;

            // get the block key and type from the mainhand item
            String blockKey = stack.getBlockKey();
            BlockType placedBlockType = BlockType.getAssetMap().getAsset(blockKey);
            if (placedBlockType == null) return;

            // get the targeted place position and the world it's in
            Vector3i pos = event.event().getTargetBlock();
            World world = event.commandBuffer().getExternalData().getWorld();

            // get the player that placed the block
            PlayerRef playerRef = event.store().getComponent(event.ref(), PlayerRef.getComponentType());

            // --- Territory terminal ---
            if (blockKey.equals(LIGHT_WELL_KEY)) {
                onLightWellPlaced(world, pos, playerRef.getUuid());
                return;
            }

            WorldRoomRegistry registry = WorldRoomRegistry.get(world);
            if (registry == null) return;

            // --- Build restriction: benches and beds require a territory ---
            if (requiresTerritory(placedBlockType)) {
                if (registry.getTerritoryAt(pos.x, pos.y, pos.z) == null) {
                    event.event().setCancelled(true);
                    return;
                }
            }

            // --- Only run room logic if inside a territory ---
            if (registry.getTerritoryAt(pos.x, pos.y, pos.z) == null) return;

            boolean isStructural = isStructural(placedBlockType);
            if (isStructural) onStructuralBlockPlaced(world, pos, placedBlockType, registry, event.ref());
            else onDecorationBlockPlaced(world, pos, placedBlockType, registry, event.ref());

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
            if (isStructural) onStructuralBlockBroken(world, pos, brokenBlockType, registry, event.ref());
            else onDecorationBlockBroken(world, pos, brokenBlockType, registry, event.ref());

        } catch (Exception e) {
            HytaleLogger.getLogger().at(Level.WARNING).log("onBreakBlock failed: %s", e.getMessage());
        }
    }

    // --- Territory Flow --- //
    private void onLightWellPlaced(World world, Vector3i pos, UUID ownerUuid) {
        WorldRoomRegistry registry = WorldRoomRegistry.get(world);
        if (registry == null) return;

        if (registry.getTerritoryAt(pos.x, pos.y, pos.z) != null) return;

        TerritoryData territory = new TerritoryData(pos, ownerUuid);
        registry.addTerritory(territory);
        registry.saveAsync(world);

        HytaleLogger.getLogger().at(Level.INFO).log(
                "[BuildSystem] Territory registered at (%d, %d, %d) by %s",
                pos.x, pos.y, pos.z, ownerUuid
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
    private void onStructuralBlockBroken(World world, Vector3i blockPos, BlockType removedBlockType, WorldRoomRegistry registry, Ref ref) {
        RoomData detected = RoomFloodFill.detectRoomFromBrokenBlock(world, blockPos);
        RoomData existing = registry.getRoomsNear(blockPos.x, blockPos.y, blockPos.z).stream().findFirst().orElse(null);

        if (detected != null && registry.findMatchingRoom(detected) == null) {
            if (existing != null) registry.removeRoom(existing);
            scanRoomContents(world, detected);
            detected.removeBlockKey(removedBlockType.getId());
            registry.addRoom(detected);
            reevaluateRoomType(detected, registry, world, ref);
        } else if (detected != null && existing != null) {
            scanRoomContents(world, existing);
            existing.removeBlockKey(removedBlockType.getId());
            reevaluateRoomType(existing, registry, world, ref);
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
    private void onDecorationBlockBroken(World world, Vector3i blockPos, BlockType removedBlockType, WorldRoomRegistry registry, Ref ref) {
        RoomData room = registry.getRoomAt(blockPos.x, blockPos.y, blockPos.z);
        if (room != null) {
            scanRoomContents(world, room);
            room.removeBlockKey(removedBlockType.getId());
            reevaluateRoomType(room, registry, world, ref);
        }
    }

    // Blocks that require being inside a territory to be placed
    private static boolean requiresTerritory(BlockType placedBlockType) {
        try {
            String[] categories = placedBlockType.getItem().getCategories();

            // check beds/bench logic
            if(!ModConfig.get().building.allow_bed_placement_outside_territory && Arrays.stream(categories).anyMatch("Furniture.Beds"::equals))
                return true;
            if(!ModConfig.get().building.allow_bench_placement_outside_territory && Arrays.stream(categories).anyMatch("Furniture.Benches"::equals))
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
        // classify the room
        RoomType newType = RoomType.classify(
            room.getInteriorSizeX(),
            room.getInteriorSizeY(),
            room.getInteriorSizeZ(),
            room.getBlockCountsInside()
        );

        // get the room name from the fresh scan or bail
        String newTypeName = newType != null ? newType.getDisplayName() : null;
        if(newTypeName == null) return;

        // try to register the new room type with the placing player
        try {
            Store<EntityStore> store = ref.getStore();
            Component_CraftingKnowledge knowledgeComp = store.getComponent(ref, Module_RPGSystem.componentTypeCraftingKnowledge);
            knowledgeComp.addDiscoveredRoomRecipe(ref, ref.getStore(), newType.name(), newTypeName);
        } catch (Exception e) {}

        // set the room type if applicable
        String oldTypeName = room.getDesignatedRoomType();
        if (!Objects.equals(newTypeName, oldTypeName)) room.setDesignatedRoomType(newTypeName);

        // save the registry either way
        registry.saveAsync(world);
    }

    static boolean isStructural(BlockType bt) {
        if (bt == null) return false;
        if (bt.getMaterial() != BlockMaterial.Solid) return false;

        if (bt.getDrawType() == DrawType.Cube) return true;

        String hitboxType = bt.getHitboxType();
        if (hitboxType != null && (hitboxType.contains("Door") || hitboxType.contains("Window"))) return true;

        // Category check for things like trapdoors that aren't caught by hitbox name
        Item item = bt.getItem();
        if (item == null) return false;
        String[] categories = item.getCategories();
        if (categories == null) return false;
        return Arrays.stream(categories).anyMatch("Furniture.Doors"::equals);
    }
}