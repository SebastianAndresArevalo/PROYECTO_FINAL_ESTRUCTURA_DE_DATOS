package modelo;

import java.util.ArrayList;
import java.util.List;

public class Usuario {

    private String id;
    private String nombre;
    private String correo;
    private int puntos;
    private Nivel nivel;
    private List<Billetera> billeteras;
    private List<Transaccion> historial;
    private List<Notificacion> notificaciones;
    private List<Canje> canjes;                       // junto a los demás campos

    public Usuario(String id, String nombre, String correo) {
        this.canjes = new ArrayList<>();
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.puntos = 0;
        this.nivel = Nivel.BRONCE;
        this.billeteras = new ArrayList<>();
        this.historial = new ArrayList<>();
        this.notificaciones = new ArrayList<>();
    }

    public void agregarBilletera(Billetera b) {
        billeteras.add(b);
    }

    public boolean sumarPuntos(int p) {
        Nivel anterior = nivel;
        puntos += p;
        nivel = Nivel.dePuntos(puntos);
        return nivel.ordinal() > anterior.ordinal();
    }

    public void restarPuntos(int p) {
        puntos = Math.max(0, puntos - p);
        nivel = Nivel.dePuntos(puntos);
    }

    public List<Canje> getCanjes() { return canjes; }
    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public int getPuntos() { return puntos; }
    public Nivel getNivel() { return nivel; }
    public List<Billetera> getBilleteras() { return billeteras; }
    public List<Transaccion> getHistorial() { return historial; }
    public List<Notificacion> getNotificaciones() { return notificaciones; }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setCorreo(String correo) { this.correo = correo; }

    @Override
    public String toString() {
        return id + " - " + nombre + " (" + nivel + ", " + puntos + " pts)";
    }
}