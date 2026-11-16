package modelo;

import java.time.LocalDateTime;

public class Canje {

    private String idBeneficio;
    private String nombre;
    private int puntosGastados;
    private LocalDateTime fecha;

    public Canje(String idBeneficio, String nombre, int puntosGastados) {
        this.idBeneficio = idBeneficio;
        this.nombre = nombre;
        this.puntosGastados = puntosGastados;
        this.fecha = LocalDateTime.now();
    }

    public String getIdBeneficio() { return idBeneficio; }
    public String getNombre() { return nombre; }
    public int getPuntosGastados() { return puntosGastados; }
    public LocalDateTime getFecha() { return fecha; }

    @Override
    public String toString() {
        return nombre + " (-" + puntosGastados + " pts)";
    }
}