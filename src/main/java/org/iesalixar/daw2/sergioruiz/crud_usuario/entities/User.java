package org.iesalixar.daw2.sergioruiz.crud_usuario.entities;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa un usuario de la aplicación.
 *
 * Se corresponde con la tabla users. Un usuario puede tener uno o varios roles
 * por lo que se forma una relación N:M
 *
 *
 * Todas las restricciones de los campos (obligatorios, longitudes, valores mínimos)
 * se validan con Bean Validation ({@code @Valid} en el controlador). Lo único que
 * no cubre {@code @Valid} y se comprueba en el controlador es la unicidad del
 * username y la selección de roles.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {

    /** Identificador único del usuario. */
    private Long id;

    /** Nombre de usuario (login). Debe ser único. */
    @NotBlank(message = "{msg.user.username.notEmpty}")
    @Size(max = 40, message = "{msg.user.username.size}")
    private String username;

    /** Contraseña del usuario (por ahora en texto plano). */
    @NotBlank(message = "{msg.user.passwordHash.notEmpty}")
    @Size(max = 500, message = "{msg.user.passwordHash.size}")
    private String passwordHash;

    /** Indica si la cuenta está activa (true) o bloqueada/desactivada (false). */
    private Boolean active;

    /** Indica si la cuenta NO está bloqueada por intentos fallidos (true = no bloqueada). */
    private Boolean accountNonLocked;

    /** Fecha y hora del último cambio de contraseña. */
    @NotNull(message = "{msg.user.lastPasswordChange.notEmpty}")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime lastPasswordChange;

    /** Fecha de caducidad de la contraseña: tres meses posterior a {@code lastPasswordChange}. */
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime passwordExpiresAt;

    /** Número de intentos de login fallidos. */
    @NotNull(message = "{msg.user.failedLoginAttempts.notNull}")
    @Min(value = 0, message = "{msg.user.failedLoginAttempts.min}")
    private Integer failedLoginAttempts = 0;

    /** Indica si el correo del usuario ha sido verificado. */
    private Boolean emailVerified;

    /** Indica si el usuario debe cambiar la contraseña en el próximo login. */
    private Boolean mustChangePassword;

    /** Roles asignados al usuario (relación N:M). */
    private List<Role> roles = new ArrayList<>();
}
