package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.enums.DbEnum;
import com.littlehorizons.erp.model.enums.DocumentType;
import com.littlehorizons.erp.model.enums.TeacherStatus;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Data access for {@code profesores}. */
public class TeacherDao extends BaseDao implements CrudDao<Teacher> {

    private static final String SELECT = "SELECT p.* FROM profesores p";
    private static final String ORDER = " ORDER BY p.primer_apellido, p.primer_nombre";

    public TeacherDao(DatabaseConnection database) {
        super(database);
    }

    @Override
    public Optional<Teacher> findById(int id) {
        return queryOne(SELECT + " WHERE p.id_profesor = ?", ps -> ps.setInt(1, id), this::map);
    }

    @Override
    public List<Teacher> findAll() {
        return queryList(SELECT + ORDER, StatementBinder.NONE, this::map);
    }

    public List<Teacher> findByStatus(TeacherStatus status) {
        return queryList(SELECT + " WHERE p.estado = ?" + ORDER, ps -> ps.setString(1, status.getDbValue()), this::map);
    }

    public Optional<Teacher> findByDocument(String document) {
        return queryOne(SELECT + " WHERE p.numero_documento = ?", ps -> ps.setString(1, document), this::map);
    }

    public Optional<Teacher> findByEmail(String email) {
        return queryOne(SELECT + " WHERE p.correo_electronico = ?", ps -> ps.setString(1, email), this::map);
    }

    public List<Teacher> search(String term) {
        String like = "%" + term.trim() + "%";
        return queryList(SELECT + " WHERE " + fullName("p") + " LIKE ? OR p.especializacion LIKE ? OR p.numero_documento LIKE ?" + ORDER,
                ps -> {
                    ps.setString(1, like);
                    ps.setString(2, like);
                    ps.setString(3, like);
                }, this::map);
    }

    @Override
    public int save(Teacher t) {
        return executeInsert("""
                INSERT INTO profesores (tipo_documento, numero_documento, primer_nombre, segundo_nombre, primer_apellido,
                       segundo_apellido, correo_electronico, telefono, especializacion, fecha_contratacion, estado)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, ps -> bind(ps, t));
    }

    @Override
    public void update(Teacher t) {
        executeUpdate("""
                UPDATE profesores SET tipo_documento = ?, numero_documento = ?, primer_nombre = ?, segundo_nombre = ?,
                       primer_apellido = ?, segundo_apellido = ?, correo_electronico = ?, telefono = ?, especializacion = ?,
                       fecha_contratacion = ?, estado = ? WHERE id_profesor = ?
                """, ps -> {
            bind(ps, t);
            ps.setInt(12, t.getId());
        });
    }

    private void bind(PreparedStatement ps, Teacher t) throws SQLException {
        ps.setString(1, t.getDocumentType().getDbValue());
        ps.setString(2, t.getDocumentNumber());
        ps.setString(3, t.getFirstName());
        setNullableString(ps, 4, t.getMiddleName());
        ps.setString(5, t.getLastName());
        setNullableString(ps, 6, t.getSecondLastName());
        ps.setString(7, t.getEmail());
        setNullableString(ps, 8, t.getPhone());
        setNullableString(ps, 9, t.getSpecialization());
        setDate(ps, 10, t.getHireDate());
        ps.setString(11, t.getStatus().getDbValue());
    }

    private Teacher map(ResultSet rs) throws SQLException {
        Teacher t = new Teacher();
        t.setId(rs.getInt("id_profesor"));
        t.setDocumentType(DbEnum.fromDb(DocumentType.class, rs.getString("tipo_documento")));
        t.setDocumentNumber(rs.getString("numero_documento"));
        t.setFirstName(rs.getString("primer_nombre"));
        t.setMiddleName(rs.getString("segundo_nombre"));
        t.setLastName(rs.getString("primer_apellido"));
        t.setSecondLastName(rs.getString("segundo_apellido"));
        t.setEmail(rs.getString("correo_electronico"));
        t.setPhone(rs.getString("telefono"));
        t.setSpecialization(rs.getString("especializacion"));
        t.setHireDate(getLocalDate(rs, "fecha_contratacion"));
        t.setStatus(DbEnum.fromDb(TeacherStatus.class, rs.getString("estado")));
        return t;
    }
}
