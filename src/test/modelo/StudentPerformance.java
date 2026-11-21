package test.modelo;

import java.util.Map;

/**
 * Academic performance of one student in the report.
 *
 * @param student         student data
 * @param subjectAverages average of each subject (subject name -> average)
 * @param average         general average (mean of the subject averages)
 * @param level           performance level: Superior, Alto, Básico or Bajo
 */
public record StudentPerformance(StudentSearchResult student, Map<String, Double> subjectAverages,
                                 double average, String level) {

    public boolean needsSupport() {
        return average < 3.0;
    }

    public boolean scholarshipCandidate() {
        return average > 4.5;
    }
}
