package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Read-only access to the grade catalog ({@code grados}). */
public class GradeDao extends BaseDao {

    public GradeDao(DatabaseConnection database) {
        super(database);
    }

    public List<Grade> findAll() {
        return queryList("SELECT * FROM grados ORDER BY nivel_numerico", StatementBinder.NONE, this::map);
    }

    public Optional<Grade> findById(int id) {
        return queryOne("SELECT * FROM grados WHERE id_grado = ?", ps -> ps.setInt(1, id), this::map);
    }

    public Optional<Grade> findByLevel(int level) {
        return queryOne("SELECT * FROM grados WHERE nivel_numerico = ?", ps -> ps.setInt(1, level), this::map);
    }

    private Grade map(ResultSet rs) throws SQLException {
        Grade grade = new Grade();
        grade.setId(rs.getInt("id_grado"));
        grade.setName(rs.getString("nombre_grado"));
        grade.setLevel(rs.getInt("nivel_numerico"));
        return grade;
    }
}
