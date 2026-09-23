# Developer Setup

## Requirements
- **JDK 25** (Eclipse Temurin). Gradle downloads JDK 21 automatically for the 1.21.1 builds.
- Git. Any IDE with Gradle support (IntelliJ IDEA recommended).

## Build targets
Stonecutter turns the single `src/` tree into four Gradle projects:

| Node | Minecraft | Loader |
|---|---|---|
| `1.21.1-fabric` | 1.21.1 | Fabric |
| `1.21.1-neoforge` | 1.21.1 | NeoForge |
| `26.1-fabric` | 26.1.2 | Fabric |
| `26.1-neoforge` | 26.1.2 | NeoForge |

Build or test **one target at a time** (memory):

```bash
./gradlew :26.1-neoforge:build      # jar + unit tests
./gradlew :1.21.1-fabric:test       # unit tests only
./gradlew :1.21.1-neoforge:runClient
./gradlew :26.1-fabric:runServer
```

Jars land in `versions/<node>/build/libs/`. `./gradlew build` builds all four (slow, memory-heavy).
Each node has its own game folder under `run/<node>/`.

## Stonecutter in 60 seconds
- Code is written once in `src/`. Differences use comments:
  `//? if neoforge {` … `//?}` and `//? if >=26.1 {` … `//?} else {` … `//?}`.
- Write `Identifier` (26.1 name). Stonecutter rewrites it to `ResourceLocation` for 1.21.1.
- Loader-only classes go in `diamondvending/platform/fabric/` or `.../neoforge/`; each loader's
  build excludes the other folder, so these files need no loader comments.
- `diamondvending/core/` is plain Java with **no Minecraft imports** — test it with JUnit.
- To edit code for another version in the IDE, run e.g.
  `./gradlew "Set active project to 1.21.1-fabric"`. **Before committing, run
  `./gradlew "Reset active project"`** — git must always hold the `26.1-neoforge` state. CI
  rejects anything else.

## Workflow
Branch → PR (fill in the checklist) → CI green → squash-merge.
