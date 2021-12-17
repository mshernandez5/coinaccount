package com.mshernandez.coinaccount.service.util;

import com.mshernandez.coinaccount.service.wallet_rpc.parameter.DepositType;

/**
 * Shares static utilities and constants involved in
 * fee calculation.
 */
public class TXFeeUtilities
{
    /**
     * TX-Level Constants
     */

    // Transaction Version Field Takes 4 Bytes
    public static final double TX_VERSION_VSIZE = 4.0;

    // Transaction Locksize Field Takes 4 Bytes
    public static final double TX_LOCKTIME_VSIZE = 4.0;

    // Marker For All Transactions Spending At Least 1 Segwit UTXO
    public static final double TX_SEGWIT_MARKER_VSIZE = 0.5;

    // For Every Input In A Transaction With At Least 1 Segwit Input
    public static final double TX_INPUT_WITNESS_ITEM_COUNTER_VSIZE = 0.25;

    /**
     * Input-Level Constants
     */

    // Takes 32 Bytes To Reference Previous TX
    public static final double TX_INPUT_TXID_VSIZE = 32.0;

    // Takes 4 Bytes To Specify TX vout To Spend
    public static final double TX_INPUT_VOUT_VSIZE = 4.0;

    // Takes 4 Bytes For Input Sequence Number
    public static final double TX_INPUT_SEQUENCE_NUMBER_VSIZE = 4.0;

    // P2PKH: 1 Byte Unlocking Script Counter + 107 Byte Unlocking Script
    public static final double P2PKH_SIG_SCRIPT_DATA_VSIZE = 108.0;

    // P2SH-P2WPKH: 1 Byte Unlocking Script Counter + 23 Byte Unlocking Script
    public static final double P2SH_P2WPKH_SIG_SCRIPT_DATA_VSIZE = 24.0;

    // Witness Data For P2WPKH Input Including Size Counter
    public static final double P2WPKH_WITNESS_DATA_VSIZE = 27.0;

    // Witness Data For Taproot Input Including Size Counter
    public static final double TAPROOT_WITNESS_DATA_VSIZE = 16.5;

    // Witness Inputs Have Empty Script Sig But Still Require Counter
    public static final double WITNESS_SCRIPT_SIG_COUNTER_VSIZE = 1.0;

    /**
     * Output-Level Constants
     */

    // Takes 8 Bytes To Specify An Output Sat Amount
    public static final double TX_OUTPUT_VALUE_VSIZE = 8.0;

    /**
     * Gets the byte size of data represented
     * by a hexadecimal String without a leading
     * prefix.
     * 
     * @param hexString The hexadecimal string, without leading prefix.
     * @return The byte size of the data.
     */
    public static int getHexStringByteSize(String hexString)
    {
        return hexString.length() / 2; // 2 Nibbles Per Byte
    }

    /**
     * Gets the byte size needed for a VarInt
     * counter to store the specified amount.
     * 
     * @param value The amount that needs to be stored by the counter.
     * @return The number of bytes it would take to store the value.
     */
    public static int getCounterByteSize(int value)
    {
        if (value <= 252L)
        {
            // [1, 252]: 1 Byte
            return 1;
        }
        else if (value <= 65535L)
        {
            // [253, 65535]: 1 Byte Prefix + 2 Bytes
            return 3;
        }
        else if (value <= 4294967295L)
        {
            // [65536, 4294967295]: 1 Byte Prefix + 4 Bytes
            return 5;
        }
        // [4294967296, as much as 8 bytes can hold i guess]: 1 Byte Prefix + 8 Bytes
        return 9;
    }

    /**
     * Return whether spending the deposit will
     * require witness data to be provided.
     * 
     * @param type The deposit type.
     * @return Whether witness data is required to spend the output.
     */
    public boolean hasWitnessData(DepositType type)
    {
        return type != DepositType.P2PKH;
    }

    /**
     * Get the vsize of an input based on its type.
     * 
     * @param type The deposit type.
     * @return The vsize contribution of this input.
     */
    public static double getInputSize(DepositType type)
    {
        // Calculate Base Input Size, Common For All Types
        double vsize = TX_INPUT_TXID_VSIZE + TX_INPUT_VOUT_VSIZE + TX_INPUT_SEQUENCE_NUMBER_VSIZE;
        // Calculate Additional Size According To Input Type
        switch (type)
        {
            case P2PKH:
                vsize += P2PKH_SIG_SCRIPT_DATA_VSIZE;
                break;
            case P2SH_P2WPKH:
                vsize += P2SH_P2WPKH_SIG_SCRIPT_DATA_VSIZE + P2WPKH_WITNESS_DATA_VSIZE;
                break;
            case P2WPKH:
                vsize += P2WPKH_WITNESS_DATA_VSIZE + WITNESS_SCRIPT_SIG_COUNTER_VSIZE;
                break;
            default: // TAPROOT
                vsize += TAPROOT_WITNESS_DATA_VSIZE + WITNESS_SCRIPT_SIG_COUNTER_VSIZE;
        }
        return vsize;
    }

    /**
     * Get the vsize of an output based on its locking script.
     * 
     * @param scriptPubKey The output locking script.
     * @return The vsize contribution of the output.
     */
    public static double getOutputSize(String scriptPubKey)
    {
        int outputLockScriptSize = getHexStringByteSize(scriptPubKey);
        int outputLockScriptCounterSize = getCounterByteSize(outputLockScriptSize);
        return TX_OUTPUT_VALUE_VSIZE + outputLockScriptCounterSize + outputLockScriptSize;
    }
}