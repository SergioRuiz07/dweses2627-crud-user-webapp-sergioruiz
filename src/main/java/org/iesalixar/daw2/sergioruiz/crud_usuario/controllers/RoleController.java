package org.iesalixar.daw2.sergioruiz.crud_usuario.controllers;

import jakarta.validation.Valid;
import org.iesalixar.daw2.sergioruiz.crud_usuario.daos.RoleDAO;
import org.iesalixar.daw2.sergioruiz.crud_usuario.entities.Role;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controlador del CRUD de roles, accesible a través de la URL {@code /roles}.
 * <p>
 * Desde este controlador NO se pueden asignar usuarios a un rol (ni en el alta, ni en
 * la edición, ni en el detalle): la asignación se gestiona exclusivamente desde el
 * CRUD de usuarios.
 * </p>
 */
@Controller
@RequestMapping("/roles")
public class RoleController {

    /** Logger para registrar eventos importantes del controlador. */
    private static final Logger logger = LoggerFactory.getLogger(RoleController.class);

    /** Vista del formulario de alta/edición. */
    private static final String VIEW_FORM = "views/role/role-form";

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private MessageSource messageSource;

    /**
     * Muestra el listado de todos los roles.
     *
     * @param model  modelo de la vista.
     * @param locale idioma actual de la petición.
     * @return nombre de la vista del listado.
     */
    @GetMapping
    public String listRoles(Model model, Locale locale) {
        logger.info("Entrando en listRoles(): solicitando la lista de roles");
        List<Role> listRoles = new ArrayList<>();
        try {
            listRoles = roleDAO.listAllRoles();
            logger.info("Se han cargado {} roles", listRoles.size());
        } catch (Exception e) {
            logger.error("Error al listar los roles: {}", e.getMessage(), e);
            model.addAttribute("errorMessage",
                    messageSource.getMessage("msg.role-controller.list.error", null, locale));
        }
        model.addAttribute("listRoles", listRoles);
        logger.info("Saliendo de listRoles()");
        return "views/role/role-list";
    }

    /**
     * Muestra el formulario para crear un nuevo rol.
     *
     * @param model modelo de la vista.
     * @return nombre de la vista del formulario.
     */
    @GetMapping("/new")
    public String showNewForm(Model model) {
        logger.info("Entrando en showNewForm(): mostrando formulario para nuevo rol");
        model.addAttribute("role", new Role());
        logger.info("Saliendo de showNewForm()");
        return VIEW_FORM;
    }

    /**
     * Procesa el alta de un rol. Comprueba que el nombre no esté repetido.
     *
     * @param role               rol enviado desde el formulario.
     * @param result             resultado del binding y de las validaciones.
     * @param redirectAttributes atributos flash para los mensajes tras la redirección.
     * @param locale             idioma actual de la petición.
     * @return el formulario si hay errores; en caso contrario, redirección al listado.
     */
    @PostMapping("/insert")
    public String insertRole(@Valid @ModelAttribute("role") Role role, BindingResult result,
                             RedirectAttributes redirectAttributes, Locale locale) {
        logger.info("Entrando en insertRole() con nombre: {}", role.getName());
        try {
            // @Valid ya comprueba que el nombre no esté vacío ni sea demasiado largo;
            // aquí solo se comprueba lo que necesita la base de datos: que no esté repetido.
            if (!result.hasFieldErrors("name") && roleDAO.existsRoleByName(role.getName())) {
                logger.warn("El rol {} ya existe", role.getName());
                result.rejectValue("name", "msg.role.name.exists");
            }
            if (result.hasErrors()) {
                logger.warn("Errores de validación al insertar el rol: {}", result.getErrorCount());
                return VIEW_FORM;
            }
            roleDAO.insertRole(role);
            logger.info("Rol {} insertado con éxito", role.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("msg.role-controller.insert.success", null, locale));
        } catch (Exception e) {
            logger.error("Error al insertar el rol {}: {}", role.getName(), e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("msg.role-controller.insert.error", null, locale));
        }
        logger.info("Saliendo de insertRole()");
        return "redirect:/roles";
    }

