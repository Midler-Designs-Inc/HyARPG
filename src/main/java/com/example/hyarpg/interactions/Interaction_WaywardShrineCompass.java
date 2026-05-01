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

// Mod Imports
import com.example.hyarpg.configs.ModConfig;
import com.example.hyarpg.configs.Config_World;

// Java Imports
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import java.util.Random;

public class Interaction_WaywardShrineCompass extends SimpleInstantInteraction {

    public static final BuilderCodec<Interaction_WaywardShrineCompass> CODEC = BuilderCodec.builder(
            Interaction_WaywardShrineCompass.class,
            Interaction_WaywardShrineCompass::new,
            SimpleInstantInteraction.CODEC
    ).build();

    private static final int SEARCH_RADIUS = 5; // regions in each direction
    private static final int MAX_DISTANCE = 3000; // blocks cutoff

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

        // get player position
        TransformComponent transform = entityStore.getComponent(ref, TransformComponent.getComponentType());
        PlayerRef playerRef = entityStore.getComponent(ref, PlayerRef.getComponentType());
        if (transform == null) return;
        Vector3d pos = transform.getPosition();
        int playerX = (int) pos.x, playerZ = (int) pos.z;

        world.execute(() -> {
            long worldSeed = world.getWorldConfig().getSeed();
            Config_World cfg = ModConfig.get().world;
            int regionSize = cfg.prefabWaywardShrineRegionSize;
            int margin = 4;

            // find the region the player is in and search surrounding regions
            int playerRegionX = Math.floorDiv(playerX, regionSize);
            int playerRegionZ = Math.floorDiv(playerZ, regionSize);

            double closestDist = Double.MAX_VALUE;
            int closestX = 0, closestZ = 0;
            boolean found = false;

            for (int rx = playerRegionX - SEARCH_RADIUS; rx <= playerRegionX + SEARCH_RADIUS; rx++) {
                for (int rz = playerRegionZ - SEARCH_RADIUS; rz <= playerRegionZ + SEARCH_RADIUS; rz++) {
                    // replicate exact shrine region seed from PrefabWorldGenListener
                    long regionSeed = (long) rx * 341873128712L + (long) rz * 132897987541L ^ worldSeed ^ 0x6L;
                    Random random = new Random(regionSeed);

                    // skip if this region didn't roll a spawn
                    if (random.nextDouble() >= Math.max(0.0, Math.min(1.0, cfg.prefabWaywardShrineSpawnChance))) continue;

                    // compute anchor position exactly as worldgen does
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

            // no shrine found within search radius or cutoff distance
            if (!found || closestDist > MAX_DISTANCE) {
                playerRef.sendMessage(Message.raw("No Wayward Shrine detected nearby."));
                return;
            }

            // compute cardinal/intercardinal direction
            double angle = Math.toDegrees(Math.atan2(closestZ - playerZ, closestX - playerX));
            if (angle < 0) angle += 360;
            String direction = toDirection(angle);

            playerRef.sendMessage(Message.raw("The compass needle points " + direction + "."));
        });
    }

    // convert angle to cardinal/intercardinal direction — 0° is East, clockwise
    private String toDirection(double angle) {
        if (angle >= 337.5 || angle < 22.5)   return "East";
        if (angle < 67.5)                       return "South-East";
        if (angle < 112.5)                      return "South";
        if (angle < 157.5)                      return "South-West";
        if (angle < 202.5)                      return "West";
        if (angle < 247.5)                      return "North-West";
        if (angle < 292.5)                      return "North";
        return                                          "North-East";
    }
}