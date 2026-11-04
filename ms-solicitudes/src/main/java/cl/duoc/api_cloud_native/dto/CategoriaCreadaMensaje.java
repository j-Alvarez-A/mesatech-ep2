package cl.duoc.api_cloud_native.dto;

/**
 * Mensaje recibido desde solicitudes.catalogo.queue.
 * Mismo contrato que publica Catalogo (CategoriaCreadaEvent):
 * eventId (String UUID), tipoEvento (String), fechaEvento (String ISO-8601 UTC),
 * categoriaId (Long, id original en RDS Catalogo), nombre (String).
 */
public record CategoriaCreadaMensaje(
        String eventId,
        String tipoEvento,
        String fechaEvento,
        Long categoriaId,
        String nombre
) {
}
