package org.iesalixar.daw2.sergioruiz.crud_usuario.entities;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa un rol de la aplicación (por ejemplo ADMIN o USER).
 * <p>
 * Se corresponde con la tabla {@code roles}. La relación N:M con {@link User}
 * se gestiona únicamente desde el lado del usuario, por lo que esta entidad
 * no contiene ninguna referencia a los usuarios que tienen el rol.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Role {

    /** Identificador único del rol. */
    private Long id;

    /** Nombre único del rol. */
    @NotBlank(message = "{msg.role.name.notEmpty}")
    @Size(max = 50, message = "{msg.role.name.size}")
    private String name;
}