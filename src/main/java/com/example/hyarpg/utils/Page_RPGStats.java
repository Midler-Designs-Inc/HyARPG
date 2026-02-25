package com.example.hyarpg.utils;

// Hytale imports
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// HyUI imports
import au.ellie.hyui.builders.PageBuilder;

public class Page_RPGStats {

    public static void open(Ref<EntityStore> ref, Store<EntityStore> store) {
        String html = """
<div class="page-overlay">
    <button id="closeBtn" style="anchor-bottom: 10;anchor-width: 500;anchor-height: 40;">Close</button>

    <div class="container"
         data-hyui-scrollbar-style="&quot;Common.ui&quot; &quot;DefaultScrollbarStyle&quot;"
         data-hyui-title="RPG Stats"
         style="anchor-width: 500;anchor-height: 700;">

        <!-- SCROLLING CONTAINER -->
       <div class="container-contents" style="layout-mode: topscrolling;">
                
                           <!-- FORCE WIDTH -->
                           <div style="layout-mode: leftcenterwrap; anchor-width: 500;">
                
                               <div style="layout-mode: top; anchor-width: 245;">
                                   <div>Head:</div>
                                   <p>asdf</p><p>asdf</p><p>asdf</p><p>asdf</p>
                
                                   <div>Chest:</div>
                                   <p>asdf</p><p>asdf</p><p>asdf</p><p>asdf</p>
                
                                   <div>Hands:</div>
                                   <p>asdf</p><p>asdf</p><p>asdf</p><p>asdf</p>
                               </div>
                
                               <div style="layout-mode: top; anchor-width: 245;">
                                   <div>Legs:</div>
                                   <p>asdf</p><p>asdf</p><p>asdf</p><p>asdf</p>
                
                                   <div>Main Hand:</div>
                                   <p>asdf</p><p>asdf</p><p>asdf</p><p>asdf</p>
                
                                   <div>Off Hand:</div>
                                   <p>asdf</p><p>asdf</p><p>asdf</p><p>asdf</p>
                               </div>
                
                           </div>
                       </div>
    </div>
</div>
""";

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        PageBuilder.pageForPlayer(playerRef)
            .fromHtml(html)
            .addEventListener("closeBtn", CustomUIEventBindingType.Activating, (ctx) -> {
                Player player = store.getComponent(ref, Player.getComponentType());
                player.getPageManager().setPage(ref, store, Page.None);
            })
            .open(store);
    }
}