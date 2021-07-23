package com.mshernandez.vertconomy.tasks;

import com.mshernandez.vertconomy.core.Vertconomy;

/**
 * A task run periodically to check for new player
 * deposits.
 */
public class CheckDepositTask implements Runnable
{
    private Vertconomy vertconomy;

    public CheckDepositTask(Vertconomy vertconomy)
    {
        this.vertconomy = vertconomy;
    }

    @Override
    public void run()
    {
        // Check For New Deposits
        vertconomy.checkForNewDeposits();
    }
}
