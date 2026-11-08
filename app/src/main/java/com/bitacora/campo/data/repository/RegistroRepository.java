package com.bitacora.campo.data.repository;

import com.bitacora.campo.data.remote.ApiClient;
import com.bitacora.campo.data.remote.RegistroApi;
import com.bitacora.campo.data.remote.RegistroDto;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Única clase que llama a la API. Traduce respuestas y fallos a {@link Respuesta}. */
public class RegistroRepository {

    private final RegistroApi api;

    public RegistroRepository() {
        this.api = ApiClient.getApi();
    }

    public Call<List<RegistroDto>> listar(Respuesta<List<RegistroDto>> r) {
        Call<List<RegistroDto>> call = api.listar();
        call.enqueue(callback(r));
        return call;
    }

    public Call<RegistroDto> obtener(int id, Respuesta<RegistroDto> r) {
        Call<RegistroDto> call = api.obtener(id);
        call.enqueue(callback(r));
        return call;
    }

    public Call<RegistroDto> crear(RegistroDto dto, Respuesta<RegistroDto> r) {
        Call<RegistroDto> call = api.crear(dto);
        call.enqueue(callback(r));
        return call;
    }

    public Call<RegistroDto> actualizar(int id, RegistroDto dto, Respuesta<RegistroDto> r) {
        Call<RegistroDto> call = api.actualizar(id, dto);
        call.enqueue(callback(r));
        return call;
    }

    public Call<Void> eliminar(int id, Respuesta<Void> r) {
        Call<Void> call = api.eliminar(id);
        call.enqueue(callback(r));
        return call;
    }

    private <T> Callback<T> callback(final Respuesta<T> r) {
        return new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                if (response.isSuccessful()) {
                    r.onExito(response.body());
                } else {
                    r.onError(TipoError.SERVIDOR, response.code(), extraerMensaje(response.errorBody()));
                }
            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                if (call.isCanceled()) return;
                if (t instanceof SocketTimeoutException) {
                    r.onError(TipoError.TIMEOUT, 0, null);
                } else if (t instanceof IOException) {
                    // UnknownHostException, ConnectException (API apagada), modo avión, etc.
                    r.onError(TipoError.SIN_CONEXION, 0, null);
                } else {
                    r.onError(TipoError.DESCONOCIDO, 0, t.getMessage());
                }
            }
        };
    }

    /**
     * Intenta leer el mensaje de error del cuerpo JSON de la API.
     * SUPUESTO: la API devuelve {"error": "..."} o {"mensaje": "..."} o {"message": "..."}.
     */
    private String extraerMensaje(ResponseBody body) {
        if (body == null) return null;
        try {
            JsonElement el = JsonParser.parseString(body.string());
            if (el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                for (String clave : new String[]{"error", "mensaje", "message"}) {
                    if (o.has(clave) && !o.get(clave).isJsonNull()) {
                        JsonElement v = o.get(clave);
                        return v.isJsonPrimitive() ? v.getAsString() : v.toString();
                    }
                }
            }
        } catch (Exception ignorado) {
            // cuerpo vacío o no-JSON: se usa el mensaje genérico por código
        }
        return null;
    }
}
