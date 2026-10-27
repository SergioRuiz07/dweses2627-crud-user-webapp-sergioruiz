package org.iesalixar.daw2.sergioruiz.crud_usuario.daos;

import org.iesalixar.daw2.sergioruiz.crud_usuario.entities.User;

import java.sql.SQLException;
import java.util.List;

/**
 * Interfaz DAO para la gestión de la entidad {@link User} (tabla {@code users})
 * y de su relación N:M con los roles (tabla {@code user_roles}).
 */
public interface UserDAO {

    /**
     * Lista todos los usuarios junto con sus roles.
     *
     * @return lista de usuarios.
     * @throws SQLException si ocurre un error de acceso a datos.
     */
    List<User> listAllUsers() throws SQLException;

    /**
     * Inserta un usuario y las asignaciones de sus roles ({@link User#getRoles()}).
     *
     * @param user usuario a insertar.
     * @throws SQLException si ocurre un error de acceso a datos.
     */
    void insertUser(User user) throws SQLException;

    /**
     * Actualiza un usuario y reemplaza las asignaciones de sus roles.
     *
     * @param user usuario con los datos actualizados (el id identifica el registro).
     * @throws SQLException si ocurre un error de acceso a datos.
     */
    void updateUser(User user) throws SQLException;

    /**
     * Elimina un usuario y sus asignaciones de roles.
     *
     * @param id identificador del usuario.
     * @throws SQLException si ocurre un error de acceso a datos.
     */
    void deleteUser(Long id) throws SQLException;

    /**
     * Recupera un usuario con sus roles por su identificador.
     *
     * @param id identificador del usuario.
     * @return el usuario o {@code null} si no existe.
     * @throws SQLException si ocurre un error de acceso a datos.
     */
    User getUserById(Long id) throws SQLException;

    /**
     * Comprueba si existe un usuario con el username indicado (sin distinguir mayúsculas).
     *
     * @param username nombre de usuario.
     * @return {@code true} si ya existe.
     * @throws SQLException si ocurre un error de acceso a datos.
     */
    boolean existsUserByUsername(String username) throws SQLException;

    /**
     * Comprueba si existe un usuario con el username indicado, excluyendo el usuario con el id dado.
     *
     * @param username nombre de usuario.
     * @param id       identificador del usuario a excluir.
     * @return {@code true} si existe otro usuario con ese username.
     * @throws SQLException si ocurre un error de acceso a datos.
     */
    boolean existsUserByUsernameAndNotId(String username, Long id) throws SQLException;
}
