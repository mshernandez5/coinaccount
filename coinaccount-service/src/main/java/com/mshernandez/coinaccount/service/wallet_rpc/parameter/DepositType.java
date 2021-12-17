package com.mshernandez.coinaccount.service.wallet_rpc.parameter;

/**
 * Every supported deposit type.
 */
public enum DepositType
{
    /**
     * Legacy P2PKH
     */
    P2PKH("legacy"),

    /**
     * P2WPKH via Legacy P2SH
     */
    P2SH_P2WPKH("p2sh-segwit"),

    /**
     * Native Segwit P2WPKH (Preferred)
     */
    P2WPKH("bech32"),
    
    /**
     * Taproot
     */
    P2TR("bech32m");

    private final String addressType;

    private DepositType(String addressType)
    {
        this.addressType = addressType;
    }

    /**
     * Get the address type corresponding
     * to the deposit type.
     * 
     * @return The corresponding address type.
     */
    public String getAddressType()
    {
        return addressType;
    }
}