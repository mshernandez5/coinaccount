package com.mshernandez.vertconomy.database;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * A singleton class to hold a static reference
 * to a pre-configured SessionFactory that
 * database access objects may use to
 * create Sessions for transactions.
 */
public class HibernateUtil
{
    private static SessionFactory sessionFactory = null;

    /**
     * Configures the static SessionFactory if it has not
     * already been done before.
     */
    public static void configure()
    {
        if (sessionFactory != null)
        {
            return;
        }
        try
        {
            sessionFactory = new Configuration()
                .addAnnotatedClass(Account.class)
                .addAnnotatedClass(BlockchainTransaction.class)
                .buildSessionFactory();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed To Configure Session Factory: " + e.getMessage());
        }
    }

    /**
     * Close SessionFactory instance, reset reference
     * to null to allow reconfiguration.
     */
    public static void reset()
    {
        try
        {
            sessionFactory.close();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed To Close Session Factory: " + e.getMessage());
        }
        sessionFactory = null;
    }

    /**
     * Get a preconfigured SessionFactory,
     * or null if the configure() method
     * has not been run yet.
     * 
     * @return A configured SessionFactory.
     */
    public static SessionFactory getSessionFactory()
    {
        return sessionFactory;
    }
}
