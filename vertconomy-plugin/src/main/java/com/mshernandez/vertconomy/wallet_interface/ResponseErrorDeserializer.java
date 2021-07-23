package com.mshernandez.vertconomy.wallet_interface;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class ResponseErrorDeserializer implements JsonDeserializer<ResponseError>
{
    @Override
    public ResponseError deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException
    {
        return ResponseError.find(json.getAsJsonObject().get("code").getAsInt());
    }
}