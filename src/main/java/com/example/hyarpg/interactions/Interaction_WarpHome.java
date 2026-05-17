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
import com.example.hyarpg.utils.rooms.TerritoryData;
import com.example.hyarpg.utils.rooms.WorldRoomRegistry;

// Java Imports
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.UUID;
import org.joml.Vector3d;
import org.joml.Vector3i;

public class Interaction_WarpHome extends SimpleInstantInteraction {

    public static final BuilderCodec<Interaction_WarpHome> CODEC = BuilderCodec.builder(Interaction_WarpHome.class, Interaction_WarpHome::new, SimpleInstantInteraction.CODEC).build();

    // horizontal XZ radius to try first, then full 3D radius if nothing found
    private static final int HORIZONTAL_RADIUS = 3;
    private static final int FULL_RADIUS       = 6;

    @Override
    protected void firstRun(@NonNullDecl InteractionType interactionType, @NonNullDecl InteractionContext context, @NonNullDecl CooldownHandler cooldownHandler) {
        final Ref<EntityStore> ref = context.getEntity();
        final Store<EntityStore> store = ref.getStore();
        final World world = store.getExternalData().getWorld();

        // bail if the entity is missing required components
        UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (uuidComponent == null || playerRef == null) return;

        // bail if no room registry is loaded for this world
        WorldRoomRegistry registry = WorldRoomRegistry.get(world);
        if (registry == null) {
            playerRef.sendMessage(Message.raw("No territory data loaded for this world."));
            return;
        }

        // find a territory owned or co-owned by this player
        UUID playerUuid = uuidComponent.getUuid();
        TerritoryData territory = null;
        for (TerritoryData t : registry.getAllTerritories()) {
            if (playerUuid.equals(t.getOwnerUuid()) || t.isCoOwner(playerUuid)) {
                territory = t;
                break;
            }
        }

        // bail if the player has no territory
        if (territory == null) {
            playerRef.sendMessage(Message.raw("You don't have a territory yet."));
            return;
        }

        // find the nearest safe 1x3x1 air gap near the lightwell center
        Vector3i lightWellBase = territory.getCenter();
        Vector3d safePos = findSafePositionNearLightWell(world, lightWellBase.x, lightWellBase.y - 1, lightWellBase.z);
        if (safePos == null) {
            playerRef.sendMessage(Message.raw("Could not find a safe position near your light well."));
            return;
        }

        // record previous position to teleport history then perform the warp
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

            Teleport teleportComponent = Teleport.createForPlayer(null, destination, new Rotation3f(0, 0, 0));
            store.addComponent(ref, Teleport.getComponentType(), teleportComponent);
        });
    }

    @Nullable
    private Vector3d findSafePositionNearLightWell(World world, int cx, int cy, int cz) {
        // pass 1: horizontal only — scan XZ rings at the same Y as the lightwell base, closest ring first
        for (int radius = 1; radius <= HORIZONTAL_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    // only the outer ring at this radius, never the lightwell's own position
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    if (dx == 0 && dz == 0) continue;
                    Vector3d result = checkAirGap(world, cx + dx, cy, cz + dz);
                    if (result != null) return result;
                }
            }
        }

        // pass 2: full 3D search — sort all candidate positions by distance and check closest first
        java.util.List<int[]> candidates = new java.util.ArrayList<>();
        for (int dx = -FULL_RADIUS; dx <= FULL_RADIUS; dx++) {
            for (int dy = -FULL_RADIUS; dy <= FULL_RADIUS; dy++) {
                for (int dz = -FULL_RADIUS; dz <= FULL_RADIUS; dz++) {
                    // skip positions already covered in pass 1
                    if (dy == 0 && Math.abs(dx) <= HORIZONTAL_RADIUS && Math.abs(dz) <= HORIZONTAL_RADIUS) continue;
                    candidates.add(new int[]{ dx, dy, dz });
                }
            }
        }
        // sort by euclidean distance from lightwell center so closest gaps win
        candidates.sort(Comparator.comparingDouble(a -> Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2])));
        for (int[] c : candidates) {
            Vector3d result = checkAirGap(world, cx + c[0], cy + c[1], cz + c[2]);
            if (result != null) return result;
        }

        return null;
    }

    // check if the given position is a valid 1x3x1 air gap — returns the feet position if so, null otherwise
    @Nullable
    private Vector3d checkAirGap(World world, int x, int y, int z) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);
        if (chunk == null) return null;

        // all 3 blocks of the column must be air
        for (int airY = y; airY <= y + 2; airY++) {
            int blockId = chunk.getBlock(x, airY, z);
            BlockType bt = BlockType.getAssetMap().getAsset(blockId);
            String id = bt != null ? bt.getId().toLowerCase() : "";
            if (!id.equals("air") && !id.equals("empty")) return null;
        }

        return new Vector3d(x + 0.5, y, z + 0.5);
    }
}