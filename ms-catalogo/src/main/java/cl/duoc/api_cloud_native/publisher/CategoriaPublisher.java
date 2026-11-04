package cl.duoc.api_cloud_native.publisher;

import cl.duoc.api_cloud_native.dto.CategoriaCreadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publica CategoriaCreadaEvent en catalogo.exchange con routing key categoria.creada.
 * - Mensaje persistente (deliveryMode = 2).
 * - messageId = eventId, para relacionar HTTP -> categoriaId -> mensaje.
 * - Espera la confirmacion (ack) del broker y revisa si el mensaje fue devuelto
 *   por no tener ruta hacia una cola (mandatory = true).
 */
@Component
public class CategoriaPublisher {

    private static final Logger log = LoggerFactory.getLogger(CategoriaPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;
    private final long confirmTimeoutSeconds;

    public CategoriaPublisher(RabbitTemplate rabbitTemplate,
                              @Value("${app.rabbitmq.exchange}") String exchange,
                              @Value("${app.rabbitmq.routing-key}") String routingKey,
                              @Value("${app.rabbitmq.confirm-timeout-seconds:10}") long confirmTimeoutSeconds) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.confirmTimeoutSeconds = confirmTimeoutSeconds;
    }

    public void publicar(CategoriaCreadaEvent evento) {
        CorrelationData correlationData = new CorrelationData(evento.eventId());

        rabbitTemplate.convertAndSend(exchange, routingKey, evento, message -> {
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            message.getMessageProperties().setMessageId(evento.eventId());
            message.getMessageProperties().setType(evento.tipoEvento());
            return message;
        }, correlationData);

        try {
            CorrelationData.Confirm confirm =
                    correlationData.getFuture().get(confirmTimeoutSeconds, TimeUnit.SECONDS);

            if (!confirm.ack()) {
                throw new AmqpException("RabbitMQ no confirmo la publicacion (nack): " + confirm.reason());
            }
            if (correlationData.getReturned() != null) {
                throw new AmqpException("RabbitMQ no pudo dirigir el mensaje a una cola (sin binding para "
                        + routingKey + ")");
            }
            log.info("[PUBLICADO] ack del broker | exchange={} routingKey={} messageId={} categoriaId={}",
                    exchange, routingKey, evento.eventId(), evento.categoriaId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AmqpException("Se interrumpio la espera de confirmacion", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new AmqpException("No se pudo confirmar la publicacion en RabbitMQ", e);
        }
    }
}
