package com.littlehorizons.erp;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.controller.AcademicController;
import com.littlehorizons.erp.controller.ExtracurricularController;
import com.littlehorizons.erp.controller.FinanceController;
import com.littlehorizons.erp.controller.ReportController;
import com.littlehorizons.erp.controller.ResourceController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.dao.AssignmentDao;
import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.EventDao;
import com.littlehorizons.erp.dao.GradeDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.InvoiceDao;
import com.littlehorizons.erp.dao.LoanDao;
import com.littlehorizons.erp.dao.PaymentConceptDao;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.exception.InputClosedException;
import com.littlehorizons.erp.service.invoice.InvoiceService;
import com.littlehorizons.erp.service.invoice.TextInvoiceGenerator;
import com.littlehorizons.erp.util.DatabaseConnection;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.util.InputReader;
import com.littlehorizons.erp.view.AcademicView;
import com.littlehorizons.erp.view.ExtracurricularView;
import com.littlehorizons.erp.view.FinanceView;
import com.littlehorizons.erp.view.Lookup;
import com.littlehorizons.erp.view.MainMenuView;
import com.littlehorizons.erp.view.ReportView;
import com.littlehorizons.erp.view.ResourceView;
import com.littlehorizons.erp.view.StudentView;
import com.littlehorizons.erp.view.TeacherView;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Entry point and composition root: creates every DAO, service, controller and view once and
 * injects them through constructors (Dependency Injection without frameworks).
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        // Force UTF-8 so accents and box-drawing characters render in IntelliJ and modern terminals.
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        ErrorLogger logger = ErrorLogger.getInstance();
        DatabaseConnection database = DatabaseConnection.getInstance();
        try {
            database.testConnection();
        } catch (DataAccessException e) {
            System.out.println("\n  ✖ " + e.getMessage());
            System.out.println("    URL configurada: " + database.getUrl());
            System.out.println("    Revise " + AppConfig.CONFIG_FILE + " y el README (sección 6). Detalle en " + logger.getLogFile());
            System.exit(1);
            return;
        }

        // ---------------------------------------------------------------- data access layer
        StudentDao studentDao = new StudentDao(database);
        GuardianDao guardianDao = new GuardianDao(database);
        GradeDao gradeDao = new GradeDao(database);
        EnrollmentDao enrollmentDao = new EnrollmentDao(database);
        SubjectDao subjectDao = new SubjectDao(database);
        TeacherDao teacherDao = new TeacherDao(database);
        AssignmentDao assignmentDao = new AssignmentDao(database);
        SubjectEnrollmentDao subjectEnrollmentDao = new SubjectEnrollmentDao(database);
        AcademicRecordDao recordDao = new AcademicRecordDao(database);
        AttendanceDao attendanceDao = new AttendanceDao(database);
        ClubDao clubDao = new ClubDao(database);
        EventDao eventDao = new EventDao(database);
        PaymentConceptDao conceptDao = new PaymentConceptDao(database);
        PaymentDao paymentDao = new PaymentDao(database);
        InvoiceDao invoiceDao = new InvoiceDao(database);
        ResourceDao resourceDao = new ResourceDao(database);
        LoanDao loanDao = new LoanDao(database);

        if (!assignmentDao.schedulesAvailable() || !conceptDao.customPricesAvailable()) {
            System.out.println("\n  ℹ La base de datos no tiene las tablas de extensión (horarios_clase, valores_personalizados).");
            System.out.println("    El sistema funciona con el script oficial, pero los horarios y los valores personalizados");
            System.out.println("    quedan deshabilitados. Ejecute docs/database.sql completo para activarlos.");
        }

        // ---------------------------------------------------------------- services and controllers
        InvoiceService invoiceService = new InvoiceService(paymentDao, studentDao, guardianDao, invoiceDao,
                List.of(new TextInvoiceGenerator()));

        StudentController studentController = new StudentController(studentDao, guardianDao, enrollmentDao, gradeDao, subjectEnrollmentDao);
        AcademicController academicController = new AcademicController(subjectDao, studentDao, teacherDao, enrollmentDao,
                assignmentDao, subjectEnrollmentDao, recordDao, attendanceDao);
        TeacherController teacherController = new TeacherController(teacherDao, assignmentDao, subjectDao);
        ExtracurricularController extracurricularController = new ExtracurricularController(clubDao, eventDao, studentDao, teacherDao);
        FinanceController financeController = new FinanceController(conceptDao, paymentDao, studentDao, guardianDao, invoiceDao, invoiceService);
        ResourceController resourceController = new ResourceController(resourceDao, loanDao, studentDao, teacherDao);
        ReportController reportController = new ReportController(recordDao, attendanceDao, clubDao, eventDao, studentDao, gradeDao);

        // ---------------------------------------------------------------- views
        InputReader input = new InputReader();
        Lookup lookup = new Lookup(input, studentController, teacherController, academicController);
        MainMenuView mainMenu = new MainMenuView(input, List.of(
                new StudentView(input, studentController, lookup),
                new AcademicView(input, academicController, lookup),
                new TeacherView(input, teacherController, lookup),
                new ExtracurricularView(input, extracurricularController, lookup),
                new FinanceView(input, financeController, studentController, lookup),
                new ResourceView(input, resourceController, lookup),
                new ReportView(input, reportController, financeController, resourceController, lookup)));

        try {
            mainMenu.show();
        } catch (InputClosedException e) {
            System.out.println("\n\n  Entrada finalizada. Cerrando el sistema.");
        } catch (RuntimeException e) {
            logger.error("Main", "Fatal error", e);
            System.out.println("\n  ✖ Error inesperado. El detalle quedó en " + logger.getLogFile());
            System.exit(2);
        }
    }
}