    /**
     * Muestra el formulario de edición de un rol existente.
     *
     * @param id identificador del rol.
     * @param model modelo de la vista.
     * @param redirectAttributes atributos flash para los mensajes de error.
     * @param locale idioma actual de la petición.
     * @return el formulario de edición, o redirección al listado si no existe.
     */
    @GetMapping("/edit")
    public String showEditForm(@RequestParam("id") Long id, Model model,
                               RedirectAttributes redirectAttributes, Locale locale) {
        logger.info("Entrando en showEditForm() con id: {}", id);
        try {
            Role role = roleDAO.getRoleById(id);
            if (role == null) {
                logger.warn("No se encontró el rol con id {}", id);
                redirectAttributes.addFlashAttribute("errorMessage",
                        messageSource.getMessage("msg.role-controller.edit.notfound", null, locale));
                return "redirect:/roles";
            }
            model.addAttribute("role", role);
        } catch (Exception e) {
            logger.error("Error al obtener el rol con id {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("msg.role-controller.edit.error", null, locale));
            return "redirect:/roles";
        }
        logger.info("Saliendo de showEditForm()");
        return VIEW_FORM;
    }

    /**
     * Procesa la actualización de un rol existente.
     *
     * @param role rol enviado desde el formulario.
     * @param result  resultado del binding y de las validaciones.
     * @param redirectAttributes atributos flash para los mensajes tras la redirección.
     * @param locale  idioma actual de la petición.
     * @return el formulario si hay errores; en caso contrario, redirección al listado.
     */
    @PostMapping("/update")
    public String updateRole(@Valid @ModelAttribute("role") Role role, BindingResult result,
                             RedirectAttributes redirectAttributes, Locale locale) {
        logger.info("Entrando en updateRole() con id: {}", role.getId());
        try {
            if (!result.hasFieldErrors("name") && roleDAO.existsRoleByNameAndNotId(role.getName(), role.getId())) {
                logger.warn("El nombre de rol {} ya existe para otro rol.", role.getName());
                result.rejectValue("name", "msg.role.name.exists");
            }
            if (result.hasErrors()) {
                logger.warn("Errores de validación al actualizar el rol {}: {}", role.getId(), result.getErrorCount());
                return VIEW_FORM;
            }
            roleDAO.updateRole(role);
            logger.info("Rol con id {} actualizado con éxito", role.getId());
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("msg.role-controller.update.success", null, locale));
        } catch (Exception e) {
            logger.error("Error al actualizar el rol con id {}: {}", role.getId(), e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("msg.role-controller.update.error", null, locale));
        }
        logger.info("Saliendo de updateRole()");
        return "redirect:/roles";
    }

    /**
     * Elimina un rol. No se permite eliminar un rol que esté asignado a algún usuario.
     *
     * @param id identificador del rol.
     * @param redirectAttributes atributos flash para los mensajes tras la redirección.
     * @param locale  idioma actual de la petición.
     * @return redirección al listado de roles.
     */
    @PostMapping("/delete")
    public String deleteRole(@RequestParam("id") Long id, RedirectAttributes redirectAttributes, Locale locale) {
        logger.info("Entrando en deleteRole() con id: {}", id);
        try {
            int usersWithRole = roleDAO.countUsersByRoleId(id);
            if (usersWithRole > 0) {
                logger.warn("No se puede eliminar el rol {}: está asignado a {} usuarios", id, usersWithRole);
                redirectAttributes.addFlashAttribute("errorMessage",
                        messageSource.getMessage("msg.role-controller.delete.inUse", null, locale));
                return "redirect:/roles";
            }
            roleDAO.deleteRole(id);
            logger.info("Rol con id {} eliminado con éxito", id);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("msg.role-controller.delete.success", null, locale));
        } catch (Exception e) {
            logger.error("Error al eliminar el rol con id {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("msg.role-controller.delete.error", null, locale));
        }
        logger.info("Saliendo de deleteRole()");
        return "redirect:/roles";
    }

    /**
     * Muestra la vista de detalle de un rol (sin usuarios asociados).
     *
     * @param id identificador del rol.
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
            Role role = roleDAO.getRoleById(id);
            if (role == null) {
                logger.warn("No se encontró el rol con id {}", id);
                redirectAttributes.addFlashAttribute("errorMessage",
                        messageSource.getMessage("msg.role-controller.edit.notfound", null, locale));
                return "redirect:/roles";
            }
            model.addAttribute("role", role);
        } catch (Exception e) {
            logger.error("Error al obtener el detalle del rol con id {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("msg.role-controller.edit.error", null, locale));
            return "redirect:/roles";
        }
        logger.info("Saliendo de showDetail()");
        return "views/role/role-detail";
    }
}
