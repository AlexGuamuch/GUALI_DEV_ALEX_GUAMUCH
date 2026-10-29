package edu.umg.guali.admin.config;

import java.io.IOException;
import java.time.Clock;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.OncePerRequestFilter;

import edu.umg.guali.admin.servicio.AutenticacionService;
import edu.umg.guali.seguridad.ErroresSeguridadJson;
import edu.umg.guali.seguridad.SeguridadProperties;
import edu.umg.guali.seguridad.SeguridadWeb;

@Configuration
public class Configuracion {

    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain seguridad(HttpSecurity http, SeguridadProperties props, ErroresSeguridadJson errores,
                                  AutenticacionService autenticacion) throws Exception {
        SeguridadWeb.base(http, props, errores)
                .addFilterAfter(new AdminActivoFilter(autenticacion, errores), BearerTokenAuthenticationFilter.class)
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/logout", "/api/visitas").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/sesion").permitAll()
                        .requestMatchers("/actuator/health/**", "/error").permitAll()
                        .anyRequest().denyAll());
        return http.build();
    }

    /**
     * Un token sigue siendo válido hasta que vence; si el administrador fue desactivado o eliminado
     * mientras tanto, se rechaza aquí de inmediato.
     */
    static final class AdminActivoFilter extends OncePerRequestFilter {

        private final AutenticacionService autenticacion;
        private final ErroresSeguridadJson errores;

        AdminActivoFilter(AutenticacionService autenticacion, ErroresSeguridadJson errores) {
            this.autenticacion = autenticacion;
            this.errores = errores;
        }

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            return !request.getRequestURI().startsWith("/api/admin/");
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !autenticacion.estaActivo(auth.getName())) {
                SecurityContextHolder.clearContext();
                errores.noAutenticado().commence(request, response, null);
                return;
            }
            chain.doFilter(request, response);
        }
    }
}
