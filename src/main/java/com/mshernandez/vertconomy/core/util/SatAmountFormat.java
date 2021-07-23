package com.mshernandez.vertconomy.core.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SatAmountFormat
{
    // Currency Configuration Settings
    private CoinScale scale;
    private String symbol;
    private String baseUnitSymbol;

    // Pattern To Recognize Valid Amount Strings
    private Pattern validAmountPattern;

    /**
     * Initialize a formatter with the given configuration.
     * 
     * @param scale The scale to use when formatting and parsing amounts.
     * @param symbol The standard symbol to use for the currency.
     * @param baseUnitSymbol The symbol to use for the base units of the currency.
     */
    public SatAmountFormat(CoinScale scale, String symbol, String baseUnitSymbol)
    {
        // Set Scale For Relative Parsing
        this.scale = scale;
        this.symbol = symbol;
        this.baseUnitSymbol = baseUnitSymbol;
        /* 
            Valid Amount Regex:
            (?<whole>\\d+) - Capture unlimited number of leading digits, need at least 1.
            (...)? - Capture optional fractional portion, if it exists it must be valid:
                \\. - Require decimal point before fractional amount.
                (?<fractional>\\d+) - Fractional digits.
        */
        validAmountPattern = Pattern.compile("^(?<whole>\\d+)"
            + "(?:\\.(?<fractional>\\d+))?$");
    }
    
    /**
     *  Parse a user-provided amount (relative to the current scale)
     *  into a satoshi amount.
     *  <p>
     *  Examples using the <code>FULL</code> scale:
     *  <ul>
     *      <li>"1" -> 100000000L</li>
     *      <li>"1.02" -> 102000000L</li>
     *  </ul>
     * 
     *  @param input An amount relative to the current scale in String format.
     *  @return The absolute amount in sats.
     *  @throws InvalidSatAmountException If an invalid input is provided.
     */
    public long parseSats(String input) throws InvalidSatAmountException
    {
        Matcher matcher = validAmountPattern.matcher(input);
        if (!matcher.matches())
        {
            throw new InvalidSatAmountException("Invalid amount formatting.");
        }
        StringBuilder strWholeAmount = new StringBuilder(matcher.group("whole"));
        int padAmount = 0;
        String strFractionalAmount = matcher.group("fractional");
        if (strFractionalAmount != null)
        {
            if (strFractionalAmount.length() > scale.NUM_VALID_FRACTION_DIGITS)
            {
                throw new InvalidSatAmountException("Too many fractional digits were provided.");
            }
            strWholeAmount.append(strFractionalAmount);
            padAmount = scale.NUM_VALID_FRACTION_DIGITS - strFractionalAmount.length();
        }
        else
        {
            padAmount = scale.NUM_VALID_FRACTION_DIGITS;
        }
        for (int i = 0; i < padAmount; i++)
        {
            strWholeAmount.append("0");
        }
        return Long.parseLong(strWholeAmount.toString());
    }

    /**
     * Format a sat amount into a readable String according
     * to the current currency settings.
     * 
     * @param amount The unformatted amount, in sats.
     * @return A formatted string representing the amount.
     */
    public String format(long amount)
    {
        return format((double) amount / scale.SAT_SCALE);
    }

    /**
     * Format a double into a readable amount according
     * to the current currency settings.
     * 
     * @param amount The unformatted amount, relative to the current scale.
     * @return A formatted string representing the amount.
     */
    public String format(double amount)
    {
        return String.format("%." + scale.NUM_VALID_FRACTION_DIGITS + "f "
            + ((scale == CoinScale.BASE) ? baseUnitSymbol : (scale.PREFIX + symbol)), amount);
    }

    /**
     * Get the relative scaled amount corresponding
     * to the absolute sat amount according to
     * the current currency settings.
     * 
     * @param amount The absolute amount in sats.
     * @return The relative amount.
     */
    public double relativeAmount(long amount)
    {
        return amount / (double) scale.SAT_SCALE;
    }

    /**
     * Get the absolute sat amount corresponding
     * to the given relative scaled amount according to
     * the current currency settings.
     * 
     * @param amount The relative amount.
     * @return The absolute amount in sats.
     */
    public long absoluteAmount(double amount)
    {
        // Cuts Off Fractional Portions, No Partial Sats
        return (long) (amount * scale.SAT_SCALE);
    }

    /**
     * How many fractional digits should be displayed
     * based on the coin scale being used.
     * 
     * @return The proper number of fractional digits.
     */
    public int getNumFractionalDigits()
    {
        return scale.NUM_VALID_FRACTION_DIGITS;
    }

    /**
     * Get the coin symbol, ex. VTC.
     * 
     * @return The coin symbol.
     */
    public String getSymbol()
    {
        return symbol;
    }
}
