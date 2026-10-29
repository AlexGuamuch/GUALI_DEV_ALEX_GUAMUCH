package edu.umg.guali.seguridad;

import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPublicKey;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

/**
 * Beans de seguridad que cada microservicio recibe automáticamente al depender de esta librería.
 * Cada servicio define su propia SecurityFilterChain con las reglas de acceso de sus endpoints.
 */
@AutoConfiguration
@EnableConfigurationProperties(SeguridadProperties.class)
public class SeguridadComunAutoConfiguration {

    /** Valida firma RSA, vigencia y emisor del JWT. Solo admin-service tiene la llave privada. */
    @Bean
    @ConditionalOnMissingBean
    JwtDecoder jwtDecoder(SeguridadProperties props) throws IOException {
        if (props.clavePublica() == null || !props.clavePublica().exists()) {
            throw new IllegalStateException("Falta guali.seguridad.clave-publica (PEM de la llave pública RSA)");
        }
        RSAPublicKey key;
        try (InputStream in = props.clavePublica().getInputStream()) {
            key = RsaKeyConverters.x509().convert(in);
        }
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(key).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(props.emisor())));
        return decoder;
    }

    @Bean
    @ConditionalOnMissingBean
    BearerTokenResolver bearerTokenResolver(SeguridadProperties props) {
        return new CookieBearerTokenResolver(props.nombreCookie());
    }

    /** Convierte el claim "roles" en autoridades ROLE_* (Spring Security busca un bean de este tipo). */
    @Bean
    @ConditionalOnMissingBean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

    @Bean
    @ConditionalOnMissingBean
    ErroresSeguridadJson erroresSeguridadJson() {
        return new ErroresSeguridadJson();
    }

    @Bean
    @ConditionalOnMissingBean
    ClienteIdResolver clienteIdResolver() {
        return new ClienteIdResolver();
    }
}
