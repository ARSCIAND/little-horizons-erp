package test.modelo;

/**
 * Course statistics of one subject in the report.
 *
 * @param subject   subject name
 * @param average   average of the whole course in the subject
 * @param best      highest student average in the subject
 * @param worst     lowest student average in the subject
 * @param bestName  name of the student with the highest average
 */
public record SubjectSummary(String subject, double average, double best, double worst, String bestName) {
}
