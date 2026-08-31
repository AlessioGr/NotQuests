package com.notquests.core.managers;

import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.updates.VersionNumber;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Owns NotQuests update checks, status, messages, cadence, and notification decisions. */
public final class UpdateManager {
  public static final long STARTUP_DELAY_TICKS = 100L;
  public static final long CHECK_INTERVAL_TICKS = 8 * 60 * 60 * 20L;
  public static final long OP_JOIN_NOTIFICATION_DELAY_TICKS = 60L;
  private static final String UPDATE_URL = "https://www.notquests.com/update/";

  private final LatestVersionSource latestVersionSource;
  private final Status status = new Status();

  public UpdateManager(final LatestVersionSource latestVersionSource) {
    this.latestVersionSource = Objects.requireNonNull(latestVersionSource, "latestVersionSource");
  }

  public static UpdateManager notQuestsWebsite() {
    return fromUrl(URI.create("https://www.notquests.com/latest-version.txt"), Duration.ofSeconds(10));
  }

  public static UpdateManager fromUrl(final URI latestVersionUri, final Duration timeout) {
    return new UpdateManager(() -> {
      final HttpClient client = HttpClient.newBuilder()
          .connectTimeout(timeout)
          .build();
      final HttpRequest request = HttpRequest.newBuilder()
          .uri(latestVersionUri)
          .timeout(timeout)
          .GET()
          .build();
      final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new IllegalStateException("Unexpected update check response: HTTP " + response.statusCode());
      }
      return response.body().trim();
    });
  }

  public Check check(final String currentVersion) throws Exception {
    status.currentVersion(currentVersion);
    final String latestVersion = latestVersionSource.latestVersion().trim();
    final boolean updateAvailable =
        VersionNumber.parse(latestVersion).compareTo(VersionNumber.parse(currentVersion)) > 0;
    final Check check = new Check(currentVersion, latestVersion, updateAvailable);
    status.update(check);
    return check;
  }

  public Status status() {
    return status;
  }

  public String checkAndConsoleMessage(final String currentVersion) throws Exception {
    check(currentVersion);
    return status.updateAvailable()
        ? consoleUpdateAvailable(status.currentVersion(), status.latestVersion())
        : "";
  }

  public String checkAndChatMessage(final String currentVersion) throws Exception {
    check(currentVersion);
    return chatMessage(currentVersion);
  }

  public String chatMessage(final String currentVersion) {
    status.currentVersion(currentVersion);
    return status.updateAvailable()
        ? chatUpdateAvailable(status.currentVersion(), status.latestVersion())
        : upToDate(status.currentVersion());
  }

  public String availableChatMessage(final String currentVersion) {
    status.currentVersion(currentVersion);
    return status.updateAvailable()
        ? chatUpdateAvailable(status.currentVersion(), status.latestVersion())
        : "";
  }

  public boolean shouldNotifyOperator(final boolean notificationsEnabled) {
    return notificationsEnabled && status.updateAvailable();
  }

  /** Starts recurring core-owned update checks using only an adapter scheduling leaf. */
  public void start(
      final String currentVersion,
      final BiConsumer<Duration, Runnable> asyncScheduler,
      final BooleanSupplier shuttingDown,
      final Consumer<String> info,
      final Consumer<String> warn) {
    status.currentVersion(currentVersion);
    if (asyncScheduler == null) {
      return;
    }
    schedule(
        currentVersion,
        asyncScheduler,
        shuttingDown == null ? () -> false : shuttingDown,
        info == null ? ignored -> {} : info,
        warn == null ? ignored -> {} : warn,
        Duration.ofMillis(STARTUP_DELAY_TICKS * 50L));
  }

  private void schedule(
      final String currentVersion,
      final BiConsumer<Duration, Runnable> asyncScheduler,
      final BooleanSupplier shuttingDown,
      final Consumer<String> info,
      final Consumer<String> warn,
      final Duration delay) {
    asyncScheduler.accept(delay, () -> {
      if (shuttingDown.getAsBoolean()) {
        return;
      }
      try {
        final String message = checkAndConsoleMessage(currentVersion);
        if (!message.isBlank()) {
          info.accept(message);
        }
      } catch (final Exception exception) {
        warn.accept("Unable to check for updates: " + exception.getMessage());
      }
      schedule(
          currentVersion,
          asyncScheduler,
          shuttingDown,
          info,
          warn,
          Duration.ofMillis(CHECK_INTERVAL_TICKS * 50L));
    });
  }

  /** Schedules a delayed operator notification using the adapter scheduler only. */
  public void operatorJoined(
      final PlatformPlayer player,
      final boolean operator,
      final boolean notificationsEnabled,
      final BiConsumer<Duration, Runnable> scheduler) {
    if (!operator || player == null || scheduler == null || !shouldNotifyOperator(notificationsEnabled)) {
      return;
    }
    scheduler.accept(Duration.ofMillis(OP_JOIN_NOTIFICATION_DELAY_TICKS * 50L), () -> {
      if (!player.hasPlayer()) {
        return;
      }
      final String message = availableChatMessage(status.currentVersion());
      if (!message.isBlank()) {
        player.sendMessage(message);
      }
    });
  }

  public static String consoleUpdateAvailable(final String currentVersion, final String latestVersion) {
    return "<warn>A new version of NotQuests is available: <green>" + latestVersion
        + "</green> (current: <red>" + currentVersion + "</red>). "
        + "Download: <highlight2>https://www.notquests.com/update</highlight2>";
  }

  public static String chatUpdateAvailable(final String currentVersion, final String latestVersion) {
    return "<hover:show_text:\"<highlight>Click to update!\"><click:open_url:\"" + UPDATE_URL + "\">"
        + "<main>[NotQuests]</main> <warn>Your version <red>"
        + currentVersion
        + "</red> is not the latest version (<green>"
        + latestVersion
        + "</green>). <bold>Click this message to update: <underlined>"
        + UPDATE_URL
        + "</underlined></bold></click></hover>";
  }

  public static String upToDate(final String currentVersion) {
    return "<success>NotQuests is up to date! (version: <green>"
        + currentVersion
        + "</green>)";
  }

  @FunctionalInterface
  public interface LatestVersionSource {
    String latestVersion() throws Exception;
  }

  public record Check(String currentVersion, String latestVersion, boolean updateAvailable) {}

  public static final class Status {
    private volatile String currentVersion = "";
    private volatile String latestVersion = "";
    private volatile boolean updateAvailable;

    public void currentVersion(final String currentVersion) {
      this.currentVersion = currentVersion == null ? "" : currentVersion;
      if (latestVersion.isBlank()) {
        latestVersion = this.currentVersion;
      }
    }

    public void update(final Check check) {
      if (check == null) {
        return;
      }
      currentVersion = check.currentVersion();
      latestVersion = check.latestVersion();
      updateAvailable = check.updateAvailable();
    }

    public String currentVersion() {
      return currentVersion;
    }

    public String latestVersion() {
      return latestVersion;
    }

    public boolean updateAvailable() {
      return updateAvailable;
    }
  }
}
