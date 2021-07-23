package com.mshernandez.vertconomy.core.util;

public interface SatAmountFormatter
{
    /**
     * Format a sat amount into a readable String according
     * to the current currency settings.
     * 
     * @param amount The unformatted amount, in sats.
     * @return A formatted string representing the amount.
     */
    public String format(long amount);

    /**
     * Format a double into a readable amount according
     * to the current currency settings.
     * 
     * @param amount The unformatted amount, relative to the current scale.
     * @return A formatted string representing the amount.
     */
    public String format(double amount);

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
    public long parseSats(String input) throws InvalidSatAmountException;

    /**
     * Get the relative scaled amount corresponding
     * to the absolute sat amount according to
     * the current currency settings.
     * 
     * @param amount The absolute amount in sats.
     * @return The relative amount.
     */
    public double relativeAmount(long amount);

    /**
     * Get the absolute sat amount corresponding
     * to the given relative scaled amount according to
     * the current currency settings.
     * 
     * @param amount The relative amount.
     * @return The absolute amount in sats.
     */
    public long absoluteAmount(double amount);

    /**
     * How many fractional digits should be displayed
     * based on the coin scale being used.
     * 
     * @return The proper number of fractional digits.
     */
    public int getNumFractionalDigits();

    /**
     * Get the coin symbol, ex. VTC.
     * 
     * @return The coin symbol.
     */
    public String getSymbol();

    /**
     * Get the base unit symbol, ex. sat.
     * 
     * @return The coin symbol.
     */
    public String getBaseUnitSymbol();
}
