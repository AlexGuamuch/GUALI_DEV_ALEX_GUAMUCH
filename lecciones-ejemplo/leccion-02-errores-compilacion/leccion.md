# Errores de compilación

El compilador revisa tu código antes de ejecutarlo. Si algo no sigue las reglas de Java, se detiene y te dice **en qué línea** y **por qué**.

Escribe este programa tal como está. Le falta algo a propósito:

```java
void main() {
    IO.println("Voy a provocar un error")
}
```

## Cómo leer el error

La consola mostrará algo así:

```
app25.java:2: error: ';' expected
```

- `app25.java` es el archivo.
- `2` es la línea donde el compilador encontró el problema.
- `';' expected` es el motivo: esperaba un punto y coma.

## Tu reto

Compila y consigue que el compilador reporte al menos un error. Luego corrígelo y compila de nuevo.
