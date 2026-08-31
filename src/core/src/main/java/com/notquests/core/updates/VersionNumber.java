package com.notquests.core.updates;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record VersionNumber(int major, int minor, int patch, List<String> prerelease)
    implements Comparable<VersionNumber> {
  private static final Pattern VERSION = Pattern.compile(
      "^(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:-([^+]+))?(?:\\+.*)?$");
  private static final Set<String> PRERELEASE_LABELS = Set.of(
      "alpha", "a", "beta", "b", "dev", "pre", "preview", "rc", "snapshot");
  private static final Set<String> PLATFORM_LABELS = Set.of(
      "bukkit", "fabric", "forge", "neoforge", "paper", "spigot");

  public VersionNumber {
    prerelease = prerelease == null ? List.of() : List.copyOf(prerelease);
  }

  public VersionNumber(final int major, final int minor, final int patch) {
    this(major, minor, patch, List.of());
  }

  public static VersionNumber parse(final String rawVersion) {
    if (rawVersion == null || rawVersion.isBlank()) {
      return new VersionNumber(0, 0, 0);
    }
    final Matcher matcher = VERSION.matcher(rawVersion.trim());
    if (!matcher.matches()) {
      return new VersionNumber(0, 0, 0);
    }
    return new VersionNumber(
        parsePart(matcher.group(1)),
        parsePart(matcher.group(2)),
        parsePart(matcher.group(3)),
        parsePrerelease(matcher.group(4)));
  }

  public boolean isBefore(final String otherVersion) {
    return compareTo(parse(otherVersion)) < 0;
  }

  @Override
  public int compareTo(final VersionNumber other) {
    int result = Integer.compare(major, other.major);
    if (result != 0) {
      return result;
    }
    result = Integer.compare(minor, other.minor);
    if (result != 0) {
      return result;
    }
    result = Integer.compare(patch, other.patch);
    if (result != 0) {
      return result;
    }
    if (prerelease.isEmpty()) {
      return other.prerelease.isEmpty() ? 0 : 1;
    }
    if (other.prerelease.isEmpty()) {
      return -1;
    }
    final int sharedIdentifiers = Math.min(prerelease.size(), other.prerelease.size());
    for (int index = 0; index < sharedIdentifiers; index++) {
      result = comparePrereleaseIdentifier(prerelease.get(index), other.prerelease.get(index));
      if (result != 0) {
        return result;
      }
    }
    return Integer.compare(prerelease.size(), other.prerelease.size());
  }

  private static List<String> parsePrerelease(final String suffix) {
    if (suffix == null || suffix.isBlank()) {
      return List.of();
    }
    final String[] identifiers = suffix.toLowerCase(Locale.ROOT).split("[.-]");
    if (identifiers.length == 0 || !PRERELEASE_LABELS.contains(identifiers[0])) {
      // Server-platform versions such as 7.0.0-26.2-paper are builds of 7.0.0,
      // not prereleases of it.
      return List.of();
    }
    final ArrayList<String> prerelease = new ArrayList<>(List.of(identifiers));
    while (!prerelease.isEmpty()
        && PLATFORM_LABELS.contains(prerelease.get(prerelease.size() - 1))) {
      prerelease.remove(prerelease.size() - 1);
    }
    return List.copyOf(prerelease);
  }

  private static int comparePrereleaseIdentifier(final String first, final String second) {
    final boolean firstNumeric = isNumeric(first);
    final boolean secondNumeric = isNumeric(second);
    if (firstNumeric && secondNumeric) {
      final String normalizedFirst = trimLeadingZeroes(first);
      final String normalizedSecond = trimLeadingZeroes(second);
      final int lengthComparison = Integer.compare(normalizedFirst.length(), normalizedSecond.length());
      return lengthComparison != 0
          ? lengthComparison
          : normalizedFirst.compareTo(normalizedSecond);
    }
    if (firstNumeric != secondNumeric) {
      return firstNumeric ? -1 : 1;
    }
    return first.compareTo(second);
  }

  private static boolean isNumeric(final String value) {
    if (value == null || value.isEmpty()) {
      return false;
    }
    for (int index = 0; index < value.length(); index++) {
      if (!Character.isDigit(value.charAt(index))) {
        return false;
      }
    }
    return true;
  }

  private static String trimLeadingZeroes(final String value) {
    int firstNonZero = 0;
    while (firstNonZero < value.length() - 1 && value.charAt(firstNonZero) == '0') {
      firstNonZero++;
    }
    return value.substring(firstNonZero);
  }

  private static int parsePart(final String value) {
    if (value == null || value.isBlank()) {
      return 0;
    }
    try {
      return Integer.parseInt(value);
    } catch (final NumberFormatException ignored) {
      return 0;
    }
  }
}
