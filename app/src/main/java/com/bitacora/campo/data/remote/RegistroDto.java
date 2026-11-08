package com.bitacora.campo.data.remote;

import com.google.gson.annotations.SerializedName;

/**
 * DTO de un registro de la Bitácora de campo.
 * SUPUESTO (tarea 2.2): la API usa estos nombres de campo en JSON.
 * Si tu API usa otros, solo cambia las anotaciones @SerializedName.
 */
public class RegistroDto {

    @SerializedName("id")
    public Integer id;

    @SerializedName("titulo")
    public String titulo;

    @SerializedName("descripcion")
    public String descripcion;

    /** Fotografía JPEG codificada en Base64 (sin prefijo data:). */
    @SerializedName("fotoBase64")
    public String fotoBase64;

    @SerializedName("fecha")
    public String fecha;

    public RegistroDto() { }

    public RegistroDto(String titulo, String descripcion, String fotoBase64) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fotoBase64 = fotoBase64;
    }
}
