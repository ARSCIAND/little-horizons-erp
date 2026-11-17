package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.LoanDao;
import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.enums.BorrowerType;
import com.littlehorizons.erp.model.enums.LoanStatus;
import com.littlehorizons.erp.model.enums.ResourceStatus;
import com.littlehorizons.erp.model.enums.StudentStatus;
import com.littlehorizons.erp.model.enums.TeacherStatus;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Business rules for the inventory and the loan system (module 4). */
public class ResourceController {

    public static final int MAX_OPEN_LOANS_PER_STUDENT = 3;
    public static final int MAX_LOAN_DAYS = 60;

    /** Inventory code prefix per category name. */
    private static final Map<String, String> CODE_PREFIX = Map.of(
            "Libro", "LIB",
            "Equipo de Laboratorio", "LAB",
            "Equipo Deportivo", "DEP",
            "Dispositivo Tecnológico", "TEC",
            "Material Didáctico", "DID");

    private final ResourceDao resourceDao;
    private final LoanDao loanDao;
    private final StudentDao studentDao;
    private final TeacherDao teacherDao;

    public ResourceController(ResourceDao resourceDao, LoanDao loanDao, StudentDao studentDao, TeacherDao teacherDao) {
        this.resourceDao = resourceDao;
        this.loanDao = loanDao;
        this.studentDao = studentDao;
        this.teacherDao = teacherDao;
    }

    // ------------------------------------------------------------------ inventory

    public List<ResourceCategory> listCategories() {
        return resourceDao.findCategories();
    }

    public List<Resource> listResources(Integer categoryId, ResourceStatus status) {
        List<Resource> resources = categoryId == null ? resourceDao.findAll() : resourceDao.findByCategory(categoryId);
        return resources.stream().filter(r -> status == null || r.getStatus() == status).toList();
    }

    public Resource getResource(int id) {
        return resourceDao.findById(id).orElseThrow(() -> new BusinessException("No existe el recurso con ID " + id + "."));
    }

    public Resource findByCode(String code) {
        return resourceDao.findByCode(code.toUpperCase()).orElseThrow(() -> new BusinessException("No existe el recurso con código " + code + "."));
    }

    /** Registers a resource generating its unique inventory code from the category (e.g. LIB-016). */
    public Resource registerResource(Resource resource, ResourceCategory category) {
        if (resource.getAcquisitionDate() != null && resource.getAcquisitionDate().isAfter(LocalDate.now())) {
            throw new BusinessException("La fecha de adquisición no puede ser futura.");
        }
        String prefix = CODE_PREFIX.getOrDefault(category.getName(), "REC");
        resource.setCode(resourceDao.nextCode(prefix));
        resource.setCategoryId(category.getId());
        resource.setStatus(ResourceStatus.AVAILABLE);
        resource.setId(resourceDao.save(resource));
        return resource;
    }

    /** Manual status changes (maintenance, decommission, back to available). "Prestado" is only set by loans. */
    public void changeStatus(int resourceId, ResourceStatus newStatus) {
        Resource resource = getResource(resourceId);
        if (newStatus == ResourceStatus.LOANED) {
            throw new BusinessException("El estado 'Prestado' se asigna automáticamente al registrar un préstamo.");
        }
        if (resource.getStatus() == ResourceStatus.LOANED) {
            throw new BusinessException("El recurso está prestado: registre primero su devolución.");
        }
        if (resource.getStatus() == ResourceStatus.DECOMMISSIONED) {
            throw new BusinessException("El recurso fue dado de baja y no puede cambiar de estado.");
        }
        resourceDao.updateStatus(resourceId, newStatus);
    }

    // ------------------------------------------------------------------ loans

