package com.mshernandez.coinaccount.service.wallet_rpc.serializer;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.mshernandez.coinaccount.service.wallet_rpc.SatAmount;
import com.mshernandez.coinaccount.service.wallet_rpc.parameter.ListUnspentQuery;

public class RPCModule extends SimpleModule
{
    public RPCModule()
    {
        // Sat Amount Serialization
        addSerializer(SatAmount.class, new SatAmountSerializer());
        addDeserializer(SatAmount.class, new SatAmountDeserializer());

        // Custom Parameter Types
        addSerializer(ListUnspentQuery.class, new ListUnspentQuerySerializer());
    }
}