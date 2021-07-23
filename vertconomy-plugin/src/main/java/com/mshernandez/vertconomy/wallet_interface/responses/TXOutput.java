package com.mshernandez.vertconomy.wallet_interface.responses;

import java.util.List;

import com.google.gson.annotations.SerializedName;
import com.mshernandez.vertconomy.wallet_interface.SatAmount;

/**
 * Stores information about a transaction output.
 */
public class TXOutput
{
    public SatAmount value;

    @SerializedName("n")
    public int index;

    public ScriptPubKey scriptPubKey;

    public class ScriptPubKey
    {
        public String asm;

        public String hex;

        public int reqSigs;

        public String type;

        List<String> addresses;
    }
}