package com.mshernandez.coinaccount.service.wallet_rpc.serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.mshernandez.coinaccount.service.wallet_rpc.SatAmount;

public class SatAmountDeserializer extends StdDeserializer<SatAmount>
{
    public SatAmountDeserializer()
    {
        super(SatAmount.class);
    }

    @Override
    public SatAmount deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException
    {
        return new SatAmount(Long.parseLong(parser.getValueAsString().replace(".", "")));
    }
}