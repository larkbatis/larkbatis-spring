package com.example.lbspring;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The point of the whole module: a service whose {@code @Transactional}
 * boundary the mapper joins without being told about it. LightBatis never
 * manages a transaction — it asks {@code DataSourceUtils} for a connection
 * and gets whichever one Spring already bound (design §10).
 */
@Service
public class AccountService {

    private final AccountMapper accounts;

    public AccountService(AccountMapper accounts) {
        this.accounts = accounts;
    }

    @Transactional
    public long open(String owner, BigDecimal balance) {
        Account account = new Account();
        account.setOwner(owner);
        account.setBalance(balance);
        accounts.insert(account);
        return account.getId();
    }

    /**
     * Two writes and a failure. If the mapper were opening its own connection,
     * the first write would survive the rollback — which is the bug this
     * module exists to make impossible.
     */
    @Transactional
    public void openThenFail(String owner, BigDecimal balance) {
        open(owner, balance);
        throw new IllegalStateException("deliberate failure after the insert");
    }

    /** Reads its own uncommitted write — same connection, same transaction. */
    @Transactional
    public int openAndCount(String owner, BigDecimal balance) {
        open(owner, balance);
        return accounts.count();
    }

    @Transactional(readOnly = true)
    public Account get(long id) {
        return accounts.findById(id);
    }
}
