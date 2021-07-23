package com.mshernandez.vertconomy.tasks;

import com.mshernandez.vertconomy.core.Vertconomy;

public class CancelExpiredRequestsTask implements Runnable
{
    private Vertconomy vertconomy;

    public CancelExpiredRequestsTask(Vertconomy vertconomy)
    {
        this.vertconomy = vertconomy;
    }
    
    @Override
    public void run()
    {
        vertconomy.cancelExpiredRequests();
    }
}
