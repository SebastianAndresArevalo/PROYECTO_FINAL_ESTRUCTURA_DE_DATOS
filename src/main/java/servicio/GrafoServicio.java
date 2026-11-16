package servicio;

import estructuras.Grafo;
import estructuras.Grafo.Arista;
import estructuras.Grafo.Ruta;
import modelo.Transaccion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Construye los grafos de transferencias a partir del repositorio y consulta sus patrones.
// Los grafos se reconstruyen en cada consulta, asi siempre reflejan el estado actual
// (reversiones y transacciones programadas incluidas).
public class GrafoServicio {

    private final RepositorioDatos repo;

    public GrafoServicio(RepositorioDatos repo) {
        this.repo = repo;
    }

    // ---------- Construccion ----------

    // Vertice = usuario. Arista A -> B = A le transfirio a B (billeteras propias no cuentan)
    public Grafo<String> grafoUsuarios() {
        return grafoUsuarios(null, null);
    }

    public Grafo<String> grafoUsuarios(LocalDateTime desde, LocalDateTime hasta) {
        Grafo<String> g = new Grafo<>();

        List<String> ids = new ArrayList<>(repo.getUsuarios().keySet());
        ids.sort(FiltroMovimientos.POR_ID);
        for (String id : ids) {
            g.agregarVertice(id);
        }

        for (Transaccion t : FiltroMovimientos.validas(repo, desde, hasta)) {
            if (!FiltroMovimientos.esTransferencia(t)) {
                continue;
            }
            String origen = FiltroMovimientos.duenoDe(repo, t.getBilleteraOrigen());
            String destino = FiltroMovimientos.duenoDe(repo, t.getBilleteraDestino());
            if (origen != null && destino != null && !origen.equals(destino)) {
                g.agregarArista(origen, destino, t.getValor());
            }
        }
        return g;
    }

    // Vertice = billetera. Incluye las transferencias entre billeteras del mismo usuario
    public Grafo<String> grafoBilleteras() {
        return grafoBilleteras(null, null);
    }

    public Grafo<String> grafoBilleteras(LocalDateTime desde, LocalDateTime hasta) {
        Grafo<String> g = new Grafo<>();

        List<String> codigos = new ArrayList<>(repo.getBilleteras().keySet());
        codigos.sort(FiltroMovimientos.POR_ID);
        for (String c : codigos) {
            g.agregarVertice(c);
        }

        for (Transaccion t : FiltroMovimientos.validas(repo, desde, hasta)) {
            if (FiltroMovimientos.esTransferencia(t)
                    && repo.getBilleteras().containsKey(t.getBilleteraOrigen())
                    && repo.getBilleteras().containsKey(t.getBilleteraDestino())) {
                g.agregarArista(t.getBilleteraOrigen(), t.getBilleteraDestino(), t.getValor());
            }
        }
        return g;
    }

    // ---------- Patrones entre usuarios ----------

    // Las n relaciones con mayor monto movido (quien le envia mas a quien)
    public List<Arista<String>> relacionesMasFuertes(int n, LocalDateTime desde, LocalDateTime hasta) {
        FiltroMovimientos.validarCantidad(n);

        List<Arista<String>> aristas = grafoUsuarios(desde, hasta).getAristas();
        aristas.sort(Comparator.<Arista<String>>comparingDouble(Arista::getPeso).reversed()
                .thenComparing(Comparator.<Arista<String>>comparingInt(Arista::getCantidad).reversed()));
        return aristas.size() > n ? new ArrayList<>(aristas.subList(0, n)) : aristas;
    }

    // Ciclos de dinero entre usuarios (A -> B -> C -> A). Los mas repetidos primero.
    public List<Ruta<String>> ciclosEntreUsuarios(int maxLongitud) {
        return ordenar(grafoUsuarios().ciclos(maxLongitud));
    }

    public List<Ruta<String>> ciclosEntreBilleteras(int maxLongitud) {
        return ordenar(grafoBilleteras().ciclos(maxLongitud));
    }

    // Rutas de varios saltos repetidas al menos minVeces en cada tramo
    public List<Ruta<String>> rutasFrecuentesEntreUsuarios(int minVeces, int maxAristas) {
        return ordenar(grafoUsuarios().rutasFrecuentes(minVeces, maxAristas));
    }

    public List<Ruta<String>> rutasFrecuentesEntreBilleteras(int minVeces, int maxAristas) {
        return ordenar(grafoBilleteras().rutasFrecuentes(minVeces, maxAristas));
    }

    private List<Ruta<String>> ordenar(List<Ruta<String>> rutas) {
        rutas.sort(Comparator.<Ruta<String>>comparingInt(Ruta::getRepeticiones).reversed()
                .thenComparing(Comparator.<Ruta<String>>comparingDouble(Ruta::getMonto).reversed())
                .thenComparing(Comparator.<Ruta<String>>comparingInt(Ruta::getLongitud).reversed()));
        return rutas;
    }

    // ---------- Consultas sobre un usuario ----------

    // A quien le envia dinero el usuario
    public List<Arista<String>> enviosDe(String idUsuario) {
        validarUsuario(idUsuario);
        return grafoUsuarios().aristasDesde(idUsuario);
    }

    // De quien recibe dinero el usuario
    public List<Arista<String>> recibosDe(String idUsuario) {
        validarUsuario(idUsuario);
        return grafoUsuarios().aristasHacia(idUsuario);
    }

    // Cadena de usuarios con menos intermediarios por la que ha fluido dinero de A hacia B
    public List<String> caminoEntreUsuarios(String desde, String hasta) {
        validarUsuario(desde);
        validarUsuario(hasta);
        return grafoUsuarios().caminoMasCorto(desde, hasta);
    }

    // Grupos de usuarios que han interactuado entre si (directa o indirectamente)
    public List<List<String>> gruposDeInteraccion() {
        return grafoUsuarios().componentesConectados();
    }

    private void validarUsuario(String id) {
        if (!repo.getUsuarios().containsKey(id)) {
            throw new OperacionException("No existe el usuario " + id);
        }
    }
}
