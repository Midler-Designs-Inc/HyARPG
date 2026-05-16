package com.example.hyarpg.interactions;

// Hytale Imports
import com.hypixel.hytale.builtin.deployables.DeployablesUtils;
import com.hypixel.hytale.builtin.deployables.config.DeployableConfig;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.protocol.InteractionChainData;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Java Imports
import javax.annotation.Nonnull;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class Interaction_SpawnDeployableAtHitLocation extends SimpleInstantInteraction {

    @Nonnull
    public static final BuilderCodec<Interaction_SpawnDeployableAtHitLocation> CODEC = BuilderCodec.builder(Interaction_SpawnDeployableAtHitLocation.class, Interaction_SpawnDeployableAtHitLocation::new, SimpleInstantInteraction.CODEC)
        .append(new KeyedCodec("Config", DeployableConfig.CODEC), (i, s) -> i.config = s, (i) -> i.config)
        .addValidator(Validators.nonNull())
        .add()
        .build();

    private DeployableConfig config;

    public boolean needsRemoteSync() { return false; }

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        try {
            // get the interaction chain data
            InteractionChain contextChain = context.getChain();
            InteractionChainData chainData = contextChain.getChainData();

            // get applicable hit location params
            Vector3f hitLocation = new Vector3f(chainData.hitLocation);
            Vector3f hitNormal = new Vector3f(chainData.hitNormal);
            Vector3d hitNormalVec = new Vector3d(hitNormal.x, hitNormal.y, hitNormal.z);

            // get the command buffer and entity store
            CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
            Store<EntityStore> store = commandBuffer.getStore();

            // spawn the deployable
            DeployablesUtils.spawnDeployable(commandBuffer, store, this.config, context.getOwningEntity(),
                new Vector3d(hitLocation.x, hitLocation.y, hitLocation.z),
                MathUtil.getRotationForHitNormal(hitNormalVec),
                MathUtil.getNameForHitNormal(hitNormalVec)
            );

        } catch (Exception _) {}
    }
}