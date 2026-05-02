package com.example.hyarpg.interactions;

// Hytale Imports
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;

// Mod Imports
import com.example.hyarpg.configs.ModConfig;
import com.example.hyarpg.configs.Config_World;

// Java Imports
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import java.util.Random;

public class Interaction_WaywardShrineCompassFindPrefab extends SimpleInstantInteraction {

    public static final BuilderCodec<Interaction_WaywardShrineCompassFindPrefab> CODEC = BuilderCodec.builder(
        Interaction_WaywardShrineCompassFindPrefab.class,
        Interaction_WaywardShrineCompassFindPrefab::new,
        SimpleInstantInteraction.CODEC
    ).build();

    private static final int SEARCH_RADIUS = 5;
    private static final int MAX_DISTANCE  = 3000;
    private static final int UNDERGROUND_PADDING = 20;

    @Override
    protected void firstRun(
        @NonNullDecl InteractionType interactionType,
        @NonNullDecl InteractionContext context,
        @NonNullDecl CooldownHandler cooldownHandler
    ) {
        Ref<EntityStore> ref = context.getEntity();
        if (ref == null || !ref.isValid()) return;

        Store<EntityStore> entityStore = ref.getStore();
        World world = entityStore.getExternalData().getWorld();

        TransformComponent transform = entityStore.getComponent(ref, TransformComponent.getComponentType());
        PlayerRef playerRef = entityStore.getComponent(ref, PlayerRef.getComponentType());
        if (transform == null || playerRef == null) return;

        Vector3d pos = transform.getPosition();
        int playerX = (int) pos.x;
        int playerY = (int) pos.y;
        int playerZ = (int) pos.z;

        world.execute(() -> {
            long worldSeed = world.getWorldConfig().getSeed();
            Config_World cfg = ModConfig.get().world;

            // determine if player is underground
            ChunkGenerator generator = (ChunkGenerator) world.getChunkStore().getGenerator();
            if (generator == null) {
                playerRef.sendMessage(Message.raw("The compass fails to locate what you desire.").color("#aaaaaa"));
                return;
            }

            int surfaceY = generator.getHeight((int) worldSeed, playerX, playerZ);
            boolean underground = playerY < surfaceY - UNDERGROUND_PADDING;

            // pick region grid params based on above/below ground
            int regionSize   = underground ? cfg.prefabUndergroundRegionSize   : cfg.prefabSurfaceRegionSize;
            double chance    = underground ? cfg.prefabUndergroundSpawnChance   : cfg.prefabSurfaceSpawnChance;
            long salt        = underground ? 0x3L                               : 0x1L;

            int playerRegionX = Math.floorDiv(playerX, regionSize);
            int playerRegionZ = Math.floorDiv(playerZ, regionSize);

            double closestDist = Double.MAX_VALUE;
            int closestX = 0, closestZ = 0;
            boolean found = false;

            for (int rx = playerRegionX - SEARCH_RADIUS; rx <= playerRegionX + SEARCH_RADIUS; rx++) {
                for (int rz = playerRegionZ - SEARCH_RADIUS; rz <= playerRegionZ + SEARCH_RADIUS; rz++) {
                    long regionSeed = (long) rx * 341873128712L + (long) rz * 132897987541L ^ worldSeed ^ salt;
                    Random random = new Random(regionSeed);

                    if (random.nextDouble() >= Math.max(0.0, Math.min(1.0, chance))) continue;

                    int margin = 4;
                    int anchorX = rx * regionSize + margin + random.nextInt(Math.max(1, regionSize - margin * 2));
                    int anchorZ = rz * regionSize + margin + random.nextInt(Math.max(1, regionSize - margin * 2));

                    double dist = Math.sqrt((anchorX - playerX) * (anchorX - playerX) + (anchorZ - playerZ) * (anchorZ - playerZ));
                    if (dist < closestDist) {
                        closestDist = dist;
                        closestX = anchorX;
                        closestZ = anchorZ;
                        found = true;
                    }
                }
            }

            if (!found || closestDist > MAX_DISTANCE) {
                playerRef.sendMessage(Message.raw("The compass fails to locate what you desire.").color("#aaaaaa"));
                return;
            }

            double angle = Math.toDegrees(Math.atan2(closestZ - playerZ, closestX - playerX));
            if (angle < 0) angle += 360;
            playerRef.sendMessage(Message.raw("The compass needle points " + toDirection(angle) + "."));
        });
    }

    private String toDirection(double angle) {
        if (angle >= 337.5 || angle < 22.5)   return "East";
        if (angle < 67.5)                     return "South-East";
        if (angle < 112.5)                    return "South";
        if (angle < 157.5)                    return "South-West";
        if (angle < 202.5)                    return "West";
        if (angle < 247.5)                    return "North-West";
        if (angle < 292.5)                    return "North";
        return                                       "North-East";
    }
}