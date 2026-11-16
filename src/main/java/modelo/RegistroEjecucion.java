package modelo;

import java.time.LocalDateTime;

public class RegistroEjecucion {

    private String idProgramada;
    private LocalDateTime fechaProgramada;
    private LocalDateTime fechaEjecucion;
    private boolean exitosa;
    private String mensaje;
    private String idTransaccion;

    public RegistroEjecucion(String idProgramada, LocalDateTime fechaProgramada,
                             LocalDateTime fechaEjecucion, boolean exitosa,
                             String mensaje, String idTransaccion) {
        this.idProgramada = idProgramada;
        this.fechaProgramada = fechaProgramada;
        this.fechaEjecucion = fechaEjecucion;
        this.exitosa = exitosa;
        this.mensaje = mensaje;
        this.idTransaccion = idTransaccion;
    }

    public String getIdProgramada() { return idProgramada; }
    public LocalDateTime getFechaProgramada() { return fechaProgramada; }
    public LocalDateTime getFechaEjecucion() { return fechaEjecucion; }
    public boolean isExitosa() { return exitosa; }
    public String getMensaje() { return mensaje; }
    public String getIdTransaccion() { return idTransaccion; }

    @Override
    public String toString() {
        return idProgramada + " | " + (exitosa ? "OK" : "FALLO") + " | " + mensaje
                + (idTransaccion != null ? " | " + idTransaccion : "");
    }
}