package com.mshernandez.vertconomy.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

/**
 * Tests designed to ensure a configured Vertconomy
 * instance properly parses sat amounts from Strings.
 */
public class SatAmountFormatTest
{
    private static final String TEST_SYMBOL = "VTC";
    private static final String TEST_BASE_UNIT = "sat";

    public SatAmountFormat createTestFormatter(CoinScale testScale)
    {
        return new SatAmountFormat(testScale, TEST_SYMBOL, TEST_BASE_UNIT);
    }

    @Test
    public void formatSatAmount()
    {
        SatAmountFormat formatter = createTestFormatter(CoinScale.FULL);
        String formatted = formatter.format(123456789L);
        assertEquals("1.23456789 VTC", formatted);
    }

    @Test
    public void formatRelativeAmount()
    {
        SatAmountFormat formatter = createTestFormatter(CoinScale.FULL);
        String formatted = formatter.format(1.0);
        assertEquals("1.00000000 VTC", formatted);
    }

    @Test
    public void parseValidRelativeAmountFullScale()
    {
        SatAmountFormat formatter = createTestFormatter(CoinScale.FULL);
        parseValidRelativeAmount(formatter, "0", 0L);
        parseValidRelativeAmount(formatter, "12", 1200000000L);
        parseValidRelativeAmount(formatter, "1.243", 124300000L);
        parseValidRelativeAmount(formatter, "100.12345678", 10012345678L);
    }

    @Test
    public void parseInvalidRelativeAmountFullScale()
    {
        SatAmountFormat formatter = createTestFormatter(CoinScale.FULL);
        parseInvalidRelativeAmount(formatter, "a");
        parseInvalidRelativeAmount(formatter, "100.01q4");
        parseInvalidRelativeAmount(formatter, "-1");
        parseInvalidRelativeAmount(formatter, "100.123456789");
    }

    @Test
    public void parseValidRelativeAmountMilliScale()
    {
        SatAmountFormat formatter = createTestFormatter(CoinScale.MILLI);
        parseValidRelativeAmount(formatter, "0", 0L);
        parseValidRelativeAmount(formatter, "1.243", 124300L);
        parseValidRelativeAmount(formatter, "100.243", 10024300L);
    }

    @Test
    public void parseInvalidRelativeAmountMilliScale()
    {
        SatAmountFormat formatter = createTestFormatter(CoinScale.MILLI);
        parseInvalidRelativeAmount(formatter, "a");
        parseInvalidRelativeAmount(formatter, "100.01q");
        parseInvalidRelativeAmount(formatter, "-1");
        parseInvalidRelativeAmount(formatter, "100.243242");
    }

    @Test
    public void parseValidRelativeAmountMicroScale()
    {
        SatAmountFormat formatter = createTestFormatter(CoinScale.MICRO);
        parseValidRelativeAmount(formatter, "0", 0L);
        parseValidRelativeAmount(formatter, "1.24", 124L);
        parseValidRelativeAmount(formatter, "140.24", 14024L);
    }

    @Test
    public void parseInvalidRelativeAmountMicroScale()
    {
        SatAmountFormat formatter = createTestFormatter(CoinScale.MICRO);
        parseInvalidRelativeAmount(formatter, "a");
        parseInvalidRelativeAmount(formatter, "100.1q");
        parseInvalidRelativeAmount(formatter, "-1");
        parseInvalidRelativeAmount(formatter, "100.243");
    }

    @Test
    public void parseValidRelativeAmountBaseScale()
    {
        SatAmountFormat formatter = createTestFormatter(CoinScale.BASE);
        parseValidRelativeAmount(formatter, "0", 0L);
        parseValidRelativeAmount(formatter, "1243", 1243L);
    }

    @Test
    public void parseInvalidRelativeAmountBaseScale()
    {
        SatAmountFormat formatter = createTestFormatter(CoinScale.BASE);
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
    public void parseValidRelativeAmount(SatAmountFormat formatter, String input, long expected)
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
    public void parseInvalidRelativeAmount(SatAmountFormat formatter, String input)
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
