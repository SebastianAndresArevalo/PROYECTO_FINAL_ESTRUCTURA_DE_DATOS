package modelo;

import java.util.ArrayList;
import java.util.List;

public class Billetera {

    private String codigo;
    private String nombre;
    private TipoBilletera tipo;
    private double saldo;
    private EstadoBilletera estado;
    private String idUsuario;
    private List<Transaccion> historial;

    public Billetera(String codigo, String nombre, TipoBilletera tipo, String idUsuario) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.tipo = tipo;
        this.idUsuario = idUsuario;
        this.saldo = 0;
        this.estado = EstadoBilletera.ACTIVA;
        this.historial = new ArrayList<>();
    }

    public boolean depositar(double valor) {
        if (valor <= 0 || estado != EstadoBilletera.ACTIVA) {
            return false;
        }
        saldo += valor;
        return true;
    }

    public boolean retirar(double valor) {
        if (valor <= 0 || estado != EstadoBilletera.ACTIVA || valor > saldo) {
            return false;
        }
        saldo -= valor;
        return true;
    }

    public void agregarTransaccion(Transaccion t) {
        historial.add(t);
    }

    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public TipoBilletera getTipo() { return tipo; }
    public double getSaldo() { return saldo; }
    public EstadoBilletera getEstado() { return estado; }
    public String getIdUsuario() { return idUsuario; }
    public List<Transaccion> getHistorial() { return historial; }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setEstado(EstadoBilletera estado) { this.estado = estado; }

    @Override
    public String toString() {
        return codigo + " - " + nombre + " (" + tipo + ") $" + saldo;
    }
}
