package cl.duoc.api_cloud_native.config;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Convierte el JSON recibido al record CategoriaCreadaMensaje.
 * La cola NO se declara aqui: se creo en el panel (el consumidor no tiene permiso Configure).
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
