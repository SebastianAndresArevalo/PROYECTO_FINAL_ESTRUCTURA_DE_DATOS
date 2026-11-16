package modelo;

import java.time.LocalDateTime;

public class Transaccion {

    private String id;
    private LocalDateTime fecha;
    private TipoTransaccion tipo;
    private double valor;
    private String billeteraOrigen;
    private String billeteraDestino;
    private EstadoTransaccion estado;
    private int puntos;
    private NivelRiesgo riesgo;
    private double comision;
    private String referencia;   // id de la transaccion revertida (solo en reversiones)

    public Transaccion(String id, LocalDateTime fecha, TipoTransaccion tipo, double valor,
                       String billeteraOrigen, String billeteraDestino,
                       EstadoTransaccion estado, int puntos) {
        this.id = id;
        this.fecha = fecha;
        this.tipo = tipo;
        this.valor = valor;
        this.billeteraOrigen = billeteraOrigen;
        this.billeteraDestino = billeteraDestino;
        this.estado = estado;
        this.puntos = puntos;
        this.riesgo = NivelRiesgo.NINGUNO;
        this.comision = 0;
    }

    public String getId() { return id; }
    public LocalDateTime getFecha() { return fecha; }
    public TipoTransaccion getTipo() { return tipo; }
    public double getValor() { return valor; }
    public String getBilleteraOrigen() { return billeteraOrigen; }
    public String getBilleteraDestino() { return billeteraDestino; }
    public EstadoTransaccion getEstado() { return estado; }
    public int getPuntos() { return puntos; }
    public NivelRiesgo getRiesgo() { return riesgo; }
    public double getComision() { return comision; }
    public String getReferencia() { return referencia; }

    public void setEstado(EstadoTransaccion estado) { this.estado = estado; }
    public void setPuntos(int puntos) { this.puntos = puntos; }
    public void setRiesgo(NivelRiesgo riesgo) { this.riesgo = riesgo; }
    public void setComision(double comision) { this.comision = comision; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    @Override
    public String toString() {
        return id + " | " + tipo + " | " + valor + " | " + billeteraOrigen
                + " -> " + billeteraDestino + " | " + estado + " | " + puntos + " pts";
    }
}