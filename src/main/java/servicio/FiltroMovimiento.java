package servicio;

import modelo.Billetera;
import modelo.EstadoTransaccion;
import modelo.TipoTransaccion;
import modelo.Transaccion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Reglas comunes de la etapa 6: que transacciones cuentan para el grafo y la analitica
final class FiltroMovimientos {

    // Ordena ids como "B2" < "B10" (primero por largo, luego alfabetico)
    static final Comparator<String> POR_ID =
            Comparator.<String>comparingInt(String::length).thenComparing(Comparator.<String>naturalOrder());

    private FiltroMovimientos() {
    }

    static void validarRango(LocalDateTime desde, LocalDateTime hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new OperacionException("La fecha inicial no puede ser posterior a la final");
        }
    }

    static void validarCantidad(int n) {
        if (n <= 0) {
            throw new OperacionException("La cantidad solicitada debe ser mayor a cero");
        }
    }

    static boolean enRango(Transaccion t, LocalDateTime desde, LocalDateTime hasta) {
        LocalDateTime f = t.getFecha();
        return (desde == null || !f.isBefore(desde)) && (hasta == null || !f.isAfter(hasta));
    }

    // Movimiento valido: ejecutado y que no sea una reversion. Las transacciones
    // revertidas y las rechazadas no cuentan.
    static boolean esValida(Transaccion t) {
        return t.getEstado() == EstadoTransaccion.EJECUTADA && t.getTipo() != TipoTransaccion.REVERSION;
    }

    // Transferencia o pago programado entre dos billeteras
    static boolean esTransferencia(Transaccion t) {
        return esValida(t)
                && (t.getTipo() == TipoTransaccion.TRANSFERENCIA || t.getTipo() == TipoTransaccion.PAGO_PROGRAMADO)
                && t.getBilleteraOrigen() != null
                && t.getBilleteraDestino() != null;
    }

    // Movimientos validos del rango, en orden cronologico (null = sin limite)
    static List<Transaccion> validas(RepositorioDatos repo, LocalDateTime desde, LocalDateTime hasta) {
        validarRango(desde, hasta);

        List<Transaccion> lista = new ArrayList<>();
        for (Transaccion t : repo.getTransacciones().values()) {
            if (esValida(t) && enRango(t, desde, hasta)) {
                lista.add(t);
            }
        }
        lista.sort(Comparator.comparing(Transaccion::getFecha)
                .thenComparing(Transaccion::getId, POR_ID));
        return lista;
    }

    static String duenoDe(RepositorioDatos repo, String codigoBilletera) {
        if (codigoBilletera == null) {
            return null;
        }
        Billetera b = repo.getBilleteras().get(codigoBilletera);
        return b == null ? null : b.getIdUsuario();
    }
}
