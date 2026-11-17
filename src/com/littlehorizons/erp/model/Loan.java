package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.BorrowerType;
import com.littlehorizons.erp.model.enums.LoanStatus;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Loan of a resource to a student or a teacher.
 */
public class Loan {

    private Integer id;
    private BorrowerType borrowerType;
    private int resourceId;
    private String resourceCode;
    private String resourceName;
    private int borrowerId;
    private String borrowerName;
    private LocalDate loanDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private LoanStatus status;
    private String notes;

    public Loan() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public BorrowerType getBorrowerType() {
        return borrowerType;
    }

    public void setBorrowerType(BorrowerType borrowerType) {
        this.borrowerType = borrowerType;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceCode() {
        return resourceCode;
    }

    public void setResourceCode(String resourceCode) {
        this.resourceCode = resourceCode;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public int getBorrowerId() {
        return borrowerId;
    }

    public void setBorrowerId(int borrowerId) {
        this.borrowerId = borrowerId;
    }

    public String getBorrowerName() {
        return borrowerName;
    }

    public void setBorrowerName(String borrowerName) {
        this.borrowerName = borrowerName;
    }

    public LocalDate getLoanDate() {
        return loanDate;
    }

    public void setLoanDate(LocalDate loanDate) {
        this.loanDate = loanDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    /** A loan is overdue when it has not been returned and its due date already passed. */
    public boolean isOverdue(LocalDate today) {
        return returnDate == null && dueDate != null && dueDate.isBefore(today);
    }

    public long getDaysOverdue(LocalDate today) {
        return isOverdue(today) ? ChronoUnit.DAYS.between(dueDate, today) : 0;
    }
}
