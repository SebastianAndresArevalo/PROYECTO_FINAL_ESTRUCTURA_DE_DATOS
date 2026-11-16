package servicio;

import modelo.Beneficio;
import modelo.Canje;
import modelo.Nivel;
import modelo.TipoNotificacion;
import modelo.Usuario;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class BeneficioServicio {

    private final RepositorioDatos repo;
    private final PuntosServicio puntos;
    private final NotificacionServicio notificaciones;

    // Catalogo indexado por id (tabla hash que conserva el orden de insercion)
    private final LinkedHashMap<String, Beneficio> catalogo = new LinkedHashMap<>();

    public BeneficioServicio(RepositorioDatos repo, PuntosServicio puntos,
                             NotificacionServicio notificaciones) {
        this.repo = repo;
        this.puntos = puntos;
        this.notificaciones = notificaciones;

        agregar(new Beneficio("BEN1", "Descuento en comisiones", 200, Nivel.BRONCE));
        agregar(new Beneficio("BEN2", "Bono de transporte", 500, Nivel.PLATA));
        agregar(new Beneficio("BEN3", "Cashback especial", 1000, Nivel.ORO));
        agregar(new Beneficio("BEN4", "Tarjeta premium", 3000, Nivel.PLATINO));
    }

    public void agregar(Beneficio b) {
        catalogo.put(b.getId(), b);
    }

    public List<Beneficio> catalogo() {
        return new ArrayList<>(catalogo.values());
    }

    public List<Beneficio> disponiblesPara(String idUsuario) {
        Usuario u = obtenerUsuario(idUsuario);
        List<Beneficio> r = new ArrayList<>();
        for (Beneficio b : catalogo.values()) {
            if (u.getNivel().ordinal() >= b.getNivelMinimo().ordinal() && u.getPuntos() >= b.getCosto()) {
                r.add(b);
            }
        }
        return r;
    }

    public Canje canjear(String idUsuario, String idBeneficio) {
        Usuario u = obtenerUsuario(idUsuario);

        try {
            Beneficio b = catalogo.get(idBeneficio);
            if (b == null) {
                throw new OperacionException("No existe el beneficio " + idBeneficio);
            }
            if (u.getNivel().ordinal() < b.getNivelMinimo().ordinal()) {
                throw new OperacionException("Nivel insuficiente: se requiere " + b.getNivelMinimo());
            }
            if (u.getPuntos() < b.getCosto()) {
                throw new OperacionException("Puntos insuficientes: tienes " + u.getPuntos()
                        + " y se requieren " + b.getCosto());
            }

            puntos.restar(u, b.getCosto());
            Canje c = new Canje(b.getId(), b.getNombre(), b.getCosto());
            u.getCanjes().add(c);

            notificaciones.notificar(u, TipoNotificacion.CANJE_BENEFICIO,
                    "Canjeaste \"" + b.getNombre() + "\" por " + b.getCosto() + " puntos");
            return c;

        } catch (OperacionException e) {
            notificaciones.notificar(u, TipoNotificacion.OPERACION_RECHAZADA, e.getMessage());
            throw e;
        }
    }

    private Usuario obtenerUsuario(String id) {
        Usuario u = repo.getUsuarios().get(id);
        if (u == null) {
            throw new OperacionException("No existe el usuario " + id);
        }
        return u;
    }
}