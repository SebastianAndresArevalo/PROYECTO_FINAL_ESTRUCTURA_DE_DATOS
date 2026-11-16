package Pruebas;

import modelo.Notificacion;
import modelo.TipoBilletera;
import modelo.Transaccion;
import modelo.Usuario;
import servicio.*;

public class PruebaEtapa4 {

    public static void main(String[] args) {

        RepositorioDatos repo = new RepositorioDatos();
        NotificacionServicio notif = new NotificacionServicio();
        PuntosServicio puntos = new PuntosServicio(notif);
        UsuarioServicio usuarios = new UsuarioServicio(repo, puntos);
        BilleteraServicio billeteras = new BilleteraServicio(repo);
        TransaccionServicio tx = new TransaccionServicio(repo, puntos, notif);
        ReversionServicio rev = new ReversionServicio(repo, puntos, notif);

        Usuario ana = usuarios.registrar("U1", "Ana", "ana@correo.com");
        Usuario luis = usuarios.registrar("U2", "Luis", "luis@correo.com");
        Usuario marta = usuarios.registrar("U3", "Marta", "marta@correo.com");

        billeteras.crear("U1", "Gastos", TipoBilletera.GASTOS_DIARIOS);   // B1
        billeteras.crear("U1", "Ahorro", TipoBilletera.AHORRO);           // B2
        billeteras.crear("U2", "Compras", TipoBilletera.COMPRAS);         // B3
        billeteras.crear("U3", "Transporte", TipoBilletera.TRANSPORTE);   // B4

        tx.recargar("B1", 1000);            // T1
        tx.transferir("B1", "B2", 300);     // T2 (propia, sin comision)
        tx.transferir("B1", "B3", 200);     // T3 (a otro usuario, comision 4)

        System.out.println("--- Antes de revertir ---");
        estado(billeteras, repo, ana);

        System.out.println("--- Revertir T2 (transferencia propia) ---");
        Transaccion r1 = rev.revertir("U1", "T2");
        System.out.println(r1);
        System.out.println("Estado de T2: " + repo.getTransacciones().get("T2").getEstado());
        estado(billeteras, repo, ana);

        System.out.println("--- Deshacer ultima (T3, transferencia a otro usuario) ---");
        Transaccion r2 = rev.deshacerUltima("U1");
        System.out.println(r2);
        estado(billeteras, repo, ana);

        System.out.println("--- Casos invalidos ---");
        probar(() -> rev.revertir("U1", "T2"));      // ya fue revertida y salio de la pila

        System.out.println("--- Luis: saldo bajo y reversion rechazada ---");
        tx.recargar("B3", 500);                      // T6
        tx.retirar("B3", 400);                       // T7 (comision 8)
        System.out.println("B3 de Luis: " + billeteras.consultarSaldo("B3"));
        probar(() -> rev.deshacerUltima("U2"));      // B3 no tiene saldo para devolver la recarga

        System.out.println("--- Limite diario (Bronce: 10 por dia) ---");
        for (int i = 0; i < 10; i++) {
            tx.recargar("B4", 100);
        }
        probar(() -> tx.recargar("B4", 100));
        System.out.println("Marta: " + marta.getPuntos() + " pts");

        System.out.println("--- Notificaciones ---");
        System.out.println("Ana: " + ana.getNotificaciones().size()
                + " | Luis: " + luis.getNotificaciones().size()
                + " | Marta: " + marta.getNotificaciones().size());
        System.out.println("Notificaciones de Luis:");
        for (Notificacion n : luis.getNotificaciones()) {
            System.out.println("  " + n);
        }
        System.out.println("Pendientes de despacho: " + notif.cantidadPendientes());
        System.out.println("Primera despachada: " + notif.despachar());
        System.out.println("Despachadas despues: " + notif.despacharTodas().size()
                + " | Pendientes: " + notif.cantidadPendientes());
    }

    private static void estado(BilleteraServicio b, RepositorioDatos repo, Usuario u) {
        System.out.println("B1=" + b.consultarSaldo("B1") + " B2=" + b.consultarSaldo("B2")
                + " B3=" + b.consultarSaldo("B3")
                + " | Puntos " + u.getNombre() + ": " + u.getPuntos()
                + " | Comisiones: " + repo.getComisionesRecaudadas());
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