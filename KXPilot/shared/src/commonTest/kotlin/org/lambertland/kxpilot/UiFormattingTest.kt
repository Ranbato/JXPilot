package org.lambertland.kxpilot

import org.lambertland.kxpilot.ui.util.formatUptime
import org.lambertland.kxpilot.ui.util.pad2
import org.lambertland.kxpilot.ui.util.roundOne
import kotlin.test.Test
import kotlin.test.assertEquals

class UiFormattingTest {
    // -----------------------------------------------------------------------
    // pad2
    // -----------------------------------------------------------------------

    @Test fun pad2_singleDigit() = assertEquals("05", 5L.pad2())

    @Test fun pad2_zero() = assertEquals("00", 0L.pad2())

    @Test fun pad2_tenIsNotPadded() = assertEquals("10", 10L.pad2())

    @Test fun pad2_largeValue() = assertEquals("59", 59L.pad2())

    // -----------------------------------------------------------------------
    // roundOne
    // -----------------------------------------------------------------------

    @Test fun roundOne_integer() = assertEquals("1.0", 1.0.roundOne())

    @Test fun roundOne_oneDecimal() = assertEquals("2.5", 2.5.roundOne())

    @Test fun roundOne_roundsUp() = assertEquals("1.4", 1.35.roundOne())

    @Test fun roundOne_zero() = assertEquals("0.0", 0.0.roundOne())

    @Test fun roundOne_negative() = assertEquals("-1.5", (-1.5).roundOne())

    @Test fun roundOne_negativeNearZero() = assertEquals("-0.3", (-0.3).roundOne())

    @Test fun roundOne_largeValue() = assertEquals("25.0", 25.0.roundOne())

    // -----------------------------------------------------------------------
    // formatUptime
    // -----------------------------------------------------------------------

    @Test fun formatUptime_zero() = assertEquals("00:00:00", formatUptime(0L))

    @Test fun formatUptime_oneSecond() = assertEquals("00:00:01", formatUptime(1_000L))

    @Test fun formatUptime_oneMinute() = assertEquals("00:01:00", formatUptime(60_000L))

    @Test fun formatUptime_oneHour() = assertEquals("01:00:00", formatUptime(3_600_000L))

    @Test fun formatUptime_complex() = assertEquals("01:02:03", formatUptime(3_723_000L))

    @Test fun formatUptime_subSecondTruncated() = assertEquals("00:00:00", formatUptime(999L))

    @Test fun formatUptime_largeHours() = assertEquals("10:00:00", formatUptime(36_000_000L))
}
