# Errores en ejecución

Que un programa compile no significa que funcione. Algunos errores solo aparecen cuando el programa se está ejecutando.

```java
void main() {
    int manzanas = 10;
    int personas = 0;
    int porPersona = manzanas / personas;
    IO.println(porPersona);
}
```

## Qué va a pasar

El compilador no ve ningún problema, pero al ejecutar Java lanza una `ArithmeticException` porque no se puede dividir un entero entre cero.

## Tu reto

Compila sin errores y ejecuta el programa hasta ver el error de ejecución en la consola.
