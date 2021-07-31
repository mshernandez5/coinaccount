package com.mshernandez.coinaccount.service.wallet_rpc.serializer;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.mshernandez.coinaccount.service.wallet_rpc.SatAmount;

public class SatAmountModule extends SimpleModule
{
    public SatAmountModule()
    {
        addSerializer(SatAmount.class, new SatAmountSerializer());
        addDeserializer(SatAmount.class, new SatAmountDeserializer());
    }
}