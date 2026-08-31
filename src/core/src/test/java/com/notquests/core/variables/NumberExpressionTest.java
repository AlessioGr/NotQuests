package com.notquests.core.variables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry;

/**
 * Tests NotQuests' real expression engine ({@link NumberExpression}, backed by Crunch) — the code
 * behind every number/boolean condition and objective.
 */
class NumberExpressionTest {

  private NotQuestsAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new NotQuestsRegistry().createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
  }

  @Test
  @DisplayName("evaluates constant arithmetic expressions")
  void evaluatesArithmetic() {
    assertEquals(8.0, new NumberExpression(adapter, "5 + 3").calculateValue(null));
    assertEquals(2.5, new NumberExpression(adapter, "10 / 4").calculateValue(null));
    assertEquals(8.0, new NumberExpression(adapter, "2 ^ 3").calculateValue(null));
  }

  @Test
  @DisplayName("boolean value uses the >= 0.98 threshold")
  void booleanThreshold() {
    assertTrue(new NumberExpression(adapter, "1").calculateBooleanValue(null), "1 -> true");
    assertTrue(new NumberExpression(adapter, "0.98").calculateBooleanValue(null), "0.98 -> true");
    assertFalse(new NumberExpression(adapter, "0.97").calculateBooleanValue(null), "0.97 -> false");
    assertFalse(new NumberExpression(adapter, "0").calculateBooleanValue(null), "0 -> false");
  }

  @Test
  @DisplayName("keeps the original raw expression string")
  void keepsRawExpression() {
    assertEquals("5 + 3", new NumberExpression(adapter, "5 + 3").getRawExpression());
  }

  @Test
  @DisplayName("evaluates variable references and expression-based variable arguments")
  void evaluatesVariableReferencesAndArguments() {
    adapter.variables()
        .numberVariable("BaseNumber")
        .displayName("Base Number")
        .description("Expression input.")
        .singular("Base Number")
        .plural("Base Numbers")
        .get((questPlayer, objects) -> 5)
        .register();
    adapter.variables()
        .numberVariable("ArgumentNumber")
        .displayName("Argument Number")
        .description("Returns its evaluated argument.")
        .singular("Argument Number")
        .plural("Argument Numbers")
        .field("amount", adapter.fields().numberExpression(), "Expression argument.")
        .get(context -> context.number("amount", 0))
        .register();
    adapter.variables()
        .booleanVariable("ArgumentBoolean")
        .displayName("Argument Boolean")
        .description("Returns its evaluated argument.")
        .singular("Argument Boolean")
        .plural("Argument Booleans")
        .field("enabled", adapter.fields().booleanExpression(), "Boolean expression argument.")
        .get(context -> context.bool("enabled", false))
        .register();

    assertEquals(
        8.0d,
        new NumberExpression(adapter, "BaseNumber + ArgumentNumber(amount:2+1)")
            .calculateValue(null));
    assertTrue(new NumberExpression(adapter, "ArgumentBoolean(enabled:0.98)")
        .calculateBooleanValue(null));
    assertFalse(new NumberExpression(adapter, "ArgumentBoolean(enabled:0.97)")
        .calculateBooleanValue(null));
  }
}
