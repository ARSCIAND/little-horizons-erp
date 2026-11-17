package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Data access for the subject catalog ({@code materias}). */
public class SubjectDao extends BaseDao implements CrudDao<Subject> {

    private static final String SELECT = "SELECT m.*, g.nombre_grado FROM materias m JOIN grados g ON g.id_grado = m.id_grado";
    private static final String ORDER = " ORDER BY g.nivel_numerico, m.nombre_materia";

    public SubjectDao(DatabaseConnection database) {
        super(database);
    }

    @Override
    public Optional<Subject> findById(int id) {
        return queryOne(SELECT + " WHERE m.id_materia = ?", ps -> ps.setInt(1, id), this::map);
    }

    @Override
    public List<Subject> findAll() {
        return queryList(SELECT + ORDER, StatementBinder.NONE, this::map);
    }

    public List<Subject> findByGrade(int gradeId, boolean onlyActive) {
        return queryList(SELECT + " WHERE m.id_grado = ?" + (onlyActive ? " AND m.esta_activa = TRUE" : "") + ORDER,
                ps -> ps.setInt(1, gradeId), this::map);
    }

    public Optional<Subject> findByCode(String code) {
        return queryOne(SELECT + " WHERE m.codigo_materia = ?", ps -> ps.setString(1, code), this::map);
    }

    @Override
    public int save(Subject s) {
        return executeInsert("""
                INSERT INTO materias (codigo_materia, nombre_materia, descripcion, id_grado, horas_semanales, esta_activa)
                VALUES (?, ?, ?, ?, ?, ?)
                """, ps -> bind(ps, s));
    }

    @Override
    public void update(Subject s) {
        executeUpdate("""
                UPDATE materias SET codigo_materia = ?, nombre_materia = ?, descripcion = ?, id_grado = ?,
                       horas_semanales = ?, esta_activa = ? WHERE id_materia = ?
                """, ps -> {
            bind(ps, s);
            ps.setInt(7, s.getId());
        });
    }

    private void bind(PreparedStatement ps, Subject s) throws SQLException {
        ps.setString(1, s.getCode());
        ps.setString(2, s.getName());
        setNullableString(ps, 3, s.getDescription());
        ps.setInt(4, s.getGradeId());
        ps.setInt(5, s.getWeeklyHours());
        ps.setBoolean(6, s.isActive());
    }

    private Subject map(ResultSet rs) throws SQLException {
        Subject s = new Subject();
        s.setId(rs.getInt("id_materia"));
        s.setCode(rs.getString("codigo_materia"));
        s.setName(rs.getString("nombre_materia"));
        s.setDescription(rs.getString("descripcion"));
        s.setGradeId(rs.getInt("id_grado"));
        s.setGradeName(rs.getString("nombre_grado"));
        s.setWeeklyHours(rs.getInt("horas_semanales"));
        s.setActive(rs.getBoolean("esta_activa"));
        return s;
    }
}
