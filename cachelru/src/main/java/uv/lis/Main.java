package uv.lis;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

/**
 * Interfaz de linea de comandos (CLI) para demostrar la cache LRU.
 *
 * <p>Modos de uso:</p>
 * <pre>
 *   java Main          -&gt; menu interactivo
 *   java Main --sim     -&gt; ejecuta una simulacion automatica y termina
 * </pre>
 *
 * <p>Todas las operaciones quedan registradas en {@code cache_operaciones.log}.</p>
 *
 */
public class Main {

    private static final String ARCHIVO_LOG = "cache_operaciones.log";

    public static void main(String[] args) {
        RegistroOperaciones registro = new RegistroOperaciones(ARCHIVO_LOG);
        System.out.println("Cache LRU - Proyecto Final\n");

        // Modo simulacion no interactivo (util para 'make sim').
        for (String arg : args) {
            if ("--sim".equals(arg)) {
                ejecutarSimulacion(new CacheLRU<String, String>(3, registro), 20, 8, 42L);
                System.out.println("Las operaciones se registraron en '" + ARCHIVO_LOG + "'.");
                registro.cerrar();
                return;
            }
        }

        Scanner sc = new Scanner(System.in);

        // Modo interactivo: pedir y validar la capacidad.
        System.out.print("Capacidad de la cache [por defecto 3]: ");
        int capacidad = 3;
        if (sc.hasNextLine()) {
            String entrada = sc.nextLine().trim();
            try {
                if (!entrada.isEmpty()) {
                    capacidad = Integer.parseInt(entrada);
                }
            } catch (NumberFormatException e) {
                System.out.println("Capacidad invalida. Se usa capacidad = 3.");
                capacidad = 3;
            }
        }

        CacheLRU<String, String> cache;
        try {
            cache = new CacheLRU<>(capacidad, registro);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage() + " Se usa capacidad = 3.");
            cache = new CacheLRU<>(3, registro);
        }

        menuInteractivo(cache, sc);

        System.out.println("\nLas operaciones se registraron en '" + ARCHIVO_LOG + "'.");
        registro.cerrar();
        sc.close();
    }

    /** Muestra el contenido de la cache de mas a menos reciente. */
    static <K, V> void imprimirEstado(CacheLRU<K, V> cache) {
        System.out.println("\nEstado actual de la cache (de mas a menos reciente):");
        List<Map.Entry<K, V>> contenido = cache.contenido();
        if (contenido.isEmpty()) {
            System.out.println("  [cache vacia]");
        } else {
            int total = contenido.size();
            for (int i = 0; i < total; i++) {
                Map.Entry<K, V> e = contenido.get(i);
                String etiqueta = (i == 0) ? "<- MRU" : (i == total - 1 ? "<- LRU" : "");
                System.out.printf("  %d. %s = %s  %s%n", i + 1, e.getKey(), e.getValue(), etiqueta);
            }
        }
        System.out.println();
    }

    /** Muestra las metricas acumuladas de la cache. */
    static <K, V> void imprimirEstadisticas(CacheLRU<K, V> cache) {
        System.out.println("\n--- Estadisticas ---");
        System.out.println(cache.estadisticas());
        System.out.println();
    }

    /**
     * Genera una secuencia de operaciones aleatorias para demostrar la cache.
     * Usa una semilla fija para que los resultados sean reproducibles.
     */
    static void ejecutarSimulacion(CacheLRU<String, String> cache,
                                   int numOperaciones, int clavesPosibles, long semilla) {
        Random rng = new Random(semilla);
        System.out.printf("%n>>> Simulacion: %d operaciones aleatorias sobre %d claves "
                        + "posibles (capacidad=%d)%n%n",
                numOperaciones, clavesPosibles, cache.getCapacidad());

        for (int i = 1; i <= numOperaciones; i++) {
            String clave = "K" + (rng.nextInt(clavesPosibles) + 1);
            if (rng.nextDouble() < 0.6) {                 // 60% consultas, 40% inserciones
                String valor = cache.get(clave);
                String estado = (valor != null) ? "ACIERTO (" + valor + ")" : "FALLO";
                System.out.printf("  %2d. GET  %-4s -> %s%n", i, clave, estado);
            } else {
                String valor = String.valueOf(100 + rng.nextInt(900));
                cache.put(clave, valor);
                System.out.printf("  %2d. PUT  %-4s = %s%n", i, clave, valor);
            }
        }

        imprimirEstado(cache);
        imprimirEstadisticas(cache);
    }

    /** Bucle principal del menu interactivo. */
    static void menuInteractivo(CacheLRU<String, String> cache, Scanner sc) {
        final String opciones =
                "\n=========== CACHE LRU - Menu ===========\n" +
                "  1. put  (insertar / actualizar)\n" +
                "  2. get  (consultar)\n" +
                "  3. Ver estado de la cache\n" +
                "  4. Ver estadisticas\n" +
                "  5. Ejecutar simulacion automatica\n" +
                "  6. Salir\n" +
                "========================================\n" +
                "Opcion: ";

        while (true) {
            System.out.print(opciones);
            if (!sc.hasNextLine()) {
                break;
            }
            String opcion = sc.nextLine().trim();

            switch (opcion) {
                case "1": {
                    System.out.print("  Clave: ");
                    String clave = sc.hasNextLine() ? sc.nextLine().trim() : "";
                    System.out.print("  Valor: ");
                    String valor = sc.hasNextLine() ? sc.nextLine().trim() : "";
                    if (clave.isEmpty()) {
                        System.out.println("  -> la clave no puede estar vacia");
                    } else {
                        cache.put(clave, valor);
                        System.out.printf("  -> almacenado %s = %s%n", clave, valor);
                    }
                    break;
                }
                case "2": {
                    System.out.print("  Clave: ");
                    String clave = sc.hasNextLine() ? sc.nextLine().trim() : "";
                    String valor = cache.get(clave);
                    if (valor == null) {
                        System.out.printf("  -> FALLO: '%s' no esta en la cache%n", clave);
                    } else {
                        System.out.printf("  -> ACIERTO: %s = %s%n", clave, valor);
                    }
                    break;
                }
                case "3":
                    imprimirEstado(cache);
                    break;
                case "4":
                    imprimirEstadisticas(cache);
                    break;
                case "5":
                    ejecutarSimulacion(cache, 20, 8, 42L);
                    break;
                case "6":
                    System.out.println("Saliendo...");
                    return;
                default:
                    System.out.println("  Opcion no valida.");
            }
        }
    }
}