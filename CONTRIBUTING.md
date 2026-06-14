# Contributing to NotQuests

## Prerequisites

The toolchain is pinned in `mise.toml`. With [mise](https://mise.jdx.dev/) installed, run:

```bash
mise install
```

This provisions everything the project needs:

- **Java 25 (Temurin)** — required by Paper 26.1.2 / Minecraft 1.26
- **Gradle 9.0.0** — matches `gradle/wrapper/gradle-wrapper.properties`

(Without mise: install a JDK 25 and Gradle 9.0.0 manually.)

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
plugin/build/libs/plugin-6.0.1.jar
```

## Running a test server

```bash
./gradlew :plugin:runServer
```

This starts a Paper 26.1.2 test server with the plugin loaded.

## Project structure

- `common/` - Shared code across platforms
- `paper/` - Paper-specific implementation (commands, events, GUIs, integrations)
- `plugin/` - Final plugin assembly (shading, plugin.yml / paper-plugin.yml generation)
