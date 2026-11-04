package cl.duoc.api_cloud_native.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * BFF: unico punto que React usa (a traves de API Gateway).
 * No accede a RDS: delega en Catalogo (EC2 A) y Solicitudes (EC2 B).
 * La autorizacion por rol esta en SecurityConfig.
 */
@RestController
@RequestMapping("/api")
public class BFFController {

    private final RestTemplate restTemplate;

    @Value("${CATALOG_SERVICE_URL}")
    private String CATALOG_URL;

    @Value("${ORDERS_SERVICE_URL}")
    private String ORDERS_URL;

    @Value("${CATEGORIAS_SERVICE_URL}")
    private String CATEGORIAS_URL;

    public BFFController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // 1. Datos del usuario autenticado
    @GetMapping("/usuario")
    public ResponseEntity<?> obtenerUsuario(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> data = new HashMap<>();
        data.put("sub", jwt.getSubject());
        data.put("usuario", correoDe(jwt));
        data.put("roles", jwt.getClaimAsStringList("roles"));
        data.put("mensaje", "Autenticado correctamente en BFF");
        return ResponseEntity.ok(data);
    }

    // 2. Catalogo completo (v1)
    @GetMapping("/catalogo")
    public ResponseEntity<?> obtenerCatalogo() {
        return llamar(() -> ResponseEntity.ok(restTemplate.getForObject(CATALOG_URL, Object.class)));
    }

    // 2b. Catalogo v2 (coexistencia de versiones, EP1)
    @GetMapping("/v2/catalogo")
    public ResponseEntity<?> obtenerCatalogoV2() {
        return llamar(() -> ResponseEntity.ok(restTemplate.getForObject(CATALOG_URL + "/v2/catalogo", Object.class)));
    }

    // 3. Crear item de catalogo (solo ADMINISTRADOR). Catalogo guarda en su RDS y publica el evento.
    //    Se reenvia tal cual el codigo de Catalogo: 201 publicado, 502 guardado sin confirmacion, 400, 409.
    @PostMapping("/catalogo")
    public ResponseEntity<?> agregarItemCatalogo(@RequestBody Object item) {
        return llamar(() -> {
            ResponseEntity<Object> r = restTemplate.postForEntity(CATALOG_URL, item, Object.class);
            return ResponseEntity.status(r.getStatusCode()).body(r.getBody());
        });
    }

    // 4. Categorias disponibles en Solicitudes (copia local recibida por RabbitMQ) - EP2
    @GetMapping("/categorias-locales")
    public ResponseEntity<?> categoriasLocales() {
        return llamar(() -> ResponseEntity.ok(restTemplate.getForObject(CATEGORIAS_URL, Object.class)));
    }

    // 4b. Una categoria: 200 si ya esta disponible en Solicitudes, 404 si aun no llega el evento
    @GetMapping("/categorias-locales/{categoriaId}")
    public ResponseEntity<?> categoriaLocal(@PathVariable Long categoriaId) {
        return llamar(() -> ResponseEntity.ok(
                restTemplate.getForObject(CATEGORIAS_URL + "/" + categoriaId, Object.class)));
    }

    // 5. Crear solicitud: se inyecta el correo del token (no se confia en el que envie el navegador)
    @PostMapping("/solicitudes")
    public ResponseEntity<?> crearSolicitud(@RequestBody Map<String, Object> body, @AuthenticationPrincipal Jwt jwt) {
        body.put("usuarioSolicitante", correoDe(jwt));
        return llamar(() -> ResponseEntity.status(HttpStatus.CREATED)
                .body(restTemplate.postForObject(ORDERS_URL, body, Object.class)));
    }

    // 6. Listar todas (Operador y Administrador)
    @GetMapping("/solicitudes")
    public ResponseEntity<?> listarTodasLasSolicitudes() {
        return llamar(() -> ResponseEntity.ok(restTemplate.getForObject(ORDERS_URL, Object.class)));
    }

    // 7. Listar las mias (Cliente)
    @GetMapping("/solicitudes/mias")
    public ResponseEntity<?> listarMisSolicitudes(@AuthenticationPrincipal Jwt jwt) {
        String url = ORDERS_URL + "/mias?usuario=" + correoDe(jwt);
        return llamar(() -> ResponseEntity.ok(restTemplate.getForObject(url, Object.class)));
    }

    // 8. Cambiar estado (la regla EN_PROCESO -> RESUELTA la aplica Solicitudes)
    @PutMapping("/solicitudes/{id}/estado")
    public ResponseEntity<?> actualizarEstado(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return llamar(() -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<Object> r = restTemplate.exchange(ORDERS_URL + "/" + id + "/estado",
                    HttpMethod.PUT, new HttpEntity<>(body, headers), Object.class);
            return ResponseEntity.status(r.getStatusCode()).body(r.getBody());
        });
    }

    /* ---------- utilidades ---------- */

    private String correoDe(Jwt jwt) {
        String correo = jwt.getClaimAsString("preferred_username");
        return correo != null ? correo : jwt.getClaimAsString("unique_name");
    }

    private interface Llamada {
        ResponseEntity<?> ejecutar();
    }

    /** Reenvia errores del microservicio con su codigo y cuerpo JSON; 503 si no responde. */
    private ResponseEntity<?> llamar(Llamada llamada) {
        try {
            return llamada.ejecutar();
        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ex.getResponseBodyAsString());
        } catch (ResourceAccessException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "El microservicio no responde: " + ex.getMessage()));
        }
    }
}
