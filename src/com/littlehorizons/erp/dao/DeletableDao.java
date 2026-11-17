package com.littlehorizons.erp.dao;

/** Implemented only by DAOs whose records can be physically deleted. */
public interface DeletableDao {

    boolean deleteById(int id);
}
