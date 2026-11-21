package test.modelo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Complete data of the academic performance report of a grade.
 *
 * @param grade          grade of the report
 * @param academicYear   academic year (2025)
 * @param generatedAt    date and time of generation
 * @param ranking        students ordered from highest to lowest average
 * @param subjects       statistics per subject
 * @param courseAverage  general average of the course
 * @param levelCounts    number of students per performance level
 */
public record PerformanceReport(GradeOption grade, int academicYear, LocalDateTime generatedAt,
                                List<StudentPerformance> ranking, List<SubjectSummary> subjects,
                                double courseAverage, Map<String, Long> levelCounts) {

    public boolean isEmpty() {
        return ranking.isEmpty();
    }
}
