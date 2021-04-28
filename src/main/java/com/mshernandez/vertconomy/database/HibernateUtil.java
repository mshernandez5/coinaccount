package com.mshernandez.vertconomy.database;

import org.h2.tools.Server;
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
    private static final SessionFactory sessionFactory;

    static
    {
        try
        {
            sessionFactory = new Configuration()
                .addAnnotatedClass(Account.class)
                .addAnnotatedClass(BlockchainTransaction.class)
                .buildSessionFactory();
            // Temporary, Only For Development Purposes (Security Risk)
            Server webServer = Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082");
            webServer.start();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed To Configure Session Factory: " + e.getMessage());
        }
    }

    /**
     * Get a preconfigured SessionFactory.
     * 
     * @return A configured SessionFactory.
     */
    public static SessionFactory getSessionFactory()
    {
        return sessionFactory;
    }
}
