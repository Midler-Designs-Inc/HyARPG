package com.example.hyarpg.interactions;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.world.SpawnParticleSystem;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class Interaction_BeamParticle extends SimpleInstantInteraction {

    private String particleSystemId = "";
    private float scale = 1.0f;

    public static final BuilderCodec<Interaction_BeamParticle> CODEC = BuilderCodec.builder(
            Interaction_BeamParticle.class, Interaction_BeamParticle::new, SimpleInstantInteraction.CODEC
    ).addField(new KeyedCodec<>("ParticleSystemId", BuilderCodec.STRING),
            (i, v) -> i.particleSystemId = v,
            i -> i.particleSystemId
    ).addField(new KeyedCodec<>("Scale", BuilderCodec.FLOAT),
            (i, v) -> i.scale = v,
            i -> i.scale
    ).build();

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) { context.getState().state = InteractionState.Failed; return; }

        Ref<EntityStore> selfRef = context.getEntity();
        Ref<EntityStore> targetRef = context.getTargetEntity();
        if (targetRef == null || !targetRef.isValid()) { context.getState().state = InteractionState.Failed; return; }

        TransformComponent selfTransform = commandBuffer.getComponent(selfRef, TransformComponent.getComponentType());
        TransformComponent targetTransform = commandBuffer.getComponent(targetRef, TransformComponent.getComponentType());
        if (selfTransform == null || targetTransform == null) { context.getState().state = InteractionState.Failed; return; }

        Vector3d from = selfTransform.getPosition();
        Vector3d to = targetTransform.getPosition();

        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        // Y axis beam: pitch -90 lays it flat, yaw then rotates horizontally
        float yaw = (float) Math.toDegrees(Math.atan2(dx, dz));
        float pitch = (float) Math.toDegrees(Math.atan2(-dy, horizontalDist)) - 90f;
        float roll = 0.0f;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Spawn at player position, not midpoint
        SpawnParticleSystem packet = new SpawnParticleSystem();
        packet.position = new Position(from.x, from.y + 1.5, from.z);
        packet.scale = (float) distance * scale;
        packet.rotation = new Direction(yaw, pitch, roll);

        packet.scale = (float) distance;

        PlayerRef playerRef = commandBuffer.getComponent(selfRef, PlayerRef.getComponentType());
        if (playerRef == null) { context.getState().state = InteractionState.Failed; return; }
        playerRef.getPacketHandler().writeNoCache(packet);
    }
}