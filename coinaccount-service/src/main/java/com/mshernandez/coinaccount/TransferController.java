package com.mshernandez.coinaccount;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import com.mshernandez.coinaccount.grpc.CoinAccountProtos.ResponseType;
import com.mshernandez.coinaccount.grpc.TransferProtos.BalanceChange;
import com.mshernandez.coinaccount.grpc.TransferProtos.BatchTransferBalanceRequest;
import com.mshernandez.coinaccount.grpc.TransferProtos.BatchTransferBalanceResponse;
import com.mshernandez.coinaccount.grpc.TransferProtos.TransferBalanceRequest;
import com.mshernandez.coinaccount.grpc.TransferProtos.TransferBalanceResponse;
import com.mshernandez.coinaccount.grpc.TransferServiceGrpc.TransferServiceImplBase;
import com.mshernandez.coinaccount.service.TransferService;
import com.mshernandez.coinaccount.service.exception.InsufficientFundsException;
import com.mshernandez.coinaccount.service.exception.UnaccountedFundsException;
import com.mshernandez.coinaccount.service.wallet_rpc.exception.WalletRequestException;

import org.jboss.logging.Logger;

import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.Blocking;

@GrpcService
public class TransferController extends TransferServiceImplBase
{
    @Inject
    Logger logger;

    @Inject
    TransferService transferService;

    @Override
    @Blocking
    public void transferBalance(TransferBalanceRequest request, StreamObserver<TransferBalanceResponse> responseObserver)
    {
        TransferBalanceResponse response;
        try
        {
            UUID senderUUID = UUID.fromString(request.getSender().getUuid());
            UUID receiverUUID = UUID.fromString(request.getReceiver().getUuid());
            transferService.transferBalance(senderUUID, receiverUUID, request.getTransferAll(), request.getAmount());
            response = TransferBalanceResponse.newBuilder()
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
            else if (e instanceof InsufficientFundsException)
            {
                responseType = ResponseType.ERROR_INSUFFICIENT_FUNDS;
            }
            else if (e instanceof WalletRequestException)
            {
                responseType = ResponseType.ERROR_NO_WALLET_CONNECTION;
            }
            else
            {
                logger.warn("transferBalance: Unexpected Exception: " + e.getMessage());
                responseType = ResponseType.ERROR_INTERNAL;
            }
            response = TransferBalanceResponse.newBuilder()
                .setResponseType(responseType)
                .build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    @Blocking
    public void batchTransferBalance(BatchTransferBalanceRequest request, StreamObserver<BatchTransferBalanceResponse> responseObserver)
    {
        BatchTransferBalanceResponse response;
        try
        {
            Map<UUID, Long> changes = new HashMap<>();
            for (BalanceChange change : request.getChangesList())
            {
                UUID accountUUID = UUID.fromString(change.getAccount().getUuid());
                changes.put(accountUUID, change.getNetBalanceChange());
            }
            transferService.batchTransfer(changes);
            response = BatchTransferBalanceResponse.newBuilder()
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
            else if (e instanceof UnaccountedFundsException)
            {
                responseType = ResponseType.ERROR_UNACCOUNTED_FUNDS;
            }
            else if (e instanceof WalletRequestException)
            {
                responseType = ResponseType.ERROR_NO_WALLET_CONNECTION;
            }
            else
            {
                logger.warn("batchTransferBalance: Unexpected Exception: " + e.getMessage());
                responseType = ResponseType.ERROR_INTERNAL;
            }
            response = BatchTransferBalanceResponse.newBuilder()
                .setResponseType(responseType)
                .build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}