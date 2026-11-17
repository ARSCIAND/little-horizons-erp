package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.enums.BorrowerType;
import com.littlehorizons.erp.model.enums.DbEnum;
import com.littlehorizons.erp.model.enums.LoanStatus;
import com.littlehorizons.erp.model.enums.ResourceStatus;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Data access for loans. Student and teacher loans live in separate tables
 * ({@code prestamos_estudiante} / {@code prestamos_profesor}); this DAO exposes both through
 * the same {@link Loan} model using {@link BorrowerType} to pick the table.
 */
public class LoanDao extends BaseDao {

    public LoanDao(DatabaseConnection database) {
        super(database);
    }

    private static String select(BorrowerType type) {
        return """
                SELECT l.*, r.codigo_recurso, r.nombre_recurso, %s AS prestatario
                  FROM %s l
                  JOIN recursos r ON r.id_recurso = l.id_recurso
                  JOIN %s b ON b.%s = l.%s
                """.formatted(fullName("b"), type.getLoanTable(), type.getBorrowerTable(),
                type.getBorrowerColumn(), type.getBorrowerColumn());
    }

    public Optional<Loan> findById(BorrowerType type, int loanId) {
        return queryOne(select(type) + " WHERE l.id_prestamo = ?", ps -> ps.setInt(1, loanId), rs -> map(rs, type));
    }

    public List<Loan> findAll(BorrowerType type) {
        return queryList(select(type) + " ORDER BY l.fecha_prestamo DESC", StatementBinder.NONE, rs -> map(rs, type));
    }

    /** Loans not yet returned (Activo or Vencido) of both borrower types. */
    public List<Loan> findOpenLoans() {
        return Stream.of(BorrowerType.values())
                .flatMap(type -> queryList(select(type) + " WHERE l.estado_prestamo IN ('Activo', 'Vencido') ORDER BY l.fecha_devolucion_esperada",
                        StatementBinder.NONE, rs -> map(rs, type)).stream())
                .toList();
    }

    public List<Loan> findByBorrower(BorrowerType type, int borrowerId) {
        return queryList(select(type) + " WHERE l." + type.getBorrowerColumn() + " = ? ORDER BY l.fecha_prestamo DESC",
                ps -> ps.setInt(1, borrowerId), rs -> map(rs, type));
    }

    public long countOpenByBorrower(BorrowerType type, int borrowerId) {
        return queryCount("SELECT COUNT(*) FROM " + type.getLoanTable() + " WHERE " + type.getBorrowerColumn()
                + " = ? AND estado_prestamo IN ('Activo', 'Vencido')", ps -> ps.setInt(1, borrowerId));
    }

    /**
     * Registers a loan atomically: locks the resource, checks that it is available,
     * inserts the loan and marks the resource as "Prestado".
     */
    public int createLoan(Loan loan) {
        BorrowerType type = loan.getBorrowerType();
        return inTransaction("create loan", connection -> {
            String status = queryList(connection, "SELECT estado_recurso FROM recursos WHERE id_recurso = ? FOR UPDATE",
                    ps -> ps.setInt(1, loan.getResourceId()), rs -> rs.getString(1)).stream().findFirst()
                    .orElseThrow(() -> new BusinessException("El recurso no existe."));
            if (!ResourceStatus.AVAILABLE.getDbValue().equals(status)) {
                throw new BusinessException("El recurso no está disponible (estado actual: " + status + ").");
            }
            int id = executeInsert(connection, "INSERT INTO " + type.getLoanTable() + " (id_recurso, " + type.getBorrowerColumn()
                    + ", fecha_prestamo, fecha_devolucion_esperada, estado_prestamo, observaciones) VALUES (?, ?, ?, ?, 'Activo', ?)", ps -> {
                ps.setInt(1, loan.getResourceId());
                ps.setInt(2, loan.getBorrowerId());
                setDate(ps, 3, loan.getLoanDate());
                setDate(ps, 4, loan.getDueDate());
                setNullableString(ps, 5, loan.getNotes());
            });
            executeUpdate(connection, "UPDATE recursos SET estado_recurso = 'Prestado' WHERE id_recurso = ?", ps -> ps.setInt(1, loan.getResourceId()));
            return id;
        });
    }

    /** Registers the return: closes the loan with the real date and makes the resource available again. */
    public void registerReturn(BorrowerType type, int loanId, LocalDate returnDate, String notes) {
        inTransaction("register return", connection -> {
            Loan loan = queryList(connection, select(type) + " WHERE l.id_prestamo = ? FOR UPDATE", ps -> ps.setInt(1, loanId),
                    rs -> map(rs, type)).stream().findFirst()
                    .orElseThrow(() -> new BusinessException("El préstamo no existe."));
            if (loan.getStatus() == LoanStatus.RETURNED) {
                throw new BusinessException("Este préstamo ya fue devuelto el " + loan.getReturnDate() + ".");
            }
            executeUpdate(connection, "UPDATE " + type.getLoanTable()
                    + " SET estado_prestamo = 'Devuelto', fecha_devolucion_real = ?, observaciones = COALESCE(?, observaciones) WHERE id_prestamo = ?",
                    ps -> {
                        setDate(ps, 1, returnDate);
                        setNullableString(ps, 2, notes);
                        ps.setInt(3, loanId);
                    });
            executeUpdate(connection, "UPDATE recursos SET estado_recurso = 'Disponible' WHERE id_recurso = ?", ps -> ps.setInt(1, loan.getResourceId()));
            return loanId;
        });
    }

    /** Marks as "Vencido" every active loan whose due date already passed. Returns how many changed. */
    public int markOverdue(LocalDate today) {
        return Stream.of(BorrowerType.values())
                .mapToInt(type -> executeUpdate("UPDATE " + type.getLoanTable()
                        + " SET estado_prestamo = 'Vencido' WHERE estado_prestamo = 'Activo' AND fecha_devolucion_esperada < ?",
                        ps -> setDate(ps, 1, today)))
                .sum();
    }

    private Loan map(ResultSet rs, BorrowerType type) throws SQLException {
        Loan loan = new Loan();
        loan.setId(rs.getInt("id_prestamo"));
        loan.setBorrowerType(type);
        loan.setResourceId(rs.getInt("id_recurso"));
        loan.setResourceCode(rs.getString("codigo_recurso"));
        loan.setResourceName(rs.getString("nombre_recurso"));
        loan.setBorrowerId(rs.getInt(type.getBorrowerColumn()));
        loan.setBorrowerName(rs.getString("prestatario"));
        loan.setLoanDate(getLocalDate(rs, "fecha_prestamo"));
        loan.setDueDate(getLocalDate(rs, "fecha_devolucion_esperada"));
        loan.setReturnDate(getLocalDate(rs, "fecha_devolucion_real"));
        loan.setStatus(DbEnum.fromDb(LoanStatus.class, rs.getString("estado_prestamo")));
        loan.setNotes(rs.getString("observaciones"));
        return loan;
    }
}
