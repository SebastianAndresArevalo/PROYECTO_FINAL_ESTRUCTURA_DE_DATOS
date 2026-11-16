package modelo;

public enum ReglaRiesgo {
    RAFAGA_TRANSFERENCIAS(2, "Transferencias consecutivas en poco tiempo"),
    MONTO_INUSUAL(3, "Monto inusualmente alto para el usuario"),
    DESTINO_REPETIDO(2, "Movimientos repetidos hacia un mismo destino"),
    FRAGMENTACION(3, "Monto fragmentado entre varias billeteras"),
    CAMBIO_FRECUENCIA(2, "Cambio brusco en la frecuencia de uso"),
    HORARIO_INUSUAL(1, "Actividad en un horario no habitual");

    private final int peso;
    private final String descripcion;

    ReglaRiesgo(int peso, String descripcion) {
        this.peso = peso;
        this.descripcion = descripcion;
    }

    public int getPeso() { return peso; }
    public String getDescripcion() { return descripcion; }
}