package modelo;

public class Beneficio {

    private String id;
    private String nombre;
    private int costo;
    private Nivel nivelMinimo;

    public Beneficio(String id, String nombre, int costo, Nivel nivelMinimo) {
        this.id = id;
        this.nombre = nombre;
        this.costo = costo;
        this.nivelMinimo = nivelMinimo;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public int getCosto() { return costo; }
    public Nivel getNivelMinimo() { return nivelMinimo; }

    @Override
    public String toString() {
        return id + " - " + nombre + " (" + costo + " pts, desde " + nivelMinimo + ")";
    }
}