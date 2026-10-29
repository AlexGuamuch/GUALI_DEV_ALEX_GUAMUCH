package edu.umg.guali.lecciones.controlador;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.umg.guali.lecciones.dto.Dtos.IntentoDto;
import edu.umg.guali.lecciones.dto.Dtos.NuevoIntento;
import edu.umg.guali.lecciones.dto.Dtos.PreferenciaDto;
import edu.umg.guali.lecciones.dto.Dtos.RespuestaCodigo;
import edu.umg.guali.lecciones.dto.Dtos.SolicitudCodigo;
import edu.umg.guali.lecciones.servicio.CodigoService;
import edu.umg.guali.lecciones.servicio.EvaluadorObjetivo.Accion;
import edu.umg.guali.lecciones.servicio.IntentoService;
import edu.umg.guali.lecciones.servicio.PreferenciaService;
import edu.umg.guali.seguridad.ClienteIdResolver;

/**
 * Funciones del estudiante (sin cuenta). Cada navegador se identifica con el id anónimo
 * que asigna el gateway, para guardar su historial y sus preferencias.
 */
@RestController
@RequestMapping("/api")
public class EstudianteController {

    private final IntentoService intentos;
    private final CodigoService codigo;
    private final PreferenciaService preferencias;
    private final ClienteIdResolver clientes;

    public EstudianteController(IntentoService intentos, CodigoService codigo, PreferenciaService preferencias,
                                ClienteIdResolver clientes) {
        this.intentos = intentos;
        this.codigo = codigo;
        this.preferencias = preferencias;
        this.clientes = clientes;
    }

    @GetMapping("/intentos")
    public List<IntentoDto> historial(HttpServletRequest request) {
        return intentos.historial(clientes.resolver(request));
    }

    @PostMapping("/intentos")
    public IntentoDto iniciar(@Valid @RequestBody NuevoIntento cuerpo, HttpServletRequest request) {
        return intentos.iniciar(clientes.resolver(request), cuerpo.leccionId());
    }

    @PostMapping("/intentos/{id}/finalizar")
    public IntentoDto finalizar(@PathVariable long id, HttpServletRequest request) {
        return intentos.finalizar(clientes.resolver(request), id);
    }

    @PostMapping("/codigo/compilar")
    public RespuestaCodigo compilar(@Valid @RequestBody SolicitudCodigo cuerpo, HttpServletRequest request) {
        return codigo.procesar(clientes.resolver(request), cuerpo, Accion.COMPILAR);
    }

    @PostMapping("/codigo/ejecutar")
    public RespuestaCodigo ejecutar(@Valid @RequestBody SolicitudCodigo cuerpo, HttpServletRequest request) {
        return codigo.procesar(clientes.resolver(request), cuerpo, Accion.EJECUTAR);
    }

    @GetMapping("/preferencias")
    public ResponseEntity<PreferenciaDto> preferencias(HttpServletRequest request) {
        return preferencias.obtener(clientes.resolver(request))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/preferencias")
    public PreferenciaDto guardarPreferencias(@Valid @RequestBody PreferenciaDto cuerpo, HttpServletRequest request) {
        return preferencias.guardar(clientes.resolver(request), cuerpo);
    }
}
