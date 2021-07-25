package com.mshernandez.vertconomy.core.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VertconomyFormatter implements SatAmountFormatter
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
    public VertconomyFormatter(CoinScale scale, String symbol, String baseUnitSymbol)
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
    
    @Override
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

    @Override
    public String format(long satAmount)
    {
        StringBuilder stringBuilder = new StringBuilder(Long.toString(satAmount));
        if (scale == CoinScale.BASE)
        {
            stringBuilder.append(' ').append(baseUnitSymbol);
        }
        else
        {
            StringBuilder zeroPadding = new StringBuilder();
            int numZeroesToPad = scale.NUM_VALID_FRACTION_DIGITS + 1 - stringBuilder.length();
            for (; numZeroesToPad > 0; numZeroesToPad--)
            {
                zeroPadding.append('0');
            }
            stringBuilder = zeroPadding.append(stringBuilder);
            stringBuilder.insert(stringBuilder.length() - scale.NUM_VALID_FRACTION_DIGITS, ".")
                .append(' ')
                .append(scale.PREFIX)
                .append(symbol);
        }
        return stringBuilder.toString();
    }

    @Override
    public String format(BigDecimal amount)
    {
        return new StringBuilder()
            .append(amount.setScale(scale.NUM_VALID_FRACTION_DIGITS, RoundingMode.FLOOR).toPlainString())
            .append(' ')
            .append((scale == CoinScale.BASE) ? baseUnitSymbol : (scale.PREFIX + symbol))
            .toString();
    }

    @Override
    public BigDecimal relativeAmount(long satAmount)
    {
        return new BigDecimal(satAmount)
            .movePointLeft(scale.NUM_VALID_FRACTION_DIGITS)
            .setScale(scale.NUM_VALID_FRACTION_DIGITS, RoundingMode.FLOOR);
    }

    @Override
    public long absoluteAmount(BigDecimal relativeAmount)
    {
        return relativeAmount.movePointRight(scale.NUM_VALID_FRACTION_DIGITS).longValue();
    }

    @Override
    public int getNumFractionalDigits()
    {
        return scale.NUM_VALID_FRACTION_DIGITS;
    }

    @Override
    public String getSymbol()
    {
        return symbol;
    }

    @Override
    public String getBaseUnitSymbol()
    {
        return baseUnitSymbol;
    }
}
