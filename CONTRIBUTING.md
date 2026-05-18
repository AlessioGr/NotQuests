# Contributing to NotQuests

## Prerequisites

- **Java 25** (install via [mise](https://mise.jdx.dev/): `mise install java@25`)
- **Gradle 9.0** (handled by the wrapper)

## Setup

```bash
git clone https://github.com/NotQuests/NotQuests.git
cd NotQuests
```

Set `JAVA_HOME` so Gradle finds Java 25:

```bash
export JAVA_HOME="$(mise where java)"
```

## Building

```bash
./gradlew clean build
```

The final plugin jar is at:

```
plugin/build/libs/plugin-6.0.0.jar
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
