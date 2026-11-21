package test.modelo;

/**
 * Academic grade shown in the search menu (Preescolar ... Once).
 *
 * @param id    primary key in the grados table
 * @param name  grade name, e.g. "Décimo"
 * @param level numeric level used to order the grades (0 = Preescolar, 11 = Once)
 */
public record GradeOption(int id, String name, int level) {
}
