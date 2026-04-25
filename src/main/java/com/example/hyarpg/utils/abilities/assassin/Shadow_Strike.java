package com.example.hyarpg.utils.abilities.assassin;

// Hytale Imports
import com.example.hyarpg.modules.Module_RPGSystem;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.HyARPGPlugin;
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.utils.abilities.Ability;
import com.example.hyarpg.utils.combat.SwingDamageGroup;
import com.example.hyarpg.utils.items.ItemFactory;

// Java Imports
import java.awt.*;
import java.util.Collections;
import java.util.List;

public class Shadow_Strike extends Ability {

    private static final float  BEHIND_OFFSET      = 1.5f; // how far behind the target to appear
    private static final int    MAX_MARKS_CONSUME   = 5;
    private static final double CRIT_DMG_PER_MARK   = 40.0; // +40% crit damage per mark consumed

    public Shadow_Strike() {
        super("Ability_Shadow_Strike", DefaultEntityStatTypes.getMana(), 10f, false, 3, false,
                List.of("Axe", "Battleaxe", "Club", "Daggers", "Longsword", "Mace", "Sword"));
    }

    @Override
    public void execute(Ref<EntityStore> ref) {
        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        // get the rpg player component
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        Component_RPG_Player rpgPlayer = store.getComponent(ref, Module_RPGSystem.componentTypeRPGPlayer);
        if (rpgPlayer == null || playerRef == null) return;

        // get the last hit target — bail if none
        Ref<EntityStore> target = rpgPlayer.marks.getLastHitTarget();
        if (target == null || !target.isValid()) {
            playerRef.sendMessage(Message.raw("You must have a valid target to use this ability. Hit an enemy to acquire them as a target.").color(Color.RED));
            return;
        }

        // consume up to 5 assassin marks if this is the same last hit target
        int marksConsumed = 0;
        if (target.equals(rpgPlayer.marks.getLastHitTarget())) {
            int available = rpgPlayer.marks.count("ASSASSIN");
            marksConsumed = Math.min(available, MAX_MARKS_CONSUME);
            if (marksConsumed > 0) rpgPlayer.marks.consume("ASSASSIN", marksConsumed);
        }

        // build the simulated mainhand hit with forced crit and mark bonus
        ItemStack mainHand = rpgPlayer.mainHandItem;
        String weaponType = mainHand != null ? ItemFactory.deriveItemType(mainHand.getItem().getId()) : null;
        List<String> damageImplicits = mainHand != null ? ItemFactory.getWeaponDamageImplicits(mainHand) : Collections.emptyList();
        double critBonus = marksConsumed * CRIT_DMG_PER_MARK;

        // build the swing group directly with forced crit and mark bonus applied
        SwingDamageGroup group = new SwingDamageGroup(ref, target, false, false, weaponType, true, critBonus);

        // resolve damage packets from weapon implicits, falling back to physical
        if (!damageImplicits.isEmpty()) {
            for (String implicit : damageImplicits) {
                String[] parts = implicit.split("\\|");
                if (parts.length < 3) continue;
                float implicitValue = Float.parseFloat(parts[1]);
                String dmgTypeId = switch (parts[0]) {
                    case "MAIN_HAND_FIRE_DAMAGE_FLAT"      -> "Fire";
                    case "MAIN_HAND_LIGHTNING_DAMAGE_FLAT" -> "Lightning";
                    case "MAIN_HAND_ICE_DAMAGE_FLAT"       -> "Ice";
                    case "MAIN_HAND_POISON_DAMAGE_FLAT"    -> "Poison";
                    case "MAIN_HAND_MAGIC_DAMAGE_FLAT"     -> "Magic";
                    case "MAIN_HAND_PHYSICAL_DAMAGE_FLAT"  -> "Physical";
                    default -> null;
                };
                if (dmgTypeId == null) continue;
                DamageCause resolvedCause = DamageCause.getAssetMap().getAsset(dmgTypeId);
                if (resolvedCause == null) continue;
                group.add(resolvedCause, implicitValue);
            }
        } else {
            // no implicits, fall back to physical with weapon's base damage
            group.add(DamageCause.getAssetMap().getAsset("Physical"), 1f);
        }

        // teleport the player behind the target facing them, then inject the damage group
        world.execute(() -> {
            // get target transform to determine where to teleport
            TransformComponent targetTransform = store.getComponent(target, TransformComponent.getComponentType());
            HeadRotation targetRotation = store.getComponent(target, HeadRotation.getComponentType());
            if (targetTransform == null || targetRotation == null) return;

            // get the player transform
            TransformComponent playerTransform = store.getComponent(ref, TransformComponent.getComponentType());
            HeadRotation playerRotation = store.getComponent(ref, HeadRotation.getComponentType());
            if (playerTransform == null || playerRotation == null) return;

            // compute the behind position — step back from the target along its facing direction
            Vector3d targetPos = targetTransform.getPosition();
            double targetYaw = targetRotation.getRotation().getYaw();
            double yawRad = Math.toRadians(targetYaw);

            // offset 90 degrees to get behind instead of to the side
            double behindX = targetPos.x - Math.cos(yawRad) * BEHIND_OFFSET;
            double behindZ = targetPos.z + Math.sin(yawRad) * BEHIND_OFFSET;

            // compute the behind position — check if solid and spiral out to find a safe spot
            Vector3d teleportPos = findSafePosition(world, behindX, targetPos.y, behindZ);

            // face the player toward the target — same yaw as target since we're behind them
            double faceYaw = targetYaw;

            // Teleport the player
            Teleport teleport = Teleport.createForPlayer(world, teleportPos, new Vector3f(0, (float) faceYaw, 0));
            store.addComponent(ref, Teleport.getComponentType(), teleport);

            // inject into the combat pipeline via the swing queue
            HyARPGPlugin.getInstance().combatSystem.injectDamageGroup(ref, target, group);
        });
    }

