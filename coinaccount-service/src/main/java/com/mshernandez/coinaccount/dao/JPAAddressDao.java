package com.mshernandez.coinaccount.dao;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;

import com.mshernandez.coinaccount.entity.Account;
import com.mshernandez.coinaccount.entity.Address;
import com.mshernandez.coinaccount.service.wallet_rpc.WalletService;
import com.mshernandez.coinaccount.service.wallet_rpc.parameter.DepositType;

@ApplicationScoped
@Transactional
public class JPAAddressDao implements AddressDao
{
    @Inject
    EntityManager entityManager;

    @Inject
    WalletService walletService;

    @Inject
    AccountDao accountDao;

    @Override
    public Address find(String address)
    {
        return entityManager.find(Address.class, address);
    }

    @Override
    public Collection<Address> findAll()
    {
        return entityManager.createQuery("SELECT a FROM Address a", Address.class)
            .getResultList();
    }

    @Override
    public Address findOrCreate(Account account, DepositType type, boolean requireUnused)
    {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Address> query = cb.createQuery(Address.class);
        Root<Address> address = query.from(Address.class);
        Predicate[] predicates = new Predicate[3];
        predicates[0] = cb.equal(address.get("owner"), account);
        predicates[1] = cb.equal(address.get("type"), type);       
        predicates[2] = cb.isFalse(address.get("used"));
        query.select(address).where(requireUnused ? predicates : Arrays.copyOf(predicates, 2));
        List<Address> results = entityManager.createQuery(query).getResultList();
        if (!results.isEmpty())
        {
            return results.get(0);
        }
        Address created = new Address(walletService.getNewAddress(account.getAccountUUID().toString(), type), type, account);
        entityManager.persist(created);
        account.addNewAddress(created);
        accountDao.update(account);
        return created;
    }

    @Override
    public Address update(Address address)
    {
        return entityManager.merge(address);
    }

    @Override
    public void refresh(Address address)
    {
        entityManager.refresh(address);
    }

    @Override
    public void remove(Address address)
    {
        entityManager.remove(address);
    }
}
