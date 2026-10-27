package org.iesalixar.daw2.sergioruiz.crud_usuario.daos;

import org.iesalixar.daw2.sergioruiz.crud_usuario.entities.Role;

import java.sql.SQLException;
import java.util.List;

/**
 * Interfaz DAO para la gestión de la entidad {@link Role} (tabla {@code roles}).
 */
public interface RoleDAO {

    /**
     * Lista todos los roles ordenados por nombre.
     *
     * @return lista de roles.
     * @throws SQLException si ocurre un error de acceso a datos.
     */
    List<Role> listAllRoles() throws SQLException;

    /**
     * Inserta un nuevo rol.
     *
     * @param role rol a insertar.
     * @throws SQLException si ocurre un error de acceso a datos.
     */
    void insertRole(Role role) throws SQLException;

    /**
     * Actualiza un rol existente.
     *
     * @param role rol con los datos actualizados (el id identifica el registro).
     * @throws SQLException si ocurre un error de acceso a datos.
     */
    void updateRole(Role role) throws SQLException;

    /**
     * Elimina un rol por su identificador.
     *
     * @param id identificador del rol.
     * @throws SQLException si ocurre un error de acceso a datos.
     */
    void deleteRole(Long id) throws SQLException;

    /**
     * Recupera un rol por su identificador.
     *
     * @param id identificador del rol.
     * @return el rol encontrado o {@code null} si no existe.
     * @throws SQLException si ocurre un error de acceso a datos.
     */
    Role getRoleById(Long id) throws SQLException;

    /**
     * Comprueba si existe un rol con el nombre indicado (sin distinguir mayúsculas).
     *
     * @param name nombre del rol.
     * @return {@code true} si ya existe.
     * @throws SQLException si ocurre un error de acceso a datos.
     */
    boolean existsRoleByName(String name) throws SQLException;

    /**
     * Comprueba si existe un rol con el nombre indicado, excluyendo el rol con el id dado.
     *
     * @param name nombre del rol.
     * @param id   identificador del rol a excluir.
     * @return {@code true} si existe otro rol con ese nombre.
     * @throws SQLException si ocurre un error de acceso a datos.
     */
    boolean existsRoleByNameAndNotId(String name, Long id) throws SQLException;

    /**
     * Cuenta cuántos usuarios tienen asignado el rol indicado.
     *
     * @param roleId identificador del rol.
     * @return número de usuarios con ese rol.
     * @throws SQLException si ocurre un error de acceso a datos.
     */
    int countUsersByRoleId(Long roleId) throws SQLException;
}
