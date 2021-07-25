package com.mshernandez.vertconomy.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/**
 * Tests designed to ensure a configured Vertconomy
 * instance properly parses sat amounts from Strings.
 */
public class VertcoinFormatterTest
{
    private static final String TEST_SYMBOL = "VTC";
    private static final String TEST_BASE_UNIT = "sat";

    public SatAmountFormatter createTestFormatter(CoinScale testScale)
    {
        return new VertconomyFormatter(testScale, TEST_SYMBOL, TEST_BASE_UNIT);
    }

    @Test
    public void formatSatAmountFullScale()
    {
        SatAmountFormatter formatter = createTestFormatter(CoinScale.FULL);
        assertEquals("1.23456789 VTC", formatter.format(123456789L));
        assertEquals("0.12345678 VTC", formatter.format(12345678L));
        assertEquals("0.00001234 VTC", formatter.format(1234L));
        assertEquals("0.00000001 VTC", formatter.format(1L));
        assertEquals("0.00000000 VTC", formatter.format(0L));
    }

    @Test
    public void formatSatAmountMilliScale()
    {
        SatAmountFormatter formatter = createTestFormatter(CoinScale.MILLI);
        assertEquals("1234.56789 mVTC", formatter.format(123456789L));
        assertEquals("0.01234 mVTC", formatter.format(1234L));
        assertEquals("0.00001 mVTC", formatter.format(1L));
        assertEquals("0.00000 mVTC", formatter.format(0L));
    }

    @Test
    public void formatSatAmountMicroScale()
    {
        SatAmountFormatter formatter = createTestFormatter(CoinScale.MICRO);
        assertEquals("1234567.89 µVTC", formatter.format(123456789L));
        assertEquals("0.12 µVTC", formatter.format(12L));
        assertEquals("0.01 µVTC", formatter.format(1L));
        assertEquals("0.00 µVTC", formatter.format(0L));
    }

    @Test
    public void formatSatAmountBaseScale()
    {
        SatAmountFormatter formatter = createTestFormatter(CoinScale.BASE);
        assertEquals("123456789 sat", formatter.format(123456789L));
        assertEquals("1234 sat", formatter.format(1234L));
        assertEquals("1 sat", formatter.format(1L));
        assertEquals("0 sat", formatter.format(0L));
    }

    @Test
    public void formatRelativeAmountFullScale()
    {
        SatAmountFormatter formatter = createTestFormatter(CoinScale.FULL);
        assertEquals("0.00000000 VTC", formatter.format(BigDecimal.ZERO));
        assertEquals("0.00000001 VTC", formatter.format(new BigDecimal("0.00000001")));
        assertEquals("0.50000000 VTC", formatter.format(new BigDecimal("0.5")));
        assertEquals("1.00000000 VTC", formatter.format(new BigDecimal("1.0")));
        assertEquals("1.00000000 VTC", formatter.format(new BigDecimal("1.000000001")));
        assertEquals("1.00000000 VTC", formatter.format(new BigDecimal("1.000000009999")));
    }

    @Test
    public void formatRelativeAmountMilliScale()
    {
        SatAmountFormatter formatter = createTestFormatter(CoinScale.MILLI);
        assertEquals("0.00000 mVTC", formatter.format(BigDecimal.ZERO));
        assertEquals("0.00001 mVTC", formatter.format(new BigDecimal("0.00001")));
        assertEquals("0.50000 mVTC", formatter.format(new BigDecimal("0.5")));
        assertEquals("1.01000 mVTC", formatter.format(new BigDecimal("1.01")));
        assertEquals("1.01000 mVTC", formatter.format(new BigDecimal("1.010001")));
        assertEquals("1.01000 mVTC", formatter.format(new BigDecimal("1.010009999")));
    }

    @Test
    public void formatRelativeAmountMicroScale()
    {
        SatAmountFormatter formatter = createTestFormatter(CoinScale.MICRO);
        assertEquals("0.00 µVTC", formatter.format(BigDecimal.ZERO));
        assertEquals("0.01 µVTC", formatter.format(new BigDecimal("0.01")));
        assertEquals("0.50 µVTC", formatter.format(new BigDecimal("0.5")));
        assertEquals("1.01 µVTC", formatter.format(new BigDecimal("1.01")));
        assertEquals("1.01 µVTC", formatter.format(new BigDecimal("1.011")));
        assertEquals("1.01 µVTC", formatter.format(new BigDecimal("1.019999")));
    }

