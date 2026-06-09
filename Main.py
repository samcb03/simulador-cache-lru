"""
main.py
=======
Interfaz de linea de comandos (CLI) para demostrar la cache LRU.

Modos de uso:
    python3 main.py          -> menu interactivo
    python3 main.py --sim     -> ejecuta una simulacion automatica y termina

Todas las operaciones quedan registradas en el archivo 'cache_operaciones.log'.
"""

import logging
import random
import sys

from cache_lru import CacheLRU

ARCHIVO_LOG = "cache_operaciones.log"


def configurar_logging():
    """Configura el registro de operaciones hacia un archivo de texto."""
    logging.basicConfig(
        filename=ARCHIVO_LOG,
        filemode="w",
        level=logging.INFO,
        format="%(asctime)s | %(message)s",
        datefmt="%H:%M:%S",
    )


def imprimir_estado(cache):
    """Muestra el contenido de la cache de mas a menos reciente."""
    print("\nEstado actual de la cache (de mas a menos reciente):")
    contenido = cache.contenido()
    if not contenido:
        print("  [cache vacia]")
    else:
        total = len(contenido)
        for i, (k, v) in enumerate(contenido, 1):
            etiqueta = "<- MRU" if i == 1 else ("<- LRU" if i == total else "")
            print("  %d. %s = %s  %s" % (i, k, v, etiqueta))
    print()


def imprimir_estadisticas(cache):
    """Muestra las metricas acumuladas de la cache."""
    print("\n--- Estadisticas ---")
    for k, v in cache.estadisticas().items():
        print("  %-20s: %s" % (k, v))
    print()


def ejecutar_simulacion(cache, num_operaciones=20, claves_posibles=8, semilla=42):
    """Genera una secuencia de operaciones aleatorias para demostrar la cache.

    Usa una semilla fija para que los resultados sean reproducibles.
    """
    random.seed(semilla)
    print("\n>>> Simulacion: %d operaciones aleatorias sobre %d claves "
          "posibles (capacidad=%d)\n" % (num_operaciones, claves_posibles, cache.capacidad))

    for i in range(1, num_operaciones + 1):
        clave = "K%d" % random.randint(1, claves_posibles)
        if random.random() < 0.6:                      # 60% consultas, 40% inserciones
            valor = cache.get(clave)
            estado = "ACIERTO (%s)" % valor if valor is not None else "FALLO"
            print("  %2d. GET  %-4s -> %s" % (i, clave, estado))
        else:
            valor = random.randint(100, 999)
            cache.put(clave, valor)
            print("  %2d. PUT  %-4s = %s" % (i, clave, valor))

    imprimir_estado(cache)
    imprimir_estadisticas(cache)


def menu_interactivo(cache):
    """Bucle principal del menu interactivo."""
    opciones = (
        "\n=========== CACHE LRU - Menu ===========\n"
        "  1. put  (insertar / actualizar)\n"
        "  2. get  (consultar)\n"
        "  3. Ver estado de la cache\n"
        "  4. Ver estadisticas\n"
        "  5. Ejecutar simulacion automatica\n"
        "  6. Salir\n"
        "========================================\n"
        "Opcion: "
    )

    while True:
        try:
            opcion = input(opciones).strip()
        except EOFError:
            break

        if opcion == "1":
            clave = input("  Clave: ").strip()
            valor = input("  Valor: ").strip()
            if clave == "":
                print("  -> la clave no puede estar vacia")
                continue
            cache.put(clave, valor)
            print("  -> almacenado %s = %s" % (clave, valor))
        elif opcion == "2":
            clave = input("  Clave: ").strip()
            valor = cache.get(clave)
            if valor is None:
                print("  -> FALLO: '%s' no esta en la cache" % clave)
            else:
                print("  -> ACIERTO: %s = %s" % (clave, valor))
        elif opcion == "3":
            imprimir_estado(cache)
        elif opcion == "4":
            imprimir_estadisticas(cache)
        elif opcion == "5":
            ejecutar_simulacion(cache)
        elif opcion == "6":
            print("Saliendo...")
            break
        else:
            print("  Opcion no valida.")


def main():
    configurar_logging()
    print("Cache LRU - Proyecto Final\n")

    # Modo simulacion no interactivo (util para 'make sim').
    if "--sim" in sys.argv:
        ejecutar_simulacion(CacheLRU(3))
        print("Las operaciones se registraron en '%s'." % ARCHIVO_LOG)
        return

    # Modo interactivo: pedir capacidad y validarla.
    try:
        entrada = input("Capacidad de la cache [por defecto 3]: ").strip()
        capacidad = int(entrada) if entrada else 3
        cache = CacheLRU(capacidad)
    except ValueError as e:
        print("Capacidad invalida (%s). Se usa capacidad = 3." % e)
        cache = CacheLRU(3)

    menu_interactivo(cache)
    print("\nLas operaciones se registraron en '%s'." % ARCHIVO_LOG)


if __name__ == "__main__":
    main()