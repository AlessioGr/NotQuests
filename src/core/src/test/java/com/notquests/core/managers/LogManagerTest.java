package com.notquests.core.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.notquests.core.config.YamlConfig;

import java.util.Map;
import java.util.logging.Level;

class LogManagerTest {
    @Test
    void recordsWarningsAndErrorsAsImmutableSnapshots() {
        final LogManager history = new LogManager(new ConfigurationManager());

        history.recordError("error one");
        history.recordWarning("warning one");
        history.recordWarning("");
        history.recordError(null);

        assertEquals(1, history.errorLogs().size());
        assertEquals(1, history.warningLogs().size());
        assertEquals("error one", history.errorLogs().getFirst());
        assertEquals("warning one", history.warningLogs().getFirst());
        assertThrows(UnsupportedOperationException.class, () -> history.errorLogs().add("mutate"));
    }

    @Test
    void clearsCollectedHistory() {
        final LogManager history = new LogManager(new ConfigurationManager());

        history.recordError("error one");
        history.recordWarning("warning one");
        history.clear();

        assertEquals(0, history.errorLogs().size());
        assertEquals(0, history.warningLogs().size());
    }

    @Test
    void preparesTheCompleteConsoleRenderingChoiceForAdapters() {
        final ConfigurationManager configuration = new ConfigurationManager();
        configuration.loadFrom(YamlConfig.fromMap(Map.of(
                "visual", Map.of("colors", Map.of("console", Map.of("enabled", false))))));
        final LogManager logs = new LogManager(configuration);

        final LogManager.ConsoleLine line = logs.consoleLine(
                Level.INFO,
                LogManager.LogCategory.DEFAULT,
                "<main>Hello</main>");

        assertEquals("[NotQuests]: ", line.prefix());
        assertEquals("Hello", line.message());
        assertEquals(false, line.miniMessage());
        assertEquals(false, line.downsampleColors());
    }
}
