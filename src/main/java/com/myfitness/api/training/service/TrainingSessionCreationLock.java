package com.myfitness.api.training.service;

import java.sql.SQLException;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.springframework.stereotype.Component;

import com.myfitness.api.user.entity.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

@Component
class TrainingSessionCreationLock {

    @PersistenceContext
    private EntityManager entityManager;

    void lock(User user, UUID idempotencyKey) {
        if (isPostgreSql()) {
            // Serializes the same user/key across application instances without
            // requiring UPDATE privilege on the users table. The lock is released
            // automatically when the surrounding transaction ends.
            entityManager.createNativeQuery(
                            "select pg_advisory_xact_lock(hashtextextended(cast(:lockValue as text), 0))")
                    .setParameter("lockValue", user.getId() + ":" + idempotencyKey)
                    .getSingleResult();
            return;
        }

        // H2 integration tests do not provide PostgreSQL advisory locks.
        entityManager.lock(user, LockModeType.PESSIMISTIC_WRITE);
    }

    private boolean isPostgreSql() {
        return entityManager.unwrap(Session.class).doReturningWork(connection -> {
            try {
                return connection.getMetaData().getDatabaseProductName()
                        .toLowerCase(Locale.ROOT)
                        .contains("postgresql");
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not identify the database", exception);
            }
        });
    }
}
