package com.mshernandez.vertconomy.wallet_interface;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

/**
 * bitcoind responses return amounts in decimal format
 * even though satoshi can be discretely represented
 * by a long integer; this deserializer parses the
 * returned amounts into long type values for ease of use
 * and less concern for accumulated floating-point errors.
 * <p>
 * Since sat amounts are used by multiple response
 * types, the SatAmount wrapper is used instead
 * of implementing custom deserializers for every
 * class with one of these fields.
 */
public class SatAmountDeserializer implements JsonDeserializer<SatAmount>
{
    @Override
    public SatAmount deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException
    {
        SatAmount satAmount = new SatAmount();
        satAmount.satAmount = Long.parseLong(json.getAsString().replace(".", ""));
        return satAmount;
    }
}