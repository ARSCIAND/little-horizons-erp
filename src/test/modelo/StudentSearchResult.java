package test.modelo;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * DTO that combines the data of a student, their enrollment and their grade.
 * It is the unit that the search filters with Stream API.
 */
public record StudentSearchResult(
        int studentId,
        String documentType,
        String documentNumber,
        String firstName,
        String middleName,
        String lastName,
        String secondLastName,
        int gradeId,
        String gradeName,
        int academicYear) {

    /**
     * Full name built only with the parts that exist, so a missing middle name
     * or second last name never leaves double spaces (special case 3).
     */
    public String fullName() {
        return Stream.of(firstName, middleName, lastName, secondLastName)
                .filter(part -> part != null && !part.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
    }

    /** Document as shown in the results, e.g. "TI 1023456789". */
    public String document() {
        return documentType + " " + documentNumber;
    }
}
