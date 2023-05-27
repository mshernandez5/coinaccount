package com.mshernandez.coinaccount.task;

import java.util.Set;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.mshernandez.coinaccount.service.WithdrawService;

import io.quarkus.scheduler.Scheduled;
import io.vertx.core.eventbus.EventBus;

/**
 * Periodically checks for and removes
 * expired withdraw requests.
 */
@ApplicationScoped
public class WithdrawRequestTask
{
    @Inject
    EventBus eventBus;

    @Inject
    WithdrawService withdrawService;

    @Scheduled(every = "{coinaccount.withdraw.expire.check}")
    public void cancelExpiredWithdrawRequests()
    {
        // Cancel All Expired Requests
        Set<UUID> affectedAccountIds = withdrawService.cancelExpiredRequests();
        // Publish Events To Notify Affected Accounts
        for (UUID accountId : affectedAccountIds)
        {
            eventBus.publish("withdraw-request-expired", new WithdrawRequestExpiredEvent(accountId));
        }
    }
}