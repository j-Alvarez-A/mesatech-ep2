package cl.duoc.api_cloud_native.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "catalogo")
public class ItemCatalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre; // Ej: "Hardware", "Alta"

    @Column(nullable = false)
    private String tipo;   // Ej: "CATEGORIA" o "PRIORIDAD"

    public ItemCatalogo() {}

    public ItemCatalogo(String nombre, String tipo) {
        this.nombre = nombre;
        this.tipo = tipo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
}