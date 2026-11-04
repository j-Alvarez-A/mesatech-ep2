package cl.duoc.api_cloud_native.listener;

import cl.duoc.api_cloud_native.dto.CategoriaCreadaMensaje;
import cl.duoc.api_cloud_native.entity.CategoriaLocal;
import cl.duoc.api_cloud_native.service.CategoriaLocalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consumidor de solicitudes.catalogo.queue (EC2 B).
 * Modo AUTO: Spring envia el ack a RabbitMQ solo cuando este metodo retorna,
 * es decir, DESPUES de guardar en RDS Solicitudes. Si guardar lanza una excepcion,
 * el mensaje NO se confirma y vuelve a la cola.
 */
@Component
public class CategoriaListener {

    private static final Logger log = LoggerFactory.getLogger(CategoriaListener.class);

    private final CategoriaLocalService service;

    public CategoriaListener(CategoriaLocalService service) {
        this.service = service;
    }

    @RabbitListener(queues = "${app.rabbitmq.queue}")
    public void consumir(CategoriaCreadaMensaje mensaje,
                         @Header(name = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {

        log.info("[RECIBIDO] messageId={} eventId={} categoriaId={} nombre={}",
                messageId, mensaje.eventId(), mensaje.categoriaId(), mensaje.nombre());

        // Mensaje incompleto: reintentarlo no lo arregla, se descarta (queda en el log)
        if (mensaje.eventId() == null || mensaje.categoriaId() == null
                || mensaje.nombre() == null || mensaje.fechaEvento() == null) {
            log.error("[DESCARTADO] mensaje sin campos obligatorios: {}", mensaje);
            throw new AmqpRejectAndDontRequeueException("Mensaje invalido: faltan campos del contrato");
        }

        CategoriaLocal guardada = service.guardar(mensaje);   // si falla -> excepcion -> sin ack -> requeue

        log.info("[RDS SOLICITUDES] copia guardada | categoriaId={} nombre={} eventId={} -> ack",
                guardada.getCategoriaId(), guardada.getNombre(), mensaje.eventId());
    }
}
