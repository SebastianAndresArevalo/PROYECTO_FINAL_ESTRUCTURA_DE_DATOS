package modelo;

import java.time.LocalDateTime;

public enum Periodicidad {
    UNICA, DIARIA, SEMANAL, MENSUAL;

    // Fecha de la siguiente ejecucion (null si es unica)
    public LocalDateTime siguiente(LocalDateTime fecha) {
        switch (this) {
            case DIARIA:  return fecha.plusDays(1);
            case SEMANAL: return fecha.plusWeeks(1);
            case MENSUAL: return fecha.plusMonths(1);
            default:      return null;
        }
    }
}