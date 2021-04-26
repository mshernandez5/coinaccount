package com.mshernandez.vertconomy;

enum CoinScale
{
    BASE(1L, 0, "", 's'),
    MICRO(100L, 2, "µ", 'µ'),
    MILLI(100000L, 5, "m", 'm'),
    FULL(100000000L, 8, "", ' ');

    public final long SAT_SCALE;
    public final int NUM_VALID_FRACTION_DIGITS;
    public final String PREFIX;
    public final char CHAR_PREFIX;

    private CoinScale(long satScale, int numValidFractionDigits, String prefix, char charPrefix)
    {
        SAT_SCALE = satScale;
        NUM_VALID_FRACTION_DIGITS = numValidFractionDigits;
        PREFIX = prefix;
        CHAR_PREFIX = charPrefix;
    }
}