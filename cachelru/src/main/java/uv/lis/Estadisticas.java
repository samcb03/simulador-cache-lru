package uv.lis;

/**
 * Objeto de valor (inmutable) que agrupa las metricas acumuladas de la cache.
 *
 * <p>Se calcula a partir de los contadores de la {@link CacheLRU} en un
 * instante dado. La tasa de aciertos se expresa como porcentaje.</p>
 *
 */
public final class Estadisticas {

    private final int capacidad;
    private final int elementos;
    private final int aciertos;
    private final int fallos;
    private final int expulsiones;
    private final int consultasTotales;
    private final double tasaAciertos;

    /**
     * Construye el resumen de metricas.
     *
     * @param capacidad   capacidad maxima de la cache.
     * @param elementos   numero de elementos actuales.
     * @param aciertos    consultas exitosas.
     * @param fallos      consultas fallidas.
     * @param expulsiones elementos expulsados por la politica LRU.
     */
    public Estadisticas(int capacidad, int elementos, int aciertos, int fallos, int expulsiones) {
        this.capacidad = capacidad;
        this.elementos = elementos;
        this.aciertos = aciertos;
        this.fallos = fallos;
        this.expulsiones = expulsiones;
        this.consultasTotales = aciertos + fallos;
        this.tasaAciertos = (consultasTotales == 0)
                ? 0.0
                : Math.round(aciertos * 10000.0 / consultasTotales) / 100.0;
    }

    public int getCapacidad() { return capacidad; }
    public int getElementos() { return elementos; }
    public int getAciertos() { return aciertos; }
    public int getFallos() { return fallos; }
    public int getExpulsiones() { return expulsiones; }
    public int getConsultasTotales() { return consultasTotales; }
    public double getTasaAciertos() { return tasaAciertos; }

    @Override
    public String toString() {
        return String.format(
                "  %-20s: %d%n" +
                "  %-20s: %d%n" +
                "  %-20s: %d%n" +
                "  %-20s: %d%n" +
                "  %-20s: %d%n" +
                "  %-20s: %d%n" +
                "  %-20s: %.2f",
                "capacidad", capacidad,
                "elementos", elementos,
                "aciertos", aciertos,
                "fallos", fallos,
                "expulsiones", expulsiones,
                "consultas_totales", consultasTotales,
                "tasa_aciertos_%", tasaAciertos);
    }
}