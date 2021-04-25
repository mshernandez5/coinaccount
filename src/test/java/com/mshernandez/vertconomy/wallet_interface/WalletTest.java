package com.mshernandez.vertconomy.wallet_interface;

import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;

/**
 * Testing integration with the core wallet API.
 * Ignored during build, requires active and
 * properly configured wallet running to succeed.
 */
@Ignore
public class WalletTest
{
    // Strictly For Testing Purposes
    private static final String WALLET_URI = "http://127.0.0.1:22555";
    private static final String WALLET_USERNAME = "vtctest";
    private static final String WALLET_PASSWORD = "vtcpass";

    // Minimum Confirmations To Consider A Transaction Valid In Tests
    private static final int MIN_CONFIRMATIONS = 6;

    private RPCWalletConnection wallet;

    @BeforeEach
    public void init()
    {
        try
        {
            wallet = new RPCWalletConnection(new URI(WALLET_URI), WALLET_USERNAME, WALLET_PASSWORD);
        }
        catch (URISyntaxException e)
        {
            System.err.println(e.getMessage());
        }
    }

    @Test
    public void checkTotalBalanceTest()
    {
        try
        {
            System.out.println(wallet.getBalance(MIN_CONFIRMATIONS));
        }
        catch (WalletRequestException e)
        {
            fail(e.getMessage());
        }
    }

    @Test
    public void checkAddressBalanceTest()
    {
        try
        {
            System.out.println(wallet.getReceivedByAddress("DLevAm2fAewP7pk9jtyHYd7BpVKyLRNKV6", MIN_CONFIRMATIONS));
        }
        catch (WalletRequestException e)
        {
            fail(e.getMessage());
        }
    }
}