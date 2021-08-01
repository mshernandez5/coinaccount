package com.mshernandez.coinaccount.service.wallet_rpc.serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.mshernandez.coinaccount.service.wallet_rpc.SatAmount;

public class SatAmountSerializer extends StdSerializer<SatAmount>
{
    public SatAmountSerializer()
    {
        super(SatAmount.class);
    }

    @Override
    public void serialize(SatAmount obj, JsonGenerator gen, SerializerProvider provider) throws IOException
    {
        String formattedAmount;
        if (obj.getSatAmount() >= 100000000L)
        {
            formattedAmount = Long.toString(obj.getSatAmount());
            formattedAmount = formattedAmount.substring(0, formattedAmount.length() - 8)
                + "." + formattedAmount.substring(formattedAmount.length() - 8);
        }
        else
        {
            formattedAmount = String.format("0.%08d", obj.getSatAmount());
        }
        gen.writeString(formattedAmount);
    }
}