package servicio;

import estructuras.Pila;
import modelo.Billetera;
import modelo.EstadoBilletera;
import modelo.EstadoTransaccion;
import modelo.TipoNotificacion;
import modelo.TipoTransaccion;
import modelo.Transaccion;
import modelo.Usuario;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

public class TransaccionServicio {

    public static final double UMBRAL_SALDO_BAJO = 100;

    private final RepositorioDatos repo;
    private final PuntosServicio puntos;
    private final NotificacionServicio notificaciones;

    public TransaccionServicio(RepositorioDatos repo, PuntosServicio puntos,
                               NotificacionServicio notificaciones) {
        this.repo = repo;
        this.puntos = puntos;
        this.notificaciones = notificaciones;
    }

    private RiesgoServicio riesgo;

    public void setRiesgoServicio(RiesgoServicio riesgo) {
        this.riesgo = riesgo;
    }

    public Transaccion recargar(String codigo, double valor) {
        return conRechazo(codigo, () -> {
            validarValor(valor);
            Billetera b = obtenerActiva(codigo);
            Usuario actor = duenoDe(b);
            validarLimite(actor);

            b.depositar(valor);

            Transaccion t = crear(TipoTransaccion.RECARGA, valor, null, codigo, 0);
            registrar(t, null, b);
            return t;
        });
    }

    public Transaccion retirar(String codigo, double valor) {
        return conRechazo(codigo, () -> {
            validarValor(valor);
            Billetera b = obtenerActiva(codigo);
            Usuario actor = duenoDe(b);
            validarLimite(actor);

            double comision = calcularComision(actor, TipoTransaccion.RETIRO, valor, false);
            validarSaldo(b, valor + comision);

            b.retirar(valor + comision);
            repo.sumarComision(comision);

            Transaccion t = crear(TipoTransaccion.RETIRO, valor, codigo, null, comision);
            registrar(t, b, null);
            avisarSaldoBajo(b, actor);
            return t;
        });
    }

    public Transaccion transferir(String codigoOrigen, String codigoDestino, double valor) {
        return conRechazo(codigoOrigen, () -> {
            validarValor(valor);
            if (codigoOrigen.equals(codigoDestino)) {
                throw new OperacionException("La billetera origen y destino deben ser distintas");
            }

            Billetera origen = obtenerActiva(codigoOrigen);
            Billetera destino = obtenerActiva(codigoDestino);
            Usuario actor = duenoDe(origen);
            validarLimite(actor);

            boolean entreUsuarios = !origen.getIdUsuario().equals(destino.getIdUsuario());
            double comision = calcularComision(actor, TipoTransaccion.TRANSFERENCIA, valor, entreUsuarios);
            validarSaldo(origen, valor + comision);

            origen.retirar(valor + comision);
            destino.depositar(valor);
            repo.sumarComision(comision);

            Transaccion t = crear(TipoTransaccion.TRANSFERENCIA, valor, codigoOrigen, codigoDestino, comision);
            registrar(t, origen, destino);
            avisarSaldoBajo(origen, actor);
            return t;
        });
    }

    public List<Transaccion> historialDeBilletera(String codigo) {
        Billetera b = repo.getBilleteras().get(codigo);
        if (b == null) {
            throw new OperacionException("No existe la billetera " + codigo);
        }
        return b.getHistorial();
    }

    public List<Transaccion> historialDeUsuario(String idUsuario) {
        Usuario u = repo.getUsuarios().get(idUsuario);
        if (u == null) {
            throw new OperacionException("No existe el usuario " + idUsuario);
        }
        return u.getHistorial();
    }

    public static int calcularPuntos(TipoTransaccion tipo, double valor) {
        return (int) (valor / 100) * tipo.getPuntosPorCien();
    }

    // ---------- Rechazos: se notifica al usuario y se vuelve a lanzar el error ----------

    private Transaccion conRechazo(String codigoActor, Supplier<Transaccion> operacion) {
        try {
            return operacion.get();
        } catch (OperacionException e) {
            Billetera b = repo.getBilleteras().get(codigoActor);
            if (b != null) {
                Usuario u = repo.getUsuarios().get(b.getIdUsuario());
                notificaciones.notificar(u, TipoNotificacion.OPERACION_RECHAZADA, e.getMessage());
            }
            throw e;
        }
    }

    // ---------- Registro ----------

