package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.dao.EventDao;
import com.littlehorizons.erp.dao.GradeDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.AttendanceRecord;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.enums.AcademicPeriod;
import com.littlehorizons.erp.model.enums.AttendanceStatus;
import com.littlehorizons.erp.model.enums.CompetitionPosition;
import com.littlehorizons.erp.model.enums.StudentStatus;
import com.littlehorizons.erp.model.report.AbsenceAlert;
import com.littlehorizons.erp.model.report.ParticipationSummary;
import com.littlehorizons.erp.model.report.StudentAverage;
import com.littlehorizons.erp.model.report.SubjectAverage;
import com.littlehorizons.erp.model.report.SupportCase;
import com.littlehorizons.erp.util.Formats;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Statistics and cross-module reports. All aggregations are performed in memory with the
 * Stream API (grouping, averaging, filtering and sorting with lambdas).
 */
public class ReportController {

    public static final double PASSING_GRADE = 3.0;
    public static final long WEEKLY_ABSENCE_THRESHOLD = 3;

    private static final Set<String> PODIUM = Set.of(CompetitionPosition.FIRST_PLACE.getDbValue(),
            CompetitionPosition.SECOND_PLACE.getDbValue(), CompetitionPosition.THIRD_PLACE.getDbValue());

    private final AcademicRecordDao recordDao;
    private final AttendanceDao attendanceDao;
    private final ClubDao clubDao;
    private final EventDao eventDao;
    private final StudentDao studentDao;
    private final GradeDao gradeDao;

    public ReportController(AcademicRecordDao recordDao, AttendanceDao attendanceDao, ClubDao clubDao, EventDao eventDao,
                            StudentDao studentDao, GradeDao gradeDao) {
        this.recordDao = recordDao;
        this.attendanceDao = attendanceDao;
        this.clubDao = clubDao;
        this.eventDao = eventDao;
        this.studentDao = studentDao;
        this.gradeDao = gradeDao;
    }

    /** Course average per subject for a grade and period (null period = regular periods of the year). */
    public List<SubjectAverage> courseAverages(int gradeId, int year, AcademicPeriod period) {
        return recordDao.findByGrade(gradeId, year).stream()
                .filter(periodFilter(period))
                .collect(Collectors.groupingBy(r -> r.getSubjectCode() + "|" + r.getSubjectName()))
                .entrySet().stream()
                .map(entry -> {
                    String[] key = entry.getKey().split("\\|", 2);
                    DoubleSummaryStatistics stats = entry.getValue().stream()
                            .collect(Collectors.summarizingDouble(AcademicRecord::getGradeValue));
                    long students = entry.getValue().stream().map(AcademicRecord::getStudentId).distinct().count();
                    return new SubjectAverage(key[0], key[1], AcademicController.round(stats.getAverage()),
                            stats.getMin(), stats.getMax(), students);
                })
                .sorted(Comparator.comparingDouble(SubjectAverage::average).reversed())
                .toList();
    }

    /** Students of a grade ranked by their general average. */
    public List<StudentAverage> ranking(int gradeId, int year, AcademicPeriod period) {
        Map<Integer, Student> students = indexStudents();
        return recordDao.findByGrade(gradeId, year).stream()
                .filter(periodFilter(period))
                .collect(Collectors.groupingBy(AcademicRecord::getStudentId))
                .entrySet().stream()
                .map(entry -> new StudentAverage(entry.getKey(), name(students, entry.getKey()), grade(students, entry.getKey()),
                        AcademicController.round(entry.getValue().stream().mapToDouble(AcademicRecord::getGradeValue).average().orElse(0)),
                        entry.getValue().stream().map(AcademicRecord::getSubjectId).distinct().count()))
                .sorted(Comparator.comparingDouble(StudentAverage::average).reversed().thenComparing(StudentAverage::studentName))
                .toList();
    }

    /**
     * Students who need additional support in the year: any subject average below 3.0
     * or a general average below 3.0 (requirement 1.5 "identificar quiénes necesitan apoyo").
     */
    public List<SupportCase> studentsNeedingSupport(int year, AcademicPeriod period) {
        Map<Integer, Student> students = indexStudents();
        Map<Integer, List<AcademicRecord>> byStudent = recordDao.findAll().stream()
                .filter(r -> r.getAcademicYear() == year)
                .filter(periodFilter(period))
                .filter(r -> students.containsKey(r.getStudentId()) && students.get(r.getStudentId()).getStatus() == StudentStatus.ACTIVE)
                .collect(Collectors.groupingBy(AcademicRecord::getStudentId));

        return byStudent.entrySet().stream()
                .map(entry -> {
                    Map<String, Double> subjectAverages = entry.getValue().stream()
                            .collect(Collectors.groupingBy(AcademicRecord::getSubjectName, Collectors.averagingDouble(AcademicRecord::getGradeValue)));
                    List<String> failing = subjectAverages.entrySet().stream()
                            .filter(e -> Formats.roundToTenth(e.getValue()) < PASSING_GRADE)
                            .sorted(Map.Entry.comparingByValue())
                            .map(e -> e.getKey() + " (" + Formats.grade(e.getValue()) + ")")
                            .toList();
                    double average = subjectAverages.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
                    return new SupportCase(entry.getKey(), name(students, entry.getKey()), grade(students, entry.getKey()),
                            AcademicController.round(average), failing);
                })
                .filter(c -> !c.failingSubjects().isEmpty() || Formats.roundToTenth(c.average()) < PASSING_GRADE)
                .sorted(Comparator.comparingInt((SupportCase c) -> c.failingSubjects().size()).reversed()
                        .thenComparingDouble(SupportCase::average))
                .toList();
    }

