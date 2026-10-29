package edu.umg.guali.lecciones.servicio;

import java.time.Clock;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.umg.guali.lecciones.dto.Dtos.PreferenciaDto;
import edu.umg.guali.lecciones.modelo.Preferencia;
import edu.umg.guali.lecciones.repositorio.PreferenciaRepository;

/** Configuración de apariencia por navegador, persistida en la base de datos. */
@Service
public class PreferenciaService {

    private final PreferenciaRepository preferencias;
    private final Clock reloj;

    public PreferenciaService(PreferenciaRepository preferencias, Clock reloj) {
        this.preferencias = preferencias;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Optional<PreferenciaDto> obtener(String clienteId) {
        return preferencias.findById(clienteId).map(p -> new PreferenciaDto(p.getTema(), p.getTamanoFuente()));
    }

    @Transactional
    public PreferenciaDto guardar(String clienteId, PreferenciaDto dto) {
        Preferencia p = preferencias.findById(clienteId).orElseGet(() -> new Preferencia(clienteId));
        p.actualizar(dto.tema(), dto.tamanoFuente(), reloj.instant());
        preferencias.save(p);
        return dto;
    }
}
