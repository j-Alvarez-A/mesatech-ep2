package cl.duoc.api_cloud_native.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Copia local (en RDS Solicitudes) de una categoria creada en Catalogo.
 * La clave es el id ORIGINAL de Catalogo: no se autogenera.
 */
@Entity
@Table(name = "categoria_local")
public class CategoriaLocal {

    @Id
    @Column(name = "categoria_id")
    private Long categoriaId;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "fecha_evento", nullable = false)
    private LocalDateTime fechaEvento;      // UTC

    @Column(name = "fecha_recepcion", nullable = false)
    private LocalDateTime fechaRecepcion;   // UTC

    public CategoriaLocal() {
    }

    public CategoriaLocal(Long categoriaId, String nombre, String eventId,
                          LocalDateTime fechaEvento, LocalDateTime fechaRecepcion) {
        this.categoriaId = categoriaId;
        this.nombre = nombre;
        this.eventId = eventId;
        this.fechaEvento = fechaEvento;
        this.fechaRecepcion = fechaRecepcion;
    }

    public Long getCategoriaId() { return categoriaId; }
    public String getNombre() { return nombre; }
    public String getEventId() { return eventId; }
    public LocalDateTime getFechaEvento() { return fechaEvento; }
    public LocalDateTime getFechaRecepcion() { return fechaRecepcion; }
}
