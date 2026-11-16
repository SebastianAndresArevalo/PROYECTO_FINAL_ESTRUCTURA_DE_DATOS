package servicio;

// Una fila de un ranking: una clave (id de usuario, codigo de billetera, categoria...) y su valor
public class Posicion {

    private final String clave;
    private final double valor;

    public Posicion(String clave, double valor) {
        this.clave = clave;
        this.valor = valor;
    }

    public String getClave() {
        return clave;
    }

    public double getValor() {
        return valor;
    }

    @Override
    public String toString() {
        String v = (valor == Math.rint(valor)) ? String.valueOf((long) valor) : String.valueOf(valor);
        return clave + " = " + v;
    }
}
