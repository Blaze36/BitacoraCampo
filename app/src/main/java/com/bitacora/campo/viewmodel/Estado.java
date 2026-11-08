package com.bitacora.campo.viewmodel;

import com.bitacora.campo.data.repository.TipoError;

/** Estado de pantalla que exponen los ViewModels con LiveData. */
public class Estado<T> {

    public enum Tipo { CARGANDO, EXITO, VACIO, ERROR }

    public final Tipo tipo;
    public final T datos;
    public final TipoError error;
    public final int codigo;
    public final String mensaje;

    private Estado(Tipo tipo, T datos, TipoError error, int codigo, String mensaje) {
        this.tipo = tipo;
        this.datos = datos;
        this.error = error;
        this.codigo = codigo;
        this.mensaje = mensaje;
    }

    public static <T> Estado<T> cargando() {
        return new Estado<>(Tipo.CARGANDO, null, null, 0, null);
    }

    public static <T> Estado<T> exito(T datos) {
        return new Estado<>(Tipo.EXITO, datos, null, 0, null);
    }

    public static <T> Estado<T> vacio() {
        return new Estado<>(Tipo.VACIO, null, null, 0, null);
    }

    public static <T> Estado<T> error(TipoError error, int codigo, String mensaje) {
        return new Estado<>(Tipo.ERROR, null, error, codigo, mensaje);
    }
}
