package com.mshernandez.vertconomy.wallet_interface;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SerializationTests
{
    private Gson gson;

    /**
     * Create Gson instance with custom serializers
     * and deserializers.
     */
    @BeforeEach
    public void setup()
    {
        gson = new GsonBuilder()
            .registerTypeAdapter(SatAmount.class, new SatAmountSerializer())
            .registerTypeAdapter(SatAmount.class, new SatAmountDeserializer())
            .create();
    }

    /**
     * Test custom serializer and deserializer
     * for 0.00000000 VTC.
     */
    @Test
    public void zeroSatAmountSerializationTest()
    {
        testSatAmountSerialization(0L, "\"0.00000000\"");
    }

    /**
     * Test custom serializer and deserializer
     * for amounts under 1.00000000 VTC.
     */
    @Test
    public void lowSatAmountSerializationTest()
    {
        testSatAmountSerialization(10000001L, "\"0.10000001\"");
    }

    /**
     * Test custom serializer and deserializer
     * for 1.00000000 VTC.
     */
    @Test
    public void satAmountSerializationTest()
    {
        testSatAmountSerialization(100000000L, "\"1.00000000\"");
    }

    /**
     * Test custom serializer and deserializer
     * for amounts over 1.00000000 VTC.
     */
    @Test
    public void highSatAmountSerializationTest()
    {
        testSatAmountSerialization(510000001L, "\"5.10000001\"");
    }

    public void testSatAmountSerialization(long amount, String expectedJson)
    {
        SatAmount satAmount = new SatAmount(amount);
        String serialized = gson.toJson(satAmount);
        assertEquals(expectedJson, serialized);
        SatAmount deserialized = gson.fromJson(serialized, SatAmount.class);
        assertEquals(amount, deserialized.satAmount);
    }
}
