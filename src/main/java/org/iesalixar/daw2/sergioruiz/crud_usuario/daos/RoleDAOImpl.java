package org.iesalixar.daw2.sergioruiz.crud_usuario.daos;

import org.iesalixar.daw2.sergioruiz.crud_usuario.entities.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Implementación JDBC de la interfaz RoleDAO basada en JdbcTemplate de Spring.
 * <p>
 * Esta clase encapsula todo el acceso a datos relacionado con la tabla
 * <b>roles</b> (alta, baja, modificación y consulta) y con la tabla de relación
 * <b>user_roles</b> para comprobar cuántos usuarios tienen asignado un rol.
 * </p>
 * <p>
 * Está anotada con @Repository, por lo que Spring la detecta como componente de
 * la capa de persistencia y la registra como bean para poder inyectarla en los
 * servicios o controladores que la necesiten. Todas las consultas utilizan
 * parámetros posicionales (?) para evitar inyección SQL.
 * </p>
 *
 * @author Sergio Ruiz
 * @version 1.0
 */
@Repository
public class RoleDAOImpl implements RoleDAO {

    /** Logger para registrar eventos importantes del DAO (entrada y salida de cada método). */
    private static final Logger logger = LoggerFactory.getLogger(RoleDAOImpl.class);

    /** Plantilla JDBC de Spring utilizada para ejecutar todas las sentencias SQL. */
    private final JdbcTemplate jdbcTemplate;

