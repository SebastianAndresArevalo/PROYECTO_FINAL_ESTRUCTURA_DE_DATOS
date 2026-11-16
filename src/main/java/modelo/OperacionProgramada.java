package modelo;

import java.time.LocalDateTime;

public class OperacionProgramada {

    private String id;
    private String idUsuario;
    private TipoTransaccion tipo;          // RECARGA o TRANSFERENCIA
    private String billeteraOrigen;        // null en recargas
    private String billeteraDestino;
    private double valor;
    private LocalDateTime fechaEjecucion;
    private Periodicidad periodicidad;
    private String concepto;
    private EstadoProgramada estado;
    private boolean avisada;
    private int prioridadNivel;            // nivel del usuario al programar (desempata)
    private int secuencia;                 // orden de creacion (ultimo desempate)
    private int ejecuciones;

    public OperacionProgramada(String id, String idUsuario, TipoTransaccion tipo,
                               String billeteraOrigen, String billeteraDestino, double valor,
                               LocalDateTime fechaEjecucion, Periodicidad periodicidad,
                               String concepto, int prioridadNivel, int secuencia) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.tipo = tipo;
        this.billeteraOrigen = billeteraOrigen;
        this.billeteraDestino = billeteraDestino;
        this.valor = valor;
        this.fechaEjecucion = fechaEjecucion;
        this.periodicidad = periodicidad;
        this.concepto = concepto;
        this.prioridadNivel = prioridadNivel;
        this.secuencia = secuencia;
        this.estado = EstadoProgramada.PENDIENTE;
        this.avisada = false;
        this.ejecuciones = 0;
    }

    public String getId() { return id; }
    public String getIdUsuario() { return idUsuario; }
    public TipoTransaccion getTipo() { return tipo; }
    public String getBilleteraOrigen() { return billeteraOrigen; }
    public String getBilleteraDestino() { return billeteraDestino; }
    public double getValor() { return valor; }
    public LocalDateTime getFechaEjecucion() { return fechaEjecucion; }
    public Periodicidad getPeriodicidad() { return periodicidad; }
    public String getConcepto() { return concepto; }
    public EstadoProgramada getEstado() { return estado; }
    public boolean isAvisada() { return avisada; }
    public int getPrioridadNivel() { return prioridadNivel; }
    public int getSecuencia() { return secuencia; }
    public int getEjecuciones() { return ejecuciones; }

    public void setFechaEjecucion(LocalDateTime f) { this.fechaEjecucion = f; }
    public void setEstado(EstadoProgramada e) { this.estado = e; }
    public void setAvisada(boolean a) { this.avisada = a; }
    public void setPrioridadNivel(int p) { this.prioridadNivel = p; }
    public void contarEjecucion() { this.ejecuciones++; }

    @Override
    public String toString() {
        return id + " | " + concepto + " | " + tipo + " | " + valor + " | "
                + billeteraOrigen + " -> " + billeteraDestino + " | " + fechaEjecucion
                + " | " + periodicidad + " | " + estado;
    }
}