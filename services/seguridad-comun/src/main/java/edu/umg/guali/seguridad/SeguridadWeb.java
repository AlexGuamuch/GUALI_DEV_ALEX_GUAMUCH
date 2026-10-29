package edu.umg.guali.seguridad;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Base común de la cadena de seguridad de cada servicio: sin sesión de servidor (stateless),
 * JWT en cookie validado con llave pública, CSRF double-submit, token interno y errores JSON.
 * Cada servicio agrega después sus reglas de autorización.
 */
public final class SeguridadWeb {

    private SeguridadWeb() {
    }

    public static HttpSecurity base(HttpSecurity http, SeguridadProperties props, ErroresSeguridadJson errores)
            throws Exception {
        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepo.setCookieCustomizer(c -> c.secure(props.cookieSegura()).sameSite("Strict").path("/"));

        return http
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(c -> c
                        .csrfTokenRepository(csrfRepo)
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                        .ignoringRequestMatchers("/interno/**"))
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                .addFilterBefore(new TokenInternoFilter(props.tokenInterno()), BasicAuthenticationFilter.class)
                .addFilterBefore(new TokenGatewayFilter(props.tokenGateway()), BasicAuthenticationFilter.class)
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .logout(l -> l.disable())
                .requestCache(r -> r.disable())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(errores.noAutenticado())
                        .accessDeniedHandler(errores.accesoDenegado()))
                .oauth2ResourceServer(o -> o
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(errores.noAutenticado())
                        .accessDeniedHandler(errores.accesoDenegado()));
    }
}
