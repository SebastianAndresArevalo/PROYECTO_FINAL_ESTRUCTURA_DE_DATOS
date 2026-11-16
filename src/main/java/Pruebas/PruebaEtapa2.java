package Pruebas;

import modelo.TipoBilletera;
import modelo.Usuario;
import servicio.*;

public class PruebaEtapa2 {


    private static RepositorioDatos repo;

    public static void main(String[] args) {

        NotificacionServicio notif = new NotificacionServicio();
        PuntosServicio puntos = new PuntosServicio(notif);
        UsuarioServicio usuarios = new UsuarioServicio(repo, puntos);
        BilleteraServicio billeteras = new BilleteraServicio(repo);
        TransaccionServicio transacciones = new TransaccionServicio(repo, puntos, notif);

        Usuario ana = usuarios.registrar("U1", "Ana", "ana@correo.com");
        Usuario luis = usuarios.registrar("U2", "Luis", "luis@correo.com");

        billeteras.crear("U1", "Gastos", TipoBilletera.GASTOS_DIARIOS);   // B1
        billeteras.crear("U1", "Ahorro", TipoBilletera.AHORRO);           // B2
        billeteras.crear("U2", "Compras", TipoBilletera.COMPRAS);         // B3

        transacciones.recargar("B1", 1000);
        transacciones.retirar("B1", 200);
        transacciones.transferir("B1", "B2", 300);
        transacciones.transferir("B1", "B3", 100);

        System.out.println("Saldo B1: " + billeteras.consultarSaldo("B1"));
        System.out.println("Saldo B2: " + billeteras.consultarSaldo("B2"));
        System.out.println("Saldo B3: " + billeteras.consultarSaldo("B3"));
        System.out.println("Puntos Ana: " + ana.getPuntos());
        System.out.println("Puntos Luis: " + luis.getPuntos());
        System.out.println("Historial B1: " + transacciones.historialDeBilletera("B1").size() + " movimientos");
        System.out.println("Historial Luis: " + transacciones.historialDeUsuario("U2").size() + " movimientos");
        System.out.println("Buscar usuario inexistente: " + usuarios.buscar("X"));

        System.out.println("--- Casos invalidos ---");
        probar(() -> transacciones.retirar("B1", 5000));
        probar(() -> transacciones.transferir("B1", "B1", 50));
        probar(() -> transacciones.recargar("B1", -5));
        probar(() -> transacciones.recargar("B99", 100));
        probar(() -> usuarios.registrar("U1", "Otro", "otro@correo.com"));
        probar(() -> usuarios.eliminar("U1"));
    }

    private static void probar(Runnable accion) {
        try {
            accion.run();
            System.out.println("ERROR: debio fallar");
        } catch (OperacionException e) {
            System.out.println("Error esperado: " + e.getMessage());
        }
    }
}
