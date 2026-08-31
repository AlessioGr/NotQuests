# Contributing to NotQuests

## Prerequisites

The toolchain is pinned in `mise.toml`. With [mise](https://mise.jdx.dev/) installed, run:

```bash
mise install
```

This provisions everything the project needs:

- **Java 25 (Temurin)** — required by Paper 26.2 / Minecraft 26.2
- **Gradle 9.7.1** — matches `gradle/wrapper/gradle-wrapper.properties`

(Without mise: install a JDK 25 and Gradle 9.7.1 manually.)

## Setup

```bash
git clone https://github.com/AlessioGr/NotQuests.git
cd NotQuests
```

Point `JAVA_HOME` at the pinned Java so Gradle launches with it (the build's
Java 25 toolchain is otherwise auto-provisioned):

```bash
export JAVA_HOME="$(mise where java)"
```

## Building

The Gradle wrapper jar (`gradle/wrapper/gradle-wrapper.jar`) is **not** committed,
so `./gradlew` does not work on a fresh clone. Use the mise-provided `gradle`, and
generate the wrapper once if you prefer `./gradlew` afterwards:

```bash
gradle wrapper        # one-time: creates gradle/wrapper/gradle-wrapper.jar
./gradlew clean build # or: gradle clean build
```

The final plugin jar is at:

```
src/paper/build/libs/notquests-7.0.0-beta.1-26.2-paper.jar
```

## Running a test server

```bash
./gradlew :paper:runServer
```

This starts a Paper 26.2 test server with the plugin loaded.

## Project structure

- `src/core/` - NotQuests itself: quests, players, progress, commands, configuration, YAML/SQL persistence, conversations, GUI behavior, migrations, registries, shared messages, and explicit platform contracts. It may use portable libraries such as Adventure/MiniMessage, but must not import Paper, Bukkit, NeoForge, or Minecraft platform classes.
- `src/builtin/` - Platform-independent built-in actions, conditions, objectives, triggers, and variables. Builtins use the core registry and platform contracts exactly like external type packs would.
- `src/paper/` - Atomic Paper/Bukkit event translation, native rendering/effects, external-plugin integrations, and final Paper jar packaging.
- `src/neoforge/` - Atomic NeoForge event translation, native rendering/effects, and mod packaging.
- `e2e/` - Real-server command sweeps used locally and in CI

## Config format

The canonical NotQuests config format is YAML. Core owns it through `ConfigurationManager`,
`DataManager`, and `YamlConfig`. Platform modules do not parse or save NotQuests configuration.
All released 6.3 conversion is isolated in `core/migrations/v6_3_0`, where old data is read once and
rewritten into the current format.
