package uv.lis;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Registro de operaciones de la cache hacia un archivo de texto.
 *
 * <p>Cada linea incluye una marca de tiempo, el tipo de operacion (GET/PUT),
 * la clave, el resultado (ACIERTO, FALLO, INSERTA, ACTUALIZA, EXPULSA) y un
 * detalle opcional. Encapsula el manejo del archivo para que la clase
 * {@link CacheLRU} no dependa de los detalles de escritura.</p>
 *
 */
public class RegistroOperaciones {

    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private final PrintWriter escritor;

    /**
     * Crea el registro abriendo (y sobrescribiendo) el archivo indicado.
     *
     * @param archivo ruta del archivo de log.
     */
    public RegistroOperaciones(String archivo) {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new FileWriter(archivo, false));
        } catch (IOException e) {
            System.err.println("No se pudo abrir el archivo de log: " + e.getMessage());
        }
        this.escritor = pw;
    }

    /**
     * Escribe una linea con el detalle de una operacion.
     *
     * @param operacion tipo de operacion (GET o PUT).
     * @param clave     clave involucrada.
     * @param resultado resultado de la operacion.
     * @param detalle   informacion adicional.
     */
    public void registrar(String operacion, String clave, String resultado, String detalle) {
        if (escritor == null) {
            return;
        }
        String marca = LocalTime.now().format(FORMATO);
        escritor.printf("%s | %-4s clave=%-8s -> %-9s %s%n",
                marca, operacion, clave, resultado, detalle);
        escritor.flush();
    }

    /** Cierra el archivo de log liberando los recursos. */
    public void cerrar() {
        if (escritor != null) {
            escritor.close();
        }
    }
}