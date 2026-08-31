package com.notquests.core.updates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VersionNumberTest {
  @Test
  void comparesSemanticVersionsAndIgnoresBuildSuffixes() {
    assertTrue(VersionNumber.parse("6.3.1").isBefore("6.4.0"));
    assertTrue(VersionNumber.parse("6.3.0").isBefore("6.3.1"));
    assertFalse(VersionNumber.parse("6.3.0-26.1.2-paper").isBefore("6.3.0"));
    assertFalse(VersionNumber.parse("not-a-version").isBefore("0.0.0"));
  }

  @Test
  void ordersPrereleasesBeforeTheStableRelease() {
    assertTrue(VersionNumber.parse("7.0.0-beta.1").isBefore("7.0.0-rc.1"));
    assertTrue(VersionNumber.parse("7.0.0-rc.1").isBefore("7.0.0"));
    assertTrue(VersionNumber.parse("7.0.0-beta.2").isBefore("7.0.0-beta.10"));
    assertTrue(VersionNumber.parse("7.0.0-beta.99999999999999999999")
        .isBefore("7.0.0-beta.100000000000000000000"));
    assertFalse(VersionNumber.parse("7.0.0").isBefore("7.0.0-rc.99"));
  }

  @Test
  void ignoresPlatformAndBuildSuffixesWithoutIgnoringPrereleases() {
    assertEquals(
        VersionNumber.parse("6.3.0"),
        VersionNumber.parse("6.3.0-26.1.2-paper"));
    assertEquals(
        VersionNumber.parse("6.3.0"),
        VersionNumber.parse("6.3.0-26.1.2-neoforge"));
    assertEquals(
        VersionNumber.parse("7.0.0"),
        VersionNumber.parse("7.0.0-paper+build.42"));
    assertTrue(VersionNumber.parse("7.0.0-beta.1-paper").isBefore("7.0.0"));
  }
}
