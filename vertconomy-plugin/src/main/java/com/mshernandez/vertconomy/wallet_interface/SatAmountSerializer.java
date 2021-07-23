package com.mshernandez.vertconomy.wallet_interface;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * This program represents Vertcoin discretely in
 * sats using a long integer to avoid accumulated
 * floating point error, but the vertcoind/bitcoind
 * API requires amounts in full units with fractional
 * values. This serializer makes requests between
 * this program and the wallet use compatible
 * representations.
 */
public class SatAmountSerializer implements JsonSerializer<SatAmount>
{

    @Override
    public JsonElement serialize(SatAmount src, Type typeOfSrc, JsonSerializationContext context)
    {
        String fullAmount;
        if (src.satAmount >= 100000000L)
        {
            fullAmount = Long.toString(src.satAmount);
            fullAmount = fullAmount.substring(0, fullAmount.length() - 8)
                + "." + fullAmount.substring(fullAmount.length() - 8);
        }
        else
        {
            fullAmount = String.format("0.%08d", src.satAmount);
        }
        return new JsonPrimitive(fullAmount);
    }
    
}