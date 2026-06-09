"""
pruebas.py
==========
Casos de prueba representativos para la cache LRU. Cubren el funcionamiento
normal y los casos limite exigidos por el proyecto (cache vacia, cache llena,
actualizacion, capacidad invalida, orden de uso).

Ejecucion:
    python3 pruebas.py      (o  make test)

Termina con codigo 0 si todas las pruebas pasan, o 1 si alguna falla.
"""

from cache_lru import CacheLRU


def prueba_insercion_y_consulta():
    c = CacheLRU(2, registrar=False)
    c.put("a", 1)
    c.put("b", 2)
    assert c.get("a") == 1
    assert c.get("b") == 2


def prueba_expulsion_lru():
    c = CacheLRU(2, registrar=False)
    c.put("a", 1)
    c.put("b", 2)
    c.get("a")              # 'a' pasa a ser MRU; 'b' queda como LRU
    c.put("c", 3)           # cache llena -> debe expulsar a 'b'
    assert c.get("b") is None
    assert c.get("a") == 1
    assert c.get("c") == 3
    assert c.expulsiones == 1


def prueba_actualizacion_no_expulsa():
    c = CacheLRU(2, registrar=False)
    c.put("a", 1)
    c.put("b", 2)
    c.put("a", 10)          # actualiza, no inserta uno nuevo
    assert c.get("a") == 10
    assert len(c) == 2
    assert c.expulsiones == 0


def prueba_cache_vacia():
    c = CacheLRU(3, registrar=False)
    assert c.esta_vacia()
    assert c.get("x") is None
    assert c.fallos == 1


def prueba_capacidad_invalida():
    for cap in (0, -1, 2.5, "tres", True):
        try:
            CacheLRU(cap, registrar=False)
        except ValueError:
            pass
        else:
            raise AssertionError("Se esperaba ValueError para capacidad=%r" % cap)


def prueba_orden_de_uso():
    c = CacheLRU(3, registrar=False)
    for k, v in [("a", 1), ("b", 2), ("c", 3)]:
        c.put(k, v)
    c.get("a")              # 'a' se vuelve MRU
    # Orden esperado de MRU a LRU: a, c, b
    assert [k for k, _ in c.contenido()] == ["a", "c", "b"]


def prueba_cache_llena_capacidad_uno():
    c = CacheLRU(1, registrar=False)
    c.put("a", 1)
    c.put("b", 2)           # expulsa 'a'
    assert c.get("a") is None
    assert c.get("b") == 2
    assert len(c) == 1


def ejecutar_todas():
    pruebas = [v for k, v in sorted(globals().items())
               if k.startswith("prueba_") and callable(v)]
    fallidas = 0
    print("Ejecutando %d pruebas...\n" % len(pruebas))
    for prueba in pruebas:
        try:
            prueba()
            print("  [OK]    %s" % prueba.__name__)
        except AssertionError as e:
            fallidas += 1
            print("  [FALLA] %s: %s" % (prueba.__name__, e))
    print("\n%d/%d pruebas superadas." % (len(pruebas) - fallidas, len(pruebas)))
    return fallidas


if __name__ == "__main__":
    import sys
    sys.exit(1 if ejecutar_todas() else 0)