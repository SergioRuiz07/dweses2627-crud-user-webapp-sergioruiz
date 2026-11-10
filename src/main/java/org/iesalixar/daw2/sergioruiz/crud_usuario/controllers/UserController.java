package org.iesalixar.daw2.sergioruiz.crud_usuario.controllers;

import jakarta.validation.Valid;
import org.iesalixar.daw2.sergioruiz.crud_usuario.daos.RoleDAO;
import org.iesalixar.daw2.sergioruiz.crud_usuario.daos.UserDAO;
import org.iesalixar.daw2.sergioruiz.crud_usuario.entities.Role;
import org.iesalixar.daw2.sergioruiz.crud_usuario.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controlador del CRUD de usuarios, accesible a través de la URL {@code /users}.
 *
 * Gestiona el listado, alta, edición, borrado y detalle de usuarios. Es el único
 * lugar desde el que se asignan roles a los usuarios (relación N:M).
 *
 */
@Controller
@RequestMapping("/users")
public class UserController {

    /**
     * Logger para registrar eventos importantes del controlador.
     */
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    /**
     * Vista del formulario de alta/edición.
     */
    private static final String VIEW_FORM = "views/user/user-form";

    /**
     * Número de meses de validez de una contraseña desde su último cambio.
     */
    private static final int PASSWORD_VALIDITY_MONTHS = 3;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private MessageSource messageSource;

    /**
     * Muestra el listado de todos los usuarios.
     *
     * @param model  modelo de la vista.
     * @param locale idioma actual de la petición.
     * @return nombre de la vista del listado.
     */
    @GetMapping
    public String listUsers(Model model, Locale locale) {
        logger.info("Entrando en listUsers(): solicitando la lista de usuarios");
        List<User> listUsers = new ArrayList<>();
        try {
            listUsers = userDAO.listAllUsers();
            logger.info("Se han cargado {} usuarios", listUsers.size());
        } catch (Exception e) {
            logger.error("Error al listar los usuarios: {}", e.getMessage(), e);
            model.addAttribute("errorMessage",
                    messageSource.getMessage("msg.user-controller.list.error", null, locale));
        }
        model.addAttribute("listUsers", listUsers);
        logger.info("Saliendo de listUsers()");
        return "views/user/user-list";
    }

    /**
     * Muestra el formulario para crear un nuevo usuario.
     *
     * @param model  modelo de la vista.
     * @param locale idioma actual de la petición.
     * @return nombre de la vista del formulario.
     */
    @GetMapping("/new")
    public String showNewForm(Model model, Locale locale) {
        logger.info("Entrando en showNewForm(): mostrando formulario para nuevo usuario");
        model.addAttribute("user", new User());
        loadFormData(model, new ArrayList<>(), locale);
        logger.info("Saliendo de showNewForm()");
        return VIEW_FORM;
    }

    /**
     * Procesa el alta de un usuario.
     *
     * Valida el formulario, comprueba que el username no esté repetido, calcula
     * passwordExpiresAt (tres meses después de lastPasswordChange)
     * y guarda el usuario junto con sus roles.
     *
     *
     * @param user  usuario enviado desde el formulario.
     * @param result  resultado del binding y de las validaciones.
     * @param roleIds  identificadores de los roles seleccionados.
     * @param model  modelo de la vista.
     * @param redirectAttributes atributos flash para los mensajes tras la redirección.
     * @param locale idioma actual de la petición.
     * @return el formulario si hay errores; en caso contrario, redirección al listado.
     */
    @PostMapping("/insert")
    public String insertUser(@Valid @ModelAttribute("user") User user, BindingResult result,
                             @RequestParam(value = "roleIds", required = false) List<Long> roleIds,
                             Model model,
                             RedirectAttributes redirectAttributes,
                             Locale locale) {
        logger.info("Entrando en insertUser() con username: {}", user.getUsername());
        try {
            if (!result.hasFieldErrors("username") && userDAO.existsUserByUsername(user.getUsername())) {
                result.rejectValue("username", "msg.user.username.exists");
            }
            List<Role> roles = new ArrayList<>();
            if (roleIds != null) {
                for (Long roleId : roleIds) {
                    Role role = roleDAO.getRoleById(roleId);
                    if (role != null) {
                        roles.add(role);
                    }
                }
            }
            if (roles.isEmpty()) {
                result.rejectValue("roles", "msg.user.roles.required");
            }
            if (result.hasErrors()) {
                logger.warn("Errores de validación al insertar el usuario {}: {}",
                        user.getUsername(), result.getErrorCount());
                loadFormData(model, roleIds, locale);
                return VIEW_FORM;
            }
            user.setRoles(roles);
            user.setPasswordExpiresAt(calculatePasswordExpiration(user));
            userDAO.insertUser(user);
            logger.info("Usuario {} insertado con éxito", user.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("msg.user-controller.insert.success", null, locale));
        } catch (Exception e) {
            logger.error("Error al insertar el usuario {}: {}", user.getUsername(), e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("msg.user-controller.insert.error", null, locale));
        }
        logger.info("Saliendo de insertUser()");
        return "redirect:/users";
    }

