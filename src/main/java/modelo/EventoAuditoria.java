package modelo;

import java.time.LocalDateTime;
import java.util.List;

public class EventoAuditoria {

    private String id;
    private String idTransaccion;
    private String idUsuario;
    private List<ReglaRiesgo> reglas;
    private NivelRiesgo nivel;
    private String detalle;
    private LocalDateTime fecha;

    public EventoAuditoria(String id, String idTransaccion, String idUsuario,
                           List<ReglaRiesgo> reglas, NivelRiesgo nivel,
                           String detalle, LocalDateTime fecha) {
        this.id = id;
        this.idTransaccion = idTransaccion;
        this.idUsuario = idUsuario;
        this.reglas = reglas;
        this.nivel = nivel;
        this.detalle = detalle;
        this.fecha = fecha;
    }

    public String getId() { return id; }
    public String getIdTransaccion() { return idTransaccion; }
    public String getIdUsuario() { return idUsuario; }
    public List<ReglaRiesgo> getReglas() { return reglas; }
    public NivelRiesgo getNivel() { return nivel; }
    public String getDetalle() { return detalle; }
    public LocalDateTime getFecha() { return fecha; }

    @Override
    public String toString() {
        return id + " | " + idTransaccion + " | " + idUsuario + " | " + nivel + " | " + detalle;
    }
}