# HyARPG

A full overhaul that transforms *Hytale* into a scalable Survival ARPG.

## Overview

HyARPG reworks core gameplay systems to introduce structured progression, skill-based combat, and a dynamic loot ecosystem.

## Core Pillars

- Progression-driven gameplay (levels, gear score, skill trees)
- Scalable ARPG combat systems
- Dynamic loot with affixes and rarity tiers
- World scaling tied to exploration distance
- Optional survival mechanics (hunger/thirst)

## Warning

This mod heavily modifies core systems. Use a fresh world and avoid conflicting mods.

## Building for Hytale 0.7.4

The Gradle build compiles against the Hytale Server 0.7.4 API and writes the mod JAR
to `build/libs/hyarpg-3.6.2.jar`. Build without deploying it with:

```powershell
.\gradlew.bat build
```

The build does not copy files into a game installation or world. Test changes in a
disposable world, and keep backups of any existing saves and configuration.
