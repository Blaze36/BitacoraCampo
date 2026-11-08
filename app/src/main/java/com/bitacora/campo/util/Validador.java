package com.bitacora.campo.util;

import android.content.Context;

import com.bitacora.campo.R;

/**
 * Validación en el cliente. SUPUESTO: estos límites deben coincidir con los de la API (tarea 2.2).
 * Ajusta las constantes si tu API usa otros valores.
 */
public final class Validador {

    public static final int TITULO_MIN = 3;
    public static final int TITULO_MAX = 100;
    public static final int DESCRIPCION_MAX = 500;

    private Validador() { }

    /** @return mensaje de error, o null si es válido. */
    public static String validarTitulo(Context c, String titulo) {
        String t = titulo == null ? "" : titulo.trim();
        if (t.isEmpty()) return c.getString(R.string.err_titulo_vacio);
        if (t.length() < TITULO_MIN) return c.getString(R.string.err_titulo_corto, TITULO_MIN);
        if (t.length() > TITULO_MAX) return c.getString(R.string.err_titulo_largo, TITULO_MAX);
        return null;
    }

    public static String validarDescripcion(Context c, String descripcion) {
        String d = descripcion == null ? "" : descripcion.trim();
        if (d.isEmpty()) return c.getString(R.string.err_desc_vacia);
        if (d.length() > DESCRIPCION_MAX) return c.getString(R.string.err_desc_larga, DESCRIPCION_MAX);
        return null;
    }
}
