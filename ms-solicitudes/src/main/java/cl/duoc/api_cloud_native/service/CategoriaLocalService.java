package cl.duoc.api_cloud_native.service;

import cl.duoc.api_cloud_native.dto.CategoriaCreadaMensaje;
import cl.duoc.api_cloud_native.entity.CategoriaLocal;
import cl.duoc.api_cloud_native.repository.CategoriaLocalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class CategoriaLocalService {

    private static final Logger log = LoggerFactory.getLogger(CategoriaLocalService.class);

    private final CategoriaLocalRepository repository;

    public CategoriaLocalService(CategoriaLocalRepository repository) {
        this.repository = repository;
    }

    /**
     * Guarda la copia local. Si el mensaje se entrega de nuevo (reentrega), la clave
     * categoria_id evita duplicar la fila: se registra y no se inserta otra vez.
     * (No es una garantia de "exactamente una vez"; solo evita la fila duplicada.)
     */
    @Transactional
    public CategoriaLocal guardar(CategoriaCreadaMensaje msg) {
        if (repository.existsById(msg.categoriaId())) {
            log.warn("[REENTREGA] categoriaId={} ya existia en RDS Solicitudes; no se inserta de nuevo (eventId={})",
                    msg.categoriaId(), msg.eventId());
            return repository.findById(msg.categoriaId()).orElseThrow();
        }
        LocalDateTime fechaEvento = LocalDateTime.ofInstant(Instant.parse(msg.fechaEvento()), ZoneOffset.UTC);
        CategoriaLocal copia = new CategoriaLocal(
                msg.categoriaId(),
                msg.nombre(),
                msg.eventId(),
                fechaEvento,
                LocalDateTime.now(ZoneOffset.UTC)
        );
        return repository.save(copia);
    }
}
