package com.example.hyarpg.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;

import javax.annotation.Nonnull;

/**
 * HyARPG Dashboard UI
 *
 * A simple interactive dashboard page showing plugin information
 * with refresh and close buttons.
 */
public class HyARPGDashboardUI extends InteractiveCustomUIPage<HyARPGDashboardUI.UIEventData> {

    // Path relative to Common/UI/Custom/
    public static final String LAYOUT = "hyarpg/Dashboard.ui";

    private final PlayerRef playerRef;
    private int refreshCount = 0;

    public HyARPGDashboardUI(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, UIEventData.CODEC);
        this.playerRef = playerRef;
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evt,
            @Nonnull Store<EntityStore> store
    ) {
        // Load base layout
        cmd.append(LAYOUT);

        // Bind refresh button
        evt.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#RefreshButton",
            new EventData().append("Action", "refresh"),
            false
        );

        // Bind close button
        evt.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CloseButton",
            new EventData().append("Action", "close"),
            false
        );
    }

    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull UIEventData data
    ) {
        if (data.action == null) return;

        switch (data.action) {
            case "refresh":
                refreshCount++;
                UICommandBuilder cmd = new UICommandBuilder();
                cmd.set("#StatusText.Text", "Refreshed " + refreshCount + " time(s)!");
                this.sendUpdate(cmd, false);

                NotificationUtil.sendNotification(
                    playerRef.getPacketHandler(),
                    Message.raw("HyARPG"),
                    Message.raw("Dashboard refreshed!"),
                    NotificationStyle.Success
                );
                break;

            case "close":
                this.close();
                break;
        }
    }

    /**
     * Event data class with codec for handling UI events.
     */
    public static class UIEventData {
        public static final BuilderCodec<UIEventData> CODEC = BuilderCodec.builder(
                UIEventData.class, UIEventData::new
        )
        .append(new KeyedCodec<>("Action", Codec.STRING), (e, v) -> e.action = v, e -> e.action)
        .add()
        .build();

        private String action;

        public UIEventData() {}
    }
}