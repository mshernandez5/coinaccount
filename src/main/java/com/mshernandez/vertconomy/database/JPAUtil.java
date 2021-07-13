package com.mshernandez.vertconomy.database;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class JPAUtil
{
    private static EntityManagerFactory entityManagerFactory;

    /**
     * Initialize a project-wide entity manager factory
     * based on an embedded configuration to allow
     * JPA-interfacing components to create entity
     * manager instances.
     */
    public static void configure()
    {
        entityManagerFactory = Persistence.createEntityManagerFactory("vertconomy");
    }

    /**
     * Destroy the entity manager factory if it
     * was configured.
     */
    public static void reset()
    {
        if (entityManagerFactory != null)
        {
            entityManagerFactory.close();
            entityManagerFactory = null;
        }
    }

    /**
     * Gets a new entity manager for persisting
     * Vertconomy entities.
     * 
     * @return A new entity manager.
     */
    public static EntityManager getEntityManager()
    {
        return entityManagerFactory.createEntityManager();
    }
}
