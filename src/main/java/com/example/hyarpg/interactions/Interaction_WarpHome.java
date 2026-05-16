package com.example.hyarpg.interactions;

// Hytale Imports
import com.hypixel.hytale.builtin.teleport.components.TeleportHistory;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.utils.rooms.RoomFloodFill;
import com.example.hyarpg.utils.rooms.TerritoryData;
import com.example.hyarpg.utils.rooms.WorldRoomRegistry;

// Java Imports
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import javax.annotation.Nullable;
import java.util.UUID;
import org.joml.Vector3d;
import org.joml.Vector3i;

public class Interaction_WarpHome extends SimpleInstantInteraction {

    public static final BuilderCodec<Interaction_WarpHome> CODEC = BuilderCodec.builder(Interaction_WarpHome.class, Interaction_WarpHome::new, SimpleInstantInteraction.CODEC).build();

    // How far around the light well to search for safe ground
    private static final int SEARCH_RADIUS = 3;

    @Override
    protected void firstRun(@NonNullDecl InteractionType interactionType, @NonNullDecl InteractionContext context, @NonNullDecl CooldownHandler cooldownHandler) {
        final Ref<EntityStore> ref = context.getEntity();
        final Store<EntityStore> store = ref.getStore();
        final World world = store.getExternalData().getWorld();

        // Get the player's UUID to find their territory
        UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (uuidComponent == null || playerRef == null) return;

        // Get the world room registry
        WorldRoomRegistry registry = WorldRoomRegistry.get(world);
        if (registry == null) {
            playerRef.sendMessage(Message.raw("No territory data loaded for this world."));
            return;
        }

        // Find the player's territory
        UUID playerUuid = uuidComponent.getUuid();
        TerritoryData territory = null;
        for (TerritoryData t : registry.getAllTerritories()) {
            if (playerUuid.equals(t.getOwnerUuid()) || t.isCoOwner(playerUuid)) {
                territory = t;
                break;
            }
        }

        if (territory == null) {
            playerRef.sendMessage(Message.raw("You don't have a territory yet."));
            return;
        }

        // plan search start based on lightwell position
        Vector3i lightWellBase = territory.getCenter();
        int searchStartY = lightWellBase.y + 3;

        Vector3d safePos = findSafePositionNearLightWell(world, lightWellBase.x, searchStartY, lightWellBase.z);
        if (safePos == null) {
            // Fallback: stand on top of the light well itself
            safePos = new Vector3d(lightWellBase.x + 0.5, lightWellBase.y + 3.0, lightWellBase.z + 0.5);
        }

        // Save to teleport history so /tp back works
        final Vector3d destination = safePos;
        world.execute(() -> {
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            HeadRotation headRotation = store.getComponent(ref, HeadRotation.getComponentType());
            if (transform != null && headRotation != null) {
                Vector3d previousPos = new Vector3d(transform.getPosition());
                Rotation3f previousHeadRotation = headRotation.getRotation();
                TeleportHistory history = store.ensureAndGetComponent(ref, TeleportHistory.getComponentType());
                history.append(world, previousPos, previousHeadRotation, "Home");
            }

            // Perform the teleport
            Teleport teleportComponent = Teleport.createForPlayer(null, destination, new Rotation3f(0, 0, 0));
            store.addComponent(ref, Teleport.getComponentType(), teleportComponent);
        });
    }

    @Nullable
    private Vector3d findSafePositionNearLightWell(World world, int cx, int lightWellTopY, int cz) {
        // check the 8 immediately adjacent positions first, then expand outward
        for (int radius = 1; radius <= SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    // only check the ring at this radius
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) continue;

                    int scanX = cx + dx;
                    int scanZ = cz + dz;

                    // start scanning from light well top level — not above it
                    int groundY = findGroundY(world, scanX, lightWellTopY, scanZ);
                    if (groundY != Integer.MIN_VALUE) {
                        return new Vector3d(scanX + 0.5, groundY + 1.0, scanZ + 0.5);
                    }
                }
            }
        }
        return null;
    }

    private int findGroundY(World world, int x, int startY, int z) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);
        if (chunk == null) return Integer.MIN_VALUE;

        int clampedStart = Math.min(startY, 319);
        for (int y = clampedStart; y >= 1; y--) {
            int blockId = chunk.getBlock(x, y, z);
            BlockType bt = BlockType.getAssetMap().getAsset(blockId);
            if (bt == null || !RoomFloodFill.isStructural(bt)) continue;

            // found a solid block — verify 3 air blocks above it for the player to stand in
            boolean hasAirGap = true;
            for (int airY = y + 1; airY <= y + 3; airY++) {
                int airBlockId = chunk.getBlock(x, airY, z);
                BlockType airBt = BlockType.getAssetMap().getAsset(airBlockId);
                if (airBt != null && RoomFloodFill.isStructural(airBt)) { hasAirGap = false; break; }
            }
            if (hasAirGap) return y;
        }
        return Integer.MIN_VALUE;
    }
}