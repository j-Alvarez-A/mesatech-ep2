package cl.duoc.api_cloud_native.service;

import cl.duoc.api_cloud_native.Repository.ItemCatalogoRepository;
import cl.duoc.api_cloud_native.dto.CategoriaCreadaEvent;
import cl.duoc.api_cloud_native.dto.ItemCatalogoRequest;
import cl.duoc.api_cloud_native.entity.ItemCatalogo;
import cl.duoc.api_cloud_native.publisher.CategoriaPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Coordina: 1) guardar en RDS Catalogo  2) publicar el evento (solo CATEGORIA).
 * Son operaciones SEPARADAS: si la publicacion falla, la fila ya quedo guardada.
 * No hay transaccion distribuida ni outbox (fuera de alcance de la EP2).
 */
@Service
public class CatalogoService {

    private static final Logger log = LoggerFactory.getLogger(CatalogoService.class);

    private final ItemCatalogoRepository repository;
    private final CategoriaPublisher publisher;

    public CatalogoService(ItemCatalogoRepository repository, CategoriaPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    /** Paso 1: persistir. Se confirma (commit) antes de publicar. */
    public ItemCatalogo guardar(ItemCatalogoRequest request) {
        ItemCatalogo item = new ItemCatalogo(request.nombre().trim(), request.tipo().toUpperCase());
        ItemCatalogo guardado = repository.save(item);
        log.info("[RDS CATALOGO] guardado | id={} nombre={} tipo={}",
                guardado.getId(), guardado.getNombre(), guardado.getTipo());
        return guardado;
    }

    /** Paso 2: publicar el evento de una categoria ya guardada. Lanza AmqpException si falla. */
    public CategoriaCreadaEvent publicarCategoriaCreada(ItemCatalogo categoria) {
        CategoriaCreadaEvent evento = new CategoriaCreadaEvent(
                UUID.randomUUID().toString(),
                CategoriaCreadaEvent.TIPO_CATEGORIA_CREADA,
                Instant.now().toString(),
                categoria.getId(),
                categoria.getNombre()
        );
        publisher.publicar(evento);
        return evento;
    }
}
