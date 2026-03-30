// Listeners_WorldStart.java
package com.example.hyarpg.listeners;

// Hytale Imports
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.universe.world.events.StartWorldEvent;

// Mod Imports
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.events.Event_WorldStart;

public class Listeners_WorldStart {
    public void register(EventRegistry eventBus) {
        eventBus.registerGlobal(StartWorldEvent.class, (StartWorldEvent event) ->
            ModEventBus.post(new Event_WorldStart(event.getWorld()))
        );
    }
}