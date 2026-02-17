# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BlackOut is a addon for Meteor Client (v1.21.11) focused on CrystalPVP functionality. It's built using Fabric mod loader and requires Java 21.

## Claudes Objective
Make this addon also viable to use on the Grim Anticheat, before this addon was specifically designed for the NoCheatPlus Anticheat which is a lot less strict than grim.
Basically all movement cheats dont work on grim due to the prediction engine that grim has. Thats why the most important change will be to Implement A Movement fix that prevents
you from sprinting sideways which flags on grim.

## Build System

**Build Commands:**
```bash
# Clean and build the project
./gradlew clean build

# Build without running tests
./gradlew build

# Run Minecraft client with the addon (if configured)
./gradlew runClient
```

**Key Files:**
- `build.gradle.kts` - Gradle build configuration (Kotlin DSL)
- `gradle.properties` - Version properties (Minecraft 1.21.11, Fabric loader 0.18.2, Meteor Client 1.21.11)
- `src/main/resources/fabric.mod.json` - Fabric mod metadata
- `src/main/resources/blackout.mixins.json` - Mixin configuration
- `src/main/resources/blackout.accesswidener` - Access widener for Minecraft internals

## Architecture

### Entry Point
- **BlackOut.java** (`kassuk.addon.blackout.BlackOut`) - Main addon class extending `MeteorAddon`
  - Implements `onInitialize()` to register modules, settings, commands, and HUD elements
  - Defines two custom categories: `BLACKOUT` and `SETTINGS`
  - Conditionally loads `AutoPvp` module only if Baritone API is present

### Module System
- **BlackOutModule.java** - Base class for all BlackOut modules, extends Meteor's `Module`
  - Provides utility methods: `sendPacket()`, `sendSequenced()`, `placeBlock()`, `interactBlock()`, `useItem()`, `clientSwing()`
  - Handles chat feedback with custom prefix formatting
  - Includes priority system via `PriorityUtils`
  - All modules inherit from this rather than Meteor's base `Module` class

### Package Structure
```
kassuk.addon.blackout/
├── commands/          # Custom commands (BlackoutGit, Coords)
├── enums/            # Enums (HoleType, RotationType, SwingHand, SwingState, SwingType)
├── events/           # Custom events (PreRotationEvent)
├── globalsettings/   # Global settings modules (FacingSettings, RangeSettings, RaytraceSettings, RotationSettings, ServerSettings, SwingSettings)
├── hud/              # HUD elements (ArmorHudPlus, BlackoutArray, GearHud, TargetHud, etc.)
├── managers/         # Singleton managers for shared functionality
├── mixins/           # Mixin classes for modifying Minecraft/Meteor behavior
├── modules/          # Combat and utility modules (60+ modules)
├── timers/           # Timer utilities
└── utils/            # Helper utilities and utilities extending Meteor functionality
    ├── meteor/       # Meteor-specific utilities
    └── RaksuTone/    # Baritone integration utilities
```

### Managers System
**Managers.java** - Central registry for singleton manager instances:
- `HOLDING` - HoldingManager (item switching state)
- `PING_SPOOF` - PingSpoofManager (packet delay)
- `ON_GROUND` - OnGroundManager (ground state tracking)
- `ROTATION` - RotationManager (rotation handling, stores `lastDir` for yaw/pitch)

### Global Settings Pattern
BlackOut uses a unique "global settings" pattern where settings are registered as modules in the `SETTINGS` category. These are accessed via `SettingUtils.java` which provides static methods wrapping the settings:
- **FacingSettings** - Block placement direction logic (`getPlaceData()`, `getPlaceDataAND()`, etc.)
- **RangeSettings** - Range checking for placing, attacking, mining (`inPlaceRange()`, `inAttackRange()`, etc.)
- **RaytraceSettings** - Raytrace validation (`placeTrace()`, `attackTrace()`)
- **RotationSettings** - Rotation requirements (`shouldRotate()`, `rotationCheck()`)
- **SwingSettings** - Swing/animation handling (`swing()`, `mineSwing()`)
- **ServerSettings** - Server-specific compatibility (`oldDamage()`, `oldCrystals()`, `cc()`)

**Important:** Always use `SettingUtils` static methods rather than directly accessing global settings modules.

### Mixins
Mixins modify Minecraft and Meteor behavior. Two types:
- **Accessor mixins** (e.g., `AccessorMinecraftClient`) - Expose private fields/methods
- **Injection mixins** (e.g., `MixinClientPlayerEntity`) - Inject custom logic into existing methods

All mixins are declared in `blackout.mixins.json`.

### Module Development
When creating new modules:
1. Extend `BlackOutModule` (not `Module`)
2. Pass the category (`BLACKOUT` or `SETTINGS`), name, and description to super constructor
3. Use `BlackOutModule` utility methods for packet sending, block interaction, and swinging
4. For range/rotation/swing checks, use `SettingUtils` static methods
5. Access managers via `Managers.ROTATION`, `Managers.HOLDING`, etc.
6. Register the module in `BlackOut.initializeModules()`

### Packet Handling
BlackOut uses sequenced packet sending for proper server synchronization:
```java
// Use sendSequenced() for block interactions
sendSequenced(s -> new PlayerInteractBlockC2SPacket(hand, hitResult, s));

// Use sendPacket() for non-sequenced packets
sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround));
```

### Rotation System
- `RotationManager` (accessed via `Managers.ROTATION`) stores the last rotation in `lastDir[0]` (yaw) and `lastDir[1]` (pitch)
- Use `SettingUtils.shouldRotate()` and `SettingUtils.rotationCheck()` before actions
- When using items, pass rotation to packet: `new PlayerInteractItemC2SPacket(hand, s, yaw, pitch)`

## Development Notes

- The addon requires Meteor Client as a dependency (loaded via `modImplementation`)
- Baritone is an optional dependency (loaded via `modCompileOnly`, checked at runtime)
- Uses Fabric access widener (`blackout.accesswidener`) to access Minecraft internals
- Target Java version: 21
- Access widener path is specified in `loom` block of build.gradle.kts
