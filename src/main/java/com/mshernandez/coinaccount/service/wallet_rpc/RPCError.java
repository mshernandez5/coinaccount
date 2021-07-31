package com.mshernandez.coinaccount.service.wallet_rpc;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RPCError
{
    /**
     * A number indicating the type of error that occured.
     */
    private int code;

    /**
     * A short description of the error.
     */
    private String message;

    /**
     * Additional data about the error.
     */
    private JsonNode data;
}