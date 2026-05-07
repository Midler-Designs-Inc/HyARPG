package com.example.hyarpg.utils.abilities.ranger;

import com.example.hyarpg.utils.abilities.Ability;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerInput;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;

public class Aerial_Maneuver extends Ability {

    private static final float UP_FORCE_GROUND = 15f;
    private static final float UP_FORCE_AIR = 15f;
    private static final float HORIZONTAL_PRESERVE_MULTIPLIER = 3f;
    private static final float HORIZONTAL_THRESHOLD = 0.1f;

    public Aerial_Maneuver() {
        super("Ability_Aerial_Maneuver", DefaultEntityStatTypes.getStamina(), 5f, false, 3, false, List.of());
    }

    @Override
    public void execute(Ref<EntityStore> ref, CommandBuffer<EntityStore> commandBuffer) {
        Store<EntityStore> store = ref.getStore();

        Velocity velocity = store.getComponent(ref, Velocity.getComponentType());
        if (velocity == null) return;

        MovementStatesComponent movementStatesComponent = store.getComponent(ref, MovementStatesComponent.getComponentType());
        if (movementStatesComponent == null) return;

        PlayerInput playerInput = store.getComponent(ref, PlayerInput.getComponentType());

        boolean onGround = movementStatesComponent.getMovementStates().onGround;
        boolean falling = movementStatesComponent.getMovementStates().falling;

        double horizontalX = velocity.getX();
        double horizontalZ = velocity.getZ();
        double currentY = velocity.getY();

        // On ground, velocity horizontal is near zero — use wish movement direction instead
        if (onGround && playerInput != null) {
            for (PlayerInput.InputUpdate update : playerInput.getMovementUpdateQueue()) {
                if (update instanceof PlayerInput.WishMovement wish) {
                    horizontalX = wish.getX();
                    horizontalZ = wish.getZ();
                    break;
                }
            }
        }

        boolean hasHorizontalVelocity =
                (horizontalX * horizontalX + horizontalZ * horizontalZ) > (HORIZONTAL_THRESHOLD * HORIZONTAL_THRESHOLD);

        double totalUpForce;
        if (onGround) {
            totalUpForce = UP_FORCE_GROUND;
        } else if (falling) {
            double fallSpeed = currentY < 0 ? Math.abs(currentY) : 0;
            if (fallSpeed <= 4.0) {
                totalUpForce = lerp(fallSpeed, 1.0, 4.0, 35.0, 45.0);
            } else if (fallSpeed <= 16.0) {
                totalUpForce = lerp(fallSpeed, 4.0, 16.0, 45.0, 55.0);
            } else {
                totalUpForce = Math.min(lerp(fallSpeed, 16.0, 50.0, 55.0, 70.0), 70.0);
            }
        } else {
            totalUpForce = UP_FORCE_AIR;
        }

        if (hasHorizontalVelocity) {
            double launchX = horizontalX * HORIZONTAL_PRESERVE_MULTIPLIER;
            double launchZ = horizontalZ * HORIZONTAL_PRESERVE_MULTIPLIER;
            velocity.addInstruction(new Vector3d(
                    launchX,
                    totalUpForce,
                    launchZ
            ), new VelocityConfig(), ChangeVelocityType.Add);
        } else {
            velocity.addInstruction(
                    new Vector3d(0, totalUpForce, 0),
                    new VelocityConfig(),
                    ChangeVelocityType.Add
            );
        }
    }

    private static double lerp(double value, double inMin, double inMax, double outMin, double outMax) {
        double t = Math.max(0.0, Math.min(1.0, (value - inMin) / (inMax - inMin)));
        return outMin + t * (outMax - outMin);
    }
}