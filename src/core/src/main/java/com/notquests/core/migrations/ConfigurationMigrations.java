package com.notquests.core.migrations;

import com.notquests.core.config.YamlConfig;
import com.notquests.core.migrations.v6_3_0.Version630Migration;
import com.notquests.core.updates.VersionNumber;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class ConfigurationMigrations {
  public static final String DATA_VERSION = "data-migration-version-do-not-edit";
  private static final List<Migration<Context>> MIGRATIONS = List.of(
      new Version630Migration());

  public static boolean prepareDataVersion(
      final YamlConfig configuration,
      final String previousConfigVersion) {
    if (configuration == null || configuration.isString(DATA_VERSION)) {
      return false;
    }
    final String existing = configuration.getString(DATA_VERSION, previousConfigVersion);
    configuration.set(DATA_VERSION, cleanVersion(existing));
    configuration.setComments(
        DATA_VERSION,
        List.of("Do not modify this line. NotQuests uses it to know which one-time data migrations already ran."));
    return true;
  }

  public boolean runStartupMigrations(final MigrationHooks hooks) {
    final String previousVersion = dataVersion(hooks.generalConfig());
    final String currentVersion = hooks.currentVersion();
    final Context context = new Context(hooks.dataFolder(), hooks.generalConfig(), hooks.warnLogger());
    try {
      run(new MigrationRun<>(
          previousVersion,
          currentVersion,
          context,
          hooks.infoLogger(),
          hooks::backup,
          version -> {
            saveDataVersion(hooks.generalConfig(), version);
            hooks.saveGeneralConfig();
          }));
      return true;
    } catch (final RuntimeException exception) {
      hooks.warnLogger().accept(
          "The configuration upgrade did not finish. The migration version was not advanced.",
          exception);
      return false;
    }
  }

  public boolean run(final MigrationRun<Context> run) {
    final List<Migration<Context>> migrations = MIGRATIONS.stream()
        .sorted((first, second) -> VersionNumber.parse(first.targetVersion())
            .compareTo(VersionNumber.parse(second.targetVersion())))
        .toList();
    final VersionNumber current = VersionNumber.parse(run.currentVersion());
    final List<Migration<Context>> pendingMigrations = migrations.stream()
        .filter(migration -> VersionNumber.parse(run.previousVersion()).isBefore(migration.targetVersion()))
        .filter(migration -> VersionNumber.parse(migration.targetVersion()).compareTo(current) <= 0)
        .toList();

    if (!pendingMigrations.isEmpty()) {
      run.backupBeforeMigrating().accept(pendingMigrations.get(pendingMigrations.size() - 1).targetVersion());
    }

    boolean changed = false;
    for (final Migration<Context> migration : pendingMigrations) {
      run.log().accept("Running config migration up to <highlight>" + migration.targetVersion()
          + "</highlight> from version <highlight2>" + displayVersion(run.previousVersion())
          + "</highlight2>...");
      changed |= migration.migrate(run.context());
    }

    if (changed) {
      run.log().accept("Config migrations completed and changed one or more category files.");
    }
    if (VersionNumber.parse(run.previousVersion()).isBefore(run.currentVersion())) {
      run.saveMigrationVersion().accept(run.currentVersion());
    }
    return changed;
  }

  private static String displayVersion(final String version) {
    return version == null || version.isBlank() ? "unknown" : version;
  }

  public static String dataVersion(final YamlConfig configuration) {
    if (configuration == null) {
      return "0.0.0";
    }
    return cleanVersion(configuration.getString(DATA_VERSION, "0.0.0"));
  }

  public static void saveDataVersion(final YamlConfig configuration, final String version) {
    if (configuration == null) {
      return;
    }
    configuration.set(DATA_VERSION, cleanVersion(version));
    configuration.setComments(
        DATA_VERSION,
        List.of("Do not modify this line. NotQuests uses it to know which one-time data migrations already ran."));
  }

  private static String cleanVersion(final String version) {
    return version == null || version.isBlank() ? "0.0.0" : version;
  }

  public interface MigrationHooks {
    Path dataFolder();

    YamlConfig generalConfig();

    String currentVersion();

    Consumer<String> infoLogger();

    BiConsumer<String, Exception> warnLogger();

    void backup(String targetVersion);

    void saveGeneralConfig();
  }

  public interface Migration<C> {
    String targetVersion();

    boolean migrate(C context);
  }

  public record Context(
      Path dataFolder,
      YamlConfig generalConfig,
      BiConsumer<String, Exception> warnLogger) {}

  public record MigrationRun<C>(
      String previousVersion,
      String currentVersion,
      C context,
      Consumer<String> log,
      Consumer<String> backupBeforeMigrating,
      Consumer<String> saveMigrationVersion) {
    public MigrationRun {
      log = log == null ? ignored -> {} : log;
      backupBeforeMigrating = backupBeforeMigrating == null ? ignored -> {} : backupBeforeMigrating;
      saveMigrationVersion = saveMigrationVersion == null ? ignored -> {} : saveMigrationVersion;
      Objects.requireNonNull(context, "context");
    }
  }
}
