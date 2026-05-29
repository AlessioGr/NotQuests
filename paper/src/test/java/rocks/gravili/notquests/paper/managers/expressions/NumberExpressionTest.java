/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * Licensed under the GNU General Public License v3. See the LICENSE file.
 */

package rocks.gravili.notquests.paper.managers.expressions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import rocks.gravili.notquests.paper.NotQuests;

/**
 * Tests NotQuests' real expression engine ({@link NumberExpression}, backed by Crunch) — the code
 * behind every number/boolean condition and objective. The only collaborator the constructor needs
 * is the variables manager (to scan for variable names), which we stub to "no variables" so we can
 * verify pure math + the boolean threshold without a running server.
 */
class NumberExpressionTest {

  private NotQuests main;

  @BeforeEach
  void setUp() {
    main = mock(NotQuests.class, RETURNS_DEEP_STUBS);
    // No NotQuests variables registered -> expressions are treated as static math.
    when(main.getVariablesManager().getVariableIdentifiers()).thenReturn(List.of());
  }

  @Test
  @DisplayName("evaluates constant arithmetic expressions")
  void evaluatesArithmetic() {
    assertEquals(8.0, new NumberExpression(main, "5 + 3").calculateValue(null));
    assertEquals(2.5, new NumberExpression(main, "10 / 4").calculateValue(null));
    assertEquals(8.0, new NumberExpression(main, "2 ^ 3").calculateValue(null));
  }

  @Test
  @DisplayName("boolean value uses the >= 0.98 threshold")
  void booleanThreshold() {
    assertTrue(new NumberExpression(main, "1").calculateBooleanValue(null), "1 -> true");
    assertTrue(new NumberExpression(main, "0.98").calculateBooleanValue(null), "0.98 -> true");
    assertFalse(new NumberExpression(main, "0.97").calculateBooleanValue(null), "0.97 -> false");
    assertFalse(new NumberExpression(main, "0").calculateBooleanValue(null), "0 -> false");
  }

  @Test
  @DisplayName("keeps the original raw expression string")
  void keepsRawExpression() {
    assertEquals("5 + 3", new NumberExpression(main, "5 + 3").getRawExpression());
  }
}
