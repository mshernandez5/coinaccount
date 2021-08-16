package com.mshernandez.coinaccount.entity;

/**
 * Every supported deposit type.
 */
public enum DepositType
{
    /**
     * Legacy P2PKH
     */
    P2PKH,

    /**
     * P2WPKH via Legacy P2SH
     */
    P2SH_P2WPKH,

    /**
     * Native Segwit P2WPKH (Preferred)
     */
    P2WPKH,
    
    /**
     * Taproot
     */
    P2TR;
}