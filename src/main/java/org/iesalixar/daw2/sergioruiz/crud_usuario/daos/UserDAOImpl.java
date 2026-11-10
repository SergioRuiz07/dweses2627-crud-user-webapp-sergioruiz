package org.iesalixar.daw2.sergioruiz.crud_usuario.daos;

import org.iesalixar.daw2.sergioruiz.crud_usuario.entities.Role;
import org.iesalixar.daw2.sergioruiz.crud_usuario.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación JDBC de la interfaz UserDAO, basada en JdbcTemplate.
 *
 * Gestiona el acceso a la tabla users y a la tabla intermedia user_roles, que
 * representa la relación N:M entre usuarios y roles.
 */
@Repository
public class UserDAOImpl implements UserDAO {

    /** Logger para registrar eventos importantes del DAO. */
    private static final Logger logger = LoggerFactory.getLogger(UserDAOImpl.class);

    /** Plantilla JDBC utilizada para ejecutar todas las consultas. */
    private final JdbcTemplate jdbcTemplate;

    /**
     * Constructor con inyección de dependencias.
     *
     * @param jdbcTemplate plantilla JDBC con la que se accede a la base de datos.
     */
    public UserDAOImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Recupera todos los usuarios ordenados por id, cada uno con su lista de roles.
     *
     * Para evitar hacer una consulta de roles por cada usuario, se leen todas las
     * asignaciones de user_roles en una única consulta y después se reparten entre
     * los usuarios. Los usuarios sin roles quedan con una lista vacía.
     *
     * @return lista de usuarios con sus roles cargados; vacía si no hay usuarios.
     */
    @Override
    public List<User> listAllUsers() {
        logger.info("Entrando en listAllUsers()");
        List<User> users = jdbcTemplate.query("SELECT * FROM users ORDER BY id", new BeanPropertyRowMapper<>(User.class));
        Map<Long, List<Role>> rolesByUser = new HashMap<>();
        jdbcTemplate.query(
                "SELECT ur.user_id, r.id, r.name FROM user_roles ur JOIN roles r ON r.id = ur.role_id ORDER BY r.name",
                (RowCallbackHandler) rs -> rolesByUser
                        .computeIfAbsent(rs.getLong("user_id"), k -> new ArrayList<>())
                        .add(new Role(rs.getLong("id"), rs.getString("name"))));
        for (User user : users) {
            user.setRoles(rolesByUser.getOrDefault(user.getId(), new ArrayList<>()));
        }
        logger.info("Saliendo de listAllUsers(): {} usuarios recuperados", users.size());
        return users;
    }

