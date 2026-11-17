package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.enums.DbEnum;
import com.littlehorizons.erp.model.enums.ResourceStatus;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Data access for the inventory ({@code recursos}) and its categories. */
public class ResourceDao extends BaseDao implements CrudDao<Resource> {

    private static final String SELECT = """
            SELECT r.*, c.nombre_categoria FROM recursos r JOIN categorias_recurso c ON c.id_categoria = r.id_categoria
            """;
    private static final String ORDER = " ORDER BY r.codigo_recurso";

    public ResourceDao(DatabaseConnection database) {
        super(database);
    }

    @Override
    public Optional<Resource> findById(int id) {
        return queryOne(SELECT + " WHERE r.id_recurso = ?", ps -> ps.setInt(1, id), this::map);
    }

    public Optional<Resource> findByCode(String code) {
        return queryOne(SELECT + " WHERE r.codigo_recurso = ?", ps -> ps.setString(1, code), this::map);
    }

    @Override
    public List<Resource> findAll() {
        return queryList(SELECT + ORDER, StatementBinder.NONE, this::map);
    }

    public List<Resource> findByCategory(int categoryId) {
        return queryList(SELECT + " WHERE r.id_categoria = ?" + ORDER, ps -> ps.setInt(1, categoryId), this::map);
    }

    public List<Resource> findByStatus(ResourceStatus status) {
        return queryList(SELECT + " WHERE r.estado_recurso = ?" + ORDER, ps -> ps.setString(1, status.getDbValue()), this::map);
    }

    public List<ResourceCategory> findCategories() {
        return queryList("SELECT * FROM categorias_recurso ORDER BY id_categoria", StatementBinder.NONE, rs -> {
            ResourceCategory c = new ResourceCategory();
            c.setId(rs.getInt("id_categoria"));
            c.setName(rs.getString("nombre_categoria"));
            c.setDescription(rs.getString("descripcion"));
            return c;
        });
    }

    /** Next free inventory code for a prefix, e.g. LIB-016. */
    public String nextCode(String prefix) {
        long next = queryCount("""
                SELECT COALESCE(MAX(CAST(SUBSTRING_INDEX(codigo_recurso, '-', -1) AS UNSIGNED)), 0) + 1
                  FROM recursos WHERE codigo_recurso LIKE ?
                """, ps -> ps.setString(1, prefix + "-%"));
        return String.format("%s-%03d", prefix, next);
    }

    @Override
    public int save(Resource r) {
        return executeInsert("""
                INSERT INTO recursos (codigo_recurso, nombre_recurso, id_categoria, descripcion, fecha_adquisicion,
                       ubicacion_fisica, estado_recurso) VALUES (?, ?, ?, ?, ?, ?, ?)
                """, ps -> bind(ps, r));
    }

    @Override
    public void update(Resource r) {
        executeUpdate("""
                UPDATE recursos SET codigo_recurso = ?, nombre_recurso = ?, id_categoria = ?, descripcion = ?,
                       fecha_adquisicion = ?, ubicacion_fisica = ?, estado_recurso = ? WHERE id_recurso = ?
                """, ps -> {
            bind(ps, r);
            ps.setInt(8, r.getId());
        });
    }

    public void updateStatus(int resourceId, ResourceStatus status) {
        executeUpdate("UPDATE recursos SET estado_recurso = ? WHERE id_recurso = ?", ps -> {
            ps.setString(1, status.getDbValue());
            ps.setInt(2, resourceId);
        });
    }

    private void bind(PreparedStatement ps, Resource r) throws SQLException {
        ps.setString(1, r.getCode());
        ps.setString(2, r.getName());
        ps.setInt(3, r.getCategoryId());
        setNullableString(ps, 4, r.getDescription());
        setNullableDate(ps, 5, r.getAcquisitionDate());
        setNullableString(ps, 6, r.getLocation());
        ps.setString(7, r.getStatus().getDbValue());
    }

    private Resource map(ResultSet rs) throws SQLException {
        Resource r = new Resource();
        r.setId(rs.getInt("id_recurso"));
        r.setCode(rs.getString("codigo_recurso"));
        r.setName(rs.getString("nombre_recurso"));
        r.setCategoryId(rs.getInt("id_categoria"));
        r.setCategoryName(rs.getString("nombre_categoria"));
        r.setDescription(rs.getString("descripcion"));
        r.setAcquisitionDate(getLocalDate(rs, "fecha_adquisicion"));
        r.setLocation(rs.getString("ubicacion_fisica"));
        r.setStatus(DbEnum.fromDb(ResourceStatus.class, rs.getString("estado_recurso")));
        return r;
    }
}
