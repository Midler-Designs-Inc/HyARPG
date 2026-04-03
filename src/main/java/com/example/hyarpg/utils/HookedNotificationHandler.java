package com.example.hyarpg.utils;

import com.example.hyarpg.events.Event_RemoveBlock;
import com.hypixel.hytale.protocol.BlockParticleEvent;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldNotificationHandler;
import com.example.hyarpg.ModEventBus;

public class HookedNotificationHandler extends WorldNotificationHandler {

    private final World world;

    public HookedNotificationHandler(World world) {
        super(world);
        this.world = world;
    }

    @Override
    public void sendBlockParticle(double x, double y, double z, int id, BlockParticleEvent particleType) {
        super.sendBlockParticle(x, y, z, id, particleType);

        if (particleType == BlockParticleEvent.Break || particleType == BlockParticleEvent.Physics) {
            int bx = (int) Math.floor(x);
            int by = (int) Math.floor(y);
            int bz = (int) Math.floor(z);
            BlockType bt = BlockType.getAssetMap().getAsset(id);
            ModEventBus.post(new Event_RemoveBlock(world, bx, by, bz, bt));
        }
    }
}