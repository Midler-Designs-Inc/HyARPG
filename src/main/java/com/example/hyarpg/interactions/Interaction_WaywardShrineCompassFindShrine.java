package com.example.hyarpg.interactions;

// Hytale Imports
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
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
import com.example.hyarpg.worldgen.PrefabRecord;
import com.example.hyarpg.worldgen.PrefabRegistry;

// Java Imports
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.joml.Vector3d;
import java.awt.*;

public class Interaction_WaywardShrineCompassFindShrine extends SimpleInstantInteraction {

    public static final BuilderCodec<Interaction_WaywardShrineCompassFindShrine> CODEC = BuilderCodec.builder(Interaction_WaywardShrineCompassFindShrine.class, Interaction_WaywardShrineCompassFindShrine::new, SimpleInstantInteraction.CODEC).build();

    private static final int MAX_DISTANCE = 3000;

    @Override
    protected void firstRun(@NonNullDecl InteractionType interactionType, @NonNullDecl InteractionContext context, @NonNullDecl CooldownHandler cooldownHandler) {
        // get the entity ref
        Ref<EntityStore> ref = context.getEntity();
        if (!ref.isValid()) return;

        // get the store and world from the entity ref
        Store<EntityStore> entityStore = ref.getStore();
        World world = entityStore.getExternalData().getWorld();

        // get entity's position
        TransformComponent transform = entityStore.getComponent(ref, TransformComponent.getComponentType());
        PlayerRef playerRef = entityStore.getComponent(ref, PlayerRef.getComponentType());
        if (transform == null) return;
        Vector3d pos = transform.getPosition();
        int playerX = (int) pos.x;
        int playerZ = (int) pos.z;

        // on world execute find the desired prefab
        world.execute(() -> {
            PrefabRegistry registry = PrefabRegistry.get(world);
            if (registry == null) { playerRef.sendMessage(Message.raw("The compass fails to locate what you desire.").color(Color.GRAY)); return; }

            PrefabRecord closest = registry.getClosest(PrefabRecord.Type.SHRINE, playerX, playerZ, MAX_DISTANCE);
            if (closest == null) { playerRef.sendMessage(Message.raw("The compass fails to locate what you desire.").color(Color.GRAY)); return; }

            double angle = Math.toDegrees(Math.atan2(closest.getAnchorZ() - playerZ, closest.getAnchorX() - playerX));
            if (angle < 0) angle += 360;
            playerRef.sendMessage(Message.raw("The compass needle points " + toDirection(angle) + ".").color(Color.GRAY));
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
        return                                  "North-East";
    }
}