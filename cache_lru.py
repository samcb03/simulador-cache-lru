"""
cache_lru.py
============
Cache con politica de reemplazo LRU (Least Recently Used) construida sobre dos
estructuras de datos de la biblioteca estandar de Python:

    * dict              -> tabla hash: mapea clave -> valor en O(1) promedio.
    * collections.deque -> cola doblemente enlazada: mantiene el orden de uso.

Se emplean las estructuras nativas (en lugar de implementarlas desde cero)
porque el problema corresponde al dominio de Sistemas Operativos y el uso
correcto de las estructuras es suficiente. Internamente, 'dict' es una tabla
hash y 'deque' es una lista doblemente enlazada, por lo que se corresponden con
las estructuras descritas en el marco teorico (tabla hash + cola para LRU).

Convencion de orden en la deque:
    * Extremo izquierdo -> elemento MENOS recientemente usado (LRU).
    * Extremo derecho   -> elemento MAS recientemente usado (MRU).
"""

import logging
from collections import deque


class CacheLRU:
    """Cache de capacidad fija con politica de reemplazo LRU."""

    def __init__(self, capacidad, registrar=True):
        """Crea la cache.

        Parametros
        ----------
        capacidad : int
            Numero maximo de elementos. Debe ser un entero positivo.
        registrar : bool
            Si es True, cada operacion se registra mediante el modulo logging.

        Lanza ValueError si la capacidad no es un entero positivo (caso limite).
        """
        if not isinstance(capacidad, int) or isinstance(capacidad, bool) or capacidad <= 0:
            raise ValueError("La capacidad debe ser un entero positivo.")

        self.capacidad = capacidad
        self._tabla = {}            # tabla hash: clave -> valor
        self._orden = deque()       # orden de uso: izquierda = LRU, derecha = MRU

        # Metricas para los reportes de operaciones.
        self.aciertos = 0
        self.fallos = 0
        self.expulsiones = 0

        self._registrar = registrar
        self._log = logging.getLogger("cache_lru")

    # ------------------------------------------------------------------ #
    # Consultas de estado                                                #
    # ------------------------------------------------------------------ #
    def __len__(self):
        return len(self._tabla)

    def esta_llena(self):
        return len(self) >= self.capacidad

    def esta_vacia(self):
        return len(self) == 0

    # ------------------------------------------------------------------ #
    # Apoyo interno                                                      #
    # ------------------------------------------------------------------ #
    def _marcar_como_reciente(self, clave):
        """Mueve una clave existente al extremo MRU (derecha) de la deque.

        La deque no permite acceso directo a un elemento interno, por lo que
        remove() recorre la cola: esta operacion es O(n). Es el costo de usar
        una cola nativa en lugar de una lista enlazada con referencias directas
        a los nodos (que lograria O(1)). Para el tamano tipico de una cache es
        despreciable.
        """
        self._orden.remove(clave)
        self._orden.append(clave)

    # ------------------------------------------------------------------ #
    # Operaciones fundamentales                                          #
    # ------------------------------------------------------------------ #
    def get(self, clave):
        """Consulta el valor de una clave.

        En caso de ACIERTO marca el elemento como el mas recientemente usado y
        devuelve su valor. En caso de FALLO devuelve None.
        """
        if clave in self._tabla:
            self._marcar_como_reciente(clave)
            self.aciertos += 1
            valor = self._tabla[clave]
            self._anotar("GET", clave, "ACIERTO", "valor=%s" % valor)
            return valor

        self.fallos += 1
        self._anotar("GET", clave, "FALLO", "clave no encontrada")
        return None

    def put(self, clave, valor):
        """Inserta o actualiza un par clave-valor.

        Si la clave ya existe, actualiza su valor y la marca como MRU.
        Si es nueva y la cache esta llena, expulsa primero el elemento LRU.
        """
        # Caso 1: la clave ya existe -> actualizar, sin expulsar.
        if clave in self._tabla:
            self._tabla[clave] = valor
            self._marcar_como_reciente(clave)
            self._anotar("PUT", clave, "ACTUALIZA", "valor=%s" % valor)
            return

        # Caso 2: clave nueva y cache llena -> expulsar el LRU (caso limite).
        if self.esta_llena():
            lru = self._orden.popleft()         # O(1): extremo izquierdo = LRU
            del self._tabla[lru]
            self.expulsiones += 1
            self._anotar("PUT", clave, "EXPULSA", "se elimino LRU clave=%s" % lru)

        # Caso 3: insertar el nuevo par y marcarlo como MRU.
        self._tabla[clave] = valor
        self._orden.append(clave)
        self._anotar("PUT", clave, "INSERTA", "valor=%s" % valor)

    # ------------------------------------------------------------------ #
    # Reportes                                                           #
    # ------------------------------------------------------------------ #
    def contenido(self):
        """Devuelve la lista de pares (clave, valor) ordenada de MRU a LRU."""
        return [(clave, self._tabla[clave]) for clave in reversed(self._orden)]

    def estadisticas(self):
        """Devuelve un diccionario con las metricas acumuladas."""
        total = self.aciertos + self.fallos
        tasa = (self.aciertos / total * 100) if total else 0.0
        return {
            "capacidad": self.capacidad,
            "elementos": len(self),
            "aciertos": self.aciertos,
            "fallos": self.fallos,
            "expulsiones": self.expulsiones,
            "consultas_totales": total,
            "tasa_aciertos_%": round(tasa, 2),
        }

    def _anotar(self, operacion, clave, resultado, detalle=""):
        """Registra una operacion en el log si el registro esta activado."""
        if self._registrar:
            self._log.info("%-4s clave=%-8s -> %-9s %s",
                           operacion, str(clave), resultado, detalle)