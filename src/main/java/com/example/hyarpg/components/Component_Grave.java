package com.example.hyarpg.components;

// Hytale Imports
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Java Imports
import javax.annotation.Nullable;
import java.util.UUID;

public class Component_Grave implements Component<ChunkStore> {

    public UUID deadPlayerUuid;
    public Ref<EntityStore> deadPlayerRef;

    public static final BuilderCodec<Component_Grave> CODEC = BuilderCodec
            .builder(Component_Grave.class, Component_Grave::new)
            .append(new KeyedCodec<>("DeadPlayerUuid", Codec.STRING),
                    (c, v) -> c.deadPlayerUuid = v != null ? UUID.fromString(v) : null,
                    c -> c.deadPlayerUuid != null ? c.deadPlayerUuid.toString() : null)
            .add()
            .build();

    public Component_Grave() {}

    public Component_Grave(UUID deadPlayerUuid, Ref<EntityStore> deadPlayerRef) {
        this.deadPlayerUuid = deadPlayerUuid;
        this.deadPlayerRef = deadPlayerRef;
    }

    @Override
    @Nullable
    public Component<ChunkStore> clone() {
        return new Component_Grave(deadPlayerUuid, deadPlayerRef);
    }
}