    // finds a safe non-solid position near the desired teleport point
    private Vector3d findSafePosition(World world, double x, double y, double z) {
        // check the desired spot first
        if (isPositionSafe(world, x, y, z)) return new Vector3d(x, y, z);

        // spiral outward in increasing radius until we find a safe spot
        for (int r = 1; r <= 3; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue; // only check ring edge
                    double nx = x + dx;
                    double nz = z + dz;
                    if (isPositionSafe(world, nx, y, nz)) return new Vector3d(nx, y, nz);
                    // also try one block up in case ground is uneven
                    if (isPositionSafe(world, nx, y + 1, nz)) return new Vector3d(nx, y + 1, nz);
                }
            }
        }

        // fallback — return original position and let the engine handle it
        return new Vector3d(x, y, z);
    }

    // checks if a position has a solid floor and non-solid space for the player to stand
    private boolean isPositionSafe(World world, double x, double y, double z) {
        try {
            long chunkIndex = com.hypixel.hytale.math.util.ChunkUtil.indexChunkFromBlock((int) x, (int) z);
            com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);
            if (chunk == null) return false;

            // feet and head blocks must be non-solid, floor must be solid
            int blockAtFeet = chunk.getBlock((int) x, (int) y, (int) z);
            int blockAtHead = chunk.getBlock((int) x, (int) y + 1, (int) z);
            int blockAtFloor = chunk.getBlock((int) x, (int) y - 1, (int) z);

            BlockType feet = BlockType.getAssetMap().getAsset(blockAtFeet);
            BlockType head = BlockType.getAssetMap().getAsset(blockAtHead);
            BlockType floor = BlockType.getAssetMap().getAsset(blockAtFloor);

            boolean feetClear = feet == null || !com.example.hyarpg.utils.rooms.RoomFloodFill.isStructural(feet);
            boolean headClear = head == null || !com.example.hyarpg.utils.rooms.RoomFloodFill.isStructural(head);
            boolean floorSolid = floor != null && com.example.hyarpg.utils.rooms.RoomFloodFill.isStructural(floor);

            return feetClear && headClear && floorSolid;
        } catch (Exception e) {
            return false;
        }
    }
}