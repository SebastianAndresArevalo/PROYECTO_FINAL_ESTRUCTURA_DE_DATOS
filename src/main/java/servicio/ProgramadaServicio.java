package servicio;

import modelo.Billetera;
import modelo.EstadoProgramada;
import modelo.OperacionProgramada;
import modelo.Periodicidad;
import modelo.RegistroEjecucion;
import modelo.TipoNotificacion;
import modelo.TipoTransaccion;
import modelo.Transaccion;
import modelo.Usuario;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

public class ProgramadaServicio {

    public static final int BONO_PAGO_PROGRAMADO = 5;
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final RepositorioDatos repo;
    private final TransaccionServicio transacciones;
    private final PuntosServicio puntos;
    private final NotificacionServicio notificaciones;

    // Orden: fecha y hora, luego nivel del usuario (mayor primero), luego orden de creacion
    private final Comparator<OperacionProgramada> orden = Comparator
            .comparing(OperacionProgramada::getFechaEjecucion)
            .thenComparing(Comparator.<OperacionProgramada>comparingInt(
                    OperacionProgramada::getPrioridadNivel).reversed())
            .thenComparingInt(OperacionProgramada::getSecuencia);

    private final PriorityQueue<OperacionProgramada> cola = new PriorityQueue<>(orden);
    private final HashMap<String, OperacionProgramada> porId = new HashMap<>();
    private final List<RegistroEjecucion> registros = new ArrayList<>();
    private int contador = 0;

    public ProgramadaServicio(RepositorioDatos repo, TransaccionServicio transacciones,
                              PuntosServicio puntos, NotificacionServicio notificaciones) {
        this.repo = repo;
        this.transacciones = transacciones;
        this.puntos = puntos;
        this.notificaciones = notificaciones;
    }

    // ---------- Programar ----------

    public OperacionProgramada programarRecarga(String idUsuario, String codigoDestino, double valor,
                                                LocalDateTime fecha, Periodicidad periodicidad,
                                                String concepto) {
        return crear(idUsuario, TipoTransaccion.RECARGA, null, codigoDestino, valor,
                fecha, periodicidad, concepto);
    }

    public OperacionProgramada programarTransferencia(String idUsuario, String codigoOrigen,
                                                      String codigoDestino, double valor,
                                                      LocalDateTime fecha, Periodicidad periodicidad,
                                                      String concepto) {
        return crear(idUsuario, TipoTransaccion.TRANSFERENCIA, codigoOrigen, codigoDestino, valor,
                fecha, periodicidad, concepto);
    }

    private OperacionProgramada crear(String idUsuario, TipoTransaccion tipo, String origen,
                                      String destino, double valor, LocalDateTime fecha,
                                      Periodicidad periodicidad, String concepto) {
        Usuario u = repo.getUsuarios().get(idUsuario);
        if (u == null) {
            throw new OperacionException("No existe el usuario " + idUsuario);
        }
        if (Double.isNaN(valor) || Double.isInfinite(valor) || valor <= 0) {
            throw new OperacionException("El valor debe ser mayor a cero");
        }
        if (fecha == null || fecha.isBefore(LocalDateTime.now())) {
            throw new OperacionException("La fecha de ejecucion debe ser futura");
        }

        Billetera bDestino = repo.getBilleteras().get(destino);
        if (bDestino == null) {
            throw new OperacionException("No existe la billetera " + destino);
        }

        if (tipo == TipoTransaccion.RECARGA) {
            if (!bDestino.getIdUsuario().equals(idUsuario)) {
                throw new OperacionException("Solo puedes programar recargas en tus billeteras");
            }
        } else {
            Billetera bOrigen = repo.getBilleteras().get(origen);
            if (bOrigen == null) {
                throw new OperacionException("No existe la billetera " + origen);
            }
            if (!bOrigen.getIdUsuario().equals(idUsuario)) {
                throw new OperacionException("La billetera origen no pertenece al usuario");
            }
            if (origen.equals(destino)) {
                throw new OperacionException("La billetera origen y destino deben ser distintas");
            }
        }

        int secuencia = ++contador;
        OperacionProgramada op = new OperacionProgramada("P" + secuencia, idUsuario, tipo, origen,
                destino, valor, fecha, periodicidad == null ? Periodicidad.UNICA : periodicidad,
                concepto == null ? "" : concepto.trim(), u.getNivel().ordinal(), secuencia);

        cola.offer(op);
        porId.put(op.getId(), op);
        return op;
    }

    // ---------- Cancelar ----------

    public void cancelar(String idUsuario, String idProgramada) {
        OperacionProgramada op = porId.get(idProgramada);
        if (op == null) {
            throw new OperacionException("No existe la operacion programada " + idProgramada);
        }
        if (!op.getIdUsuario().equals(idUsuario)) {
            throw new OperacionException("La operacion programada no pertenece al usuario");
        }
        if (op.getEstado() != EstadoProgramada.PENDIENTE) {
            throw new OperacionException("La operacion " + idProgramada + " ya no esta pendiente");
        }
        cola.remove(op);
        op.setEstado(EstadoProgramada.CANCELADA);
    }

