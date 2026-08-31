package com.notquests.core.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.core.commands.framework.*;

import java.util.List;

class DoubleDashFlagParserTest {
    @Test
    void parsesPresenceAndValueFlags() {
        final DoubleDashFlagParser.ParseResult parsed = DoubleDashFlagParser.parse(
                "--silent --delay 500ms",
                List.of(new Flag("silent", true), new Flag("delay", false)),
                Flag::name,
                Flag::presence,
                (flag, raw) -> raw);

        assertTrue(parsed.present().contains("silent"));
        assertTrue(parsed.present().contains("delay"));
        assertEquals("500ms", parsed.values().get("delay"));
        assertFalse(parsed.values().containsKey("silent"));
    }

    @Test
    void consumesMultiWordValuesUntilNextFlag() {
        final DoubleDashFlagParser.ParseResult parsed = DoubleDashFlagParser.parse(
                "--nametag_equals Angry Zombie --delay 1s",
                List.of(new Flag("nametag_equals", false), new Flag("delay", false)),
                Flag::name,
                Flag::presence,
                (flag, raw) -> raw);

        assertEquals("Angry Zombie", parsed.values().get("nametag_equals"));
        assertEquals("1s", parsed.values().get("delay"));
    }

    @Test
    void ignoresUnknownFlagsAndBadValues() {
        final DoubleDashFlagParser.ParseResult parsed = DoubleDashFlagParser.parse(
                "--unknown x --amount bad --silent",
                List.of(new Flag("amount", false), new Flag("silent", true)),
                Flag::name,
                Flag::presence,
                (flag, raw) -> raw.equals("bad") ? null : raw);

        assertTrue(parsed.present().contains("amount"));
        assertTrue(parsed.present().contains("silent"));
        assertFalse(parsed.present().contains("unknown"));
        assertFalse(parsed.values().containsKey("amount"));
    }

    @Test
    void identifiesTheFlagWhoseValueIsBeingTyped() {
        final List<Flag> flags = List.of(new Flag("silent", true), new Flag("delay", false));

        assertEquals(
                "delay",
                DoubleDashFlagParser.awaitingValue("--delay ", flags, Flag::name, Flag::presence).name());
        assertEquals(
                "delay",
                DoubleDashFlagParser.awaitingValue("--delay 50", flags, Flag::name, Flag::presence).name());
        assertEquals(null, DoubleDashFlagParser.awaitingValue("--silent ", flags, Flag::name, Flag::presence));
        assertEquals(8, DoubleDashFlagParser.currentTokenStart("--delay 50"));
        assertEquals(11, DoubleDashFlagParser.currentTokenStart("--delay 50 "));
    }

    private record Flag(String name, boolean presence) {}
}
