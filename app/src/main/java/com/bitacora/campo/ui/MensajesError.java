package com.bitacora.campo.ui;

import android.content.Context;

import com.bitacora.campo.R;
import com.bitacora.campo.viewmodel.Estado;

/** Convierte un estado de error en un mensaje comprensible para el usuario. */
final class MensajesError {

    private MensajesError() { }

    static String texto(Context c, Estado<?> e) {
        switch (e.error) {
            case SIN_CONEXION:
                return c.getString(R.string.msg_sin_conexion);
            case TIMEOUT:
                return c.getString(R.string.msg_timeout);
            case SERVIDOR:
                // Si el servidor mandó un mensaje (p. ej. validación 400), se muestra tal cual.
                if (e.mensaje != null && !e.mensaje.isEmpty()) return e.mensaje;
                return c.getString(R.string.msg_error_servidor, e.codigo);
            default:
                return c.getString(R.string.msg_error_desconocido);
        }
    }
}
