package com.mshernandez.coinaccount.service.wallet_rpc;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import jakarta.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.mshernandez.coinaccount.service.wallet_rpc.exception.WalletRequestException;
import com.mshernandez.coinaccount.service.wallet_rpc.exception.WalletResponseError;
import com.mshernandez.coinaccount.service.wallet_rpc.exception.WalletResponseException;
import com.mshernandez.coinaccount.service.wallet_rpc.parameter.CreateRawTransactionInput;
import com.mshernandez.coinaccount.service.wallet_rpc.parameter.DepositType;
import com.mshernandez.coinaccount.service.wallet_rpc.parameter.ListUnspentQuery;
import com.mshernandez.coinaccount.service.wallet_rpc.result.DecodeRawTransactionResult;
import com.mshernandez.coinaccount.service.wallet_rpc.result.EstimateSmartFeeResult;
import com.mshernandez.coinaccount.service.wallet_rpc.result.GetAddressInfoResult;
import com.mshernandez.coinaccount.service.wallet_rpc.result.GetWalletInfoResult;
import com.mshernandez.coinaccount.service.wallet_rpc.result.ListUnspentUTXO;
import com.mshernandez.coinaccount.service.wallet_rpc.result.SignRawTransactionWithWalletResult;
import com.mshernandez.coinaccount.service.wallet_rpc.result.ValidateAddressResult;
import com.mshernandez.coinaccount.service.wallet_rpc.serializer.RPCModule;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Invokes wallet methods through JSON-RPC requests.
 * <p>
 * A lightweight JSON-RPC client that only supports
 * the subset of wallet methods CoinAccount needs
 * to function.
 */
@ApplicationScoped
public class WalletService
{
    private static final String JSON_RPC_VERSION = "2.0";
    private static final String JSON_RPC_REQUEST_ID = "CoinAccount Wallet Service";

    // Connection Properties
    private final URI uri;
    private final String basicAuth;

    // JSON Object Mapper
    private final ObjectMapper objectMapper;

