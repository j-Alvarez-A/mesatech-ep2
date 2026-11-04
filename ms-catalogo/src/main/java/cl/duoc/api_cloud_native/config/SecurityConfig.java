package cl.duoc.api_cloud_native.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

     @Bean
     SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {


        http


            /*
             * Habilitar CORS
             */
            .cors(
                Customizer.withDefaults()
            )


            /*
             * API REST
             */
            .csrf(
                csrf ->
                    csrf.disable()
            )


            /*
             * No utilizaremos
             * sesiones HTTP.
             */
            .sessionManagement(
                session ->
                    session
                        .sessionCreationPolicy(
                            SessionCreationPolicy
                                .STATELESS
                        )
            )


            /*
             * Seguridad de URIs
             */
            .authorizeHttpRequests(

                auth -> auth


                    /*
                     * URI pública
                     */
                    .requestMatchers(
                        "/public/**",
                            "/v1/**"
                    )
                    .permitAll()


                    /*
                     * URI protegida
                     */
                    .requestMatchers(
                        "/api/**"
                    )
                    .hasAuthority(
                        "SCOPE_access_as_user"
                    )


                    /*
                     * Todo lo demás
                     */
                    .anyRequest()
                    .authenticated()

            )


            /*
             * Validación JWT
             */
            .oauth2ResourceServer(

                oauth2 ->

                    oauth2.jwt(
                        Customizer
                            .withDefaults()
                    )

            );


        return http.build();

    }



    /*
     * Configuración CORS
     */
    @Bean
    CorsConfigurationSource
        corsConfigurationSource() {


        CorsConfiguration config =
            new CorsConfiguration();


        /*
         * React
         */
        config.setAllowedOrigins(

            List.of(
                "http://localhost:3000"
            )

        );


        /*
         * Métodos HTTP
         */
        config.setAllowedMethods(

            List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "OPTIONS"
            )

        );


        /*
         * Headers permitidos
         */
        config.setAllowedHeaders(

            List.of(
                "Authorization",
                "Content-Type"
            )

        );


        UrlBasedCorsConfigurationSource
            source =
                new UrlBasedCorsConfigurationSource();


        source.registerCorsConfiguration(
            "/**",
            config
        );


        return source;

    }

}
