package com.mshernandez.vertconomy.wallet_interface;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Base64;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;

/**
 * A class to interface with vertcoind,
 * which must be properly set up to
 * accept RPC commands.
 */
public class RPCWalletConnection
{
    private static final String DEFAULT_REQUEST_ID = "vertconomy";

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
                    // Authentication Issue
                    throw new WalletAuthenticationException(user);
                default:
                    // Response Indicates Various/Other Issue
                    throw new WalletRequestException("Invalid Response: " + jsonRequest.method);
            }
            return response.body();
        }
        catch (IOException | InterruptedException e)
        {
            // Failed To Get Response
            throw new WalletRequestException("Failed To Make Request: " + jsonRequest.method);
        }
    }

    /**
     * Gets a WalletInfoResponse holding general information
     * about the wallet and its status.
     * 
     * @return A WalletInfoResponse holding wallet status information.
     */
    public WalletInfoResponse.Result getWalletStatus() throws WalletRequestException
    {
        JsonArray params = new JsonArray();
        WalletRequest jsonRequest = new WalletRequest()
            .setId(DEFAULT_REQUEST_ID)
            .setMethod("getwalletinfo")
            .setParams(params);
        WalletInfoResponse response = parser.fromJson(makeRequest(jsonRequest), WalletInfoResponse.class);
        return response.result;
    }

    /**
     * Get the balance of the entire wallet,
     * only considering transactions with a minimum
     * number of confirmations.
     * 
     * @param minConfirmations The minimum number of confirmations a transaction must have.
     * @return The balance of the wallet, in base units (ex. satoshis).
     * @throws WalletRequestException If the wallet could not be reached or execute the command.
     */
    public long getBalance(int minConfirmations) throws WalletRequestException
    {
        JsonArray params = new JsonArray();
        params.add("*");
        params.add(minConfirmations);
        WalletRequest jsonRequest = new WalletRequest()
            .setId(DEFAULT_REQUEST_ID)
            .setMethod("getbalance")
            .setParams(params);
        GeneralWalletResponse response = parser.fromJson(makeRequest(jsonRequest), GeneralWalletResponse.class);
        return Long.parseLong(response.result.replace(".", ""));
    }

    /**
     * Return all transactions.
     * 
     * @param label The address label.
     * @return A list of transaction information objects.
     * @throws WalletRequestException If the wallet could not be reached or execute the command.
     */
    public List<ListTransactionResponse.Transaction> getTransactions() throws WalletRequestException
    {
        JsonArray params = new JsonArray();
        WalletRequest jsonRequest = new WalletRequest()
            .setId(DEFAULT_REQUEST_ID)
            .setMethod("listtransactions")
            .setParams(params);
        ListTransactionResponse response = parser.fromJson(makeRequest(jsonRequest), ListTransactionResponse.class);
        return response.result;
    }

    /**
     * Return all transactions for addresses under the
     * given label.
     * 
     * @param label The address label.
     * @return A list of transaction information objects.
     * @throws WalletRequestException If the wallet could not be reached or execute the command.
     */
    public List<ListTransactionResponse.Transaction> getTransactions(String label) throws WalletRequestException
    {
        JsonArray params = new JsonArray();
        params.add(label);
        WalletRequest jsonRequest = new WalletRequest()
            .setId(DEFAULT_REQUEST_ID)
            .setMethod("listtransactions")
            .setParams(params);
        ListTransactionResponse response = parser.fromJson(makeRequest(jsonRequest), ListTransactionResponse.class);
        return response.result;
    }

    /**
     * Get the balance of a specific address,
     * only considering transactions with a minimum
     * number of confirmations.
     * 
     * @param address The address to check the balance of.
     * @param minConfirmations The minimum number of confirmations a transaction must have.
     * @return The balance of the wallet, in base units (ex. satoshis).
     * @throws WalletRequestException If the wallet could not be reached or execute the command.
     */
    public long getReceivedByAddress(String address, int minConfirmations) throws WalletRequestException
    {
        JsonArray params = new JsonArray();
        params.add(address);
        params.add(minConfirmations);
        WalletRequest jsonRequest = new WalletRequest()
            .setId(DEFAULT_REQUEST_ID)
            .setMethod("getreceivedbyaddress")
            .setParams(params);
        GeneralWalletResponse response = parser.fromJson(makeRequest(jsonRequest), GeneralWalletResponse.class);
        return Long.parseLong(response.result.replace(".", ""));
    }

    /**
     * Create a new wallet address with no label.
     * 
     * @return A new wallet address.
     * @throws WalletRequestException If the wallet could not be reached or execute the command.
     */
    public String getNewAddress() throws WalletRequestException
    {
        JsonArray params = new JsonArray();
        WalletRequest jsonRequest = new WalletRequest()
            .setId(DEFAULT_REQUEST_ID)
            .setMethod("getnewaddress")
            .setParams(params);
        GeneralWalletResponse response = parser.fromJson(makeRequest(jsonRequest), GeneralWalletResponse.class);
        return response.result;
    }

    /**
     * Create a new wallet address with the given label.
     * 
     * @return A new wallet address.
     * @throws WalletRequestException If the wallet could not be reached or execute the command.
     */
    public String getNewAddress(String label) throws WalletRequestException
    {
        JsonArray params = new JsonArray();
        params.add(label);
        WalletRequest jsonRequest = new WalletRequest()
            .setId(DEFAULT_REQUEST_ID)
            .setMethod("getnewaddress")
            .setParams(params);
        GeneralWalletResponse response = parser.fromJson(makeRequest(jsonRequest), GeneralWalletResponse.class);
        return response.result;
    }

    /**
     * Estimates the approximate reqiored fee per kilobyte
     * in base units (ex. satoshis).
     * This is not a fee per transaction.
     * 
     * @param confirmationTarget
     * @return The approximate TX fee per KB, in base units (ex. satoshis).
     * @throws WalletRequestException If the wallet could not be reached or execute the command.
     */
    public long estimateSmartFee(int confirmationTarget) throws WalletRequestException
    {
        JsonArray params = new JsonArray();
        params.add(confirmationTarget);
        WalletRequest jsonRequest = new WalletRequest()
            .setId(DEFAULT_REQUEST_ID)
            .setMethod("estimatesmartfee")
            .setParams(params);
        SmartFeeResponse response = parser.fromJson(makeRequest(jsonRequest), SmartFeeResponse.class);
        return Long.parseLong((response.result.feeRate).replace(".", ""));
    }
}