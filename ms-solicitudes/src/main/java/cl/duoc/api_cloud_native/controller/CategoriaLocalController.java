package cl.duoc.api_cloud_native.controller;

import cl.duoc.api_cloud_native.entity.CategoriaLocal;
import cl.duoc.api_cloud_native.repository.CategoriaLocalRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Categorias disponibles en Solicitudes (copia local recibida por RabbitMQ).
 * Se exponen hacia React solo a traves del BFF (protegido con JWT).
 */
@RestController
@RequestMapping("/v1/categorias")
public class CategoriaLocalController {

    private final CategoriaLocalRepository repository;

    public CategoriaLocalController(CategoriaLocalRepository repository) {
        this.repository = repository;
    }

    // GET /v1/categorias -> lista de categorias locales
    @GetMapping
    public List<CategoriaLocal> listar() {
        return repository.findAllByOrderByNombreAsc();
    }

    // GET /v1/categorias/{categoriaId} -> 200 si ya esta disponible, 404 si aun no llega el evento
    @GetMapping("/{categoriaId}")
    public ResponseEntity<?> obtener(@PathVariable Long categoriaId) {
        return repository.findById(categoriaId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).body(Map.of(
                        "categoriaId", categoriaId,
                        "disponible", false,
                        "mensaje", "Aun no se recibe el evento de esta categoria")));
    }
}
