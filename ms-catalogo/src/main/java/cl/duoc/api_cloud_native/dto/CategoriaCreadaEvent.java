package cl.duoc.api_cloud_native.dto;

/**
 * Contrato del evento publicado en RabbitMQ cuando se crea una categoria.
 *
 * eventId     String  UUID v4 del evento (tambien se usa como messageId AMQP)
 * tipoEvento  String  Siempre "CATEGORIA_CREADA"
 * fechaEvento String  Fecha/hora UTC en ISO-8601, ej: 2026-10-29T22:15:30.123Z
 * categoriaId Long    id ORIGINAL de la categoria en RDS Catalogo
 * nombre      String  Nombre de la categoria
 */
public record CategoriaCreadaEvent(
        String eventId,
        String tipoEvento,
        String fechaEvento,
        Long categoriaId,
        String nombre
) {
    public static final String TIPO_CATEGORIA_CREADA = "CATEGORIA_CREADA";
}
