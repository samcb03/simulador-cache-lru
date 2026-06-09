package uv.lis;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Cache generica con politica de reemplazo LRU (Least Recently Used).
 *
 * <p>Integra dos estructuras de datos de la biblioteca estandar de Java:</p>
 * <ul>
 *   <li>{@link HashMap} (tabla hash): mapea clave -&gt; valor en O(1) promedio.</li>
 *   <li>{@link Deque} implementado con {@link LinkedList} (cola doblemente
 *       enlazada): mantiene el orden de uso. El primer elemento es el menos
 *       recientemente usado (LRU) y el ultimo es el mas reciente (MRU).</li>
 * </ul>
 *
 * <p>La combinacion permite que {@code get} y {@code put} operen en O(1)
 * promedio para la busqueda y la expulsion; reubicar una clave usada al
 * extremo MRU recorre la cola, por lo que es O(n).</p>
 *
 * @param <K> tipo de las claves.
 * @param <V> tipo de los valores.
 */
public class CacheLRU<K, V> {

    private final int capacidad;
    private final Map<K, V> tabla;     // tabla hash: clave -> valor
    private final Deque<K> orden;      // orden de uso: primero = LRU, ultimo = MRU

    private int aciertos;
    private int fallos;
    private int expulsiones;

    private final RegistroOperaciones registro;

    /**
     * Crea una cache sin registro de operaciones.
     *
     * @param capacidad capacidad maxima (entero positivo).
     */
    public CacheLRU(int capacidad) {
        this(capacidad, null);
    }

    /**
     * Crea una cache con un registro de operaciones opcional.
     *
     * @param capacidad capacidad maxima (entero positivo).
     * @param registro  registro de operaciones, o {@code null} para no registrar.
     * @throws IllegalArgumentException si la capacidad no es un entero positivo.
     */
    public CacheLRU(int capacidad, RegistroOperaciones registro) {
        if (capacidad <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser un entero positivo.");
        }
        this.capacidad = capacidad;
        this.tabla = new HashMap<>();
        this.orden = new LinkedList<>();
        this.registro = registro;
        this.aciertos = 0;
        this.fallos = 0;
        this.expulsiones = 0;
    }

    // ------------------------------------------------------------------ //
    // Consultas de estado                                                //
    // ------------------------------------------------------------------ //

    public int tamano() { return tabla.size(); }

    public int getCapacidad() { return capacidad; }

    public boolean estaLlena() { return tabla.size() >= capacidad; }

    public boolean estaVacia() { return tabla.isEmpty(); }

    public int getAciertos() { return aciertos; }

    public int getFallos() { return fallos; }

    public int getExpulsiones() { return expulsiones; }

    // ------------------------------------------------------------------ //
    // Apoyo interno                                                      //
    // ------------------------------------------------------------------ //

    /**
     * Mueve una clave existente al extremo MRU (final) de la cola.
     *
     * <p>La cola no permite acceso directo a un elemento interno, por lo que
     * {@code remove} la recorre: esta operacion es O(n). Es el costo de usar
     * una cola nativa en lugar de una lista enlazada con referencias directas
     * a los nodos.</p>
     */
    private void marcarComoReciente(K clave) {
        orden.remove(clave);
        orden.addLast(clave);
    }

    private void anotar(String operacion, K clave, String resultado, String detalle) {
        if (registro != null) {
            registro.registrar(operacion, String.valueOf(clave), resultado, detalle);
        }
    }

    // ------------------------------------------------------------------ //
    // Operaciones fundamentales                                          //
    // ------------------------------------------------------------------ //

    /**
     * Consulta el valor asociado a una clave.
     *
     * <p>En caso de acierto marca el elemento como el mas recientemente usado y
     * devuelve su valor; en caso de fallo devuelve {@code null}.</p>
     *
     * @param clave clave a consultar.
     * @return el valor asociado, o {@code null} si no esta en la cache.
     */
    public V get(K clave) {
        if (tabla.containsKey(clave)) {
            marcarComoReciente(clave);
            aciertos++;
            V valor = tabla.get(clave);
            anotar("GET", clave, "ACIERTO", "valor=" + valor);
            return valor;
        }
        fallos++;
        anotar("GET", clave, "FALLO", "clave no encontrada");
        return null;
    }

    /**
     * Inserta o actualiza un par clave-valor.
     *
     * <p>Si la clave ya existe, actualiza su valor y la marca como MRU. Si es
     * nueva y la cache esta llena, expulsa primero el elemento LRU.</p>
     *
     * @param clave clave a insertar o actualizar.
     * @param valor valor asociado.
     */
    public void put(K clave, V valor) {
        // Caso 1: la clave ya existe -> actualizar, sin expulsar.
        if (tabla.containsKey(clave)) {
            tabla.put(clave, valor);
            marcarComoReciente(clave);
            anotar("PUT", clave, "ACTUALIZA", "valor=" + valor);
            return;
        }
        // Caso 2: clave nueva y cache llena -> expulsar el LRU (caso limite).
        if (estaLlena()) {
            K lru = orden.removeFirst();   // O(1): primer elemento = LRU
            tabla.remove(lru);
            expulsiones++;
            anotar("PUT", clave, "EXPULSA", "se elimino LRU clave=" + lru);
        }
        // Caso 3: insertar el nuevo par y marcarlo como MRU.
        tabla.put(clave, valor);
        orden.addLast(clave);
        anotar("PUT", clave, "INSERTA", "valor=" + valor);
    }

    // ------------------------------------------------------------------ //
    // Reportes                                                           //
    // ------------------------------------------------------------------ //

    /**
     * Devuelve el contenido de la cache ordenado del mas reciente (MRU) al
     * menos reciente (LRU).
     *
     * @return lista de pares (clave, valor).
     */
    public List<Map.Entry<K, V>> contenido() {
        List<Map.Entry<K, V>> lista = new ArrayList<>();
        Iterator<K> it = orden.descendingIterator();   // del MRU (ultimo) al LRU (primero)
        while (it.hasNext()) {
            K clave = it.next();
            lista.add(new AbstractMap.SimpleEntry<>(clave, tabla.get(clave)));
        }
        return lista;
    }

    /**
     * Devuelve un resumen inmutable de las metricas acumuladas.
     *
     * @return objeto {@link Estadisticas}.
     */
    public Estadisticas estadisticas() {
        return new Estadisticas(capacidad, tabla.size(), aciertos, fallos, expulsiones);
    }
}