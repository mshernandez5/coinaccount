package com.mshernandez.coinaccount;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import com.mshernandez.coinaccount.grpc.CoinaccountService.ResponseType;
import com.mshernandez.coinaccount.grpc.TransferServiceGrpc.TransferServiceImplBase;
import com.mshernandez.coinaccount.grpc.TransferServiceOuterClass.BalanceChange;
import com.mshernandez.coinaccount.grpc.TransferServiceOuterClass.BatchTransferBalanceRequest;
import com.mshernandez.coinaccount.grpc.TransferServiceOuterClass.BatchTransferBalanceResponse;
import com.mshernandez.coinaccount.grpc.TransferServiceOuterClass.TransferBalanceRequest;
import com.mshernandez.coinaccount.grpc.TransferServiceOuterClass.TransferBalanceResponse;
import com.mshernandez.coinaccount.service.TransferService;
import com.mshernandez.coinaccount.service.exception.InsufficientFundsException;
import com.mshernandez.coinaccount.service.exception.UnaccountedFundsException;
import com.mshernandez.coinaccount.service.wallet_rpc.exception.WalletRequestException;

import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.Blocking;

@GrpcService
public class TransferController extends TransferServiceImplBase
{
    @Inject
    TransferService transferService;

    @Override
    @Blocking
    public void transferBalance(TransferBalanceRequest request, StreamObserver<TransferBalanceResponse> responseObserver)
    {
        ResponseType responseType = ResponseType.ERROR_UNKNOWN;
        try
        {
            UUID senderUUID = UUID.fromString(request.getSenderId().getUuid());
            UUID receiverUUID = UUID.fromString(request.getReceiverId().getUuid());
            transferService.transferBalance(senderUUID, receiverUUID, request.getTransferAll(), request.getAmount());
        }
        catch (IllegalArgumentException e)
        {
            responseType = ResponseType.ERROR_INVALID_ACCOUNT_IDENTIFIER;
        }
        catch (InsufficientFundsException e)
        {
            responseType = ResponseType.ERROR_INSUFFICIENT_FUNDS;
        }
        catch (WalletRequestException e)
        {
            responseType = ResponseType.ERROR_NO_WALLET_CONNECTION;
        }
        TransferBalanceResponse response = TransferBalanceResponse.newBuilder()
            .setResponseType(responseType)
            .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    @Blocking
    public void batchTransferBalance(BatchTransferBalanceRequest request, StreamObserver<BatchTransferBalanceResponse> responseObserver)
    {
        ResponseType responseType = ResponseType.SUCCESS;
        try
        {
            Map<UUID, Long> changes = new HashMap<>();
            for (BalanceChange change : request.getChangesList())
            {
                UUID accountUUID = UUID.fromString(change.getAccountId().getUuid());
                changes.put(accountUUID, change.getNetBalanceChange());
            }
            transferService.batchTransfer(changes);
        }
        catch (IllegalArgumentException e)
        {
            responseType = ResponseType.ERROR_INVALID_ACCOUNT_IDENTIFIER;
        }
        catch (UnaccountedFundsException e)
        {
            responseType = ResponseType.ERROR_UNACCOUNTED_FUNDS;
        }
        catch (WalletRequestException e)
        {
            responseType = ResponseType.ERROR_NO_WALLET_CONNECTION;
        }
        BatchTransferBalanceResponse response = BatchTransferBalanceResponse.newBuilder()
            .setResponseType(responseType)
            .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}