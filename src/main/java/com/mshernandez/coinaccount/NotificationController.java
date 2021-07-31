package com.mshernandez.coinaccount;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.mshernandez.coinaccount.grpc.CoinaccountService.AccountIdentifier;
import com.mshernandez.coinaccount.grpc.NotificationServiceGrpc.NotificationServiceImplBase;
import com.mshernandez.coinaccount.grpc.NotificationServiceOuterClass.Notification;
import com.mshernandez.coinaccount.grpc.NotificationServiceOuterClass.NotificationRequest;
import com.mshernandez.coinaccount.grpc.NotificationServiceOuterClass.NotificationType;
import com.mshernandez.coinaccount.task.DepositConfirmedEvent;
import com.mshernandez.coinaccount.task.WithdrawRequestExpiredEvent;

import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.quarkus.vertx.ConsumeEvent;

@GrpcService
public class NotificationController extends NotificationServiceImplBase
{
    // Maps Observers To The Accounts They Want To Receive Notifications For
    ConcurrentHashMap<ServerCallStreamObserver<Notification>, Set<String>> observers;

    NotificationController()
    {
        observers = new ConcurrentHashMap<>();
    }

    @ConsumeEvent(value = "deposit-confirmed")
    void handleConfirmedDepositEvent(DepositConfirmedEvent e)
    {
        AccountIdentifier accountIdentifier  = AccountIdentifier.newBuilder()
            .setUuid(e.getAccountId().toString())
            .build();
        Notification notification = Notification.newBuilder()
            .setNotificationType(NotificationType.CONFIRMED_DEPOSIT)
            .setAccountId(accountIdentifier)
            .setAmount(e.getAmount())
            .build();
        pushNotification(notification);
    }

    @ConsumeEvent(value = "withdraw-request-expired")
    void handleExpiredWithdrawRequestEvent(WithdrawRequestExpiredEvent e)
    {
        AccountIdentifier accountIdentifier  = AccountIdentifier.newBuilder()
            .setUuid(e.getAccountId().toString())
            .build();
        Notification notification = Notification.newBuilder()
            .setNotificationType(NotificationType.EXPIRED_WITHDRAW_REQUEST)
            .setAccountId(accountIdentifier)
            .setAmount(0L)
            .build();
        pushNotification(notification);
    }

    private void pushNotification(Notification notification)
    {
        Iterator<Entry<ServerCallStreamObserver<Notification>, Set<String>>> it = observers.entrySet().iterator();
        while (it.hasNext())
        {
            Entry<ServerCallStreamObserver<Notification>, Set<String>> entry = it.next();
            ServerCallStreamObserver<Notification> observer = entry.getKey();
            if (observer.isCancelled())
            {
                observer.onCompleted();
                it.remove();
            }
            else if (entry.getValue().contains(notification.getAccountId().getUuid()))
            {
                observer.onNext(notification);
            }
        }
    }

    @Override
    public void receiveNotifications(NotificationRequest request, StreamObserver<Notification> responseObserver)
    {
        try
        {
            // Subscribe To Notifications
            List<AccountIdentifier> accounts = request.getAccountIdList();
            Set<String> accountIds = accounts.stream()
                .map(a -> a.getUuid())
                .collect(Collectors.toCollection(HashSet::new));
            observers.put((ServerCallStreamObserver<Notification>) responseObserver, accountIds);
        }
        catch (IllegalArgumentException e)
        {
            // Bad UUID Specified, End Stream
            responseObserver.onError(e);
        }
    }
}