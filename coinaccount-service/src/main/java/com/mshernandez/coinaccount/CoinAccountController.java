package com.mshernandez.coinaccount;

import javax.inject.Inject;

import com.mshernandez.coinaccount.grpc.CoinAccountServiceGrpc.CoinAccountServiceImplBase;
import com.mshernandez.coinaccount.grpc.CoinaccountService.CheckCoinAccountStatusResponse;
import com.mshernandez.coinaccount.grpc.CoinaccountService.Empty;
import com.mshernandez.coinaccount.grpc.CoinaccountService.GetCoinRepresentationResponse;
import com.mshernandez.coinaccount.grpc.CoinaccountService.GetDepositConfigurationResponse;
import com.mshernandez.coinaccount.grpc.CoinaccountService.GetWithdrawConfigurationResponse;
import com.mshernandez.coinaccount.grpc.CoinaccountService.ResponseType;
import com.mshernandez.coinaccount.service.wallet_rpc.WalletService;
import com.mshernandez.coinaccount.service.wallet_rpc.exception.WalletRequestException;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.Blocking;

@GrpcService
public class CoinAccountController extends CoinAccountServiceImplBase
{
    @ConfigProperty(name = "coinaccount.coin.symbol")
    String coinSymbol;

    @ConfigProperty(name = "coinaccount.coin.base.symbol")
    String coinBaseUnitSymbol;

    @ConfigProperty(name = "coinaccount.deposit.confirmations")
    int minDepositConfirmations;

    @ConfigProperty(name = "coinaccount.withdraw.target")
    int targetBlockTime;

    @Inject
    WalletService walletService;

    @Override
    @Blocking
    public void checkCoinAccountStatus(Empty empty, StreamObserver<CheckCoinAccountStatusResponse> responseObserver)
    {
        boolean connectedToWallet = true;
        try
        {
            walletService.getWalletInfo();
        }
        catch (WalletRequestException e)
        {
            connectedToWallet = false;
        }
        CheckCoinAccountStatusResponse response = CheckCoinAccountStatusResponse.newBuilder()
            .setResponseType(ResponseType.SUCCESS)
            .setHasWalletConnection(connectedToWallet)
            .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getCoinRepresentation(Empty empty, StreamObserver<GetCoinRepresentationResponse> responseObserver)
    {
        GetCoinRepresentationResponse response =  GetCoinRepresentationResponse.newBuilder()
            .setResponseType(ResponseType.SUCCESS)
            .setCoinSymbol(coinSymbol)
            .setCoinBaseUnitSymbol(coinBaseUnitSymbol)
            .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getDepositConfiguration(Empty empty, StreamObserver<GetDepositConfigurationResponse> responseObserver)
    {
        GetDepositConfigurationResponse response = GetDepositConfigurationResponse.newBuilder()
            .setResponseType(ResponseType.SUCCESS)
            .setMinDepositConfirmations(minDepositConfirmations)
            .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getWithdrawConfiguration(Empty empty, StreamObserver<GetWithdrawConfigurationResponse> responseObserver)
    {
        GetWithdrawConfigurationResponse response = GetWithdrawConfigurationResponse.newBuilder()
            .setResponseType(ResponseType.SUCCESS)
            .setTargetBlockTime(targetBlockTime)
            .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}