package uv.lis;

import java.util.List;
import java.util.Map;

/**
 * Casos de prueba para la cache LRU. No usa bibliotecas externas (como JUnit)
 * para que pueda compilarse y ejecutarse solo con el JDK.
 *
 * <p>Cubre el funcionamiento normal y los casos limite exigidos por el
 * proyecto: cache vacia, cache llena, actualizacion, capacidad invalida y
 * orden de uso.</p>
 *
 * <p>Ejecucion: {@code java Pruebas} (o {@code make test}). Termina con codigo
 * 0 si todas las pruebas pasan, o 1 si alguna falla.</p>
 *
 */
public class Pruebas {

    private static int total = 0;
    private static int fallidas = 0;

    /** Interfaz funcional que representa una prueba ejecutable. */
    interface Prueba {
        void ejecutar();
    }

    public static void main(String[] args) {
        System.out.println("Ejecutando pruebas...\n");

        correr("insercion_y_consulta", Pruebas::pruebaInsercionYConsulta);
        correr("expulsion_lru", Pruebas::pruebaExpulsionLru);
        correr("actualizacion_no_expulsa", Pruebas::pruebaActualizacionNoExpulsa);
        correr("cache_vacia", Pruebas::pruebaCacheVacia);
        correr("capacidad_invalida", Pruebas::pruebaCapacidadInvalida);
        correr("orden_de_uso", Pruebas::pruebaOrdenDeUso);
        correr("cache_llena_capacidad_uno", Pruebas::pruebaCacheLlenaCapacidadUno);

        System.out.printf("%n%d/%d pruebas superadas.%n", total - fallidas, total);
        System.exit(fallidas == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------ //
    // Infraestructura de pruebas                                         //
    // ------------------------------------------------------------------ //

    private static void correr(String nombre, Prueba prueba) {
        total++;
        try {
            prueba.ejecutar();
            System.out.println("  [OK]    " + nombre);
        } catch (AssertionError e) {
            fallidas++;
            System.out.println("  [FALLA] " + nombre + ": " + e.getMessage());
        }
    }

    private static void afirmar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }

    // ------------------------------------------------------------------ //
    // Casos de prueba                                                    //
    // ------------------------------------------------------------------ //

    static void pruebaInsercionYConsulta() {
        CacheLRU<String, Integer> c = new CacheLRU<>(2);
        c.put("a", 1);
        c.put("b", 2);
        afirmar(c.get("a") == 1, "se esperaba a=1");
        afirmar(c.get("b") == 2, "se esperaba b=2");
    }

    static void pruebaExpulsionLru() {
        CacheLRU<String, Integer> c = new CacheLRU<>(2);
        c.put("a", 1);
        c.put("b", 2);
        c.get("a");                 // 'a' pasa a ser MRU; 'b' queda como LRU
        c.put("c", 3);              // cache llena -> debe expulsar a 'b'
        afirmar(c.get("b") == null, "'b' deberia haber sido expulsada");
        afirmar(c.get("a") == 1, "se esperaba a=1");
        afirmar(c.get("c") == 3, "se esperaba c=3");
        afirmar(c.getExpulsiones() == 1, "se esperaba 1 expulsion");
    }

    static void pruebaActualizacionNoExpulsa() {
        CacheLRU<String, Integer> c = new CacheLRU<>(2);
        c.put("a", 1);
        c.put("b", 2);
        c.put("a", 10);             // actualiza, no inserta uno nuevo
        afirmar(c.get("a") == 10, "se esperaba a=10");
        afirmar(c.tamano() == 2, "el tamano deberia seguir en 2");
        afirmar(c.getExpulsiones() == 0, "no deberia haber expulsiones");
    }

    static void pruebaCacheVacia() {
        CacheLRU<String, Integer> c = new CacheLRU<>(3);
        afirmar(c.estaVacia(), "la cache deberia estar vacia");
        afirmar(c.get("x") == null, "consultar en cache vacia deberia dar null");
        afirmar(c.getFallos() == 1, "se esperaba 1 fallo");
    }

    static void pruebaCapacidadInvalida() {
        int[] capacidades = {0, -1, -100};
        for (int cap : capacidades) {
            boolean lanzo = false;
            try {
                new CacheLRU<String, Integer>(cap);
            } catch (IllegalArgumentException e) {
                lanzo = true;
            }
            afirmar(lanzo, "se esperaba IllegalArgumentException para capacidad=" + cap);
        }
    }

    static void pruebaOrdenDeUso() {
        CacheLRU<String, Integer> c = new CacheLRU<>(3);
        c.put("a", 1);
        c.put("b", 2);
        c.put("c", 3);
        c.get("a");                 // 'a' se vuelve MRU
        // Orden esperado de MRU a LRU: a, c, b
        List<Map.Entry<String, Integer>> cont = c.contenido();
        String[] esperado = {"a", "c", "b"};
        afirmar(cont.size() == 3, "se esperaban 3 elementos");
        for (int i = 0; i < esperado.length; i++) {
            afirmar(cont.get(i).getKey().equals(esperado[i]),
                    "orden incorrecto en la posicion " + i);
        }
    }

    static void pruebaCacheLlenaCapacidadUno() {
        CacheLRU<String, Integer> c = new CacheLRU<>(1);
        c.put("a", 1);
        c.put("b", 2);              // expulsa 'a'
        afirmar(c.get("a") == null, "'a' deberia haber sido expulsada");
        afirmar(c.get("b") == 2, "se esperaba b=2");
        afirmar(c.tamano() == 1, "el tamano deberia ser 1");
    }
}