    // ---------- Alertas de proximidad ----------

    // Avisa las pendientes que se ejecutaran dentro de la ventana (una sola vez por ejecucion)
    public int avisarProximas(LocalDateTime ahora, Duration ventana) {
        LocalDateTime limite = ahora.plus(ventana);
        int avisadas = 0;

        for (OperacionProgramada op : cola) {
            if (!op.isAvisada()
                    && op.getFechaEjecucion().isAfter(ahora)
                    && !op.getFechaEjecucion().isAfter(limite)) {
                Usuario u = repo.getUsuarios().get(op.getIdUsuario());
                notificaciones.notificar(u, TipoNotificacion.PROGRAMADA_PROXIMA,
                        "Se ejecutara " + op.getId() + " (" + op.getConcepto() + ") el "
                                + op.getFechaEjecucion().format(FORMATO));
                op.setAvisada(true);
                avisadas++;
            }
        }
        return avisadas;
    }

    // ---------- Procesamiento ----------

    // Ejecuta, en orden de prioridad, todo lo que venza hasta el momento indicado
    public List<RegistroEjecucion> procesarHasta(LocalDateTime momento) {
        List<RegistroEjecucion> ejecutados = new ArrayList<>();

        while (!cola.isEmpty() && !cola.peek().getFechaEjecucion().isAfter(momento)) {
            OperacionProgramada op = cola.poll();
            ejecutados.add(ejecutar(op, momento));
        }
        return ejecutados;
    }

    // Simula el paso del tiempo avanzando de "paso" en "paso"
    public List<RegistroEjecucion> simular(LocalDateTime desde, LocalDateTime hasta,
                                           Duration paso, Duration ventanaAviso) {
        List<RegistroEjecucion> ejecutados = new ArrayList<>();

        for (LocalDateTime t = desde; !t.isAfter(hasta); t = t.plus(paso)) {
            avisarProximas(t, ventanaAviso);
            ejecutados.addAll(procesarHasta(t));
        }
        ejecutados.addAll(procesarHasta(hasta));
        return ejecutados;
    }

    private RegistroEjecucion ejecutar(OperacionProgramada op, LocalDateTime momento) {
        RegistroEjecucion registro;
        boolean exito;

        try {
            Transaccion t = (op.getTipo() == TipoTransaccion.RECARGA)
                    ? transacciones.recargar(op.getBilleteraDestino(), op.getValor())
                    : transacciones.transferir(op.getBilleteraOrigen(), op.getBilleteraDestino(), op.getValor());

            // Bono por pago programado ejecutado con exito (queda guardado en la transaccion
            // para que una reversion tambien lo descuente)
            Usuario actor = repo.getUsuarios().get(op.getIdUsuario());
            int extra = puntos.sumar(actor, BONO_PAGO_PROGRAMADO);
            t.setPuntos(t.getPuntos() + extra);

            registro = new RegistroEjecucion(op.getId(), op.getFechaEjecucion(), momento, true,
                    "Ejecutada: " + op.getConcepto(), t.getId());
            exito = true;
        } catch (OperacionException e) {
            registro = new RegistroEjecucion(op.getId(), op.getFechaEjecucion(), momento, false,
                    e.getMessage(), null);
            exito = false;
        }

        op.contarEjecucion();
        registros.add(registro);

        if (op.getPeriodicidad() == Periodicidad.UNICA) {
            op.setEstado(exito ? EstadoProgramada.COMPLETADA : EstadoProgramada.FALLIDA);
        } else {
            // Periodica: se reprograma aunque esta ejecucion haya fallado
            op.setFechaEjecucion(op.getPeriodicidad().siguiente(op.getFechaEjecucion()));
            op.setAvisada(false);
            Usuario u = repo.getUsuarios().get(op.getIdUsuario());
            if (u != null) {
                op.setPrioridadNivel(u.getNivel().ordinal());
            }
            cola.offer(op);
        }
        return registro;
    }

    // ---------- Consultas ----------

    public List<OperacionProgramada> pendientesDeUsuario(String idUsuario) {
        List<OperacionProgramada> r = new ArrayList<>();
        for (OperacionProgramada op : cola) {
            if (op.getIdUsuario().equals(idUsuario)) {
                r.add(op);
            }
        }
        r.sort(orden);
        return r;
    }

    public List<OperacionProgramada> proximas(int cantidad) {
        List<OperacionProgramada> todas = new ArrayList<>(cola);
        todas.sort(orden);
        return new ArrayList<>(todas.subList(0, Math.min(cantidad, todas.size())));
    }

    public List<RegistroEjecucion> getRegistros() {
        return registros;
    }

    public int cantidadPendientes() {
        return cola.size();
    }
}