    private Transaccion crear(TipoTransaccion tipo, double valor, String origen, String destino, double comision) {
        Transaccion t = new Transaccion(repo.nuevoIdTransaccion(), LocalDateTime.now(), tipo, valor,
                origen, destino, EstadoTransaccion.EJECUTADA, calcularPuntos(tipo, valor));
        t.setComision(comision);
        return t;
    }

    // Guarda la transaccion en billeteras y usuarios, suma puntos al que inicia la
    // operacion y, si es reversible, la apila para poder deshacerla
    private void registrar(Transaccion t, Billetera origen, Billetera destino) {
        Usuario uOrigen = null;
        Usuario uDestino = null;

        if (origen != null) {
            origen.agregarTransaccion(t);
            uOrigen = repo.getUsuarios().get(origen.getIdUsuario());
            uOrigen.getHistorial().add(t);
        }
        if (destino != null) {
            destino.agregarTransaccion(t);
            uDestino = repo.getUsuarios().get(destino.getIdUsuario());
            if (uDestino != uOrigen) {
                uDestino.getHistorial().add(t);
            }
        }

        repo.getTransacciones().put(t.getId(), t);

        Usuario actor = (uOrigen != null) ? uOrigen : uDestino;
        t.setPuntos(puntos.sumar(actor, t.getPuntos()));

        if (esReversible(t.getTipo())) {
            repo.getReversibles().computeIfAbsent(actor.getId(), k -> new Pila<>()).push(t);
        }

        if (riesgo != null) {
            riesgo.evaluar(t, actor);
        }
    }

    private boolean esReversible(TipoTransaccion tipo) {
        return tipo == TipoTransaccion.RECARGA || tipo == TipoTransaccion.TRANSFERENCIA;
    }

    // ---------- Comisiones, limites y alertas ----------

    // Se cobra en retiros y transferencias a otros usuarios, segun el nivel
    private double calcularComision(Usuario actor, TipoTransaccion tipo, double valor, boolean entreUsuarios) {
        boolean cobra = tipo == TipoTransaccion.RETIRO
                || (tipo == TipoTransaccion.TRANSFERENCIA && entreUsuarios);
        if (!cobra) {
            return 0;
        }
        double c = valor * actor.getNivel().getComision() / 100;
        return Math.round(c * 100) / 100.0;
    }

    private void validarLimite(Usuario actor) {
        LocalDate hoy = LocalDate.now();
        int realizadas = 0;

        for (Transaccion t : actor.getHistorial()) {
            if (t.getTipo() != TipoTransaccion.REVERSION
                    && t.getEstado() != EstadoTransaccion.RECHAZADA
                    && t.getFecha().toLocalDate().equals(hoy)
                    && laInicio(actor, t)) {
                realizadas++;
            }
        }

        if (realizadas >= actor.getNivel().getLimiteDiario()) {
            throw new OperacionException("Limite diario de transacciones alcanzado (nivel "
                    + actor.getNivel() + ")");
        }
    }

    private boolean laInicio(Usuario u, Transaccion t) {
        String codigo = (t.getBilleteraOrigen() != null) ? t.getBilleteraOrigen() : t.getBilleteraDestino();
        Billetera b = repo.getBilleteras().get(codigo);
        return b != null && b.getIdUsuario().equals(u.getId());
    }

    private void avisarSaldoBajo(Billetera b, Usuario actor) {
        if (b.getSaldo() < UMBRAL_SALDO_BAJO) {
            notificaciones.notificar(actor, TipoNotificacion.SALDO_BAJO,
                    "Saldo bajo en " + b.getCodigo() + ": " + b.getSaldo());
        }
    }

    // ---------- Validaciones ----------

    private Usuario duenoDe(Billetera b) {
        return repo.getUsuarios().get(b.getIdUsuario());
    }

    private Billetera obtenerActiva(String codigo) {
        Billetera b = repo.getBilleteras().get(codigo);
        if (b == null) {
            throw new OperacionException("No existe la billetera " + codigo);
        }
        if (b.getEstado() != EstadoBilletera.ACTIVA) {
            throw new OperacionException("La billetera " + codigo + " no esta activa");
        }
        return b;
    }

    private void validarValor(double valor) {
        if (Double.isNaN(valor) || Double.isInfinite(valor) || valor <= 0) {
            throw new OperacionException("El valor debe ser mayor a cero");
        }
    }

    private void validarSaldo(Billetera b, double requerido) {
        if (requerido > b.getSaldo()) {
            throw new OperacionException("Saldo insuficiente en la billetera " + b.getCodigo());
        }
    }
}