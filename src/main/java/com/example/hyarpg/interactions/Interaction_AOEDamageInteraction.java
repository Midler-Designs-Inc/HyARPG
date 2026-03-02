package com.example.hyarpg.interactions;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector4d;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.meta.DynamicMetaStore;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class Interaction_AOEDamageInteraction extends SimpleInstantInteraction {

    private float radius = 5.0f;
    private float damage = 10.0f;
    @Nullable
    private String damageCauseId;

    public static final BuilderCodec<Interaction_AOEDamageInteraction> CODEC = BuilderCodec.builder(
            Interaction_AOEDamageInteraction.class, Interaction_AOEDamageInteraction::new, SimpleInstantInteraction.CODEC
    ).addField(new KeyedCodec<>("Radius", BuilderCodec.FLOAT),
            (instance, value) -> instance.radius = value,
            instance -> instance.radius
    ).addField(new KeyedCodec<>("Damage", BuilderCodec.FLOAT),
            (instance, value) -> instance.damage = value,
            instance -> instance.damage
    ).addField(new KeyedCodec<>("DamageCause", DamageCause.CHILD_ASSET_CODEC),
            (instance, value) -> instance.damageCauseId = value,
            instance -> instance.damageCauseId
    ).build();

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        // Resolve the damage cause — fall back to ENVIRONMENT if not set or not found
        DamageCause resolvedCause = damageCauseId != null
                ? DamageCause.getAssetMap().getAsset(damageCauseId)
                : null;
        if (resolvedCause == null) {
            resolvedCause = DamageCause.ENVIRONMENT;
        }

        Ref<EntityStore> selfRef = context.getEntity();
        DamageCause finalResolvedCause = resolvedCause;

        // Determine origin: prefer hit location, fall back to entity position
        Vector4d hitLocation = (Vector4d) context.getMetaStore().getIfPresentMetaObject(Interaction.HIT_LOCATION);

        Vector3d origin;
        if (hitLocation != null) {
            origin = new Vector3d(hitLocation.x, hitLocation.y, hitLocation.z);
        } else {
            // ProjectileMiss — projectile entity IS at impact position
            TransformComponent transform = (TransformComponent) commandBuffer.getComponent(
                    context.getEntity(), TransformComponent.getComponentType()
            );
            if (transform == null) {
                context.getState().state = InteractionState.Failed;
                return;
            }
            origin = transform.getPosition();
        }

        List<Ref<EntityStore>> targets = new ArrayList<>();
        Selector.selectNearbyEntities(
                commandBuffer,
                origin,
                radius,
                targets::add,
                ref -> {
                    if(ref.equals(selfRef)) return false;
                    if (commandBuffer.getComponent(ref, Player.getComponentType()) != null) return false;
                    return true;
                }
        );

        Damage.Source damageSource = new Damage.EntitySource(selfRef);

        for (Ref<EntityStore> targetRef : targets) {
            DamageSystems.executeDamage(
                    targetRef,
                    commandBuffer,
                    new Damage(damageSource, finalResolvedCause, damage)
            );
        }
    }
}