package Pruebas;

import modelo.EstadoTransaccion;
import modelo.EventoAuditoria;
import modelo.NivelRiesgo;
import modelo.Notificacion;
import modelo.TipoBilletera;
import modelo.TipoNotificacion;
import modelo.TipoTransaccion;
import modelo.Transaccion;
import modelo.Usuario;
import servicio.*;

import java.time.LocalDateTime;

public class PruebaEtapa7 {

    private static int seq = 0;
    private static RepositorioDatos repo = new RepositorioDatos();
    private static NotificacionServicio notif = new NotificacionServicio();
    private static PuntosServicio puntos = new PuntosServicio(notif);
    private static UsuarioServicio usuarios = new UsuarioServicio(repo, puntos);
    private static BilleteraServicio billeteras = new BilleteraServicio(repo);
    private static TransaccionServicio tx = new TransaccionServicio(repo, puntos, notif);
    private static RiesgoServicio riesgo = new RiesgoServicio(repo, notif);

    public static void main(String[] args) {

        tx.setRiesgoServicio(riesgo);

        Usuario ana = usuario("U1", "Ana");
        Usuario luis = usuario("U2", "Luis");
        Usuario marta = usuario("U3", "Marta");
        Usuario eva = usuario("U4", "Eva");
        Usuario pedro = usuario("U5", "Pedro");
        Usuario sara = usuario("U6", "Sara");
        Usuario juan = usuario("U7", "Juan");
        Usuario rosa = usuario("U8", "Rosa");
        Usuario tomas = usuario("U9", "Tomas");
        Usuario ximena = usuario("U10", "Ximena");

        String bAna = billetera(ana), bLuis = billetera(luis), bMarta = billetera(marta);
        String bEva1 = billetera(eva), bEva2 = billetera(eva), bEva3 = billetera(eva);
        String bPedro = billetera(pedro), bSara = billetera(sara), bJuan = billetera(juan);
        String bRosa = billetera(rosa), bTomas = billetera(tomas), bXimena = billetera(ximena);

        // Fecha fija: las reglas se miden con la fecha de cada transaccion, no con el reloj real
        LocalDateTime base = LocalDateTime.of(2026, 11, 16, 14, 0);

        System.out.println("--- 1. Comportamiento normal ---");
        for (int d = 6; d >= 1; d--) {
            sembrar(ana, mk(TipoTransaccion.RETIRO, 100, bAna, null, base.minusDays(d)));
        }
        mostrar("Retiro de 120 (promedio 100)", registrar(ana, mk(TipoTransaccion.RETIRO, 120, bAna, null, base)));

        System.out.println("--- 2. Rafaga de transferencias ---");
        mostrar("Transferencia 1", registrar(luis, mk(TipoTransaccion.TRANSFERENCIA, 100, bLuis, bMarta, base)));
        mostrar("Transferencia 2 (+30 s)", registrar(luis, mk(TipoTransaccion.TRANSFERENCIA, 100, bLuis, bMarta, base.plusSeconds(30))));
        mostrar("Transferencia 3 (+60 s)", registrar(luis, mk(TipoTransaccion.TRANSFERENCIA, 100, bLuis, bMarta, base.plusSeconds(60))));

        System.out.println("--- 3. Mismo destino en intervalos cortos (sin rafaga) ---");
        mostrar("Transferencia 1", registrar(pedro, mk(TipoTransaccion.TRANSFERENCIA, 100, bPedro, bMarta, base)));
        mostrar("Transferencia 2 (+4 min)", registrar(pedro, mk(TipoTransaccion.TRANSFERENCIA, 100, bPedro, bMarta, base.plusMinutes(4))));
        mostrar("Transferencia 3 (+8 min)", registrar(pedro, mk(TipoTransaccion.TRANSFERENCIA, 100, bPedro, bMarta, base.plusMinutes(8))));

        System.out.println("--- 4. Monto alto respecto al promedio del usuario ---");
        int[] minutos = {30, 40, 50, 60, 70};
        for (int m : minutos) {
            sembrar(sara, mk(TipoTransaccion.RETIRO, 100, bSara, null, base.minusMinutes(m)));
        }
        mostrar("Retiro de 1000 (promedio 100)", registrar(sara, mk(TipoTransaccion.RETIRO, 1000, bSara, null, base)));

        System.out.println("--- 5. Monto alto sin historial (umbral absoluto) ---");
        mostrar("Retiro de 60000", registrar(juan, mk(TipoTransaccion.RETIRO, 60000, bJuan, null, base)));

        System.out.println("--- 6. Fragmentacion entre billeteras ---");
        mostrar("Retiro 20000 desde billetera 1", registrar(eva, mk(TipoTransaccion.RETIRO, 20000, bEva1, null, base)));
        mostrar("Retiro 20000 desde billetera 2", registrar(eva, mk(TipoTransaccion.RETIRO, 20000, bEva2, null, base.plusMinutes(1))));
        mostrar("Retiro 20000 desde billetera 3", registrar(eva, mk(TipoTransaccion.RETIRO, 20000, bEva3, null, base.plusMinutes(2))));

        System.out.println("--- 7. Cambio brusco de frecuencia ---");
        for (int d = 5; d >= 2; d--) {
            sembrar(rosa, mk(TipoTransaccion.RECARGA, 100, null, bRosa, base.minusDays(d)));
        }
        int[] horas = {20, 15, 10, 5};
        for (int h : horas) {
            sembrar(rosa, mk(TipoTransaccion.RECARGA, 100, null, bRosa, base.minusHours(h)));
        }
        mostrar("Recarga (5ta operacion en 24 h)", registrar(rosa, mk(TipoTransaccion.RECARGA, 100, null, bRosa, base)));

        System.out.println("--- 8. Horario no habitual ---");
        LocalDateTime madrugada = LocalDateTime.of(2026, 11, 16, 3, 0);
        for (int d = 6; d >= 1; d--) {
            sembrar(tomas, mk(TipoTransaccion.RECARGA, 100, null, bTomas, madrugada.minusDays(d).withHour(10)));
        }
        mostrar("Retiro de 50 a las 03:00 (habitual 10:00)", registrar(tomas, mk(TipoTransaccion.RETIRO, 50, bTomas, null, madrugada)));

        System.out.println("--- 9. Flujo completo con el servicio de transacciones ---");
        tx.recargar(bXimena, 5000);
        Transaccion t1 = tx.transferir(bXimena, bMarta, 100);
        Transaccion t2 = tx.transferir(bXimena, bMarta, 100);
        Transaccion t3 = tx.transferir(bXimena, bMarta, 100);
        System.out.println("Riesgo de las transferencias: " + t1.getRiesgo() + ", " + t2.getRiesgo() + ", " + t3.getRiesgo());

        int alertas = 0;
        for (Notificacion n : ximena.getNotificaciones()) {
            if (n.getTipo() == TipoNotificacion.ALERTA_SEGURIDAD) {
                alertas++;
            }
        }
        System.out.println("Alertas de seguridad de Ximena: " + alertas);

        System.out.println("--- Consultas de auditoria ---");
        System.out.println("Total de eventos: " + riesgo.getAuditoria().size());
        System.out.println("Nivel ALTO: " + riesgo.eventosPorNivel(NivelRiesgo.ALTO).size());
        System.out.println("Nivel MEDIO: " + riesgo.eventosPorNivel(NivelRiesgo.MEDIO).size());
        System.out.println("Nivel BAJO: " + riesgo.eventosPorNivel(NivelRiesgo.BAJO).size());
        System.out.println("Eventos de Ximena: " + riesgo.eventosDeUsuario("U10").size());
        System.out.println("Eventos de Ana: " + riesgo.eventosDeUsuario("U1").size());
        System.out.println("Eventos de " + t3.getId() + ": " + riesgo.eventosDeTransaccion(t3.getId()).size());
        System.out.println("Por regla: " + riesgo.contarPorRegla());
    }

    private static Usuario usuario(String id, String nombre) {
        return usuarios.registrar(id, nombre, nombre.toLowerCase() + "@correo.com");
    }

    private static String billetera(Usuario u) {
        return billeteras.crear(u.getId(), "Principal", TipoBilletera.GASTOS_DIARIOS).getCodigo();
    }

    private static Transaccion mk(TipoTransaccion tipo, double valor, String origen, String destino, LocalDateTime fecha) {
        return new Transaccion("X" + (++seq), fecha, tipo, valor, origen, destino, EstadoTransaccion.EJECUTADA, 0);
    }

    // Historial previo: se agrega sin evaluar
    private static void sembrar(Usuario u, Transaccion t) {
        u.getHistorial().add(t);
    }

    // Igual que el servicio de transacciones: queda en el historial y luego se evalua
    private static EventoAuditoria registrar(Usuario u, Transaccion t) {
        u.getHistorial().add(t);
        return riesgo.evaluar(t, u);
    }

    private static void mostrar(String etiqueta, EventoAuditoria e) {
        System.out.println(etiqueta + " -> " + (e == null ? "sin alerta" : e.getNivel() + " " + e.getReglas()));
    }
}