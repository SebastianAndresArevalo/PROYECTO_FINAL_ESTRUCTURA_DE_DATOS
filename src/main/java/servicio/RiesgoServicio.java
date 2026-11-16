package servicio;

import modelo.Billetera;
import modelo.EstadoTransaccion;
import modelo.EventoAuditoria;
import modelo.NivelRiesgo;
import modelo.ReglaRiesgo;
import modelo.TipoNotificacion;
import modelo.TipoTransaccion;
import modelo.Transaccion;
import modelo.Usuario;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RiesgoServicio {

    // ---------- Parametros de las reglas (ajustables) ----------
    public static final Duration VENTANA_RAFAGA = Duration.ofMinutes(2);
    public static final int MIN_RAFAGA = 3;                  // transferencias (incluida la actual)

    public static final Duration VENTANA_DESTINO = Duration.ofMinutes(10);
    public static final int MIN_DESTINO = 3;                 // transferencias al mismo usuario

    public static final Duration VENTANA_FRAGMENTACION = Duration.ofMinutes(10);
    public static final int MIN_BILLETERAS = 3;              // billeteras distintas del mismo usuario

    public static final double FACTOR_MONTO = 5.0;           // veces el promedio del usuario
    public static final double MONTO_MINIMO = 500;           // evita alertas por montos triviales
    public static final double UMBRAL_ABSOLUTO = 50000;      // si no hay historial suficiente
    public static final int MIN_MUESTRAS = 3;                // salidas previas para tener promedio

    public static final Duration VENTANA_RECIENTE = Duration.ofHours(24);
    public static final Duration VENTANA_BASE = Duration.ofDays(8);
    public static final int MIN_RECIENTES = 5;
    public static final int MIN_BASE = 3;
    public static final double FACTOR_FRECUENCIA = 3.0;

    public static final int MIN_HISTORIAL_HORARIO = 5;
    public static final double PROPORCION_HORARIO = 0.10;

    public static final int MAX_REVISION = 500;              // operaciones previas que se revisan

    private final RepositorioDatos repo;
    private final NotificacionServicio notificaciones;

    private final List<EventoAuditoria> auditoria = new ArrayList<>();
    private final HashMap<String, List<EventoAuditoria>> porUsuario = new HashMap<>();
    private int contador = 0;

    public RiesgoServicio(RepositorioDatos repo, NotificacionServicio notificaciones) {
        this.repo = repo;
        this.notificaciones = notificaciones;
    }

    // Analiza una transaccion ya registrada en el historial de su usuario.
    // Si detecta patrones inusuales: marca el riesgo, registra la auditoria y alerta.
    public EventoAuditoria evaluar(Transaccion t, Usuario actor) {
        if (t == null || actor == null || t.getTipo() == TipoTransaccion.REVERSION) {
            return null;
        }

        boolean transferencia = t.getTipo() == TipoTransaccion.TRANSFERENCIA;
        boolean saliente = transferencia || t.getTipo() == TipoTransaccion.RETIRO;
        String destinoUsuario = transferencia ? duenoDe(t.getBilleteraDestino()) : null;
        boolean aOtroUsuario = destinoUsuario != null && !destinoUsuario.equals(actor.getId());

        // Acumuladores (se calculan en una sola pasada, de lo mas reciente a lo mas antiguo)
        int transferenciasRafaga = transferencia ? 1 : 0;
        int mismoDestino = aOtroUsuario ? 1 : 0;
        Set<String> billeterasVentana = new HashSet<>();
        double sumaVentana = 0;
        if (saliente) {
            billeterasVentana.add(t.getBilleteraOrigen());
            sumaVentana = t.getValor();
        }
        int muestras = 0;
        double sumaMuestras = 0;
        int recientes = 1;
        int base = 0;
        int previas = 0;
        int cercanas = 0;

        List<Transaccion> historial = actor.getHistorial();
        int revisadas = 0;

        for (int i = historial.size() - 1; i >= 0 && revisadas < MAX_REVISION; i--) {
            Transaccion h = historial.get(i);

            if (h.getId().equals(t.getId())
                    || h.getTipo() == TipoTransaccion.REVERSION
                    || h.getEstado() == EstadoTransaccion.RECHAZADA
                    || !loInicio(actor, h)) {
                continue;
            }
            long seg = Duration.between(h.getFecha(), t.getFecha()).getSeconds();
            if (seg < 0) {
                continue;
            }
            revisadas++;

            boolean hTransferencia = h.getTipo() == TipoTransaccion.TRANSFERENCIA;
            boolean hSaliente = hTransferencia || h.getTipo() == TipoTransaccion.RETIRO;

            // Promedio "normal": salidas anteriores, sin contar la rafaga que se esta evaluando
            if (hSaliente && seg > VENTANA_FRAGMENTACION.getSeconds()) {
                muestras++;
                sumaMuestras += h.getValor();
            }
            if (transferencia && hTransferencia && seg <= VENTANA_RAFAGA.getSeconds()) {
                transferenciasRafaga++;
            }
            if (aOtroUsuario && hTransferencia && seg <= VENTANA_DESTINO.getSeconds()
                    && destinoUsuario.equals(duenoDe(h.getBilleteraDestino()))) {
                mismoDestino++;
            }
            if (saliente && hSaliente && seg <= VENTANA_FRAGMENTACION.getSeconds()) {
                billeterasVentana.add(h.getBilleteraOrigen());
                sumaVentana += h.getValor();
            }
            if (seg <= VENTANA_RECIENTE.getSeconds()) {
                recientes++;
            } else if (seg <= VENTANA_BASE.getSeconds()) {
                base++;
            }

            previas++;
            if (distanciaHoras(h.getFecha().getHour(), t.getFecha().getHour()) <= 1) {
                cercanas++;
            }
        }

        double umbral = (muestras >= MIN_MUESTRAS)
                ? Math.max(FACTOR_MONTO * sumaMuestras / muestras, MONTO_MINIMO)
                : UMBRAL_ABSOLUTO;

        // ---------- Reglas ----------
        List<ReglaRiesgo> reglas = new ArrayList<>();

        if (transferenciasRafaga >= MIN_RAFAGA) {
            reglas.add(ReglaRiesgo.RAFAGA_TRANSFERENCIAS);
        }
        if (saliente && t.getValor() >= umbral) {
            reglas.add(ReglaRiesgo.MONTO_INUSUAL);
        }
        if (mismoDestino >= MIN_DESTINO) {
            reglas.add(ReglaRiesgo.DESTINO_REPETIDO);
        }
        if (saliente && billeterasVentana.size() >= MIN_BILLETERAS
                && sumaVentana >= umbral && t.getValor() < umbral) {
            reglas.add(ReglaRiesgo.FRAGMENTACION);
        }
        if (recientes >= MIN_RECIENTES && base >= MIN_BASE
                && recientes > FACTOR_FRECUENCIA * Math.max(base / 7.0, 1)) {
            reglas.add(ReglaRiesgo.CAMBIO_FRECUENCIA);
        }
        if (previas >= MIN_HISTORIAL_HORARIO && cercanas < PROPORCION_HORARIO * previas) {
            reglas.add(ReglaRiesgo.HORARIO_INUSUAL);
        }

        if (reglas.isEmpty()) {
            return null;
        }
        return registrarEvento(t, actor, reglas);
    }

    private EventoAuditoria registrarEvento(Transaccion t, Usuario actor, List<ReglaRiesgo> reglas) {
        int puntaje = 0;
        StringBuilder detalle = new StringBuilder();
        for (ReglaRiesgo r : reglas) {
            puntaje += r.getPeso();
            if (detalle.length() > 0) {
                detalle.append("; ");
            }
            detalle.append(r.getDescripcion());
        }

        NivelRiesgo nivel = (puntaje >= 4) ? NivelRiesgo.ALTO
                : (puntaje >= 2) ? NivelRiesgo.MEDIO : NivelRiesgo.BAJO;

        t.setRiesgo(nivel);

        EventoAuditoria e = new EventoAuditoria("E" + (++contador), t.getId(), actor.getId(),
                reglas, nivel, detalle.toString(), t.getFecha());
        auditoria.add(e);
        porUsuario.computeIfAbsent(actor.getId(), k -> new ArrayList<>()).add(e);

        notificaciones.notificar(actor, TipoNotificacion.ALERTA_SEGURIDAD,
                "Riesgo " + nivel + " en " + t.getId() + ": " + detalle);
        return e;
    }

    // ---------- Consultas de auditoria ----------

    public List<EventoAuditoria> getAuditoria() {
        return auditoria;
    }

    public List<EventoAuditoria> eventosDeUsuario(String idUsuario) {
        List<EventoAuditoria> r = porUsuario.get(idUsuario);
        return (r != null) ? r : new ArrayList<>();
    }

    public List<EventoAuditoria> eventosPorNivel(NivelRiesgo nivel) {
        List<EventoAuditoria> r = new ArrayList<>();
        for (EventoAuditoria e : auditoria) {
            if (e.getNivel() == nivel) {
                r.add(e);
            }
        }
        return r;
    }

    public List<EventoAuditoria> eventosDeTransaccion(String idTransaccion) {
        List<EventoAuditoria> r = new ArrayList<>();
        for (EventoAuditoria e : auditoria) {
            if (e.getIdTransaccion().equals(idTransaccion)) {
                r.add(e);
            }
        }
        return r;
    }

    public EnumMap<ReglaRiesgo, Integer> contarPorRegla() {
        EnumMap<ReglaRiesgo, Integer> conteo = new EnumMap<>(ReglaRiesgo.class);
        for (ReglaRiesgo r : ReglaRiesgo.values()) {
            conteo.put(r, 0);
        }
        for (EventoAuditoria e : auditoria) {
            for (ReglaRiesgo r : e.getReglas()) {
                conteo.put(r, conteo.get(r) + 1);
            }
        }
        return conteo;
    }

    // ---------- Auxiliares ----------

    private String duenoDe(String codigoBilletera) {
        if (codigoBilletera == null) {
            return null;
        }
        Billetera b = repo.getBilleteras().get(codigoBilletera);
        return (b == null) ? null : b.getIdUsuario();
    }

    // La operacion es del usuario si salio de su billetera (o, en una recarga, entro a ella)
    private boolean loInicio(Usuario u, Transaccion h) {
        String codigo = (h.getBilleteraOrigen() != null) ? h.getBilleteraOrigen() : h.getBilleteraDestino();
        return u.getId().equals(duenoDe(codigo));
    }

    private int distanciaHoras(int a, int b) {
        int d = Math.abs(a - b);
        return Math.min(d, 24 - d);
    }
}