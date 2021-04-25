package com.mshernandez.vertconomy.wallet_interface;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Base64;

import com.google.gson.Gson;
import com.google.gson.JsonArray;

/**
 * A class to interface with vertcoind,
 * which must be properly set up to
 * accept RPC commands.
 */
public class RPCWalletConnection
{
    private URI uri;
    private String user;
    private String basicAuth;
    private Gson parser;

    /**
     * Create a new wallet connection.
     * 
     * @param url The vertcoind RPC address, including port.
     * @param user The RPC username.
     * @param pass The RPC password.
     */
    public RPCWalletConnection(URI uri, String user, String pass)
    {
        this.uri = uri;
        this.user = user;
        // Encode Credentials For Use In HTTP Header
        String credentials =  user + ":" + pass;
        basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes());
        // Initialize JSON Parser
        parser = new Gson();
    }

    /**
     * Send a command to the wallet through the RPC API.
     * 
     * @param jsonRequest JSON with request details.
     * @return A JSON string containing the wallet response.
     * @throws WalletRequestException If the wallet could not be reached or execute the command.
     */
    private String makeRequest(WalletRequest jsonRequest) throws WalletRequestException
    {
        String json = parser.toJson(jsonRequest);
        HttpRequest request = HttpRequest.newBuilder(uri)
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .header("Authorization", "Basic " + basicAuth)
            .header("Content-type", "text/plain")
            .build();
        try
        {
            HttpResponse<String> response = HttpClient.newBuilder()
            .build()
            .send(request, BodyHandlers.ofString());
            switch (response.statusCode())
            {
                case 200:
                    // All Good
                    break;
                case 401:
                    throw new WalletAuthenticationException(user);
                default:
                    throw new WalletRequestException("Invalid Response: " + jsonRequest.method);
            }
            return response.body();
        }
        catch (IOException | InterruptedException e)
        {
            throw new WalletRequestException("Failed To Make Request: " + jsonRequest.method);
        }
    }

    /**
     * Get the balance of the entire wallet,
     * only considering transactions with a minimum
     * number of confirmations.
     * 
     * @param minConfirmations The minimum number of confirmations a transaction must have.
     * @return The balance of the wallet, in base units (ex. satoshis).
     */
    public long getBalance(int minConfirmations) throws WalletRequestException
    {
        JsonArray params = new JsonArray();
        params.add("*");
        params.add(minConfirmations);
        WalletRequest jsonRequest = new WalletRequest()
            .setId("vertconomy")
            .setMethod("getbalance")
            .setParams(params);
        BalanceResponse response = parser.fromJson(makeRequest(jsonRequest), BalanceResponse.class);
        return Long.parseLong(response.result.replace(".", ""));
    }

    /**
     * Get the balance of a specific address,
     * only considering transactions with a minimum
     * number of confirmations.
     * 
     * @param address The address to check the balance of.
     * @param minConfirmations The minimum number of confirmations a transaction must have.
     * @return The balance of the wallet, in base units (ex. satoshis).
     */
    public long getReceivedByAddress(String address, int minConfirmations) throws WalletRequestException
    {
        JsonArray params = new JsonArray();
        params.add(address);
        params.add(minConfirmations);
        WalletRequest jsonRequest = new WalletRequest()
            .setId("vertconomy")
            .setMethod("getreceivedbyaddress")
            .setParams(params);
        BalanceResponse response = parser.fromJson(makeRequest(jsonRequest), BalanceResponse.class);
        return Long.parseLong(response.result.replace(".", ""));
    }
}