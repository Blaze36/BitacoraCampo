package com.bitacora.campo.data.remote;

import com.bitacora.campo.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Configuración única de OkHttp y Retrofit. La URL base viene de BuildConfig.BASE_URL. */
public final class ApiClient {

    private static RegistroApi api;

    private ApiClient() { }

    public static synchronized RegistroApi getApi() {
        if (api == null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS); // la foto en Base64 hace el cuerpo grande

            // Registro de peticiones/respuestas SOLO en modo depuración.
            if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                logging.setLevel(HttpLoggingInterceptor.Level.BODY);
                builder.addInterceptor(logging);
            }

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL)
                    .client(builder.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            api = retrofit.create(RegistroApi.class);
        }
        return api;
    }
}
