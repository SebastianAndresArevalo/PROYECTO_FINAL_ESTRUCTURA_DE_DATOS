package estructuras;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Grafo dirigido y ponderado con lista de adyacencia.
// Si se agrega dos veces la misma arista (origen -> destino) no se duplica:
// se acumula el peso (monto) y se incrementa la cantidad de veces.
public class Grafo<T> {

    // ---------- Arista: una relacion origen -> destino ----------

    public static class Arista<T> {
        private final T origen;
        private final T destino;
        private double peso;
        private int cantidad;

        Arista(T origen, T destino, double peso) {
            this.origen = origen;
            this.destino = destino;
            this.peso = peso;
            this.cantidad = 1;
        }

        void acumular(double valor) {
            peso += valor;
            cantidad++;
        }

        public T getOrigen() { return origen; }
        public T getDestino() { return destino; }
        public double getPeso() { return peso; }
        public int getCantidad() { return cantidad; }

        @Override
        public String toString() {
            return origen + " -> " + destino + " (x" + cantidad + ", total " + peso + ")";
        }
    }

    // ---------- Ruta: camino o ciclo encontrado en el grafo ----------

    public static class Ruta<T> {
        private final List<T> nodos;
        private final boolean cerrada;      // true = ciclo (el ultimo nodo vuelve al primero)
        private final int repeticiones;     // la arista menos repetida de la ruta
        private final double monto;         // suma del monto de todas sus aristas

        Ruta(List<T> nodos, boolean cerrada, int repeticiones, double monto) {
            this.nodos = nodos;
            this.cerrada = cerrada;
            this.repeticiones = repeticiones;
            this.monto = monto;
        }

        public List<T> getNodos() { return nodos; }
        public boolean esCiclo() { return cerrada; }
        public int getRepeticiones() { return repeticiones; }
        public double getMonto() { return monto; }

