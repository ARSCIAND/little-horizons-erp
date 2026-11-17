package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.DocumentType;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base class for every person handled by the ERP (students, guardians, teachers).
 * Groups identity and contact data shared by all of them (inheritance + encapsulation).
 */
public abstract class Person {

    private Integer id;
    private DocumentType documentType;
    private String documentNumber;
    private String firstName;
    private String middleName;
    private String lastName;
    private String secondLastName;
    private String email;
    private String phone;
    private String address;

    protected Person() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getSecondLastName() {
        return secondLastName;
    }

    public void setSecondLastName(String secondLastName) {
        this.secondLastName = secondLastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    /** Full name in Colombian order: first name, middle name, first and second last names. */
    public String getFullName() {
        return Stream.of(firstName, middleName, lastName, secondLastName)
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
    }

    /** Short display name: first name and first last name. */
    public String getShortName() {
        return firstName + " " + lastName;
    }

    public String getDocumentLabel() {
        return (documentType == null ? "" : documentType.getDbValue() + " ") + documentNumber;
    }
}
