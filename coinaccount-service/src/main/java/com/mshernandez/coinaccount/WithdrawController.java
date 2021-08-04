package com.mshernandez.coinaccount;

import java.util.UUID;

import javax.inject.Inject;

import com.mshernandez.coinaccount.grpc.CoinaccountService.AccountIdentifier;
import com.mshernandez.coinaccount.grpc.CoinaccountService.ResponseType;
import com.mshernandez.coinaccount.grpc.WithdrawServiceGrpc.WithdrawServiceImplBase;
import com.mshernandez.coinaccount.grpc.WithdrawServiceOuterClass.CancelWithdrawResponse;
import com.mshernandez.coinaccount.grpc.WithdrawServiceOuterClass.CheckForPendingWithdrawResponse;
import com.mshernandez.coinaccount.grpc.WithdrawServiceOuterClass.CompleteWithdrawResponse;
import com.mshernandez.coinaccount.grpc.WithdrawServiceOuterClass.InitiateWithdrawRequest;
import com.mshernandez.coinaccount.grpc.WithdrawServiceOuterClass.InitiateWithdrawResponse;
import com.mshernandez.coinaccount.service.WithdrawService;
import com.mshernandez.coinaccount.service.exception.CannotAffordFeesException;
import com.mshernandez.coinaccount.service.exception.InsufficientFundsException;
import com.mshernandez.coinaccount.service.exception.InvalidAddressException;
import com.mshernandez.coinaccount.service.exception.NotEnoughWithdrawableFundsException;
import com.mshernandez.coinaccount.service.exception.WithdrawRequestAlreadyExistsException;
import com.mshernandez.coinaccount.service.exception.WithdrawRequestNotFoundException;
import com.mshernandez.coinaccount.service.result.WithdrawRequestResult;
import com.mshernandez.coinaccount.service.wallet_rpc.exception.WalletRequestException;

import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.Blocking;

@GrpcService
public class WithdrawController extends WithdrawServiceImplBase
{
    @Inject
    WithdrawService withdrawService;

    @Override
    @Blocking
    public void initiateWithdraw(InitiateWithdrawRequest request, StreamObserver<InitiateWithdrawResponse> responseObserver)
    {
        InitiateWithdrawResponse response;
        try
        {
            UUID initiatorUUID = UUID.fromString(request.getAccountId().getUuid());
            WithdrawRequestResult result = withdrawService.initiateWithdrawRequest(initiatorUUID, request.getDestAddress(), request.getWithdrawAll(), request.getAmount());
            response = InitiateWithdrawResponse.newBuilder()
                .setResponseType(ResponseType.SUCCESS)
                .setWithdrawAmount(result.getWithdrawAmount())
                .setFeeAmount(result.getFeeAmount())
                .setTotalCost(result.getTotalCost())
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
            else if (e instanceof InsufficientFundsException)
            {
                responseType = ResponseType.ERROR_INSUFFICIENT_FUNDS;
            }
            else if (e instanceof NotEnoughWithdrawableFundsException)
            {
                responseType = ResponseType.ERROR_NOT_ENOUGH_WITHDRAWABLE_FUNDS;
            }
            else if (e instanceof CannotAffordFeesException)
            {
                responseType = ResponseType.ERROR_CANNOT_AFFORD_FEES;
            }
            else if (e instanceof WithdrawRequestAlreadyExistsException)
            {
                responseType = ResponseType.ERROR_WITHDRAW_REQUEST_ALREADY_EXISTS;
            }
            else
            {
                responseType = ResponseType.ERROR_INTERNAL;
            }
            response = InitiateWithdrawResponse.newBuilder()
                .setResponseType(responseType)
                .setWithdrawAmount(0L)
                .setFeeAmount(0L)
                .setTotalCost(0L)
                .build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    @Blocking
    public void cancelWithdraw(AccountIdentifier accountId, StreamObserver<CancelWithdrawResponse> responseObserver)
    {
        CancelWithdrawResponse response;
        try
        {
            UUID initiatorUUID = UUID.fromString(accountId.getUuid());
            withdrawService.cancelWithdraw(initiatorUUID);
            response = CancelWithdrawResponse.newBuilder()
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
            else if (e instanceof WithdrawRequestNotFoundException)
            {
                responseType = ResponseType.ERROR_WITHDRAW_REQUEST_NOT_FOUND;
            }
            else
            {
                responseType = ResponseType.ERROR_INTERNAL;
            }
            response = CancelWithdrawResponse.newBuilder()
                .setResponseType(responseType)
                .build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    @Blocking
    public void completeWithdraw(AccountIdentifier accountId, StreamObserver<CompleteWithdrawResponse> responseObserver)
    {
        CompleteWithdrawResponse response;
        try
        {
            UUID initiatorUUID = UUID.fromString(accountId.getUuid());
            response = CompleteWithdrawResponse.newBuilder()
                .setResponseType(ResponseType.SUCCESS)
                .setTxid(withdrawService.completeWithdraw(initiatorUUID))
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
            response = CompleteWithdrawResponse.newBuilder()
                .setResponseType(responseType)
                .setTxid("ERROR")
                .build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    @Blocking
    public void checkForPendingWithdraw(AccountIdentifier accountId, StreamObserver<CheckForPendingWithdrawResponse> responseObserver)
    {
        CheckForPendingWithdrawResponse response;
        try
        {
            UUID initiatorUUID = UUID.fromString(accountId.getUuid());
            WithdrawRequestResult result = withdrawService.getWithdrawRequest(initiatorUUID);
            response = CheckForPendingWithdrawResponse.newBuilder()
                .setResponseType(ResponseType.SUCCESS)
                .setWithdrawRequestExists(true)
                .setWithdrawAmount(result.getWithdrawAmount())
                .setFeeAmount(result.getFeeAmount())
                .setTotalCost(result.getTotalCost())
                .build();
        }
        catch (Exception e)
        {
            ResponseType responseType = ResponseType.ERROR_UNKNOWN;
            if (e instanceof IllegalArgumentException)
            {
                responseType = ResponseType.ERROR_INVALID_ACCOUNT_IDENTIFIER;
            }
            else
            {
                responseType = ResponseType.ERROR_INTERNAL;
            }
            response = CheckForPendingWithdrawResponse.newBuilder()
                .setResponseType(responseType)
                .setWithdrawRequestExists(false)
                .setWithdrawAmount(0L)
                .setFeeAmount(0L)
                .setTotalCost(0L)
                .build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