        // Cantidad de aristas que recorre la ruta
        public int getLongitud() {
            return cerrada ? nodos.size() : nodos.size() - 1;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < nodos.size(); i++) {
                if (i > 0) {
                    sb.append(" -> ");
                }
                sb.append(nodos.get(i));
            }
            if (cerrada) {
                sb.append(" -> ").append(nodos.get(0));
            }
            return sb + " (min " + repeticiones + " veces, total " + monto + ")";
        }
    }

    // ---------- Estructura interna ----------

    private final Map<T, List<Arista<T>>> adyacencia = new LinkedHashMap<>();
    private final Map<T, Integer> gradoEntrada = new HashMap<>();
    private int numAristas;

    // ---------- Vertices y aristas ----------

    public boolean agregarVertice(T vertice) {
        if (vertice == null) {
            throw new IllegalArgumentException("El vertice no puede ser nulo");
        }
        if (adyacencia.containsKey(vertice)) {
            return false;
        }
        adyacencia.put(vertice, new ArrayList<>());
        gradoEntrada.put(vertice, 0);
        return true;
    }

    public void agregarArista(T origen, T destino, double peso) {
        if (origen == null || destino == null) {
            throw new IllegalArgumentException("Los vertices no pueden ser nulos");
        }
        if (origen.equals(destino)) {
            throw new IllegalArgumentException("No se permiten aristas de un vertice a si mismo");
        }
        if (peso <= 0) {
            throw new IllegalArgumentException("El peso debe ser mayor a cero");
        }

        agregarVertice(origen);
        agregarVertice(destino);

        Arista<T> existente = getArista(origen, destino);
        if (existente != null) {
            existente.acumular(peso);
        } else {
            adyacencia.get(origen).add(new Arista<>(origen, destino, peso));
            gradoEntrada.merge(destino, 1, Integer::sum);
            numAristas++;
        }
    }

    public boolean contiene(T vertice) {
        return adyacencia.containsKey(vertice);
    }

    public Arista<T> getArista(T origen, T destino) {
        List<Arista<T>> salientes = adyacencia.get(origen);
        if (salientes == null) {
            return null;
        }
        for (Arista<T> a : salientes) {
            if (a.getDestino().equals(destino)) {
                return a;
            }
        }
        return null;
    }

    public boolean existeArista(T origen, T destino) {
        return getArista(origen, destino) != null;
    }

    public List<T> getVertices() {
        return new ArrayList<>(adyacencia.keySet());
    }

    public List<Arista<T>> getAristas() {
        List<Arista<T>> todas = new ArrayList<>();
        for (List<Arista<T>> lista : adyacencia.values()) {
            todas.addAll(lista);
        }
        return todas;
    }

    // Aristas que salen del vertice (a quien le envia)
    public List<Arista<T>> aristasDesde(T vertice) {
        List<Arista<T>> lista = adyacencia.get(vertice);
        return lista == null ? new ArrayList<>() : new ArrayList<>(lista);
    }

    // Aristas que llegan al vertice (de quien recibe)
    public List<Arista<T>> aristasHacia(T vertice) {
        List<Arista<T>> r = new ArrayList<>();
        for (List<Arista<T>> lista : adyacencia.values()) {
            for (Arista<T> a : lista) {
                if (a.getDestino().equals(vertice)) {
                    r.add(a);
                }
            }
        }
        return r;
    }

    public int gradoSalida(T vertice) {
        List<Arista<T>> lista = adyacencia.get(vertice);
        return lista == null ? 0 : lista.size();
    }

    public int gradoEntrada(T vertice) {
        return gradoEntrada.getOrDefault(vertice, 0);
    }

    public int numVertices() {
        return adyacencia.size();
    }

    public int numAristas() {
        return numAristas;
    }

    // ---------- Recorridos ----------

    // Recorrido en anchura: usa la Cola propia
    public List<T> bfs(T inicio) {
        List<T> orden = new ArrayList<>();
        if (!contiene(inicio)) {
            return orden;
        }

        Set<T> visitados = new HashSet<>();
        Cola<T> cola = new Cola<>();
        cola.encolar(inicio);
        visitados.add(inicio);

        while (!cola.estaVacia()) {
            T actual = cola.desencolar();
            orden.add(actual);
            for (Arista<T> a : adyacencia.get(actual)) {
                if (visitados.add(a.getDestino())) {
                    cola.encolar(a.getDestino());
                }
            }
        }
        return orden;
    }

    // Recorrido en profundidad iterativo: usa la Pila propia
    public List<T> dfs(T inicio) {
        List<T> orden = new ArrayList<>();
        if (!contiene(inicio)) {
            return orden;
        }

        Set<T> visitados = new HashSet<>();
        Pila<T> pila = new Pila<>();
        pila.push(inicio);

        while (!pila.estaVacia()) {
            T actual = pila.pop();
            if (!visitados.add(actual)) {
                continue;
            }
            orden.add(actual);

            // Se apilan al reves para visitar primero al primer vecino
            List<Arista<T>> salientes = adyacencia.get(actual);
            for (int i = salientes.size() - 1; i >= 0; i--) {
                T vecino = salientes.get(i).getDestino();
                if (!visitados.contains(vecino)) {
                    pila.push(vecino);
                }
            }
        }
        return orden;
    }

    public boolean existeCamino(T origen, T destino) {
        return bfs(origen).contains(destino);
    }

    // Camino con menos saltos (BFS). Lista vacia si no existe.
    public List<T> caminoMasCorto(T origen, T destino) {
        List<T> camino = new ArrayList<>();
        if (!contiene(origen) || !contiene(destino)) {
            return camino;
        }

        Map<T, T> padre = new HashMap<>();
        Set<T> visitados = new HashSet<>();
        Cola<T> cola = new Cola<>();
        cola.encolar(origen);
        visitados.add(origen);

        while (!cola.estaVacia() && !visitados.contains(destino)) {
            T actual = cola.desencolar();
            for (Arista<T> a : adyacencia.get(actual)) {
                T sig = a.getDestino();
                if (visitados.add(sig)) {
                    padre.put(sig, actual);
                    cola.encolar(sig);
                }
            }
        }

        if (!visitados.contains(destino)) {
            return camino;
        }
        for (T v = destino; v != null; v = padre.get(v)) {
            camino.add(v);
        }
        Collections.reverse(camino);
        return camino;
    }

    // Grupos de vertices conectados ignorando la direccion de las aristas
    public List<List<T>> componentesConectados() {
        Map<T, List<T>> vecinos = new LinkedHashMap<>();
        for (T v : adyacencia.keySet()) {
            vecinos.put(v, new ArrayList<>());
        }
        for (Arista<T> a : getAristas()) {
            vecinos.get(a.getOrigen()).add(a.getDestino());
            vecinos.get(a.getDestino()).add(a.getOrigen());
        }

        List<List<T>> componentes = new ArrayList<>();
        Set<T> visitados = new HashSet<>();

        for (T inicio : adyacencia.keySet()) {
            if (visitados.contains(inicio)) {
                continue;
            }
            List<T> grupo = new ArrayList<>();
            Cola<T> cola = new Cola<>();
            cola.encolar(inicio);
            visitados.add(inicio);

            while (!cola.estaVacia()) {
                T actual = cola.desencolar();
                grupo.add(actual);
                for (T vecino : vecinos.get(actual)) {
                    if (visitados.add(vecino)) {
                        cola.encolar(vecino);
                    }
                }
            }
            componentes.add(grupo);
        }
        return componentes;
    }

    // ---------- Ciclos y rutas frecuentes ----------

    // Ciclos simples de 2 hasta maxLongitud aristas. Cada ciclo se reporta una
    // sola vez: empieza en su vertice de menor posicion de insercion.
    public List<Ruta<T>> ciclos(int maxLongitud) {
        if (maxLongitud < 2) {
            throw new IllegalArgumentException("La longitud maxima debe ser al menos 2");
        }

        Map<T, Integer> indice = new HashMap<>();
        int i = 0;
        for (T v : adyacencia.keySet()) {
            indice.put(v, i++);
        }

        List<Ruta<T>> resultado = new ArrayList<>();
        for (T inicio : adyacencia.keySet()) {
            List<T> camino = new ArrayList<>();
            Set<T> enCamino = new HashSet<>();
            camino.add(inicio);
            enCamino.add(inicio);
            buscarCiclos(inicio, inicio, indice, camino, enCamino, maxLongitud, resultado);
        }
        return resultado;
    }

    private void buscarCiclos(T inicio, T actual, Map<T, Integer> indice, List<T> camino,
                              Set<T> enCamino, int maxLongitud, List<Ruta<T>> resultado) {
        for (Arista<T> a : adyacencia.get(actual)) {
            T sig = a.getDestino();

            if (sig.equals(inicio)) {
                if (camino.size() >= 2) {
                    resultado.add(crearRuta(camino, true));
                }
            } else if (indice.get(sig) > indice.get(inicio)
                    && !enCamino.contains(sig)
                    && camino.size() < maxLongitud) {
                camino.add(sig);
                enCamino.add(sig);
                buscarCiclos(inicio, sig, indice, camino, enCamino, maxLongitud, resultado);
                camino.remove(camino.size() - 1);
                enCamino.remove(sig);
            }
        }
    }

    // Rutas simples de 2 a maxAristas aristas donde CADA tramo se repitio al menos minVeces
    public List<Ruta<T>> rutasFrecuentes(int minVeces, int maxAristas) {
        if (minVeces < 1) {
            throw new IllegalArgumentException("minVeces debe ser al menos 1");
        }
        if (maxAristas < 2) {
            throw new IllegalArgumentException("maxAristas debe ser al menos 2");
        }

        List<Ruta<T>> resultado = new ArrayList<>();
        for (T inicio : adyacencia.keySet()) {
            List<T> camino = new ArrayList<>();
            Set<T> enCamino = new HashSet<>();
            camino.add(inicio);
            enCamino.add(inicio);
            buscarRutas(inicio, minVeces, maxAristas, camino, enCamino, resultado);
        }
        return resultado;
    }

    private void buscarRutas(T actual, int minVeces, int maxAristas, List<T> camino,
                             Set<T> enCamino, List<Ruta<T>> resultado) {
        for (Arista<T> a : adyacencia.get(actual)) {
            T sig = a.getDestino();
            if (a.getCantidad() < minVeces || enCamino.contains(sig)) {
                continue;
            }
            camino.add(sig);
            enCamino.add(sig);

            int aristas = camino.size() - 1;
            if (aristas >= 2) {
                resultado.add(crearRuta(camino, false));
            }
            if (aristas < maxAristas) {
                buscarRutas(sig, minVeces, maxAristas, camino, enCamino, resultado);
            }

            camino.remove(camino.size() - 1);
            enCamino.remove(sig);
        }
    }

    private Ruta<T> crearRuta(List<T> camino, boolean cerrada) {
        int minimo = Integer.MAX_VALUE;
        double monto = 0;
        int tramos = cerrada ? camino.size() : camino.size() - 1;

        for (int i = 0; i < tramos; i++) {
            T desde = camino.get(i);
            T hasta = camino.get((i + 1) % camino.size());
            Arista<T> a = getArista(desde, hasta);
            minimo = Math.min(minimo, a.getCantidad());
            monto += a.getPeso();
        }
        return new Ruta<>(new ArrayList<>(camino), cerrada, minimo, monto);
    }
}

