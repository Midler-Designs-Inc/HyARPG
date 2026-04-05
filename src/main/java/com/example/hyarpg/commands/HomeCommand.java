package com.example.hyarpg.commands;

import com.example.hyarpg.utils.rooms.RoomFloodFill;
import com.example.hyarpg.utils.rooms.TerritoryData;
import com.example.hyarpg.utils.rooms.WorldRoomRegistry;
import com.hypixel.hytale.builtin.teleport.components.TeleportHistory;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class HomeCommand extends CommandBase {

    // How far around the light well to search for safe ground
    private static final int SEARCH_RADIUS = 3;

    // How many blocks above the light well top to start scanning downward
    private static final int SCAN_HEIGHT_OFFSET = 5;

    public HomeCommand() {
        super("home", "Teleport to your territory's light well.", false);
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        commandContext.senderAs(Player.class).getWorld().execute(() -> {
            Player player = commandContext.senderAs(Player.class);
            World world = player.getWorld();
            Ref<EntityStore> ref = player.getReference();
            Store<EntityStore> store = ref.getStore();

            // Get the player's UUID to find their territory
            UUIDComponent uuidComponent = (UUIDComponent) store.getComponent(ref, UUIDComponent.getComponentType());
            if (uuidComponent == null) {
                player.sendMessage(Message.raw("Could not determine your player identity."));
                return;
            }
            UUID playerUuid = uuidComponent.getUuid();

            // Find their territory
            WorldRoomRegistry registry = WorldRoomRegistry.get(world);
            if (registry == null) {
                player.sendMessage(Message.raw("No territory data loaded for this world."));
                return;
            }

            TerritoryData territory = null;
            for (TerritoryData t : registry.getAllTerritories()) {
                if (playerUuid.equals(t.getOwnerUuid())) {
                    territory = t;
                    break;
                }
            }

            if (territory == null) {
                player.sendMessage(Message.raw("You don't have a territory yet."));
                return;
            }

            // Light well is 1x3x1 — center is the base block, top is centerY + 2
            // We want to land just beside it, so search around centerX/Z at centerY + 3
            Vector3i lightWellBase = territory.getCenter();
            int searchStartY = lightWellBase.y + 2 + SCAN_HEIGHT_OFFSET;

            Vector3d safePos = findSafePositionNearLightWell(world, lightWellBase.x, searchStartY, lightWellBase.z);
            if (safePos == null) {
                // Fallback: stand on top of the light well itself
                safePos = new Vector3d(lightWellBase.x + 0.5, lightWellBase.y + 3.0, lightWellBase.z + 0.5);
            }

            // Save to teleport history so /tp back works
            TransformComponent transform = (TransformComponent) store.getComponent(ref, TransformComponent.getComponentType());
            HeadRotation headRotation = (HeadRotation) store.getComponent(ref, HeadRotation.getComponentType());
            if (transform != null && headRotation != null) {
                Vector3d previousPos = transform.getPosition().clone();
                Vector3f previousHeadRotation = headRotation.getRotation().clone();
                TeleportHistory history = (TeleportHistory) store.ensureAndGetComponent(ref, TeleportHistory.getComponentType());
                history.append(world, previousPos, previousHeadRotation, "Home");
            }

            // Perform the teleport
            final Vector3d destination = safePos;
            Teleport teleportComponent = Teleport.createForPlayer((World) null, destination, new Vector3f(0, 0, 0));
            store.addComponent(ref, Teleport.getComponentType(), teleportComponent);
        });
    }

    /**
     * Searches in a small ring around the light well base for a safe ground position.
     * Skips the light well column itself (x == cx && z == cz) since that's occupied.
     */
    @Nullable
    private Vector3d findSafePositionNearLightWell(World world, int cx, int startY, int cz) {
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                // Skip the light well column itself
                if (dx == 0 && dz == 0) continue;

                int scanX = cx + dx;
                int scanZ = cz + dz;
                int groundY = findGroundY(world, scanX, startY, scanZ);
                if (groundY != Integer.MIN_VALUE) {
                    return new Vector3d(scanX + 0.5, groundY + 1.0, scanZ + 0.5);
                }
            }
        }
        return null;
    }

    /**
     * Scans downward from startY and returns the Y of the first solid block, or Integer.MIN_VALUE if none found.
     */
    private int findGroundY(World world, int x, int startY, int z) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);
        if (chunk == null) return Integer.MIN_VALUE;

        int clampedStart = Math.min(startY, 319);
        for (int y = clampedStart; y >= 1; y--) {
            int blockId = chunk.getBlock(x, y, z);
            BlockType bt = BlockType.getAssetMap().getAsset(blockId);
            if (bt != null && RoomFloodFill.isStructural(bt)) return y;
        }
        return Integer.MIN_VALUE;
    }
}