    /**
     * Constructor con inyección de dependencias de JdbcTemplate.
     * Spring lo invoca automáticamente al crear el bean del repositorio.
     *
     * @param jdbcTemplate plantilla JDBC configurada con el DataSource de la aplicación.
     */
    public RoleDAOImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Recupera todos los roles almacenados en la base de datos, ordenados
     * alfabéticamente por nombre.
     * <p>
     * Utiliza BeanPropertyRowMapper para convertir cada fila del resultado en un
     * objeto Role, haciendo coincidir los nombres de las columnas con las
     * propiedades de la entidad.
     * </p>
     *
     * @return lista de roles ordenada por nombre; vacía si no hay ninguno.
     */
    @Override
    public List<Role> listAllRoles() {
        logger.info("Entrando en listAllRoles()");
        String sql = "SELECT id, name FROM roles ORDER BY name";
        List<Role> roles = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Role.class));
        logger.info("Saliendo de listAllRoles(): {} roles recuperados", roles.size());
        return roles;
    }

    /**
     * Inserta un nuevo rol en la base de datos.
     * <p>
     * Solo se inserta el nombre; el identificador lo genera la propia base de
     * datos (autoincremental).
     * </p>
     *
     * @param role rol a insertar; debe tener el nombre informado.
     */
    @Override
    public void insertRole(Role role) {
        logger.info("Entrando en insertRole() con nombre: {}", role.getName());
        String sql = "INSERT INTO roles (name) VALUES (?)";
        int rows = jdbcTemplate.update(sql, role.getName());
        logger.info("Saliendo de insertRole(). Filas afectadas: {}", rows);
    }

    /**
     * Actualiza el nombre de un rol existente identificado por su id.
     *
     * @param role rol con el id del registro a modificar y el nuevo nombre.
     */
    @Override
    public void updateRole(Role role) {
        logger.info("Entrando en updateRole() con id: {}", role.getId());
        String sql = "UPDATE roles SET name = ? WHERE id = ?";
        int rows = jdbcTemplate.update(sql, role.getName(), role.getId());
        logger.info("Saliendo de updateRole(). Filas afectadas: {}", rows);
    }

    /**
     * Elimina un rol de la base de datos a partir de su identificador.
     * <p>
     * Si el rol está asignado a algún usuario, la restricción de clave foránea
     * puede impedir el borrado; por ello conviene comprobar antes con
     * countUsersByRoleId.
     * </p>
     *
     * @param id identificador del rol a eliminar.
     */
    @Override
    public void deleteRole(Long id) {
        logger.info("Entrando en deleteRole() con id: {}", id);
        String sql = "DELETE FROM roles WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        logger.info("Saliendo de deleteRole(). Filas afectadas: {}", rows);
    }

    /**
     * Busca un rol por su identificador.
     * <p>
     * queryForObject lanza EmptyResultDataAccessException cuando no hay
     * resultados; esa excepción se captura y se traduce en un valor null, de modo
     * que el llamador pueda comprobar fácilmente la inexistencia del rol.
     * </p>
     *
     * @param id identificador del rol buscado.
     * @return el rol encontrado, o null si no existe ningún rol con ese id.
     */
    @Override
    public Role getRoleById(Long id) {
        logger.info("Entrando en getRoleById() con id: {}", id);
        String sql = "SELECT id, name FROM roles WHERE id = ?";
        try {
            Role role = jdbcTemplate.queryForObject(sql,new BeanPropertyRowMapper<>(Role.class), id);
            logger.info("Saliendo de getRoleById(): rol encontrado '{}'", role != null ? role.getName() : null);
            return role;
        } catch (EmptyResultDataAccessException e) {
            logger.warn("Saliendo de getRoleById(): no existe ningún rol con id {}", id);
            return null;
        }
    }

    /**
     * Comprueba si ya existe un rol con el nombre indicado, sin distinguir
     * mayúsculas de minúsculas.
     * <p>
     * Se utiliza al crear un rol nuevo para evitar nombres duplicados. La
     * comparación se hace pasando ambos lados a mayúsculas (UPPER en SQL y
     * toUpperCase en Java).
     * </p>
     *
     * @param name nombre del rol a comprobar.
     * @return true si existe al menos un rol con ese nombre; false en caso contrario.
     */
    @Override
    public boolean existsRoleByName(String name) {
        logger.info("Entrando en existsRoleByName() con nombre: {}", name);
        String sql = "SELECT COUNT(*) FROM roles WHERE UPPER(name) = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, name.toUpperCase());
        boolean exists = count != null && count > 0;
        logger.info("Saliendo de existsRoleByName(): {}", exists);
        return exists;
    }

    /**
     * Comprueba si existe otro rol con el mismo nombre (sin distinguir
     * mayúsculas de minúsculas), excluyendo el rol cuyo id se indica.
     * <p>
     * Se utiliza al editar un rol: permite conservar su propio nombre sin que se
     * detecte como duplicado, pero impide usar el nombre de otro rol distinto.
     * </p>
     *
     * @param name nombre del rol a comprobar.
     * @param id   identificador del rol que se excluye de la comprobación.
     * @return true si otro rol distinto ya usa ese nombre; false en caso contrario.
     */
    @Override
    public boolean existsRoleByNameAndNotId(String name, Long id) {
        logger.info("Entrando en existsRoleByNameAndNotId() con nombre: {} excluyendo id: {}", name, id);
        String sql = "SELECT COUNT(*) FROM roles WHERE UPPER(name) = ? AND id <> ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, name.toUpperCase(), id);
        boolean exists = count != null && count > 0;
        logger.info("Saliendo de existsRoleByNameAndNotId(): {}", exists);
        return exists;
    }

    /**
     * Cuenta cuántos usuarios tienen asignado un determinado rol, consultando la
     * tabla intermedia user_roles.
     * <p>
     * Es útil para impedir el borrado de roles que todavía están en uso.
     * </p>
     *
     * @param roleId identificador del rol.
     * @return número de usuarios con ese rol; 0 si ninguno lo tiene.
     */
    @Override
    public int countUsersByRoleId(Long roleId) {
        logger.info("Entrando en countUsersByRoleId() con roleId: {}", roleId);
        String sql = "SELECT COUNT(*) FROM user_roles WHERE role_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, roleId);
        int result = count != null ? count : 0;
        logger.info("Saliendo de countUsersByRoleId(): {} usuarios", result);
        return result;
    }
}