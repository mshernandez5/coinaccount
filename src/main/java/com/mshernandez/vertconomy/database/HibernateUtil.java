package com.mshernandez.vertconomy.database;

import java.sql.SQLException;

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
    private static SessionFactory sessionFactory = null;
    private static Server webServer = null;

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
        stopWebServer();
    }

    /**
     * Starts the H2 web console for development purposes.
     * 
     * @throws SQLException If the web server could not be started.
     */
    public static void startWebServer() throws SQLException
    {
        if (webServer != null)
        {
            return;
        }
        webServer = Server.createWebServer("-webPort", "8082");
        webServer.start();
    }

    /**
     * Stops the H2 web console if it is running.
     */
    public static void stopWebServer()
    {
        if (webServer == null)
        {
            return;
        }
        webServer.stop();
        webServer = null;
    }
}
