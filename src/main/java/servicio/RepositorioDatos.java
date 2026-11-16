package servicio;

import estructuras.Pila;
import modelo.Billetera;
import modelo.Transaccion;
import modelo.Usuario;

import java.util.HashMap;

public class RepositorioDatos {

    private final HashMap<String, Usuario> usuarios = new HashMap<>();
    private final HashMap<String, Billetera> billeteras = new HashMap<>();
    private final HashMap<String, Transaccion> transacciones = new HashMap<>();
    private final HashMap<String, Pila<Transaccion>> reversibles = new HashMap<>();

    private int contadorTransacciones = 0;
    private double comisionesRecaudadas = 0;

    public HashMap<String, Usuario> getUsuarios() {
        return usuarios;
    }

    public HashMap<String, Billetera> getBilleteras() {
        return billeteras;
    }

    public HashMap<String, Transaccion> getTransacciones() {
        return transacciones;
    }

    // Una pila de operaciones reversibles por usuario (tope = la mas reciente)
    public HashMap<String, Pila<Transaccion>> getReversibles() {
        return reversibles;
    }

    public String nuevoIdTransaccion() {
        return "T" + (++contadorTransacciones);
    }

    public double getComisionesRecaudadas() {
        return comisionesRecaudadas;
    }

    public void sumarComision(double c) {
        comisionesRecaudadas += c;
    }

    public void restarComision(double c) {
        comisionesRecaudadas -= c;
    }
}