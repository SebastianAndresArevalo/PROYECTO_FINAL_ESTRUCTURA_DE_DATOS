package servicio;

public class OperacionException extends RuntimeException {

    public OperacionException(String mensaje) {
        super(mensaje);
    }
}