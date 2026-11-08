package com.bitacora.campo.data.repository;

/** Callback del repositorio hacia el ViewModel. */
public interface Respuesta<T> {
    void onExito(T datos);

    /**
     * @param tipo   categoría del error
     * @param codigo código HTTP (0 si no aplica)
     * @param mensaje mensaje del servidor si lo hubo (p. ej. un 400 de validación), o null
     */
    void onError(TipoError tipo, int codigo, String mensaje);
}
