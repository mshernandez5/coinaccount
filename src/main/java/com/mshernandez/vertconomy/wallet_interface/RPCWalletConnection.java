package com.mshernandez.vertconomy.wallet_interface;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.mshernandez.vertconomy.wallet_interface.requests.FundRawTransactionOptions;
import com.mshernandez.vertconomy.wallet_interface.requests.RawTransactionInput;
import com.mshernandez.vertconomy.wallet_interface.responses.DecodeTransactionResponse;
import com.mshernandez.vertconomy.wallet_interface.responses.SatResponse;
import com.mshernandez.vertconomy.wallet_interface.responses.SignRawTransactionResponse;
import com.mshernandez.vertconomy.wallet_interface.responses.SmartFeeResponse;
import com.mshernandez.vertconomy.wallet_interface.responses.TransactionListResponse;
import com.mshernandez.vertconomy.wallet_interface.responses.UnspentOutputResponse;
import com.mshernandez.vertconomy.wallet_interface.responses.WalletInfoResponse;

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
    private Gson gson;

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
        // Initialize Gson Instance
        gson = new GsonBuilder()
            .registerTypeAdapter(SatAmount.class, new SatAmountSerializer())
            .registerTypeAdapter(SatAmount.class, new SatAmountDeserializer())
            .create();
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
        String json = gson.toJson(jsonRequest);
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
     * Creates a new raw transaction using the
     * provided inputs and output addresses/amounts.
     * 
     * @return A hex-encoded raw transaction.
     * @throws WalletRequestException If the wallet could not be reached or execute the command.
     */
    public String createRawTransaction(List<RawTransactionInput> inputs, Map<String, Long> outputs) throws WalletRequestException
    {
        JsonArray params = new JsonArray();
        params.add(gson.toJson(inputs));
        Map<String, SatAmount> boxedAmounts = new HashMap<>();
        for (String address : outputs.keySet())
        {
            boxedAmounts.put(address, new SatAmount(outputs.get(address)));
        }
        params.add(gson.toJson(boxedAmounts));
        WalletRequest jsonRequest = new WalletRequest()
            .setId(DEFAULT_REQUEST_ID)
            .setMethod("createrawtransaction")
            .setParams(params);
        WalletResponse<String> response = gson.fromJson(makeRequest(jsonRequest), new TypeToken<WalletResponse<String>>() {}.getType());
        return response.result;
    }

    /**
     * Decodes raw transaction information.
     * 
     * @return A hex-encoded raw transaction.
     * @throws WalletRequestException If the wallet could not be reached or execute the command.
     */
    public DecodeTransactionResponse.Result decodeRawTransaction(String rawTransactionHex) throws WalletRequestException
    {
        JsonArray params = new JsonArray();
        params.add(rawTransactionHex);
        WalletRequest jsonRequest = new WalletRequest()
            .setId(DEFAULT_REQUEST_ID)
            .setMethod("decoderawtransaction")
            .setParams(params);
        DecodeTransactionResponse response = gson.fromJson(makeRequest(jsonRequest), DecodeTransactionResponse.class);
        return response.result;
    }

    /**
     * Funds a raw transaction.
     * 
     * @return A hex-encoded raw transaction.
     * @throws WalletRequestException If the wallet could not be reached or execute the command.
     */
    public String fundRawTransaction(String rawTransactionHex, FundRawTransactionOptions options) throws WalletRequestException
    {
        JsonArray params = new JsonArray();
        params.add(rawTransactionHex);
        params.add(gson.toJson(options));
        WalletRequest jsonRequest = new WalletRequest()
            .setId(DEFAULT_REQUEST_ID)
            .setMethod("fundrawtransaction")
            .setParams(params);
        WalletResponse<String> response = gson.fromJson(makeRequest(jsonRequest), new TypeToken<WalletResponse<String>>() {}.getType());
        return response.result;
    }

    /**
     * Signs a raw transaction with the server wallet.
     * 
     * @param rawTransactionHex The hex string representing the raw transaction.
     * @return A response object holding the signed transaction hex string.
     * @throws WalletRequestException If the wallet could not be reached or execute the command.
     */
    public SignRawTransactionResponse.Result signRawTransactionWithWallet(String rawTransactionHex) throws WalletRequestException
    {
        JsonArray params = new JsonArray();
        params.add(rawTransactionHex);
        WalletRequest jsonRequest = new WalletRequest()
            .setId(DEFAULT_REQUEST_ID)
            .setMethod("signrawtransactionwithwallet")
            .setParams(params);
        SignRawTransactionResponse response = gson.fromJson(makeRequest(jsonRequest), SignRawTransactionResponse.class);
        return response.result;
    }

    /**
     * Submit a raw transaction to the network.
     * 
     * @param rawTransactionHex The hex string representing the raw transaction.
     * @return The transaction hash (TXID) in hex.
     * @throws WalletRequestException If the wallet could not be reached or execute the command.
     */
    public String sendRawTransaction(String rawTransactionHex) throws WalletRequestException
    {
        JsonArray params = new JsonArray();
        params.add(rawTransactionHex);
        WalletRequest jsonRequest = new WalletRequest()
            .setId(DEFAULT_REQUEST_ID)
            .setMethod("sendrawtransaction")
            .setParams(params);
        WalletResponse<String> response = gson.fromJson(makeRequest(jsonRequest), new TypeToken<WalletResponse<String>>() {}.getType());
        return response.result;
    }

    /**
     * Gets a WalletInfoResponse holding general information
     * about the wallet and its status.
     * 
     * @return A WalletInfoResponse holding wallet status information.
     * @throws WalletRequestException If the wallet could not be reached or execute the command.
     */
    public ResponseError getWalletInfo() throws WalletRequestException
    {
        JsonArray params = new JsonArray();
        WalletRequest jsonRequest = new WalletRequest()
            .setId(DEFAULT_REQUEST_ID)
            .setMethod("getwalletinfo")
            .setParams(params);
        WalletInfoResponse response = gson.fromJson(makeRequest(jsonRequest), WalletInfoResponse.class);
        return response.error;
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
            SatResponse response = gson.fromJson(makeRequest(jsonRequest), SatResponse.class);
        return response.result.satAmount;
    }

    /**
     * Return all transactions.
     * 
     * @param label The address label.
     * @return A list of transaction information objects.
     * @throws WalletRequestException If the wallet could not be reached or execute the command.
     */
    public List<TransactionListResponse.Transaction> getTransactions() throws WalletRequestException
    {
        JsonArray params = new JsonArray();
        WalletRequest jsonRequest = new WalletRequest()
            .setId(DEFAULT_REQUEST_ID)
            .setMethod("listtransactions")
            .setParams(params);
        TransactionListResponse response = gson.fromJson(makeRequest(jsonRequest), TransactionListResponse.class);
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
    public List<TransactionListResponse.Transaction> getTransactions(String label) throws WalletRequestException
    {
        JsonArray params = new JsonArray();
        params.add(label);
        WalletRequest jsonRequest = new WalletRequest()
            .setId(DEFAULT_REQUEST_ID)
            .setMethod("listtransactions")
            .setParams(params);
        TransactionListResponse response = gson.fromJson(makeRequest(jsonRequest), TransactionListResponse.class);
        return response.result;
    }

    /**
     * Return all unspent outputs with a minimum
     * number of confirmations for the given address.
     * 
     * @param label The address that received the transactions.
     * @param minConfirmations The minimum number of confirmations a transaction must have.
     * @return A list of unspent output information objects.
     * @throws WalletRequestException If the wallet could not be reached or execute the command.
     */
    public List<UnspentOutputResponse.UnspentOutput> getUnspentOutputs(String address) throws WalletRequestException
    {
        JsonArray params = new JsonArray();
        params.add(0);
        params.add(9999999);
        JsonArray addresses = new JsonArray();
        addresses.add(address);
        params.add(addresses);
        WalletRequest jsonRequest = new WalletRequest()
            .setId(DEFAULT_REQUEST_ID)
            .setMethod("listunspent")
            .setParams(params);
        UnspentOutputResponse response = gson.fromJson(makeRequest(jsonRequest), UnspentOutputResponse.class);
        return response.result;
    }

    /**
     * Return all unspent outputs for the
     * given address including unconfirmed transactions.
     * 
     * @param label The address label.
     * @return A list of transaction information objects.
     * @throws WalletRequestException If the wallet could not be reached or execute the command.
     */
    public List<UnspentOutputResponse.UnspentOutput> getUnspentOutputs(String address, int minConfirmations) throws WalletRequestException
    {
        JsonArray params = new JsonArray();
        params.add(minConfirmations);
        params.add(9999999);
        JsonArray addresses = new JsonArray();
        addresses.add(address);
        params.add(addresses);
        WalletRequest jsonRequest = new WalletRequest()
            .setId(DEFAULT_REQUEST_ID)
            .setMethod("listunspent")
            .setParams(params);
        UnspentOutputResponse response = gson.fromJson(makeRequest(jsonRequest), UnspentOutputResponse.class);
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
            SatResponse response = gson.fromJson(makeRequest(jsonRequest), SatResponse.class);
        return response.result.satAmount;
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
        WalletResponse<String> response = gson.fromJson(makeRequest(jsonRequest), new TypeToken<WalletResponse<String>>() {}.getType());
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
            WalletResponse<String> response = gson.fromJson(makeRequest(jsonRequest), new TypeToken<WalletResponse<String>>() {}.getType());
        return response.result;
    }

    /**
     * Estimates the approximate fee in sats/vbyte.
     * This is not a fee per transaction.
     * 
     * @param confirmationTarget The target number of block for confirmation.
     * @return The approximate TX fee in sats per vbyte.
     * @throws WalletRequestException If the wallet could not be reached or execute the command.
     */
    public double estimateSmartFee(int confirmationTarget) throws WalletRequestException
    {
        JsonArray params = new JsonArray();
        params.add(confirmationTarget);
        WalletRequest jsonRequest = new WalletRequest()
            .setId(DEFAULT_REQUEST_ID)
            .setMethod("estimatesmartfee")
            .setParams(params);
        SmartFeeResponse response = gson.fromJson(makeRequest(jsonRequest), SmartFeeResponse.class);
        // Fee Initially In sat/KB, Want sat/vbyte
        return response.result.feeRate.satAmount / 1000.0;
    }
}