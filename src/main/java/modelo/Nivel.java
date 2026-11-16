package modelo;

public enum Nivel {
    BRONCE(0, 500, 2.0, 0, 10),
    PLATA(501, 1000, 1.5, 5, 20),
    ORO(1001, 5000, 1.0, 10, 50),
    PLATINO(5001, Integer.MAX_VALUE, 0.0, 20, 200);

    private final int minimo;
    private final int maximo;
    private final double comision;      // % de comision por operacion
    private final int bonoPuntos;       // % extra de puntos
    private final int limiteDiario;     // transacciones por dia

    Nivel(int minimo, int maximo, double comision, int bonoPuntos, int limiteDiario) {
        this.minimo = minimo;
        this.maximo = maximo;
        this.comision = comision;
        this.bonoPuntos = bonoPuntos;
        this.limiteDiario = limiteDiario;
    }

    public int getMinimo() { return minimo; }
    public int getMaximo() { return maximo; }
    public double getComision() { return comision; }
    public int getBonoPuntos() { return bonoPuntos; }
    public int getLimiteDiario() { return limiteDiario; }

    public static Nivel dePuntos(int puntos) {
        for (Nivel n : values()) {
            if (puntos >= n.minimo && puntos <= n.maximo) {
                return n;
            }
        }
        return BRONCE;
    }
}