    /**
     * Inserta un nuevo usuario y sus asignaciones de roles.
     *
     * Primero se inserta la fila en users, después se recupera el id asignado
     * buscando por nombre de usuario (que es único) y se guarda en el propio
     * objeto, y por último se insertan en user_roles las filas correspondientes
     * a los roles del usuario.
     *
     * @param user usuario a insertar; tras la llamada tendrá asignado su id.
     */
    @Override
    public void insertUser(User user) {
        logger.info("Entrando en insertUser() con username: {}", user.getUsername());
        String sql = "INSERT INTO users (username, password_hash, active, account_non_locked, last_password_change, " +
                "password_expires_at, failed_login_attempts, email_verified, must_change_password) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int rowsAffected = jdbcTemplate.update(sql,
                user.getUsername(), user.getPasswordHash(), user.getActive(), user.getAccountNonLocked(), user.getLastPasswordChange(),
                user.getPasswordExpiresAt(), user.getFailedLoginAttempts(), user.getEmailVerified(), user.getMustChangePassword());
        logger.info("Se han insertado {} datos nuevos en la tabla", rowsAffected);
        user.setId(jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, user.getUsername()));
        saveRoles(user.getId(), user.getRoles());
    }

    /**
     * Actualiza los datos de un usuario y reemplaza sus roles.
     *
     * Se actualiza la fila de users, se eliminan todas las asignaciones previas
     * del usuario en user_roles y se insertan de nuevo las que trae el objeto.
     *
     * @param user usuario con los datos nuevos; debe tener el id del usuario a modificar.
     */
    @Override
    public void updateUser(User user) {
        logger.info("Entrando en updateUser() con id: {}", user.getId());
        String sql = "UPDATE users SET username = ?, password_hash = ?, active = ?, account_non_locked = ?, "
                + "last_password_change = ?, password_expires_at = ?, failed_login_attempts = ?, "
                + "email_verified = ?, must_change_password = ? WHERE id = ?";
        int rows = jdbcTemplate.update(sql,
                user.getUsername(), user.getPasswordHash(), user.getActive(), user.getAccountNonLocked(), user.getLastPasswordChange(),
                user.getPasswordExpiresAt(), user.getFailedLoginAttempts(), user.getEmailVerified(), user.getMustChangePassword(), user.getId());
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", user.getId());
        saveRoles(user.getId(), user.getRoles());
        logger.info("Usuario actualizado. Filas afectadas: {}", rows);
    }

    /**
     * Elimina un usuario junto con sus asignaciones de roles.
     *
     * Primero se borran las filas de user_roles que referencian al usuario y
     * después la fila de users. Si el id no existe, no se modifica nada.
     *
     * @param id identificador del usuario a eliminar.
     */
    @Override
    public void deleteUser(Long id) {
        logger.info("Entrando en deleteUser() con id: {}", id);
        int removedRoles = jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", id);
        logger.info("Asignaciones de roles eliminadas: {}", removedRoles);
        int rows = jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
        logger.info("Saliendo de deleteUser(). Filas afectadas: {}", rows);
    }

    /**
     * Busca un usuario por su identificador y carga también sus roles, ordenados por nombre.
     *
     * @param id identificador del usuario.
     * @return el usuario con sus roles, o null si no existe ningún usuario con ese id.
     */
    @Override
    public User getUserById(Long id) {
        logger.info("Entrando en getUserById() con id: {}", id);
        try {
            User user = jdbcTemplate.queryForObject("SELECT * FROM users WHERE id = ?", new BeanPropertyRowMapper<>(User.class), id);
            if (user != null) {
                List<Role> roles = jdbcTemplate.query(
                        "SELECT r.id, r.name FROM roles r JOIN user_roles ur ON ur.role_id = r.id "
                                + "WHERE ur.user_id = ? ORDER BY r.name",
                        (rs, rowNum) -> new Role(rs.getLong("id"), rs.getString("name")),
                        id);
                user.setRoles(roles);
            }
            logger.info("Saliendo de getUserById(): usuario encontrado '{}'", user != null ? user.getUsername() : null);
            return user;
        } catch (EmptyResultDataAccessException e) {
            logger.warn("Saliendo de getUserById(): no existe ningún usuario con id {}", id);
            return null;
        }
    }

    /**
     * Inserta en user_roles una fila por cada rol del usuario.
     *
     * Es un método auxiliar de insertUser y updateUser, que lo llaman tras
     * guardar el usuario. Si la lista de roles es null no se hace nada.
     *
     * @param userId identificador del usuario al que se asignan los roles.
     * @param roles roles a asignar; puede ser null o estar vacía.
     */
    private void saveRoles(Long userId, List<Role> roles) {
        if (roles == null) {
            return;
        }
        for (Role role : roles) {
            jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role.getId());
        }
    }

    /**
     * Comprueba si ya existe un usuario con el nombre de usuario indicado.
     *
     * La comparación no distingue entre mayúsculas y minúsculas. Se usa en el
     * alta de usuarios para evitar nombres repetidos.
     *
     * @param username nombre de usuario a comprobar; no debe ser null.
     * @return true si existe algún usuario con ese nombre, false en caso contrario.
     */
    @Override
    public boolean existsUserByUsername(String username) {
        logger.info("Entrando en existsUserByUsername() con username: {}", username);
        String sql = "SELECT COUNT(*) FROM users WHERE LOWER(username) = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username.toLowerCase());
        boolean exists = count != null && count > 0;
        logger.info("Saliendo de existsUserByUsername(): {}", exists);
        return exists;
    }

    /**
     * Comprueba si existe otro usuario, distinto del indicado, con ese nombre de usuario.
     *
     * La comparación no distingue entre mayúsculas y minúsculas. Se usa en la
     * edición de usuarios, para que un usuario pueda conservar su propio nombre
     * sin que se considere repetido.
     *
     * @param username nombre de usuario a comprobar; no debe ser null.
     * @param id identificador del usuario que se está editando y que se excluye de la comprobación.
     * @return true si otro usuario ya tiene ese nombre, false en caso contrario.
     */
    @Override
    public boolean existsUserByUsernameAndNotId(String username, Long id) {
        logger.info("Entrando en existsUserByUsernameAndNotId() con username: {} excluyendo id: {}", username, id);
        String sql = "SELECT COUNT(*) FROM users WHERE LOWER(username) = ? AND id <> ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username.toLowerCase(), id);
        boolean exists = count != null && count > 0;
        logger.info("Saliendo de existsUserByUsernameAndNotId(): {}", exists);
        return exists;
    }
}