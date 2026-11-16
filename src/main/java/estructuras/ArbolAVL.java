package estructuras;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

public class ArbolAVL<T> {

    private class NodoAVL {
        T dato;
        NodoAVL izq, der;
        int altura = 1;

        NodoAVL(T dato) {
            this.dato = dato;
        }
    }

    private NodoAVL raiz;
    private final Comparator<T> comparador;
    private int tamano;
    private boolean modificado;

    public ArbolAVL(Comparator<T> comparador) {
        this.comparador = comparador;
    }

    // ---------- Altura y factor de equilibrio (FE = altura der - altura izq) ----------

    private int altura(NodoAVL n) {
        return n == null ? 0 : n.altura;
    }

    private void actualizar(NodoAVL n) {
        n.altura = 1 + Math.max(altura(n.izq), altura(n.der));
    }

    private int fe(NodoAVL n) {
        return n == null ? 0 : altura(n.der) - altura(n.izq);
    }

    // ---------- Rotaciones ----------

    private NodoAVL rotarDerecha(NodoAVL y) {
        NodoAVL x = y.izq;
        y.izq = x.der;
        x.der = y;
        actualizar(y);
        actualizar(x);
        return x;
    }

    private NodoAVL rotarIzquierda(NodoAVL x) {
        NodoAVL y = x.der;
        x.der = y.izq;
        y.izq = x;
        actualizar(x);
        actualizar(y);
        return y;
    }

    private NodoAVL balancear(NodoAVL n) {
        actualizar(n);
        int f = fe(n);

        if (f < -1) {                       // pesado a la izquierda
            if (fe(n.izq) > 0) {            // caso izquierda-derecha
                n.izq = rotarIzquierda(n.izq);
            }
            return rotarDerecha(n);
        }
        if (f > 1) {                        // pesado a la derecha
            if (fe(n.der) < 0) {            // caso derecha-izquierda
                n.der = rotarDerecha(n.der);
            }
            return rotarIzquierda(n);
        }
        return n;
    }

    // ---------- Insertar ----------

    public boolean insertar(T dato) {
        modificado = false;
        raiz = insertar(raiz, dato);
        if (modificado) {
            tamano++;
        }
        return modificado;
    }

    private NodoAVL insertar(NodoAVL n, T dato) {
        if (n == null) {
            modificado = true;
            return new NodoAVL(dato);
        }
        int c = comparador.compare(dato, n.dato);
        if (c < 0) {
            n.izq = insertar(n.izq, dato);
        } else if (c > 0) {
            n.der = insertar(n.der, dato);
        } else {
            return n;                       // duplicado: no se inserta
        }
        return balancear(n);
    }

    // ---------- Eliminar (con dos hijos: sucesor del subarbol derecho) ----------

    public boolean eliminar(T dato) {
        modificado = false;
        raiz = eliminar(raiz, dato);
        if (modificado) {
            tamano--;
        }
        return modificado;
    }

    private NodoAVL eliminar(NodoAVL n, T dato) {
        if (n == null) {
            return null;
        }
        int c = comparador.compare(dato, n.dato);
        if (c < 0) {
            n.izq = eliminar(n.izq, dato);
        } else if (c > 0) {
            n.der = eliminar(n.der, dato);
        } else {
            modificado = true;
            if (n.izq == null) {
                return n.der;
            }
            if (n.der == null) {
                return n.izq;
            }
            NodoAVL sucesor = n.der;
            while (sucesor.izq != null) {
                sucesor = sucesor.izq;
            }
            n.dato = sucesor.dato;
            n.der = eliminar(n.der, sucesor.dato);
        }
        return balancear(n);
    }

    // ---------- Buscar ----------

    public T buscar(T dato) {
        NodoAVL n = raiz;
        while (n != null) {
            int c = comparador.compare(dato, n.dato);
            if (c == 0) {
                return n.dato;
            }
            n = (c < 0) ? n.izq : n.der;
        }
        return null;
    }

    // ---------- Busqueda por rango (omite subarboles que no aportan) ----------
    // Requiere que el comparador ordene primero por la clave indicada.

    public List<T> rango(ToIntFunction<T> clave, int min, int max) {
        List<T> resultado = new ArrayList<>();
        if (min <= max) {
            rango(raiz, clave, min, max, resultado);
        }
        return resultado;
    }

    private void rango(NodoAVL n, ToIntFunction<T> clave, int min, int max, List<T> r) {
        if (n == null) {
            return;
        }
        int k = clave.applyAsInt(n.dato);
        if (k >= min) {
            rango(n.izq, clave, min, max, r);
        }
        if (k >= min && k <= max) {
            r.add(n.dato);
        }
        if (k <= max) {
            rango(n.der, clave, min, max, r);
        }
    }

    // ---------- Recorridos ----------

    public List<T> inorden() {
        List<T> r = new ArrayList<>();
        inorden(raiz, r);
        return r;
    }

    private void inorden(NodoAVL n, List<T> r) {
        if (n == null) {
            return;
        }
        inorden(n.izq, r);
        r.add(n.dato);
        inorden(n.der, r);
    }

    public List<T> mayores(int cantidad) {
        List<T> r = new ArrayList<>();
        mayores(raiz, cantidad, r);
        return r;
    }

    private void mayores(NodoAVL n, int cantidad, List<T> r) {
        if (n == null || r.size() >= cantidad) {
            return;
        }
        mayores(n.der, cantidad, r);
        if (r.size() < cantidad) {
            r.add(n.dato);
        }
        mayores(n.izq, cantidad, r);
    }

    public int altura() {
        return altura(raiz);
    }

    public int tamano() {
        return tamano;
    }

    public boolean estaVacio() {
        return raiz == null;
    }
}