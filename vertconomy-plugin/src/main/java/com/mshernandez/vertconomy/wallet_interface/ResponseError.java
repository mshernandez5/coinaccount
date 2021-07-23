package com.mshernandez.vertconomy.wallet_interface;

import java.util.HashMap;
import java.util.Map;

/**
 * All possible RPC error codes, adapted from
 * <a href="https://github.com/bitcoin/bitcoin/blob/master/src/rpc/protocol.h">
 *    constants defined in the Bitcoin source code
 * </a>.
 */
public enum ResponseError
{
    // General Errors
    RPC_MISC_ERROR(-1),
    RPC_FORBIDDEN_BY_SAFE_MODE(-2),
    RPC_TYPE_ERROR(-3),
    RPC_INVALID_ADDRESS_OR_KEY(-5),
    RPC_OUT_OF_MEMORY(-7),
    RPC_INVALID_PARAMETER(-8),
    RPC_DATABASE_ERROR(-20),
    RPC_DESERIALIZATION_ERROR(-22),
    RPC_VERIFY_ERROR(-25),
    RPC_VERIFY_REJECTED(-26),
    RPC_VERIFY_ALREADY_IN_CHAIN(-27),
    RPC_IN_WARMUP(-28),

    // P2P Client Errors
    RPC_CLIENT_NOT_CONNECTED(-9),
    RPC_CLIENT_IN_INITIAL_DOWNLOAD(-10),
    RPC_CLIENT_NODE_ALREADY_ADDED(-23),
    RPC_CLIENT_NODE_NOT_ADDED(-24),
    RPC_CLIENT_NODE_NOT_CONNECTED(-29),
    RPC_CLIENT_INVALID_IP_OR_SUBNET(-30),
    RPC_CLIENT_P2P_DISABLED(-31),

    // Chain Errors
    RPC_CLIENT_MEMPOOL_DISABLED(-33),

    // Wallet Errors
    RPC_WALLET_ERROR(-4),
    RPC_WALLET_INSUFFICIENT_FUNDS(-6),
    RPC_WALLET_INVALID_ACCOUNT_NAME(-11),
    RPC_WALLET_KEYPOOL_RAN_OUT(-12),
    RPC_WALLET_UNLOCK_NEEDED(-13),
    RPC_WALLET_PASSPHRASE_INCORRECT(-14),
    RPC_WALLET_WRONG_ENC_STATE(-15),
    RPC_WALLET_ENCRYPTION_FAILED(-16),
    RPC_WALLET_ALREADY_UNLOCKED(-17),
    RPC_WALLET_NOT_FOUND(-18),
    RPC_WALLET_NOT_SPECIFIED(-19),
    RPC_WALLET_ALREADY_LOADED(-35);

    // Instance Fields & Initialization
    private int errorCode;

    private ResponseError(int errorCode)
    {
        this.errorCode = errorCode;
    }

    /**
     * Get the error code for this response error.
     * 
     * @return The error code for this response error.
     */
    public int code()
    {
        return errorCode;
    }

    // Static Error Lookup Based On Code
    private static final Map<Integer, ResponseError> codeMap = new HashMap<>();

    static
    {
        for (ResponseError e : values())
        {
            codeMap.put(e.errorCode, e);
        }
    }

    /**
     * Get the response error associated with the given error code.
     * 
     * @param errorCode The error code.
     * @return The associated response error or null if none exists.
     */
    public static ResponseError find(int errorCode)
    {
        return codeMap.get(errorCode);
    }
}