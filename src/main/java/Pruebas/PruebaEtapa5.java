package Pruebas;

import modelo.Canje;
import modelo.Notificacion;
import modelo.OperacionProgramada;
import modelo.Periodicidad;
import modelo.RegistroEjecucion;
import modelo.TipoBilletera;
import modelo.Usuario;
import servicio.*;

import java.time.Duration;
import java.time.LocalDateTime;

public class PruebaEtapa5 {

    public static void main(String[] args) {

        RepositorioDatos repo = new RepositorioDatos();
        NotificacionServicio notif = new NotificacionServicio();
        PuntosServicio puntos = new PuntosServicio(notif);
        UsuarioServicio usuarios = new UsuarioServicio(repo, puntos);
        BilleteraServicio billeteras = new BilleteraServicio(repo);
        TransaccionServicio tx = new TransaccionServicio(repo, puntos, notif);
        ProgramadaServicio prog = new ProgramadaServicio(repo, tx, puntos, notif);
        BeneficioServicio beneficios = new BeneficioServicio(repo, puntos, notif);

        Usuario ana = usuarios.registrar("U1", "Ana", "ana@correo.com");
        Usuario luis = usuarios.registrar("U2", "Luis", "luis@correo.com");

        billeteras.crear("U1", "Gastos", TipoBilletera.GASTOS_DIARIOS);   // B1
        billeteras.crear("U1", "Ahorro", TipoBilletera.AHORRO);           // B2
        billeteras.crear("U2", "Compras", TipoBilletera.COMPRAS);         // B3

        tx.recargar("B1", 1000);   // Ana: 10 pts

        LocalDateTime ahora = LocalDateTime.now();

        System.out.println("--- Programar ---");
        OperacionProgramada p1 = prog.programarTransferencia("U1", "B1", "B2", 100,
                ahora.plusDays(7), Periodicidad.SEMANAL, "Ahorro semanal");
        prog.programarTransferencia("U1", "B1", "B3", 50,
                ahora.plusDays(1), Periodicidad.UNICA, "Pago a Luis");
        prog.programarRecarga("U1", "B1", 200,
                ahora.plusDays(2), Periodicidad.UNICA, "Recarga automatica");
        prog.programarTransferencia("U1", "B1", "B2", 5000,
                ahora.plusDays(3), Periodicidad.UNICA, "Pago grande");
        OperacionProgramada p5 = prog.programarRecarga("U1", "B2", 10,
                ahora.plusDays(4), Periodicidad.UNICA, "Para cancelar");

        System.out.println("Pendientes: " + prog.cantidadPendientes());
        System.out.println("Orden de ejecucion:");
        for (OperacionProgramada op : prog.proximas(10)) {
            System.out.println("  " + op.getId() + " " + op.getConcepto());
        }

        prog.cancelar("U1", p5.getId());
        System.out.println("Tras cancelar P5: " + prog.cantidadPendientes() + " pendientes, estado " + p5.getEstado());

        System.out.println("--- Casos invalidos ---");
        probar(() -> prog.programarRecarga("U1", "B1", -5, ahora.plusDays(1), Periodicidad.UNICA, "x"));
        probar(() -> prog.programarRecarga("U1", "B1", 10, ahora.minusDays(1), Periodicidad.UNICA, "x"));
        probar(() -> prog.programarTransferencia("U2", "B1", "B3", 10, ahora.plusDays(1), Periodicidad.UNICA, "x"));
        probar(() -> prog.programarRecarga("U9", "B1", 10, ahora.plusDays(1), Periodicidad.UNICA, "x"));
        probar(() -> prog.cancelar("U2", "P1"));
        probar(() -> prog.cancelar("U1", "P5"));

        System.out.println("--- Simulacion (8 dias, paso de 1 dia, aviso 24 h) ---");
        for (RegistroEjecucion r : prog.simular(ahora, ahora.plusDays(8), Duration.ofDays(1), Duration.ofHours(24))) {
            System.out.println("  " + r);
        }

        System.out.println("B1=" + billeteras.consultarSaldo("B1") + " B2=" + billeteras.consultarSaldo("B2")
                + " B3=" + billeteras.consultarSaldo("B3"));
        System.out.println("Puntos Ana: " + ana.getPuntos() + " | Comisiones: " + repo.getComisionesRecaudadas());
        System.out.println("P1 (semanal): proxima " + (p1.getFechaEjecucion().isAfter(ahora.plusDays(13)) ? "en 14 dias" : "?")
                + ", ejecuciones " + p1.getEjecuciones() + ", estado " + p1.getEstado());
        System.out.println("Pendientes tras simular: " + prog.cantidadPendientes());

        System.out.println("Notificaciones de Ana:");
        for (Notificacion n : ana.getNotificaciones()) {
            System.out.println("  " + n.getTipo());
        }

        System.out.println("--- Prioridad por nivel (misma fecha) ---");
        billeteras.crear("U2", "Extra", TipoBilletera.AHORRO);   // B4
        tx.recargar("B3", 150000);                               // Luis: 1500 pts -> ORO
        LocalDateTime mismaFecha = ahora.plusDays(20);
        prog.programarRecarga("U1", "B1", 10, mismaFecha, Periodicidad.UNICA, "De Ana (BRONCE)");
        prog.programarRecarga("U2", "B3", 10, mismaFecha, Periodicidad.UNICA, "De Luis (ORO)");
        for (OperacionProgramada op : prog.proximas(3)) {
            System.out.println("  " + op.getId() + " " + op.getConcepto());
        }

        System.out.println("--- Beneficios ---");
        System.out.println("Catalogo: " + beneficios.catalogo().size());
        System.out.println("Disponibles Ana (" + ana.getPuntos() + " pts): " + beneficios.disponiblesPara("U1").size());
        System.out.println("Disponibles Luis (" + luis.getPuntos() + " pts): " + beneficios.disponiblesPara("U2").size());

        probar(() -> beneficios.canjear("U1", "BEN3"));    // nivel insuficiente
        probar(() -> beneficios.canjear("U1", "BEN1"));    // puntos insuficientes
        probar(() -> beneficios.canjear("U1", "BEN9"));    // no existe

        Canje c = beneficios.canjear("U2", "BEN3");
        System.out.println("Canje: " + c);
        System.out.println("Luis ahora: " + luis.getPuntos() + " pts, nivel " + luis.getNivel()
                + ", canjes " + luis.getCanjes().size());

        System.out.println("--- Despacho de notificaciones ---");
        System.out.println("Pendientes de despacho: " + notif.cantidadPendientes());
        System.out.println("Despachadas: " + notif.despacharTodas().size());
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