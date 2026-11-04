package cl.duoc.api_cloud_native.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Seguridad del BFF (continuidad EP1):
 * - Valida el JWT emitido por Entra ID.
 * - Autoriza por los roles de la aplicacion (claim "roles" del access token):
 *   ROLE_CLIENTE, ROLE_OPERADOR, ROLE_ADMINISTRADOR.
 * Sin token -> 401. Token valido pero sin el rol requerido -> 403.
 */
@Configuration
public class SecurityConfig {

    private static final String ADMIN = "ROLE_ADMINISTRADOR";
    private static final String OPERADOR = "ROLE_OPERADOR";
    private static final String CLIENTE = "ROLE_CLIENTE";

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/public/**").permitAll()

                // Catalogo
                .requestMatchers(HttpMethod.POST, "/api/catalogo").hasAuthority(ADMIN)
                .requestMatchers(HttpMethod.GET, "/api/catalogo", "/api/v2/catalogo").authenticated()

                // Copia local de categorias en Solicitudes (EP2)
                .requestMatchers(HttpMethod.GET, "/api/categorias-locales", "/api/categorias-locales/**").authenticated()

                // Solicitudes
                .requestMatchers(HttpMethod.POST, "/api/solicitudes").hasAnyAuthority(CLIENTE, ADMIN)
                .requestMatchers(HttpMethod.GET, "/api/solicitudes/mias").hasAnyAuthority(CLIENTE, ADMIN)
                .requestMatchers(HttpMethod.GET, "/api/solicitudes").hasAnyAuthority(OPERADOR, ADMIN)
                .requestMatchers(HttpMethod.PUT, "/api/solicitudes/*/estado").hasAnyAuthority(OPERADOR, ADMIN)

                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(rolesYScopes()))
            );

        return http.build();
    }

    /** Convierte el claim "roles" (ROLE_...) y el claim "scp" (SCOPE_...) en authorities. */
    private Converter<Jwt, AbstractAuthenticationToken> rolesYScopes() {
        return jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles != null) {
                roles.forEach(r -> authorities.add(new SimpleGrantedAuthority(r)));
            }
            String scp = jwt.getClaimAsString("scp");
            if (scp != null) {
                Arrays.stream(scp.split(" "))
                      .forEach(s -> authorities.add(new SimpleGrantedAuthority("SCOPE_" + s)));
            }
            String nombre = jwt.getClaimAsString("preferred_username");
            return new JwtAuthenticationToken(jwt, authorities, nombre != null ? nombre : jwt.getSubject());
        };
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
