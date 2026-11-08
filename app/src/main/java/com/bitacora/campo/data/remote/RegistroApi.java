package com.bitacora.campo.data.remote;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Los cinco endpoints de la tarea 2.2.
 * SUPUESTO: rutas relativas a BASE_URL (que termina en "/api/").
 */
public interface RegistroApi {

    @GET("registros")
    Call<List<RegistroDto>> listar();

    @GET("registros/{id}")
    Call<RegistroDto> obtener(@Path("id") int id);

    @POST("registros")
    Call<RegistroDto> crear(@Body RegistroDto registro);

    @PUT("registros/{id}")
    Call<RegistroDto> actualizar(@Path("id") int id, @Body RegistroDto registro);

    @DELETE("registros/{id}")
    Call<Void> eliminar(@Path("id") int id);
}
