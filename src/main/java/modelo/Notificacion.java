package modelo;

import java.time.LocalDateTime;

public class Notificacion {

    private String idUsuario;
    private TipoNotificacion tipo;
    private String mensaje;
    private LocalDateTime fecha;
    private boolean enviada;

    public Notificacion(String idUsuario, TipoNotificacion tipo, String mensaje) {
        this.idUsuario = idUsuario;
        this.tipo = tipo;
        this.mensaje = mensaje;
        this.fecha = LocalDateTime.now();
        this.enviada = false;
    }

    public String getIdUsuario() { return idUsuario; }
    public TipoNotificacion getTipo() { return tipo; }
    public String getMensaje() { return mensaje; }
    public LocalDateTime getFecha() { return fecha; }
    public boolean isEnviada() { return enviada; }

    public void setEnviada(boolean enviada) { this.enviada = enviada; }

    @Override
    public String toString() {
        return "[" + tipo + "] " + mensaje;
    }
}