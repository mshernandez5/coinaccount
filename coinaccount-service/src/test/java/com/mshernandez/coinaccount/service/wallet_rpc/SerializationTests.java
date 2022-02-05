package com.mshernandez.coinaccount.service.wallet_rpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mshernandez.coinaccount.service.wallet_rpc.serializer.RPCModule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SerializationTests
{
    private ObjectMapper objectMapper;

    /**
     * Create Gson instance with custom serializers
     * and deserializers.
     */
    @BeforeEach
    public void setup()
    {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new RPCModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
        try
        {
            SatAmount satAmount = new SatAmount(amount);
            String serialized = objectMapper.writeValueAsString(satAmount);
            assertEquals(expectedJson, serialized);
            SatAmount deserialized = objectMapper.readValue(serialized, SatAmount.class);
            assertEquals(amount, deserialized.getSatAmount());
        }
        catch (JsonProcessingException e)
        {
            fail("Failed To Serialize/Parse JSON: " + e.getMessage());
        }
    }
}