package com.example.hyarpg;

// Java Imports
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

// A small custom event bus for internal mod use
public class ModEventBus {

    // Map of event type → list of listeners
    private static final Map<Class<?>, List<Consumer<?>>> listeners = new ConcurrentHashMap<>();

    private ModEventBus() {} // static class only

    // Register a listener for a specific event type
    public static <T> void register(Class<T> eventClass, Consumer<T> listener) {
        listeners.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(listener);
    }

    // Post an event to all listeners of that type
    @SuppressWarnings("unchecked")
    public static <T> void post(T event) {
        List<Consumer<?>> eventListeners = listeners.getOrDefault(event.getClass(), Collections.emptyList());
        for (Consumer<?> listener : eventListeners) {
            ((Consumer<T>) listener).accept(event);
        }
    }
}
