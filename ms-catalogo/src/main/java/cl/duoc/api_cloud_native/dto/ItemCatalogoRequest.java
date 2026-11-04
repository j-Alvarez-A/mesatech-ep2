package cl.duoc.api_cloud_native.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos que llegan desde el BFF para crear un item del catalogo.
 */
public record ItemCatalogoRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre admite hasta 100 caracteres")
        String nombre,

        @NotBlank(message = "El tipo es obligatorio")
        @Pattern(regexp = "(?i)CATEGORIA|PRIORIDAD", message = "El tipo debe ser CATEGORIA o PRIORIDAD")
        String tipo
) {
}
