package com.mshernandez.coinaccount.service.wallet_rpc.serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.mshernandez.coinaccount.service.wallet_rpc.parameter.ListUnspentQuery;

public class ListUnspentQuerySerializer extends StdSerializer<ListUnspentQuery>
{
    public ListUnspentQuerySerializer()
    {
        super(ListUnspentQuery.class);
    }

    @Override
    public void serialize(ListUnspentQuery obj, JsonGenerator gen, SerializerProvider provider) throws IOException
    {
        // Begin Parameters Array
        gen.writeStartArray();
        gen.writeNumber(obj.getMinConfirmations());
        gen.writeNumber(obj.getMaxConfirmations());
        // Begin Address Array
        gen.writeStartArray();
        for (String address : obj.getAddresses())
        {
            gen.writeString(address);
        }
        gen.writeEndArray();
        // End Address Array
        gen.writeBoolean(obj.isIncludeUnsafe());
        // Query Options Object
        if (obj.getQueryOptions() != null)
        {
            // Start Query Options Object
            ListUnspentQuery.QueryOptions queryOptions = obj.getQueryOptions();
            gen.writeStartObject();
            gen.writeObjectField("minimumAmount", queryOptions.getMinimumAmount());
            if (queryOptions.getMaximumAmount() != null)
            {
                gen.writeObjectField("maximumAmount", queryOptions.getMaximumAmount());
            }
            if (queryOptions.getMaximumCount() >= 0)
            {
                gen.writeObjectField("maximumCount", queryOptions.getMaximumCount());
            }
            if (queryOptions.getMinimumSumAmount() != null)
            {
                gen.writeObjectField("minimumSumAmount", queryOptions.getMinimumSumAmount());
            }
            gen.writeEndObject();
            // End Query Options Object
        }
        gen.writeEndArray();
        // End Parameters Array
    }
}