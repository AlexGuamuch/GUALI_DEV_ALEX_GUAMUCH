package edu.umg.guali.admin.servicio;

import java.util.Locale;

import org.springframework.http.HttpStatus;

/** Requisitos mínimos de contraseña para administradores. */
public final class PoliticaContrasenas {

    public static final String DESCRIPCION = "La contraseña debe tener al menos 12 caracteres, "
            + "con mayúscula, minúscula y número, y no puede contener el usuario.";

    private PoliticaContrasenas() {
    }

    public static void validar(String password, String username) {
        boolean valida = password != null
                && password.length() >= 12
                && password.length() <= 128
                && password.chars().anyMatch(Character::isUpperCase)
                && password.chars().anyMatch(Character::isLowerCase)
                && password.chars().anyMatch(Character::isDigit)
                && (username == null || !password.toLowerCase(Locale.ROOT).contains(username.toLowerCase(Locale.ROOT)));
        if (!valida) throw new ReglaException(HttpStatus.BAD_REQUEST, DESCRIPCION);
    }
}