    /**
     * Muestra el formulario de edición de un usuario existente.
     *
     * @param id identificador del usuario.
     * @param model  modelo de la vista.
     * @param redirectAttributes atributos flash para los mensajes de error.
     * @param locale  idioma actual de la petición.
     * @return el formulario de edición, o redirección al listado si no existe.
     */
    @GetMapping("/edit")
    public String showEditForm(@RequestParam("id") Long id, Model model,
                               RedirectAttributes redirectAttributes, Locale locale) {
        logger.info("Entrando en showEditForm() con id: {}", id);
        try {
            User user = userDAO.getUserById(id);
            if (user == null) {
                logger.warn("No se encontró el usuario con id {}", id);
                redirectAttributes.addFlashAttribute("errorMessage",
                        messageSource.getMessage("msg.user-controller.edit.notfound", null, locale));
                return "redirect:/users";
            }
            List<Long> selectedRoleIds = new ArrayList<>();
            for (Role role : user.getRoles()) {
                selectedRoleIds.add(role.getId());
            }
            model.addAttribute("user", user);
            loadFormData(model, selectedRoleIds, locale);
        } catch (Exception e) {
            logger.error("Error al obtener el usuario con id {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("msg.user-controller.edit.error", null, locale));
            return "redirect:/users";
        }
        logger.info("Saliendo de showEditForm()");
        return VIEW_FORM;
    }

    /**
     * Procesa la actualización de un usuario existente.
     *
     * @param user  usuario enviado desde el formulario.
     * @param result  resultado del binding y de las validaciones.
     * @param roleIds  identificadores de los roles seleccionados.
     * @param model modelo de la vista.
     * @param redirectAttributes atributos flash para los mensajes tras la redirección.
     * @param locale idioma actual de la petición.
     * @return el formulario si hay errores; en caso contrario, redirección al listado.
     */
    @PostMapping("/update")
    public String updateUser(@Valid @ModelAttribute("user") User user,
                             BindingResult result,
                             @RequestParam(value = "roleIds", required = false) List<Long> roleIds,
                             Model model,
                             RedirectAttributes redirectAttributes,
                             Locale locale) {
        logger.info("Entrando en updateUser() con id: {}", user.getId());
        try {
            if (!result.hasFieldErrors("username") && userDAO.existsUserByUsernameAndNotId(user.getUsername(),user.getId())) {
                result.rejectValue("username", "msg.user.username.exists");
            }
            List<Role> roles = new ArrayList<>();
            if (roleIds != null) {
                for (Long roleId : roleIds) {
                    Role role = roleDAO.getRoleById(roleId);
                    if (role != null) {
                        roles.add(role);
                    }
                }
            }
            if (roles.isEmpty()) {
                result.rejectValue("roles", "msg.user.roles.required");
            }
            if (result.hasErrors()) {
                logger.warn("Errores de validación al actualizar el usuario {}: {}",
                        user.getId(), result.getErrorCount());
                loadFormData(model, roleIds, locale);
                return VIEW_FORM;
            }
            user.setRoles(roles);
            user.setPasswordExpiresAt(calculatePasswordExpiration(user));
            userDAO.updateUser(user);
            logger.info("Usuario con id {} actualizado con éxito", user.getId());
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("msg.user-controller.update.success", null, locale));
        } catch (Exception e) {
            logger.error("Error al actualizar el usuario con id {}: {}", user.getId(), e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("msg.user-controller.update.error", null, locale));
        }
        logger.info("Saliendo de updateUser()");
        return "redirect:/users";
    }

