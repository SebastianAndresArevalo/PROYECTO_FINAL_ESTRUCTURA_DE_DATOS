package servicio;

import estructuras.Cola;
import modelo.Notificacion;
import modelo.TipoNotificacion;
import modelo.Usuario;

import java.util.ArrayList;
import java.util.List;

public class NotificacionServicio {

    public static final int MAX_RECIENTES = 20;

    // Notificaciones generadas que aun no se despachan (orden de llegada)
    private final Cola<Notificacion> pendientes = new Cola<>();

    public Notificacion notificar(Usuario u, TipoNotificacion tipo, String mensaje) {
        if (u == null) {
            return null;
        }
        Notificacion n = new Notificacion(u.getId(), tipo, mensaje);

        // Historial reciente del usuario (se descartan las mas antiguas)
        List<Notificacion> recientes = u.getNotificaciones();
        recientes.add(n);
        if (recientes.size() > MAX_RECIENTES) {
            recientes.remove(0);
        }

        pendientes.encolar(n);
        return n;
    }

    public Notificacion despachar() {
        if (pendientes.estaVacia()) {
            return null;
        }
        Notificacion n = pendientes.desencolar();
        n.setEnviada(true);
        return n;
    }

    public List<Notificacion> despacharTodas() {
        List<Notificacion> enviadas = new ArrayList<>();
        while (!pendientes.estaVacia()) {
            enviadas.add(despachar());
        }
        return enviadas;
    }

    public int cantidadPendientes() {
        return pendientes.getTamano();
    }
}