package com.bitacora.campo.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Base64;

import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class ImagenUtil {

    public static final int MAX_LADO_PX = 1024;
    public static final int CALIDAD_JPEG = 70;

    private ImagenUtil() { }

    /**
     * Lee el archivo de la foto, corrige la rotación EXIF, reduce el lado mayor a 1024 px,
     * comprime a JPEG calidad 70 y devuelve Base64 (sin saltos de línea ni prefijo data:).
     * Es una operación pesada: llamarla fuera del hilo principal.
     */
    public static String reducirABase64(String rutaArchivo) throws IOException {
        // 1) Solo dimensiones, para calcular inSampleSize y no cargar la foto completa en memoria.
        BitmapFactory.Options opciones = new BitmapFactory.Options();
        opciones.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(rutaArchivo, opciones);
        if (opciones.outWidth <= 0 || opciones.outHeight <= 0) {
            throw new IOException("No se pudo leer la imagen");
        }

        int muestra = 1;
        int mayor = Math.max(opciones.outWidth, opciones.outHeight);
        while (mayor / (muestra * 2) >= MAX_LADO_PX) {
            muestra *= 2;
        }

        // 2) Decodificar reducido.
        opciones = new BitmapFactory.Options();
        opciones.inSampleSize = muestra;
        Bitmap bmp = BitmapFactory.decodeFile(rutaArchivo, opciones);
        if (bmp == null) throw new IOException("No se pudo decodificar la imagen");

        // 3) Corregir orientación según EXIF.
        int rotacion = leerRotacion(rutaArchivo);
        if (rotacion != 0) {
            Matrix m = new Matrix();
            m.postRotate(rotacion);
            Bitmap rotado = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
            if (rotado != bmp) bmp.recycle();
            bmp = rotado;
        }

        // 4) Ajuste fino al máximo de 1024 px.
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int lado = Math.max(w, h);
        if (lado > MAX_LADO_PX) {
            float escala = (float) MAX_LADO_PX / lado;
            Bitmap esc = Bitmap.createScaledBitmap(bmp, Math.round(w * escala), Math.round(h * escala), true);
            if (esc != bmp) bmp.recycle();
            bmp = esc;
        }

        // 5) JPEG calidad 70 -> Base64.
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, salida);
        bmp.recycle();
        return Base64.encodeToString(salida.toByteArray(), Base64.NO_WRAP);
    }

    /** Decodifica Base64 a Bitmap para mostrarlo. Devuelve null si falla. */
    public static Bitmap decodificarBase64(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            // Tolerar un prefijo "data:image/jpeg;base64," si la API lo incluye.
            int coma = base64.indexOf(',');
            String limpio = (base64.startsWith("data:") && coma >= 0) ? base64.substring(coma + 1) : base64;
            byte[] bytes = Base64.decode(limpio, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static int leerRotacion(String ruta) {
        try {
            ExifInterface exif = new ExifInterface(ruta);
            int o = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (o) {
                case ExifInterface.ORIENTATION_ROTATE_90: return 90;
                case ExifInterface.ORIENTATION_ROTATE_180: return 180;
                case ExifInterface.ORIENTATION_ROTATE_270: return 270;
                default: return 0;
            }
        } catch (IOException e) {
            return 0;
        }
    }
}
