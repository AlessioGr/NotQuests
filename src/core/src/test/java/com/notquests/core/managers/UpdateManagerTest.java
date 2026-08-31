package com.notquests.core.managers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.core.managers.UpdateManager.Check;

class UpdateManagerTest {
  @Test
  void detectsNewerLatestVersion() throws Exception {
    final UpdateManager checker = new UpdateManager(() -> "6.3.1");

    final Check check = checker.check("6.3.0-26.1.2-paper");

    assertTrue(check.updateAvailable());
  }

  @Test
  void treatsSameLeadingVersionWithBuildSuffixAsCurrent() throws Exception {
    final UpdateManager checker = new UpdateManager(() -> "6.3.0");

    final Check check = checker.check("6.3.0-26.1.2-paper");

    assertFalse(check.updateAvailable());
  }

  @Test
  void reportsAStableReleaseAsAnUpdateForAPrereleaseBuild() throws Exception {
    final UpdateManager checker = new UpdateManager(() -> "7.0.0");

    final Check check = checker.check("7.0.0-beta.1-paper");

    assertTrue(check.updateAvailable());
  }
}
