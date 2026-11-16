package modelo;

public enum TipoTransaccion {
    RECARGA(1),
    RETIRO(2),
    TRANSFERENCIA(3),
    PAGO_PROGRAMADO(3),
    REVERSION(0);

    private final int puntosPorCien;

    TipoTransaccion(int puntosPorCien) {
        this.puntosPorCien = puntosPorCien;
    }

    public int getPuntosPorCien() {
        return puntosPorCien;
    }
}