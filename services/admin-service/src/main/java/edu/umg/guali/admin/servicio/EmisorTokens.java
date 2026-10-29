package edu.umg.guali.admin.servicio;

import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseCookie;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import edu.umg.guali.admin.modelo.Usuario;
import edu.umg.guali.seguridad.SeguridadProperties;

/**
 * Firma los JWT de sesión con la llave privada RSA (solo existe en este servicio).
 * Los demás servicios solo tienen la llave pública: pueden validar tokens, pero no fabricarlos.
 */
@Component
public class EmisorTokens {

    private final JwtEncoder encoder;
    private final SeguridadProperties props;
    private final Clock reloj;

    public EmisorTokens(SeguridadProperties props, Clock reloj,
                        @Value("${guali.seguridad.clave-privada}") Resource clavePrivada) throws IOException {
        this.props = props;
        this.reloj = reloj;
        RSAPrivateKey privada;
        RSAPublicKey publica;
        try (InputStream in = clavePrivada.getInputStream()) {
            privada = RsaKeyConverters.pkcs8().convert(in);
        }
        try (InputStream in = props.clavePublica().getInputStream()) {
            publica = RsaKeyConverters.x509().convert(in);
        }
        RSAKey jwk = new RSAKey.Builder(publica).privateKey(privada).keyID("guali-1").build();
        this.encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    }

    public Token emitir(Usuario usuario) {
        Instant ahora = reloj.instant();
        Instant expira = ahora.plus(props.duracion());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.emisor())
                .subject(usuario.getUsername())
                .issuedAt(ahora)
                .expiresAt(expira)
                .id(UUID.randomUUID().toString())
                .claim("name", usuario.getNombreCompleto())
                .claim("roles", List.of("ADMIN"))
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId("guali-1").build();
        String valor = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new Token(valor, expira);
    }

    /** Cookie HttpOnly (inaccesible para JavaScript), SameSite=Strict y Secure en producción. */
    public ResponseCookie cookie(Token token) {
        return ResponseCookie.from(props.nombreCookie(), token.valor())
                .httpOnly(true)
                .secure(props.cookieSegura())
                .sameSite("Strict")
                .path("/")
                .maxAge(props.duracion())
                .build();
    }

    public ResponseCookie cookieVacia() {
        return ResponseCookie.from(props.nombreCookie(), "")
                .httpOnly(true)
                .secure(props.cookieSegura())
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
    }

    public record Token(String valor, Instant expiraEn) {
    }
}
