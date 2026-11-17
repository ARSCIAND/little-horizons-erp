package com.littlehorizons.erp.dao;

import java.util.List;
import java.util.Optional;

/**
 * Generic Create / Read / Update contract shared by the main DAOs.
 * Deletion is kept in a separate interface ({@link DeletableDao}) because most school records
 * must never be physically deleted (they change state instead) — Interface Segregation Principle.
 *
 * @param <T> entity type
 */
public interface CrudDao<T> {

    Optional<T> findById(int id);

    List<T> findAll();

    /** Inserts the entity and returns the generated primary key. */
    int save(T entity);

    void update(T entity);
}
