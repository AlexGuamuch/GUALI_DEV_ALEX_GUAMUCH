package edu.umg.guali.compilador;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param jdkHome                 ruta que se entrega a guali-dev (en Docker: envoltorios con límites)
 * @param tiempoLimiteSegundos    tiempo máximo de ejecución del programa del estudiante
 * @param esperaMaximaSegundos    cuánto puede esperar una solicitud en la fila antes de responder "ocupado"
 * @param salidaMaximaCaracteres  recorte de la salida para no saturar la red ni el navegador
 * @param aislado                 true solo dentro del contenedor: después de cada ejecución se terminan
 *                                todos los procesos que haya dejado el programa y se limpia /tmp
 */
@ConfigurationProperties(prefix = "guali.compilador")
public record CompiladorProperties(String jdkHome, int tiempoLimiteSegundos, int esperaMaximaSegundos,
                                   int salidaMaximaCaracteres, boolean aislado) {
}
