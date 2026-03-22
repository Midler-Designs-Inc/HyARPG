package com.example.hyarpg.interactions;

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
import com.hypixel.hytale.protocol.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

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
            InteractionChain contextChain = context.getChain();
            assert contextChain != null;
            InteractionChainData chainData = contextChain.getChainData();
            Vector3f hitLocation = chainData.hitLocation;
            log("[SpawnFixed] hitLocation null=" + (hitLocation == null) + " owningEntity null=" + (context.getOwningEntity() == null));
            if (hitLocation != null) {
                CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
                assert commandBuffer != null;
                Store<EntityStore> store = commandBuffer.getStore();
                Vector3f hitNormal = chainData.hitNormal;
                com.hypixel.hytale.math.vector.Vector3f hitNormalVec = new com.hypixel.hytale.math.vector.Vector3f(hitNormal.x, hitNormal.y, hitNormal.z);
                DeployablesUtils.spawnDeployable(commandBuffer, store, this.config, context.getOwningEntity(),
                        new com.hypixel.hytale.math.vector.Vector3f(hitLocation.x, hitLocation.y, hitLocation.z),
                        MathUtil.getRotationForHitNormal(hitNormalVec),
                        MathUtil.getNameForHitNormal(hitNormalVec));
                log("[SpawnFixed] spawnDeployable called successfully");
            }
        } catch (Exception e) {
            log("[SpawnFixed] exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void log(String message) {
        for (PlayerRef player : Universe.get().getPlayers()) {
            player.sendMessage(Message.raw("[Turret] " + message));
        }
    }
}