package com.littlehorizons.erp.model.enums;

/**
 * Who borrows a resource. Each borrower type has its own loan table in the database
 * (prestamos_estudiante / prestamos_profesor), which preserves full referential integrity.
 * Table and column names are constants (never user input), so it is safe to use them in SQL.
 */
public enum BorrowerType {
    STUDENT("Estudiante", "prestamos_estudiante", "id_estudiante", "estudiantes"),
    TEACHER("Profesor", "prestamos_profesor", "id_profesor", "profesores");

    private final String label;
    private final String loanTable;
    private final String borrowerColumn;
    private final String borrowerTable;

    BorrowerType(String label, String loanTable, String borrowerColumn, String borrowerTable) {
        this.label = label;
        this.loanTable = loanTable;
        this.borrowerColumn = borrowerColumn;
        this.borrowerTable = borrowerTable;
    }

    public String getLabel() {
        return label;
    }

    public String getLoanTable() {
        return loanTable;
    }

    public String getBorrowerColumn() {
        return borrowerColumn;
    }

    public String getBorrowerTable() {
        return borrowerTable;
    }
}
