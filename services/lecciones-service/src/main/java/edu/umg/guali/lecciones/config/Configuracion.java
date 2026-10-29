package edu.umg.guali.lecciones.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import edu.umg.guali.seguridad.ErroresSeguridadJson;
import edu.umg.guali.seguridad.SeguridadProperties;
import edu.umg.guali.seguridad.SeguridadWeb;

@Configuration
public class Configuracion {

    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }

    /**
     * Estudiantes: acceso público (sin autenticación) a lecciones, historial, preferencias y código.
     * Administradores: /api/admin/** exige un JWT válido con rol ADMIN.
     */
    @Bean
    SecurityFilterChain seguridad(HttpSecurity http, SeguridadProperties props, ErroresSeguridadJson errores)
            throws Exception {
        SeguridadWeb.base(http, props, errores)
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/**", "/interno/**", "/actuator/health/**", "/error").permitAll()
                        .anyRequest().denyAll());
        return http.build();
    }
}
