package servicio;

import estructuras.ArbolAVL;
import modelo.Nivel;
import modelo.TipoNotificacion;
import modelo.Usuario;

import java.util.Comparator;
import java.util.List;

public class PuntosServicio {

    private final NotificacionServicio notificaciones;

    // Orden: puntos y, si empatan, id (los puntos no son unicos)
    private final ArbolAVL<Usuario> ranking = new ArbolAVL<>(
            Comparator.comparingInt(Usuario::getPuntos).thenComparing(Usuario::getId));

    public PuntosServicio(NotificacionServicio notificaciones) {
        this.notificaciones = notificaciones;
    }

    public void agregar(Usuario u) {
        ranking.insertar(u);
    }

    public void quitar(Usuario u) {
        ranking.eliminar(u);
    }

    // Suma puntos (con bono del nivel). Devuelve los puntos realmente otorgados.
    public int sumar(Usuario u, int puntosBase) {
        int bono = puntosBase * u.getNivel().getBonoPuntos() / 100;
        int total = puntosBase + bono;

        ranking.eliminar(u);                 // la clave cambia: se saca antes
        boolean asciende = u.sumarPuntos(total);
        ranking.insertar(u);                 // y se vuelve a insertar

        if (asciende) {
            notificaciones.notificar(u, TipoNotificacion.ASCENSO_NIVEL,
                    "Subiste al nivel " + u.getNivel());
        }
        return total;
    }

    public void restar(Usuario u, int puntos) {
        ranking.eliminar(u);
        u.restarPuntos(puntos);
        ranking.insertar(u);
    }

    public List<Usuario> porRango(int min, int max) {
        return ranking.rango(Usuario::getPuntos, min, max);
    }

    public List<Usuario> porNivel(Nivel nivel) {
        return porRango(nivel.getMinimo(), nivel.getMaximo());
    }

    public List<Usuario> ordenadosAscendente() {
        return ranking.inorden();
    }

    public List<Usuario> top(int cantidad) {
        return ranking.mayores(cantidad);
    }

    public int cantidad() {
        return ranking.tamano();
    }

    public int altura() {
        return ranking.altura();
    }
}