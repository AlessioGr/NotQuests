package com.notquests.core.structs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class QuestCooldownDisplayTest {
    @Test
    void noCooldownBucketIsUsedWhenQuestIsAcceptable() {
        final Quest.CooldownDisplay display = Quest.CooldownDisplay.from(
                new Quest.AcceptCheck(Quest.AcceptCheck.Status.ACCEPTABLE, 0, 0, 0, 0));

        assertEquals(Quest.CooldownDisplay.Bucket.NO_COOLDOWN, display.bucket());
        assertEquals("", display.value());
    }

    @Test
    void choosesMinuteBuckets() {
        assertEquals(
                Quest.CooldownDisplay.Bucket.MINUTE,
                Quest.CooldownDisplay.from(cooldown(1)).bucket());

        final Quest.CooldownDisplay minutes = Quest.CooldownDisplay.from(cooldown(59));
        assertEquals(Quest.CooldownDisplay.Bucket.MINUTES, minutes.bucket());
        assertEquals("59", minutes.value());
    }

    @Test
    void choosesHourBuckets() {
        assertEquals(
                Quest.CooldownDisplay.Bucket.HOUR,
                Quest.CooldownDisplay.from(cooldown(60)).bucket());

        final Quest.CooldownDisplay hours = Quest.CooldownDisplay.from(cooldown(90));
        assertEquals(Quest.CooldownDisplay.Bucket.HOURS, hours.bucket());
        assertEquals("1.5", hours.value());
    }

    @Test
    void choosesDayBuckets() {
        assertEquals(
                Quest.CooldownDisplay.Bucket.DAY,
                Quest.CooldownDisplay.from(cooldown(1440)).bucket());

        final Quest.CooldownDisplay days = Quest.CooldownDisplay.from(cooldown(2880));
        assertEquals(Quest.CooldownDisplay.Bucket.DAYS, days.bucket());
        assertEquals("2.0", days.value());
    }

    @Test
    void formatsWithExistingTranslationComposition() {
        assertEquals(
                "prefixminutes:59",
                Quest.CooldownDisplay.from(cooldown(59)).format(new Text()));
        assertEquals(
                "hours:1.5",
                Quest.CooldownDisplay.from(cooldown(90)).format(new Text()));
    }

    private static Quest.AcceptCheck cooldown(final long minutes) {
        return new Quest.AcceptCheck(Quest.AcceptCheck.Status.COOLDOWN, 0, 0, 0, minutes);
    }

    private static final class Text implements Quest.CooldownDisplay.Text {
        @Override
        public String prefix() {
            return "prefix";
        }

        @Override
        public String noCooldown() {
            return "no-cooldown";
        }

        @Override
        public String minute() {
            return "minute";
        }

        @Override
        public String minutes(final String minutes) {
            return "minutes:" + minutes;
        }

        @Override
        public String hour() {
            return "hour";
        }

        @Override
        public String hours(final String hours) {
            return "hours:" + hours;
        }

        @Override
        public String day() {
            return "day";
        }

        @Override
        public String days(final String days) {
            return "days:" + days;
        }
    }
}
