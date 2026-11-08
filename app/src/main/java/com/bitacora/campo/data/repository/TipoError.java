package com.bitacora.campo.data.repository;

/** Tipos de resultado que la interfaz sabe mostrar. */
public enum TipoError {
    SIN_CONEXION,
    TIMEOUT,
    SERVIDOR,   // 4xx y 5xx
    DESCONOCIDO
}
