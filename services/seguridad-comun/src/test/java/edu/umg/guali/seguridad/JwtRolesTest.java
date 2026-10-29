package edu.umg.guali.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtRolesTest {

    @Test
    void elClaimRolesSeConvierteEnRoleAdmin() {
        Jwt jwt = Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .subject("aguamuch")
                .claim("roles", List.of("ADMIN"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        var auth = new SeguridadComunAutoConfiguration().jwtAuthenticationConverter().convert(jwt);

        assertThat(auth.getName()).isEqualTo("aguamuch");
        // Spring Security 7 agrega también FACTOR_BEARER (factor de autenticación usado).
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).contains("ROLE_ADMIN");
    }
}
