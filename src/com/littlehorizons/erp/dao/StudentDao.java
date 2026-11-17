package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.enums.DbEnum;
import com.littlehorizons.erp.model.enums.DocumentType;
import com.littlehorizons.erp.model.enums.Gender;
import com.littlehorizons.erp.model.enums.StudentStatus;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Data access for the {@code estudiantes} table. */
public class StudentDao extends BaseDao implements CrudDao<Student>, DeletableDao {

    private static final String SELECT = """
            SELECT e.*,
                   (SELECT g.nombre_grado FROM matriculas m JOIN grados g ON g.id_grado = m.id_grado
                     WHERE m.id_estudiante = e.id_estudiante
                     ORDER BY m.año_academico DESC LIMIT 1) AS grado_actual
              FROM estudiantes e
            """;
    private static final String ORDER = " ORDER BY e.primer_apellido, e.segundo_apellido, e.primer_nombre";

    public StudentDao(DatabaseConnection database) {
        super(database);
    }

    @Override
    public Optional<Student> findById(int id) {
        return queryOne(SELECT + " WHERE e.id_estudiante = ?", ps -> ps.setInt(1, id), this::map);
    }

    @Override
    public List<Student> findAll() {
        return queryList(SELECT + ORDER, StatementBinder.NONE, this::map);
    }

    public List<Student> findByStatus(StudentStatus status) {
        return queryList(SELECT + " WHERE e.estado = ?" + ORDER, ps -> ps.setString(1, status.getDbValue()), this::map);
    }

    public Optional<Student> findByDocument(String documentNumber) {
        return queryOne(SELECT + " WHERE e.numero_documento = ?", ps -> ps.setString(1, documentNumber), this::map);
    }

    /** Case-insensitive search by any part of the name or the document number. */
    public List<Student> search(String term) {
        String like = "%" + term.trim() + "%";
        return queryList(SELECT + " WHERE CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido) LIKE ?"
                + " OR e.numero_documento LIKE ?" + ORDER, ps -> {
            ps.setString(1, like);
            ps.setString(2, like);
        }, this::map);
    }

    /** Students with an active enrollment in the given grade and year. */
    public List<Student> findActiveByGrade(int gradeId, int year) {
        return queryList(SELECT + """
                 WHERE e.estado = 'Activo' AND EXISTS (
                       SELECT 1 FROM matriculas m WHERE m.id_estudiante = e.id_estudiante
                          AND m.id_grado = ? AND m.año_academico = ? AND m.estado_matricula = 'Activa')
                """ + ORDER, ps -> {
            ps.setInt(1, gradeId);
            ps.setInt(2, year);
        }, this::map);
    }

    public List<Student> findByGuardian(int guardianId) {
        return queryList(SELECT + " JOIN estudiante_acudiente ea ON ea.id_estudiante = e.id_estudiante WHERE ea.id_acudiente = ?" + ORDER,
                ps -> ps.setInt(1, guardianId), this::map);
    }

    @Override
    public int save(Student s) {
        return executeInsert("""
                INSERT INTO estudiantes (tipo_documento, numero_documento, primer_nombre, segundo_nombre, primer_apellido,
                       segundo_apellido, fecha_nacimiento, genero, direccion, telefono, correo_electronico, tipo_sangre,
                       alergias, condiciones_medicas, fecha_ingreso, estado)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, ps -> {
            bindCommon(ps, s);
            ps.setString(16, s.getStatus().getDbValue());
        });
    }

    @Override
    public void update(Student s) {
        executeUpdate("""
                UPDATE estudiantes SET tipo_documento = ?, numero_documento = ?, primer_nombre = ?, segundo_nombre = ?,
                       primer_apellido = ?, segundo_apellido = ?, fecha_nacimiento = ?, genero = ?, direccion = ?, telefono = ?,
                       correo_electronico = ?, tipo_sangre = ?, alergias = ?, condiciones_medicas = ?, fecha_ingreso = ?, estado = ?
                 WHERE id_estudiante = ?
                """, ps -> {
            bindCommon(ps, s);
            ps.setString(16, s.getStatus().getDbValue());
            ps.setInt(17, s.getId());
        });
    }

    public void updateStatus(int studentId, StudentStatus status) {
        executeUpdate("UPDATE estudiantes SET estado = ? WHERE id_estudiante = ?", ps -> {
            ps.setString(1, status.getDbValue());
            ps.setInt(2, studentId);
        });
    }

    /** True when the student already has grades, payments or attendance (cannot be physically deleted). */
    public boolean hasHistory(int studentId) {
        return queryCount("""
                SELECT (SELECT COUNT(*) FROM registros_academicos WHERE id_estudiante = ?)
                     + (SELECT COUNT(*) FROM pagos WHERE id_estudiante = ?)
                     + (SELECT COUNT(*) FROM asistencias WHERE id_estudiante = ?)
                """, ps -> {
            ps.setInt(1, studentId);
            ps.setInt(2, studentId);
            ps.setInt(3, studentId);
        }) > 0;
    }

    @Override
    public boolean deleteById(int id) {
        return executeUpdate("DELETE FROM estudiantes WHERE id_estudiante = ?", ps -> ps.setInt(1, id)) > 0;
    }

    private void bindCommon(PreparedStatement ps, Student s) throws SQLException {
        ps.setString(1, s.getDocumentType().getDbValue());
        ps.setString(2, s.getDocumentNumber());
        ps.setString(3, s.getFirstName());
        setNullableString(ps, 4, s.getMiddleName());
        ps.setString(5, s.getLastName());
        setNullableString(ps, 6, s.getSecondLastName());
        setDate(ps, 7, s.getBirthDate());
        ps.setString(8, s.getGender().getDbValue());
        setNullableString(ps, 9, s.getAddress());
        setNullableString(ps, 10, s.getPhone());
        setNullableString(ps, 11, s.getEmail());
        setNullableString(ps, 12, s.getBloodType());
        setNullableString(ps, 13, s.getAllergies());
        setNullableString(ps, 14, s.getMedicalConditions());
        setDate(ps, 15, s.getAdmissionDate());
    }

    private Student map(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setId(rs.getInt("id_estudiante"));
        s.setDocumentType(DbEnum.fromDb(DocumentType.class, rs.getString("tipo_documento")));
        s.setDocumentNumber(rs.getString("numero_documento"));
        s.setFirstName(rs.getString("primer_nombre"));
        s.setMiddleName(rs.getString("segundo_nombre"));
        s.setLastName(rs.getString("primer_apellido"));
        s.setSecondLastName(rs.getString("segundo_apellido"));
        s.setBirthDate(getLocalDate(rs, "fecha_nacimiento"));
        s.setGender(DbEnum.fromDb(Gender.class, rs.getString("genero")));
        s.setAddress(rs.getString("direccion"));
        s.setPhone(rs.getString("telefono"));
        s.setEmail(rs.getString("correo_electronico"));
        s.setBloodType(rs.getString("tipo_sangre"));
        s.setAllergies(rs.getString("alergias"));
        s.setMedicalConditions(rs.getString("condiciones_medicas"));
        s.setAdmissionDate(getLocalDate(rs, "fecha_ingreso"));
        s.setStatus(DbEnum.fromDb(StudentStatus.class, rs.getString("estado")));
        s.setCurrentGrade(rs.getString("grado_actual"));
        return s;
    }
}
