package com.mshernandez.vertconomy.core.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.mshernandez.vertconomy.core.entity.Account;
import com.mshernandez.vertconomy.core.entity.Deposit;

/**
 * For internal transfers, pre-selects
 * deposit shares to transfer based on
 * certain conditions (ex. avoid
 * creation of additional shares)
 * before passing remaining inputs
 * to a normal selector.
 */
public class InternalTransferPreselector
{
    CoinEvaluator<Deposit> evaluator;
    CoinSelector<Deposit> primarySelector;

    Account sender;
    Account receiver;

    public InternalTransferPreselector(CoinEvaluator<Deposit> evaluator, CoinSelector<Deposit> primarySelector, Account sender, Account receiver)
    {
        this.primarySelector = primarySelector;
        this.sender = sender;
        this.receiver = receiver;
    }

    /**
     * Select inputs to meet the given target,
     * first transferring shares of any deposits
     * the sender and receiver have in common.
     * 
     * @param target The target amount to transfer.
     * @return A set of inputs to meet the target amount, or null if such a set cannot be formed.
     */
    public Set<Deposit> selectInputs(long target)
    {
        Set<Deposit> senderDeposits = sender.getDeposits();
        Set<Deposit> receiverDeposits = receiver.getDeposits();
        // Get Sender Deposits That Receiver Also Has Share In
        List<Deposit> sorted = senderDeposits.stream()
            .filter(d -> receiverDeposits.contains(d))
            .sorted(evaluator)
            .collect(Collectors.toCollection(ArrayList::new));
        // Transfer These Deposits First
        Set<Deposit> selected = new LinkedHashSet<>();
        Iterator<Deposit> iterator = sorted.iterator();
        while (iterator.hasNext() && target > 0L)
        {
            Deposit next = iterator.next();
            selected.add(next);
            target -= evaluator.costAdjustedValue(next);
        }
        // Select Remaining Deposits With Regular Algorithm
        if (target > 0L)
        {
            senderDeposits.removeAll(selected);
            Set<Deposit> additional = primarySelector.selectInputs(evaluator, senderDeposits, target);
            if (additional == null)
            {
                return null;
            }
            selected.addAll(additional);
        }
        return selected;
    }
}