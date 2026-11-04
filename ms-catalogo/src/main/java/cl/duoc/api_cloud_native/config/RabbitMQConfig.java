package cl.duoc.api_cloud_native.config;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Convierte el evento Java a JSON al publicarlo.
 * El exchange, la cola y el binding NO se declaran aqui: se crearon en el panel
 * de RabbitMQ (el usuario publicador no tiene permiso Configure).
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
