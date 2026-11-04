package cl.duoc.api_cloud_native.controller;

import cl.duoc.api_cloud_native.entity.CategoriaLocal;
import cl.duoc.api_cloud_native.entity.Solicitud;
import cl.duoc.api_cloud_native.repository.CategoriaLocalRepository;
import cl.duoc.api_cloud_native.repository.SolicitudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/solicitudes")
public class SolicitudController {

    private final SolicitudRepository solicitudRepository;
    private final CategoriaLocalRepository categoriaLocalRepository;

    public SolicitudController(SolicitudRepository solicitudRepository,
                               CategoriaLocalRepository categoriaLocalRepository) {
        this.solicitudRepository = solicitudRepository;
        this.categoriaLocalRepository = categoriaLocalRepository;
    }

    // 1. Crear solicitud (POST /v1/solicitudes)
    //    EP2: la categoria debe existir en la COPIA LOCAL (categoria_local) de RDS Solicitudes.
    @PostMapping
    public ResponseEntity<?> crearSolicitud(@RequestBody Solicitud solicitud) {
        Optional<CategoriaLocal> categoria = Optional.empty();

        if (solicitud.getCategoriaId() != null) {
            categoria = categoriaLocalRepository.findById(solicitud.getCategoriaId());
        } else if (solicitud.getCategoria() != null && !solicitud.getCategoria().isBlank()) {
            // compatibilidad EP1: si solo llega el nombre, se busca por nombre en la copia local
            categoria = categoriaLocalRepository.findFirstByNombreIgnoreCase(solicitud.getCategoria().trim());
        }

        if (categoria.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "La categoria no esta disponible en Solicitudes. "
                            + "Si se acaba de crear, espere unos segundos a que se procese el evento."));
        }

        solicitud.setId(null);
        solicitud.setEstado(null); // siempre parte en CREADA (@PrePersist)
        solicitud.setCategoriaId(categoria.get().getCategoriaId());
        solicitud.setCategoria(categoria.get().getNombre());

        Solicitud nueva = solicitudRepository.save(solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(nueva);
    }

    // 2. Listar todas las solicitudes (GET /v1/solicitudes)
    @GetMapping
    public List<Solicitud> listarTodas() {
        return solicitudRepository.findAll();
    }

    // 3. Listar solicitudes del usuario autenticado (GET /v1/solicitudes/mias?usuario=correo)
    @GetMapping("/mias")
    public List<Solicitud> listarPorUsuario(@RequestParam String usuario) {
        return solicitudRepository.findByUsuarioSolicitante(usuario);
    }

    // 4. Actualizar estado respetando la regla de negocio (PUT /v1/solicitudes/{id}/estado)
    @PutMapping("/{id}/estado")
    public ResponseEntity<?> actualizarEstado(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Optional<Solicitud> optSolicitud = solicitudRepository.findById(id);

        if (optSolicitud.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Solicitud no encontrada"));
        }

        String nuevoEstado = body.get("estado");
        if (nuevoEstado == null || nuevoEstado.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Debe indicar el nuevo estado"));
        }

        Solicitud solicitud = optSolicitud.get();

        // Regla de negocio: no puede pasar a RESUELTA si antes no esta EN_PROCESO
        if ("RESUELTA".equalsIgnoreCase(nuevoEstado) && !"EN_PROCESO".equalsIgnoreCase(solicitud.getEstado())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Regla de negocio violada: Para marcar como RESUELTA, debe estar previamente EN_PROCESO");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        solicitud.setEstado(nuevoEstado.toUpperCase());
        solicitudRepository.save(solicitud);

        return ResponseEntity.ok(solicitud);
    }
}
