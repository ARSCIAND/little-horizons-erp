package com.littlehorizons.erp.model;

/**
 * Category of lendable resources.
 */
public class ResourceCategory {

    private Integer id;
    private String name;
    private String description;

    public ResourceCategory() {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return name;
    }
}
