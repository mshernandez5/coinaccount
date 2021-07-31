package com.mshernandez.coinaccount;

import java.util.UUID;

import javax.inject.Inject;

import com.mshernandez.coinaccount.grpc.AccountInfoServiceGrpc.AccountInfoServiceImplBase;
import com.mshernandez.coinaccount.grpc.AccountInfoServiceOuterClass.GetBalanceInfoResponse;
import com.mshernandez.coinaccount.grpc.AccountInfoServiceOuterClass.GetDepositAddressResponse;
import com.mshernandez.coinaccount.grpc.AccountInfoServiceOuterClass.GetReturnAddressResponse;
import com.mshernandez.coinaccount.grpc.AccountInfoServiceOuterClass.SetReturnAddressRequest;
import com.mshernandez.coinaccount.grpc.AccountInfoServiceOuterClass.SetReturnAddressResponse;
import com.mshernandez.coinaccount.grpc.CoinaccountService.AccountIdentifier;
import com.mshernandez.coinaccount.grpc.CoinaccountService.ResponseType;
import com.mshernandez.coinaccount.service.AccountInfoService;
import com.mshernandez.coinaccount.service.exception.InvalidAddressException;
import com.mshernandez.coinaccount.service.result.AccountBalanceInfo;
import com.mshernandez.coinaccount.service.wallet_rpc.exception.WalletRequestException;

import org.jboss.logging.Logger;

import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;

@GrpcService
public class AccountInfoController extends AccountInfoServiceImplBase
{
    @Inject
    Logger logger;

    @Inject
    AccountInfoService accountInfoService;

    @Override
    public void getBalanceInfo(AccountIdentifier accountId, StreamObserver<GetBalanceInfoResponse> responseObserver)
    {
        GetBalanceInfoResponse response;
        try
        {
            UUID accountUUID = UUID.fromString(accountId.getUuid());
            AccountBalanceInfo info = accountInfoService.getBalanceInfo(accountUUID);
            response = GetBalanceInfoResponse.newBuilder()
                .setResponseType(ResponseType.SUCCESS)
                .setConfirmedBalance(info.getConfirmedBalance())
                .setWithdrawableBalance(info.getWithdrawableBalance())
                .setUnconfirmedBalance(info.getUnconfirmedBalance())
                .build();
        }
        catch (Exception e)
        {
            ResponseType responseType = ResponseType.SUCCESS;
            if (e instanceof IllegalArgumentException)
            {
                responseType = ResponseType.ERROR_INVALID_ACCOUNT_IDENTIFIER;
            }
            else if (e instanceof WalletRequestException)
            {
                responseType = ResponseType.ERROR_NO_WALLET_CONNECTION;
            }
            else
            {
                responseType = ResponseType.ERROR_INTERNAL;
            }
            response = GetBalanceInfoResponse.newBuilder()
                .setResponseType(responseType)
                .setConfirmedBalance(0L)
                .setWithdrawableBalance(0L)
                .setUnconfirmedBalance(0L)
                .build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getDepositAddress(AccountIdentifier accountId, StreamObserver<GetDepositAddressResponse> responseObserver)
    {
        GetDepositAddressResponse response;
        try
        {
            UUID accountUUID = UUID.fromString(accountId.getUuid());
            response = GetDepositAddressResponse.newBuilder()
                .setResponseType(ResponseType.SUCCESS)
                .setDepositAddress(accountInfoService.getDepositAddress(accountUUID))
                .build();
        }
        catch (Exception e)
        {
            ResponseType responseType = ResponseType.ERROR_UNKNOWN;
            if (e instanceof IllegalArgumentException)
            {
                responseType = ResponseType.ERROR_INVALID_ACCOUNT_IDENTIFIER;
            }
            else if (e instanceof WalletRequestException)
            {
                responseType = ResponseType.ERROR_NO_WALLET_CONNECTION;
            }
            else
            {
                responseType = ResponseType.ERROR_INTERNAL;
            }
            response = GetDepositAddressResponse.newBuilder()
                .setResponseType(responseType)
                .setDepositAddress("")
                .build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getReturnAddress(AccountIdentifier accountId, StreamObserver<GetReturnAddressResponse> responseObserver)
    {
        GetReturnAddressResponse response;
        try
        {
            UUID accountUUID = UUID.fromString(accountId.getUuid());
            response = GetReturnAddressResponse.newBuilder()
                .setResponseType(ResponseType.SUCCESS)
                .setReturnAddress(accountInfoService.getReturnAddress(accountUUID))
                .build();
        }
        catch (Exception e)
        {
            ResponseType responseType = ResponseType.ERROR_UNKNOWN;
            if (e instanceof IllegalArgumentException)
            {
                responseType = ResponseType.ERROR_INVALID_ACCOUNT_IDENTIFIER;
            }
            else if (e instanceof WalletRequestException)
            {
                responseType = ResponseType.ERROR_NO_WALLET_CONNECTION;
            }
            else
            {
                responseType = ResponseType.ERROR_INTERNAL;
            }
            response = GetReturnAddressResponse.newBuilder()
                .setResponseType(responseType)
                .setReturnAddress("")
                .build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void setReturnAddress(SetReturnAddressRequest request, StreamObserver<SetReturnAddressResponse> responseObserver)
    {
        SetReturnAddressResponse response;
        try
        {
            UUID accountUUID = UUID.fromString(request.getAccountId().getUuid());
            accountInfoService.setReturnAddress(accountUUID, request.getReturnAddress());
            response = SetReturnAddressResponse.newBuilder()
                .setResponseType(ResponseType.SUCCESS)
                .build();
        }
        catch (Exception e)
        {
            ResponseType responseType = ResponseType.ERROR_UNKNOWN;
            if (e instanceof IllegalArgumentException)
            {
                responseType = ResponseType.ERROR_INVALID_ACCOUNT_IDENTIFIER;
            }
            else if (e instanceof WalletRequestException)
            {
                responseType = ResponseType.ERROR_NO_WALLET_CONNECTION;
            }
            else if (e instanceof InvalidAddressException)
            {
                responseType = ResponseType.ERROR_INVALID_ADDRESS;
            }
            else
            {
                responseType = ResponseType.ERROR_INTERNAL;
            }
            response = SetReturnAddressResponse.newBuilder()
                .setResponseType(responseType)
                .build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
