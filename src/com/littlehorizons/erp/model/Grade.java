package com.littlehorizons.erp.model;

/**
 * School grade (Preescolar = level 0, Primero = 1 ... Once = 11).
 */
public class Grade {

    private Integer id;
    private String name;
    private int level;

    public Grade() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public String toString() {
        return name;
    }
}
