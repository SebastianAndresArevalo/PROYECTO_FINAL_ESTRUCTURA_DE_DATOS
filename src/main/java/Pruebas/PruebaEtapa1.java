package Pruebas;

import estructuras.Cola;
import estructuras.Pila;
import modelo.*;

import java.time.LocalDateTime;

public class PruebaEtapa1 {

    public static void main(String[] args) {

        Usuario u = new Usuario("U1", "Ana", "ana@correo.com");
        Billetera b = new Billetera("B1", "Gastos", TipoBilletera.GASTOS_DIARIOS, u.getId());
        u.agregarBilletera(b);

        b.depositar(1000);
        b.retirar(300);
        System.out.println("Saldo: " + b.getSaldo());   // 700.0
        System.out.println("Retiro excesivo: " + b.retirar(5000)); // false

        boolean asciende = u.sumarPuntos(600);
        System.out.println(u + " asciende: " + asciende); // PLATA, true

        Pila<Transaccion> reversibles = new Pila<>();
        reversibles.push(new Transaccion("T1", LocalDateTime.now(), TipoTransaccion.RECARGA,
                1000, null, "B1", EstadoTransaccion.EJECUTADA, 10));
        reversibles.push(new Transaccion("T2", LocalDateTime.now(), TipoTransaccion.RETIRO,
                300, "B1", null, EstadoTransaccion.EJECUTADA, 6));
        reversibles.imprimir();

        Cola<Notificacion> pendientes = new Cola<>();
        pendientes.encolar(new Notificacion("U1", TipoNotificacion.SALDO_BAJO, "Saldo bajo en B1"));
        pendientes.imprimir();
    }
}