    /**
     * Creates a new wallet RPC service instance.
     * 
     * @param address The wallet address including port, ex. http://127.0.0.7:5888
     * @param username The wallet RPC username.
     * @param password The wallet RPC password.
     * @throws URISyntaxException If the wallet address is not a valid URI.
     */
    public WalletService(@ConfigProperty(name = "coinaccount.wallet.address") String address,
                         @ConfigProperty(name = "coinaccount.wallet.user") String user,
                         @ConfigProperty(name = "coinaccount.wallet.pass") String pass,
                         ObjectMapper objectMapper) throws URISyntaxException
    {
        // Create URI
        uri = new URI(address);
        // Encode Authentication Parameters
        String credentials = user + ":" + pass;
        basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes());
        // Configure JSON Serialization
        this.objectMapper = objectMapper;
        objectMapper.registerModule(new RPCModule());
    }

    /**
     * Send a JSON-RPC method invocation request to the wallet.
     * <p>
     * Only use this method for non-generic result types!
     * 
     * @param <T> The type of result expected to be returned by the response.
     * @param request The request to make to the wallet.
     * @param resultType The class type of the expected result.
     * @return An RPCResponse parameterized to match the expected result type.
     * @throws WalletRequestException If there was an issue making the RPC request.
     * @throws WalletResponseException If the response indicates an error.
     */
    private <T> T makeRequest(RPCRequest request, Class<T> resultType)
    {
        return makeRequest(request, resultType, false, null);
    }

    /**
     * Send a JSON-RPC method invocation request to the wallet.
     * 
     * @param <T> The type of result expected to be returned by the response.
     * @param request The request to make to the wallet.
     * @param resultType The class type of the expected result.
     * @param isGeneric Must use true for generic result types that need to be parameterized.
     * @param parameterType Only required for generic result types, the type to use as a parameter to the result type.
     * @return An RPCResponse parameterized to match the expected result type.
     * @throws WalletRequestException If there was an issue making the RPC request.
     * @throws WalletResponseException If the response indicates an error.
     */
    private <T, P> T makeRequest(RPCRequest request, Class<T> resultType, boolean isGeneric, Class<P> parameterType)
    {
        request.setJsonRpcVersion(JSON_RPC_VERSION).setId(JSON_RPC_REQUEST_ID);
        String json;
        try
        {
            json = objectMapper.writeValueAsString(request);
        }
        catch (JsonProcessingException e)
        {
            throw new WalletRequestException("Request Serialization Error: " + request.getMethod());
        }
        HttpRequest httpRequest = HttpRequest.newBuilder(uri)
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .header("Authorization", "Basic " + basicAuth)
            .header("Content-type", "application/json")
            .build();
        try
        {
            HttpResponse<String> httpResponse = HttpClient.newBuilder()
                .build()
                .send(httpRequest, BodyHandlers.ofString());
            switch (httpResponse.statusCode())
            {
                case 200:
                    // All Good
                    break;
                case 500:
                    // Internal Service Error, Will Check Specific Error Later
                    break;
                default:
                    // HTTP Response Indicates Issue
                    throw new WalletRequestException("HTTP Response Error " + httpResponse.statusCode() + ": " + request.getMethod());
            }
            JavaType responseType;
            if (isGeneric)
            {
                JavaType innerType = objectMapper.getTypeFactory().constructParametricType(resultType, parameterType);
                responseType = objectMapper.getTypeFactory().constructParametricType(RPCResponse.class, innerType);
            }
            else
            {
                responseType = objectMapper.getTypeFactory().constructParametricType(RPCResponse.class, resultType);
            }
            RPCResponse<T> response = objectMapper.readValue(httpResponse.body(), responseType);
            if (response.getError() != null)
            {
                throw new WalletResponseException(WalletResponseError.find(response.getError().getCode()));
            }
            return response.getResult();
        }
        catch (IOException | InterruptedException e)
        {
            // Failed To Get Response
            throw new WalletRequestException(request.getMethod() + " RPC Failed Due To " + e.getClass().getSimpleName());
        }
    }

    /**
     * Creates a new unsigned raw transaction using the
     * provided inputs and output addresses/amounts.
     * 
     * @param inputs A set of transaction inputs.
     * @param outputs A mapping of output address to amount.
     * @return An unsigned hex-encoded raw transaction.
     * @throws WalletRequestException If there was an issue making the RPC request.
     * @throws WalletResponseException If the response indicates an error.
     */
    public String createRawTransaction(Set<CreateRawTransactionInput> inputs, Map<String, Long> outputs)
    {
        ArrayNode params = objectMapper.createArrayNode();
        // Add TX Inputs To Parameters
        params.add(objectMapper.valueToTree(inputs));
        // Add TX Outputs To Parameters
        ArrayNode jsonOutputs = objectMapper.createArrayNode();
        for (Entry<String, Long> e : outputs.entrySet())
        {
            TextNode amount = objectMapper.valueToTree(new SatAmount(e.getValue()));
            ObjectNode kvPair = objectMapper.createObjectNode();
            kvPair.set(e.getKey(), amount);
            jsonOutputs.add(kvPair);
        }
        params.add(jsonOutputs);
        // Make Request
        RPCRequest request = new RPCRequest().setMethod("createrawtransaction").setParams(params);
        return makeRequest(request, String.class);
    }

    /**
     * Decodes and returns detailed information about a
     * hex-encoded raw transaction.
     * 
     * @param txHex The transaction hex String.
     * @return A result object.
     * @throws WalletRequestException If there was an issue making the RPC request.
     * @throws WalletResponseException If the response indicates an error.
     */
    public DecodeRawTransactionResult decodeRawTransaction(String txHex)
    {
        ArrayNode params = objectMapper.createArrayNode();
        params.add(txHex);
        RPCRequest request = new RPCRequest().setMethod("decoderawtransaction").setParams(params);
        return makeRequest(request, DecodeRawTransactionResult.class);
    }

    /**
     * Submit a raw transaction to the local node and network.
     * 
     * @param signedTxHex The signed transaction hex String.
     * @return The transaction hash in hex.
     * @throws WalletRequestException If there was an issue making the RPC request.
     * @throws WalletResponseException If the response indicates an error.
     */
    public String sendRawTransaction(String signedTxHex)
    {
        ArrayNode params = objectMapper.createArrayNode();
        params.add(signedTxHex);
        RPCRequest request = new RPCRequest().setMethod("sendrawtransaction").setParams(params);
        return makeRequest(request, String.class);
    }

    /**
     * Signs inputs for a raw transaction.
     * 
     * @param txHex The transaction hex String.
     * @return A result object.
     * @throws WalletRequestException If there was an issue making the RPC request.
     * @throws WalletResponseException If the response indicates an error.
     */
    public SignRawTransactionWithWalletResult signRawTransactionWithWallet(String txHex)
    {
        ArrayNode params = objectMapper.createArrayNode();
        params.add(txHex);
        RPCRequest request = new RPCRequest().setMethod("signrawtransactionwithwallet").setParams(params);
        return makeRequest(request, SignRawTransactionWithWalletResult.class);
    }

    /**
     * Returns a new address for receiving payments.
     * 
     * @param label The label name for the address to be linked to.
     * @return The new address.
     * @throws WalletRequestException If there was an issue making the RPC request.
     * @throws WalletResponseException If the response indicates an error.
     */
    public String getNewAddress(String label)
    {
        ArrayNode params = objectMapper.createArrayNode();
        params.add(label);
        RPCRequest request = new RPCRequest().setMethod("getnewaddress").setParams(params);
        return makeRequest(request, String.class);
    }

    /**
     * Returns a new address for receiving payments.
     * 
     * @param label The label name for the address to be linked to.
     * @param type The type of address to be created.
     * @return The new address.
     * @throws WalletRequestException If there was an issue making the RPC request.
     * @throws WalletResponseException If the response indicates an error.
     */
    public String getNewAddress(String label, DepositType type)
    {
        ArrayNode params = objectMapper.createArrayNode();
        params.add(label);
        params.add(type.getAddressType());
        RPCRequest request = new RPCRequest().setMethod("getnewaddress").setParams(params);
        return makeRequest(request, String.class);
    }

    /**
     * Returns information about the given address.
     * 
     * @param address The address to validate.
     * @return A result object.
     * @throws WalletRequestException If there was an issue making the RPC request.
     * @throws WalletResponseException If the response indicates an error.
     */
    public ValidateAddressResult validateAddress(String address)
    {
        ArrayNode params = objectMapper.createArrayNode();
        params.add(address);
        RPCRequest request = new RPCRequest().setMethod("validateaddress").setParams(params);
        return makeRequest(request, ValidateAddressResult.class);
    }

    /**
     * Returns detailed information about the given address.
     * 
     * @param address The address to get information for.
     * @return A result object.
     * @throws WalletRequestException If there was an issue making the RPC request.
     * @throws WalletResponseException If the response indicates an error.
     */
    public GetAddressInfoResult getAddressInfo(String address)
    {
        ArrayNode params = objectMapper.createArrayNode();
        params.add(address);
        RPCRequest request = new RPCRequest().setMethod("getaddressinfo").setParams(params);
        return makeRequest(request, GetAddressInfoResult.class);
    }

    /**
     * Return all unspent outputs for the given addresses.
     * 
     * @param addresses The addresses to filter.
     * @return A list of UTXO data.
     * @throws WalletRequestException If there was an issue making the RPC request.
     * @throws WalletResponseException If the response indicates an error.
     */
    @SuppressWarnings("unchecked")
    public List<ListUnspentUTXO> listUnspent(Set<String> addresses)
    {
        ArrayNode params = objectMapper.createArrayNode();
        params.add(0);
        params.add(9999999);
        ArrayNode addressNode = objectMapper.createArrayNode();
        for (String address : addresses)
        {
            addressNode.add(address);
        }
        params.add(addressNode);
        RPCRequest request = new RPCRequest().setMethod("listunspent").setParams(params);
        return makeRequest(request, (Class<List<ListUnspentUTXO>>)(Class<?>) List.class, true, ListUnspentUTXO.class);
    }

    /**
     * Return all unspent outputs for the given addresses.
     * 
     * @param addresses The addresses to filter.
     * @return A list of UTXO data.
     * @throws WalletRequestException If there was an issue making the RPC request.
     * @throws WalletResponseException If the response indicates an error.
     */
    @SuppressWarnings("unchecked")
    public List<ListUnspentUTXO> listUnspent(ListUnspentQuery listUnspentQuery)
    {
        ArrayNode params = objectMapper.valueToTree(listUnspentQuery);
        RPCRequest request = new RPCRequest().setMethod("listunspent").setParams(params);
        return makeRequest(request, (Class<List<ListUnspentUTXO>>)(Class<?>) List.class, true, ListUnspentUTXO.class);
    }

    /**
     * Estimates the approximate fee per kilobyte needed
     * for a transaction to begin confirmation within
     * <code>confirmationTarget</code> blocks if possible.
     * 
     * @param confirmationTarget The target number of blocks for confirmation.
     * @return A result object.
     * @throws WalletRequestException If there was an issue making the RPC request.
     * @throws WalletResponseException If the response indicates an error.
     */
    public EstimateSmartFeeResult estimateSmartFee(int confirmationTarget)
    {
        ArrayNode params = objectMapper.createArrayNode();
        params.add(confirmationTarget);
        RPCRequest request = new RPCRequest().setMethod("estimatesmartfee").setParams(params);
        return makeRequest(request, EstimateSmartFeeResult.class);
    }

    /**
     * Gets general information about the wallet.
     * 
     * @return A result object.
     * @throws WalletRequestException If there was an issue making the RPC request.
     * @throws WalletResponseException If the response indicates an error.
     */
    public GetWalletInfoResult getWalletInfo()
    {
        ArrayNode params = objectMapper.createArrayNode();
        RPCRequest request = new RPCRequest().setMethod("getwalletinfo").setParams(params);
        return makeRequest(request, GetWalletInfoResult.class);
    }
}