package servicio;

import estructuras.Nodo;
import estructuras.Pila;
import modelo.Billetera;
import modelo.EstadoBilletera;
import modelo.EstadoTransaccion;
import modelo.TipoNotificacion;
import modelo.TipoTransaccion;
import modelo.Transaccion;
import modelo.Usuario;

import java.time.LocalDateTime;

public class ReversionServicio {

    private final RepositorioDatos repo;
    private final PuntosServicio puntos;
    private final NotificacionServicio notificaciones;

    public ReversionServicio(RepositorioDatos repo, PuntosServicio puntos,
                             NotificacionServicio notificaciones) {
        this.repo = repo;
        this.puntos = puntos;
        this.notificaciones = notificaciones;
    }

    // Deshace la operacion reversible mas reciente del usuario (tope de la pila)
    public Transaccion deshacerUltima(String idUsuario) {
        Pila<Transaccion> pila = pilaDe(idUsuario);
        if (pila.estaVacia()) {
            throw new OperacionException("No hay operaciones para deshacer");
        }
        return revertir(idUsuario, pila.obtenerCima().getId());
    }

    // Revierte una operacion concreta del usuario
    public Transaccion revertir(String idUsuario, String idTransaccion) {
        Usuario actor = repo.getUsuarios().get(idUsuario);
        if (actor == null) {
            throw new OperacionException("No existe el usuario " + idUsuario);
        }
        Pila<Transaccion> pila = pilaDe(idUsuario);

        try {
            Transaccion objetivo = buscarEnPila(pila, idTransaccion);   // solo lectura
            Transaccion reversion = aplicar(actor, objetivo);           // valida y modifica
            quitarDePila(pila, idTransaccion);                          // solo si todo salio bien
            return reversion;
        } catch (OperacionException e) {
            notificaciones.notificar(actor, TipoNotificacion.OPERACION_RECHAZADA, e.getMessage());
            throw e;
        }
    }

    private Pila<Transaccion> pilaDe(String idUsuario) {
        Pila<Transaccion> pila = repo.getReversibles().get(idUsuario);
        return (pila != null) ? pila : new Pila<>();
    }

    // Recorre la pila sin sacar elementos
    private Transaccion buscarEnPila(Pila<Transaccion> pila, String id) {
        for (Nodo<Transaccion> n = pila.getCima(); n != null; n = n.getSiguienteNodo()) {
            if (n.getValorNodo().getId().equals(id)) {
                return n.getValorNodo();
            }
        }
        throw new OperacionException("La transaccion " + id + " no es reversible para este usuario");
    }

    // Misma tecnica de los parciales: pila auxiliar para restaurar el orden
    private void quitarDePila(Pila<Transaccion> pila, String id) {
        Pila<Transaccion> aux = new Pila<>();
        while (!pila.estaVacia()) {
            Transaccion t = pila.pop();
            if (t.getId().equals(id)) {
                break;
            }
            aux.push(t);
        }
        while (!aux.estaVacia()) {
            pila.push(aux.pop());
        }
    }

    private Transaccion aplicar(Usuario actor, Transaccion t) {
        if (t.getEstado() != EstadoTransaccion.EJECUTADA) {
            throw new OperacionException("La transaccion " + t.getId() + " ya fue revertida");
        }

        Billetera origen = (t.getBilleteraOrigen() == null) ? null
                : repo.getBilleteras().get(t.getBilleteraOrigen());
        Billetera destino = repo.getBilleteras().get(t.getBilleteraDestino());

        // Validaciones (antes de tocar saldos)
        if (destino == null || destino.getEstado() != EstadoBilletera.ACTIVA) {
            throw new OperacionException("La billetera destino no esta disponible");
        }
        if (t.getBilleteraOrigen() != null
                && (origen == null || origen.getEstado() != EstadoBilletera.ACTIVA)) {
            throw new OperacionException("La billetera origen no esta disponible");
        }
        if (destino.getSaldo() < t.getValor()) {
            throw new OperacionException("No se puede revertir: saldo insuficiente en " + destino.getCodigo());
        }

        // Saldos: el destino devuelve el valor; el origen recupera valor + comision
        destino.retirar(t.getValor());
        if (origen != null) {
            origen.depositar(t.getValor() + t.getComision());
            repo.restarComision(t.getComision());
        }

        // Estado, historial y puntos
        t.setEstado(EstadoTransaccion.REVERTIDA);

        Transaccion reversion = new Transaccion(repo.nuevoIdTransaccion(), LocalDateTime.now(),
                TipoTransaccion.REVERSION, t.getValor(),
                t.getBilleteraDestino(), t.getBilleteraOrigen(),
                EstadoTransaccion.EJECUTADA, -t.getPuntos());
        reversion.setReferencia(t.getId());
        registrarHistorial(reversion, destino, origen);

        puntos.restar(actor, t.getPuntos());

        // Alertas
        Usuario duenoDestino = repo.getUsuarios().get(destino.getIdUsuario());
        notificaciones.notificar(actor, TipoNotificacion.REVERSION_APLICADA,
                "Se revirtio la operacion " + t.getId());
        if (duenoDestino != actor) {
            notificaciones.notificar(duenoDestino, TipoNotificacion.REVERSION_APLICADA,
                    "Se revirtio la operacion " + t.getId() + " que afecto tu billetera " + destino.getCodigo());
        }
        return reversion;
    }

    private void registrarHistorial(Transaccion rev, Billetera destino, Billetera origen) {
        repo.getTransacciones().put(rev.getId(), rev);

        destino.agregarTransaccion(rev);
        Usuario ud = repo.getUsuarios().get(destino.getIdUsuario());
        ud.getHistorial().add(rev);

        if (origen != null) {
            origen.agregarTransaccion(rev);
            Usuario uo = repo.getUsuarios().get(origen.getIdUsuario());
            if (uo != ud) {
                uo.getHistorial().add(rev);
            }
        }
    }
}