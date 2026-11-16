package servicio;

import estructuras.ArbolAVL;
import modelo.Billetera;
import modelo.TipoTransaccion;
import modelo.Transaccion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Informes e indicadores de uso. Todas las consultas aceptan un rango de fechas
// (desde / hasta, null = sin limite) y solo cuentan movimientos EJECUTADOS que no
// sean reversiones: lo revertido y lo rechazado no aparece en los indicadores.
public class AnaliticaServicio {

    private final RepositorioDatos repo;

    public AnaliticaServicio(RepositorioDatos repo) {
        this.repo = repo;
    }

    // Billeteras con mas movimientos (cuenta cada vez que aparece como origen o destino)
    public List<Posicion> billeterasMasUsadas(int n, LocalDateTime desde, LocalDateTime hasta) {
        FiltroMovimientos.validarCantidad(n);
        return ranking(usoPorBilletera(desde, hasta), n);
    }

    // Usuarios que mas transferencias han realizado (como quien envia)
    public List<Posicion> usuariosConMasTransferencias(int n, LocalDateTime desde, LocalDateTime hasta) {
        FiltroMovimientos.validarCantidad(n);

        Map<String, Double> conteo = new HashMap<>();
        for (Transaccion t : FiltroMovimientos.validas(repo, desde, hasta)) {
            if (FiltroMovimientos.esTransferencia(t)) {
                sumar(conteo, FiltroMovimientos.duenoDe(repo, t.getBilleteraOrigen()));
            }
        }
        return ranking(conteo, n);
    }

    // Tipos de billetera mas activos (suma del uso de todas sus billeteras)
    public List<Posicion> categoriasMasActivas(LocalDateTime desde, LocalDateTime hasta) {
        Map<String, Double> conteo = new HashMap<>();
        for (Map.Entry<String, Double> e : usoPorBilletera(desde, hasta).entrySet()) {
            Billetera b = repo.getBilleteras().get(e.getKey());
            conteo.merge(b.getTipo().name(), e.getValue(), Double::sum);
        }
        return ranking(conteo, Integer.MAX_VALUE);
    }

    // Cuantas transacciones hubo de cada tipo
    public Map<TipoTransaccion, Integer> frecuenciaPorTipo(LocalDateTime desde, LocalDateTime hasta) {
        Map<TipoTransaccion, Integer> frecuencia = new EnumMap<>(TipoTransaccion.class);
        for (TipoTransaccion tipo : TipoTransaccion.values()) {
            if (tipo != TipoTransaccion.REVERSION) {
                frecuencia.put(tipo, 0);
            }
        }
        for (Transaccion t : FiltroMovimientos.validas(repo, desde, hasta)) {
            frecuencia.merge(t.getTipo(), 1, Integer::sum);
        }
        return frecuencia;
    }

    // Suma del valor de todos los movimientos del rango
    public double montoTotalMovilizado(LocalDateTime desde, LocalDateTime hasta) {
        double total = 0;
        for (Transaccion t : FiltroMovimientos.validas(repo, desde, hasta)) {
            total += t.getValor();
        }
        return total;
    }

    // Usuario con mas transacciones iniciadas en el periodo (null si no hubo ninguna)
    public Posicion usuarioMasActivo(LocalDateTime desde, LocalDateTime hasta) {
        Map<String, Double> conteo = new HashMap<>();
        for (Transaccion t : FiltroMovimientos.validas(repo, desde, hasta)) {
            String codigo = (t.getBilleteraOrigen() != null) ? t.getBilleteraOrigen() : t.getBilleteraDestino();
            sumar(conteo, FiltroMovimientos.duenoDe(repo, codigo));
        }
        List<Posicion> top = ranking(conteo, 1);
        return top.isEmpty() ? null : top.get(0);
    }

    // Las n transacciones de mayor valor, de mayor a menor, usando un ArbolAVL
    public List<Transaccion> transaccionesMayorValor(int n, LocalDateTime desde, LocalDateTime hasta) {
        FiltroMovimientos.validarCantidad(n);

        // En empate de valor, la mas antigua se considera "mayor"
        Comparator<Transaccion> orden = Comparator.comparingDouble(Transaccion::getValor)
                .thenComparing(Transaccion::getId, FiltroMovimientos.POR_ID.reversed());

        ArbolAVL<Transaccion> arbol = new ArbolAVL<>(orden);
        for (Transaccion t : FiltroMovimientos.validas(repo, desde, hasta)) {
            arbol.insertar(t);
        }
        return arbol.mayores(n);
    }

    // ---------- Apoyo ----------

    private Map<String, Double> usoPorBilletera(LocalDateTime desde, LocalDateTime hasta) {
        Map<String, Double> uso = new HashMap<>();
        for (Transaccion t : FiltroMovimientos.validas(repo, desde, hasta)) {
            if (repo.getBilleteras().containsKey(t.getBilleteraOrigen())) {
                sumar(uso, t.getBilleteraOrigen());
            }
            if (repo.getBilleteras().containsKey(t.getBilleteraDestino())) {
                sumar(uso, t.getBilleteraDestino());
            }
        }
        return uso;
    }

    private void sumar(Map<String, Double> conteo, String clave) {
        if (clave != null) {
            conteo.merge(clave, 1.0, Double::sum);
        }
    }

    // Mayor a menor valor; en empate, por id
    private List<Posicion> ranking(Map<String, Double> valores, int n) {
        List<Posicion> lista = new ArrayList<>();
        for (Map.Entry<String, Double> e : valores.entrySet()) {
            lista.add(new Posicion(e.getKey(), e.getValue()));
        }
        lista.sort(Comparator.comparingDouble(Posicion::getValor).reversed()
                .thenComparing(Posicion::getClave, FiltroMovimientos.POR_ID));
        return lista.size() > n ? new ArrayList<>(lista.subList(0, n)) : lista;
    }
}
