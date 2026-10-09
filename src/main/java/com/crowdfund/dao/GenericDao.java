package com.crowdfund.dao;

import java.util.List;
import java.util.Optional;

/**
 * Generic Data Access Object Interface demonstrating Generics and OOP Interfaces.
 *
 * @param <T>  The domain entity type
 * @param <ID> The entity identifier type
 */
public interface GenericDao<T, ID> {

    T save(T entity);

    Optional<T> findById(ID id);

    List<T> findAll();

    void deleteById(ID id);

    long count();
}