    /**
     * Elimina un usuario (y sus asignaciones de roles).
     *
     * @param id                 identificador del usuario.
     * @param redirectAttributes atributos flash para los mensajes tras la redirección.
     * @param locale             idioma actual de la petición.
     * @return redirección al listado de usuarios.
     */
    @PostMapping("/delete")
    public String deleteUser(@RequestParam("id") Long id, RedirectAttributes redirectAttributes, Locale locale) {
        logger.info("Entrando en deleteUser() con id: {}", id);
        try {
            userDAO.deleteUser(id);
            logger.info("Usuario con id {} eliminado con éxito", id);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("msg.user-controller.delete.success", null, locale));
        } catch (Exception e) {
            logger.error("Error al eliminar el usuario con id {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("msg.user-controller.delete.error", null, locale));
        }
        logger.info("Saliendo de deleteUser()");
        return "redirect:/users";
    }

    /**
     * Muestra la vista de detalle de un usuario, incluyendo sus roles.
     *
     * @param id identificador del usuario.
     * @param model  modelo de la vista.
     * @param redirectAttributes atributos flash para los mensajes de error.
     * @param locale idioma actual de la petición.
     * @return la vista de detalle, o redirección al listado si no existe.
     */
    @GetMapping("/detail")
    public String showDetail(@RequestParam("id") Long id, Model model,
                             RedirectAttributes redirectAttributes, Locale locale) {
        logger.info("Entrando en showDetail() con id: {}", id);
        try {
            User user = userDAO.getUserById(id);
            if (user == null) {
                logger.warn("No se encontró el usuario con id {}", id);
                redirectAttributes.addFlashAttribute("errorMessage",
                        messageSource.getMessage("msg.user-controller.edit.notfound", null, locale));
                return "redirect:/users";
            }
            model.addAttribute("user", user);
        } catch (Exception e) {
            logger.error("Error al obtener el detalle del usuario con id {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("msg.user-controller.edit.error", null, locale));
            return "redirect:/users";
        }
        logger.info("Saliendo de showDetail()");
        return "views/user/user-detail";
    }

    /**
     * Calcula la fecha de caducidad de la contraseña: tres meses posterior a
     * {@code lastPasswordChange}.
     *
     * @param user usuario con {@code lastPasswordChange} informado.
     * @return fecha de caducidad de la contraseña.
     */
    private LocalDateTime calculatePasswordExpiration(User user) {
        logger.info("Entrando en calculatePasswordExpiration()");
        LocalDateTime expiresAt = user.getLastPasswordChange().plusMonths(PASSWORD_VALIDITY_MONTHS);
        logger.info("Saliendo de calculatePasswordExpiration(): {}", expiresAt);
        return expiresAt;
    }

    /**
     * Carga en el modelo los datos que necesita el formulario: todos los roles disponibles
     * y los identificadores de los roles marcados.
     *
     * @param model  modelo de la vista.
     * @param selectedRoleIds ids de los roles seleccionados (puede ser {@code null}).
     * @param locale idioma actual de la petición.
     */
    private void loadFormData(Model model, List<Long> selectedRoleIds, Locale locale) {
        logger.info("Entrando en loadFormData()");
        List<Role> allRoles = new ArrayList<>();
        try {
            allRoles = roleDAO.listAllRoles();
        } catch (Exception e) {
            logger.error("Error al cargar los roles: {}", e.getMessage(), e);
            model.addAttribute("errorMessage",
                    messageSource.getMessage("msg.user-controller.roles.error", null, locale));
        }
        model.addAttribute("allRoles", allRoles);
        model.addAttribute("selectedRoleIds", selectedRoleIds != null ? selectedRoleIds : new ArrayList<Long>());
        logger.info("Saliendo de loadFormData(): {} roles disponibles", allRoles.size());
    }
}