    /**
     * Lends a resource. Rules: borrower active, due date between tomorrow and 60 days, borrower without
     * overdue loans and students with at most 3 open loans. Availability is checked atomically in the DAO.
     */
    public int lend(BorrowerType type, int resourceId, int borrowerId, LocalDate dueDate, String notes) {
        LocalDate today = LocalDate.now();
        checkCanBorrow(type, borrowerId);
        requireAvailable(resourceId);
        if (!dueDate.isAfter(today) || ChronoUnit.DAYS.between(today, dueDate) > MAX_LOAN_DAYS) {
            throw new BusinessException("La fecha de devolución debe estar entre mañana y " + MAX_LOAN_DAYS + " días.");
        }
        Loan loan = new Loan();
        loan.setBorrowerType(type);
        loan.setResourceId(resourceId);
        loan.setBorrowerId(borrowerId);
        loan.setLoanDate(today);
        loan.setDueDate(dueDate);
        loan.setNotes(notes);
        return loanDao.createLoan(loan);
    }

    /** Validates (before filling the form) that the resource can be lent right now. */
    public Resource requireAvailable(int resourceId) {
        Resource resource = getResource(resourceId);
        if (resource.getStatus() != ResourceStatus.AVAILABLE) {
            throw new BusinessException(resource.getCode() + " no está disponible (estado actual: " + resource.getStatus() + ").");
        }
        return resource;
    }

    /**
     * Validates the borrower: active, without overdue loans and, for students, below the open-loan limit.
     */
    public void checkCanBorrow(BorrowerType type, int borrowerId) {
        loanDao.markOverdue(LocalDate.now());
        String borrowerName = requireActiveBorrower(type, borrowerId);
        List<Loan> open = loanDao.findByBorrower(type, borrowerId).stream()
                .filter(l -> l.getStatus() != LoanStatus.RETURNED)
                .toList();
        if (open.stream().anyMatch(l -> l.getStatus() == LoanStatus.OVERDUE)) {
            throw new BusinessException(borrowerName + " tiene préstamos vencidos sin devolver. Debe regularizarlos primero.");
        }
        if (type == BorrowerType.STUDENT && open.size() >= MAX_OPEN_LOANS_PER_STUDENT) {
            throw new BusinessException(borrowerName + " ya tiene " + open.size() + " préstamos activos (máximo " + MAX_OPEN_LOANS_PER_STUDENT + ").");
        }
    }

    public void registerReturn(BorrowerType type, int loanId, LocalDate returnDate, String notes) {
        Loan loan = loanDao.findById(type, loanId).orElseThrow(() -> new BusinessException("No existe el préstamo " + loanId + "."));
        if (returnDate.isBefore(loan.getLoanDate()) || returnDate.isAfter(LocalDate.now())) {
            throw new BusinessException("La fecha de devolución debe estar entre " + loan.getLoanDate() + " y hoy.");
        }
        loanDao.registerReturn(type, loanId, returnDate, notes);
    }

    /** Loans not yet returned (statuses refreshed first). */
    public List<Loan> openLoans() {
        loanDao.markOverdue(LocalDate.now());
        return loanDao.findOpenLoans();
    }

    public List<Loan> loanHistory(BorrowerType type, int borrowerId) {
        return loanDao.findByBorrower(type, borrowerId);
    }

    /** Overdue loans report (requirement 4.2), the oldest debts first. */
    public List<Loan> overdueLoans(LocalDate today) {
        loanDao.markOverdue(today);
        return loanDao.findOpenLoans().stream()
                .filter(l -> l.isOverdue(today))
                .sorted(Comparator.comparingLong((Loan l) -> l.getDaysOverdue(today)).reversed())
                .toList();
    }

    private String requireActiveBorrower(BorrowerType type, int borrowerId) {
        if (type == BorrowerType.STUDENT) {
            var student = studentDao.findById(borrowerId).orElseThrow(() -> new BusinessException("El estudiante no existe."));
            if (student.getStatus() != StudentStatus.ACTIVE) {
                throw new BusinessException("Solo los estudiantes activos pueden solicitar préstamos.");
            }
            return student.getShortName();
        }
        var teacher = teacherDao.findById(borrowerId).orElseThrow(() -> new BusinessException("El profesor no existe."));
        if (teacher.getStatus() != TeacherStatus.ACTIVE) {
            throw new BusinessException("Solo los profesores activos pueden solicitar préstamos.");
        }
        return teacher.getShortName();
    }
}
