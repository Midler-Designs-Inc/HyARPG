package com.example.hyarpg.utils.abilities.juggernaut;

// Hytale Imports
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.utils.abilities.Ability;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.CombatSupport;

// Java Imports
import java.util.List;
import org.joml.Vector3d;

public class Chain_Pull extends Ability {

    private static final float RANGE = 15f;
    private static final float HEIGHT = 5f;
    private static final float UP_FORCE = 25f;
    private static final float PULL_STRENGTH_MULTIPLIER = 5f;

    // register this ability with its name, resource cost, cooldown and flags
    public Chain_Pull() {
        super("Ability_Chain_Pull", DefaultEntityStatTypes.getStamina(), 5f, false, 3, false, List.of(), false);
    }

    @Override
    public void execute(Ref<EntityStore> ref, CommandBuffer<EntityStore> commandBuffer) {
        // grab the store and the caster's transform so we know where the pull originates
        Store<EntityStore> store = ref.getStore();
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        // define a box around the caster to scan for pullable targets
        Vector3d origin = transform.getPosition();
        Vector3d min = new Vector3d(origin.x - RANGE, origin.y - HEIGHT, origin.z - RANGE);
        Vector3d max = new Vector3d(origin.x + RANGE, origin.y + HEIGHT, origin.z + RANGE);

        // loop over every entity inside that box
        for (Ref<EntityStore> targetRef : TargetUtil.getAllEntitiesInBox(min, max, store)) {
            // skip invalid entities and the caster itself
            if (!targetRef.isValid() || targetRef.equals(ref)) continue;

            // fetch the target's position, bail if it has none
            TransformComponent targetTransform = store.getComponent(targetRef, TransformComponent.getComponentType());
            if (targetTransform == null) continue;

            // work out how far away the target actually is
            Vector3d targetPos = targetTransform.getPosition();
            double dx = targetPos.x - origin.x;
            double dy = targetPos.y - origin.y;
            double dz = targetPos.z - origin.z;

            // skip anything outside the pull radius or vertical height band
            if (dx * dx + dz * dz > RANGE * RANGE) continue;
            if (dy > HEIGHT || dy < -HEIGHT) continue;

            // fetch the target's NPC/role data so we can check hostility
            NPCEntity npc = store.getComponent(targetRef, NPCEntity.getComponentType());
            if (npc == null) continue;
            Role role = npc.getRole();

            // only pull NPCs that are actually hostile to the caster
            if (role == null || !CombatSupport.get(targetRef, store).getCanCauseDamage(targetRef, ref, store)) continue;

            // fetch the target's velocity component so we can move it
            Velocity vc = store.getComponent(targetRef, Velocity.getComponentType());
            if (vc == null) continue;

            // launch the target upward first, so the pull reads as an arc rather than a slide
            Vector3d upward = new Vector3d(0, UP_FORCE, 0);
            vc.addInstruction(upward, new VelocityConfig(), ChangeVelocityType.Add);

            // pull the target toward the caster, scaled by distance so they land near the player
            double distance = Math.sqrt(dx * dx + dz * dz);
            Vector3d pull = new Vector3d(origin.x - targetPos.x, 0, origin.z - targetPos.z).normalize().mul(distance * PULL_STRENGTH_MULTIPLIER);
            vc.addInstruction(pull, new VelocityConfig(), ChangeVelocityType.Add);
        }
    }

}