    @Test
    public void formatRelativeAmountBaseScale()
    {
        SatAmountFormatter formatter = createTestFormatter(CoinScale.BASE);
        assertEquals("0 sat", formatter.format(BigDecimal.ZERO));
        assertEquals("1 sat", formatter.format(new BigDecimal("1")));
        assertEquals("1000 sat", formatter.format(new BigDecimal("1000")));
        assertEquals("1000 sat", formatter.format(new BigDecimal("1000.1")));
        assertEquals("1000 sat", formatter.format(new BigDecimal("1000.9999")));
    }

    @Test
    public void parseValidRelativeAmountFullScale()
    {
        SatAmountFormatter formatter = createTestFormatter(CoinScale.FULL);
        parseValidRelativeAmount(formatter, "0", 0L);
        parseValidRelativeAmount(formatter, "0.00000001", 1L);
        parseValidRelativeAmount(formatter, "12", 1200000000L);
        parseValidRelativeAmount(formatter, "1.243", 124300000L);
        parseValidRelativeAmount(formatter, "100.12345678", 10012345678L);
    }

    @Test
    public void parseInvalidRelativeAmountFullScale()
    {
        SatAmountFormatter formatter = createTestFormatter(CoinScale.FULL);
        parseInvalidRelativeAmount(formatter, "a");
        parseInvalidRelativeAmount(formatter, "100.01q4");
        parseInvalidRelativeAmount(formatter, "-1");
        parseInvalidRelativeAmount(formatter, "100.123456789");
    }

    @Test
    public void parseValidRelativeAmountMilliScale()
    {
        SatAmountFormatter formatter = createTestFormatter(CoinScale.MILLI);
        parseValidRelativeAmount(formatter, "0", 0L);
        parseValidRelativeAmount(formatter, "1.243", 124300L);
        parseValidRelativeAmount(formatter, "100.243", 10024300L);
    }

    @Test
    public void parseInvalidRelativeAmountMilliScale()
    {
        SatAmountFormatter formatter = createTestFormatter(CoinScale.MILLI);
        parseInvalidRelativeAmount(formatter, "a");
        parseInvalidRelativeAmount(formatter, "100.01q");
        parseInvalidRelativeAmount(formatter, "-1");
        parseInvalidRelativeAmount(formatter, "100.243242");
    }

    @Test
    public void parseValidRelativeAmountMicroScale()
    {
        SatAmountFormatter formatter = createTestFormatter(CoinScale.MICRO);
        parseValidRelativeAmount(formatter, "0", 0L);
        parseValidRelativeAmount(formatter, "1.24", 124L);
        parseValidRelativeAmount(formatter, "140.24", 14024L);
    }

    @Test
    public void parseInvalidRelativeAmountMicroScale()
    {
        SatAmountFormatter formatter = createTestFormatter(CoinScale.MICRO);
        parseInvalidRelativeAmount(formatter, "a");
        parseInvalidRelativeAmount(formatter, "100.1q");
        parseInvalidRelativeAmount(formatter, "-1");
        parseInvalidRelativeAmount(formatter, "100.243");
    }

    @Test
    public void parseValidRelativeAmountBaseScale()
    {
        SatAmountFormatter formatter = createTestFormatter(CoinScale.BASE);
        parseValidRelativeAmount(formatter, "0", 0L);
        parseValidRelativeAmount(formatter, "1243", 1243L);
    }

    @Test
    public void parseInvalidRelativeAmountBaseScale()
    {
        SatAmountFormatter formatter = createTestFormatter(CoinScale.BASE);
        parseInvalidRelativeAmount(formatter, "a");
        parseInvalidRelativeAmount(formatter, "-1");
        parseInvalidRelativeAmount(formatter, "1.1");
    }

    /**
     * Attempt to parse a particular input into a
     * sat amount, expecting the operation to be
     * successful and provide a specific result.
     * 
     * @param formatter A configured instance to parse amounts.
     * @param input The string to parse into a sat amount.
     * @param expected The expected number of sats.
     */
    public void parseValidRelativeAmount(SatAmountFormatter formatter, String input, long expected)
    {
        try
        {
            long satAmount = formatter.parseSats(input);
            assertEquals(expected, satAmount);
        }
        catch (InvalidSatAmountException e)
        {
            fail("Exception parsing valid input: " + input);
        }
    }

    /**
     * Attempt to parse a particular input into a
     * sat amount, expecting failure.
     * 
     * @param formatter A configured instance to parse amounts.
     * @param input The string to parse into a sat amount.
     */
    public void parseInvalidRelativeAmount(SatAmountFormatter formatter, String input)
    {
        try
        {
            formatter.parseSats(input);
            fail("No exception on parsing invalid input: " + input);
        }
        catch (InvalidSatAmountException e)
        {
            // All Good
        }
    }
}