    /** Students with more than {@value #WEEKLY_ABSENCE_THRESHOLD} absences within the same week (Monday based). */
    public List<AbsenceAlert> absencePatterns(LocalDate from, LocalDate to) {
        Map<Integer, Student> students = indexStudents();
        Function<AttendanceRecord, LocalDate> weekStart = r -> r.getDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return attendanceDao.findByStatusBetween(AttendanceStatus.ABSENT, from, to).stream()
                .collect(Collectors.groupingBy(r -> Map.entry(r.getStudentId(), weekStart.apply(r)), Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > WEEKLY_ABSENCE_THRESHOLD)
                .map(entry -> new AbsenceAlert(entry.getKey().getKey(), name(students, entry.getKey().getKey()),
                        grade(students, entry.getKey().getKey()), entry.getKey().getValue(), entry.getValue()))
                .sorted(Comparator.comparingLong(AbsenceAlert::absences).reversed().thenComparing(AbsenceAlert::weekStart))
                .toList();
    }

    /** Extracurricular participation of active students: active clubs, events and podium finishes. */
    public List<ParticipationSummary> participation() {
        Map<Integer, Long> clubs = clubDao.findAllActiveMemberships().stream()
                .collect(Collectors.groupingBy(ClubMembership::getStudentId, Collectors.counting()));
        List<EventParticipant> participations = eventDao.findAllParticipations();
        Map<Integer, Long> events = participations.stream()
                .collect(Collectors.groupingBy(EventParticipant::getStudentId, Collectors.counting()));
        Map<Integer, Long> podiums = participations.stream()
                .filter(p -> p.getPosition() != null && PODIUM.contains(p.getPosition()))
                .collect(Collectors.groupingBy(EventParticipant::getStudentId, Collectors.counting()));
        return studentDao.findByStatus(StudentStatus.ACTIVE).stream()
                .map(s -> new ParticipationSummary(s.getId(), s.getFullName(), s.getCurrentGrade(),
                        clubs.getOrDefault(s.getId(), 0L), events.getOrDefault(s.getId(), 0L), podiums.getOrDefault(s.getId(), 0L)))
                .sorted(Comparator.comparingLong(ParticipationSummary::score).reversed().thenComparing(ParticipationSummary::studentName))
                .toList();
    }

    /** Headcount of students per status and per current grade. */
    public Map<String, Long> studentsByStatus() {
        return studentDao.findAll().stream()
                .collect(Collectors.groupingBy(s -> s.getStatus().getDbValue(), LinkedHashMap::new, Collectors.counting()));
    }

    /** Active students per current grade, ordered from Preescolar to Once. */
    public Map<String, Long> activeStudentsByGrade() {
        Map<String, Long> counts = studentDao.findByStatus(StudentStatus.ACTIVE).stream()
                .filter(s -> s.getCurrentGrade() != null)
                .collect(Collectors.groupingBy(Student::getCurrentGrade, Collectors.counting()));
        Map<String, Long> ordered = new LinkedHashMap<>();
        gradeDao.findAll().forEach(grade -> ordered.put(grade.getName(), counts.getOrDefault(grade.getName(), 0L)));
        return ordered;
    }

    // ------------------------------------------------------------------ helpers

    private static Predicate<AcademicRecord> periodFilter(AcademicPeriod period) {
        return period == null ? r -> r.getPeriod().isRegular() : r -> r.getPeriod() == period;
    }

    private Map<Integer, Student> indexStudents() {
        return studentDao.findAll().stream().collect(Collectors.toMap(Student::getId, Function.identity()));
    }

    private static String name(Map<Integer, Student> students, int id) {
        Student s = students.get(id);
        return s == null ? "ID " + id : s.getFullName();
    }

    private static String grade(Map<Integer, Student> students, int id) {
        Student s = students.get(id);
        return s == null ? "-" : s.getCurrentGrade();
    }
}
