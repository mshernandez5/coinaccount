package com.mshernandez.vertconomy.core;

/**
 * Defines valid coin scales that may be used
 * by Vertconomy.
 * <p>
 * For example, 1.00000000 VTC would be represented
 * as follows using different scales:
 * <p>
 * <table>
 *     <tr>
 *         <td>CoinScale</td>
 *         <td>Representation</td>
 *     </tr>
 *     <tr>
 *         <td><code>FULL</code></td>
 *         <td>1.00000000 VTC</td>
 *     </tr>
 *     <tr>
 *         <td><code>MILLI</code></td>
 *         <td>1000.00000 mVTC</td>
 *     </tr>
 *     <tr>
 *         <td><code>MICRO</code></td>
 *         <td>1000000.00 µVTC</td>
 *     </tr>
 *     <tr>
 *         <td><code>BASE</code></td>
 *         <td>100000000 sat</td>
 *     </tr>
 * </table>
 */
public enum CoinScale
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