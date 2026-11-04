package cl.duoc.api_cloud_native.controller;

import cl.duoc.api_cloud_native.Repository.ItemCatalogoRepository;
import cl.duoc.api_cloud_native.dto.CategoriaCreadaEvent;
import cl.duoc.api_cloud_native.dto.ItemCatalogoRequest;
import cl.duoc.api_cloud_native.entity.ItemCatalogo;
import cl.duoc.api_cloud_native.service.CatalogoService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/catalogo")
public class CatalogoController {

    private static final Logger log = LoggerFactory.getLogger(CatalogoController.class);

    private final ItemCatalogoRepository catalogoRepository;
    private final CatalogoService catalogoService;

    public CatalogoController(ItemCatalogoRepository catalogoRepository, CatalogoService catalogoService) {
        this.catalogoRepository = catalogoRepository;
        this.catalogoService = catalogoService;
    }

    @GetMapping
    public List<ItemCatalogo> listarTodo() {
        return catalogoRepository.findAll();
    }

    @GetMapping("/tipo/{tipo}")
    public List<ItemCatalogo> listarPorTipo(@PathVariable String tipo) {
        return catalogoRepository.findByTipo(tipo.toUpperCase());
    }

    /**
     * Crea un item. Si es CATEGORIA: guarda en RDS Catalogo y publica CATEGORIA_CREADA.
     * 201 -> guardado y publicado (confirmado por RabbitMQ)
     * 502 -> guardado en RDS, pero la publicacion NO fue confirmada (no se revierte)
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> agregarItem(@Valid @RequestBody ItemCatalogoRequest request) {

        ItemCatalogo guardado = catalogoService.guardar(request);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", guardado.getId());
        body.put("nombre", guardado.getNombre());
        body.put("tipo", guardado.getTipo());
        body.put("guardadoEnRds", true);

        if (!"CATEGORIA".equals(guardado.getTipo())) {
            body.put("publicado", false);
            body.put("mensaje", "Prioridad guardada (las prioridades no generan evento)");
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        }

        try {
            CategoriaCreadaEvent evento = catalogoService.publicarCategoriaCreada(guardado);
            body.put("publicado", true);
            body.put("eventId", evento.eventId());
            body.put("mensaje", "Categoria guardada y publicada. Estara disponible en Solicitudes en unos segundos");
            return ResponseEntity.status(HttpStatus.CREATED).body(body);

        } catch (AmqpException e) {
            log.error("[PUBLICACION FALLIDA] categoriaId={} guardada en RDS pero sin confirmacion de RabbitMQ: {}",
                    guardado.getId(), e.getMessage());
            body.put("publicado", false);
            body.put("error", "La categoria se guardo en RDS Catalogo, pero no se pudo confirmar su "
                    + "publicacion en RabbitMQ. Aun no estara disponible en Solicitudes.");
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
        }
    }

    @GetMapping("/v2/catalogo")
    public ResponseEntity<?> obtenerCatalogoV2() {
        // Version 2: Retorna el catalogo extendido con metadatos de auditoria
        Map<String, Object> response = new HashMap<>();
        response.put("version", "v2.0");
        response.put("fechaConsulta", LocalDateTime.now().toString());
        response.put("items", catalogoRepository.findAll());
        return ResponseEntity.ok(response);
    }

    /* ---------- Errores controlados ---------- */

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException ex) {
        Map<String, Object> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> errores.put(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("error", "Datos invalidos", "detalle", errores));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> duplicado(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Ya existe un item con ese nombre y tipo"));
    }
}
