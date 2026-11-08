package com.bitacora.campo.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.bitacora.campo.R;
import com.bitacora.campo.data.remote.RegistroDto;
import com.bitacora.campo.util.ImagenUtil;
import com.bitacora.campo.util.Validador;
import com.bitacora.campo.viewmodel.Estado;
import com.bitacora.campo.viewmodel.FormularioViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Crear (sin EXTRA_ID) o editar (con EXTRA_ID). Foto con cámara + descripción por voz. */
public class FormularioActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "id";
    private static final String KEY_RUTA_FOTO = "rutaFoto";
    private static final String KEY_FOTO_NUEVA = "fotoNueva";

    private FormularioViewModel vm;
    private Integer idEdicion;

    private EditText etTitulo, etDescripcion;
    private ImageView ivFoto;
    private Button btnFoto, btnVoz, btnGuardar;
    private ProgressBar progress;
    private View layoutError;
    private TextView tvError;

    private File archivoFoto;          // foto tomada con la cámara (archivo temporal)
    private boolean hayFotoNueva;
    private String fotoBase64Existente; // al editar sin cambiar la foto
    private boolean errorEsDeCarga;     // para saber qué reintentar
    private RegistroDto ultimoDto;      // último envío, para Reintentar

    private final ExecutorService hilo = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Uri> lanzadorCamara = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            ok -> {
                if (ok && archivoFoto != null && archivoFoto.exists()) {
                    hayFotoNueva = true;
                    mostrarFotoLocal();
                } else {
                    hayFotoNueva = false;
                }
            });

    private final ActivityResultLauncher<Intent> lanzadorVoz = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    ArrayList<String> textos =
                            result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (textos != null && !textos.isEmpty()) {
                        String actual = etDescripcion.getText().toString().trim();
                        String nuevo = textos.get(0);
                        etDescripcion.setText(actual.isEmpty() ? nuevo : actual + " " + nuevo);
                        etDescripcion.setSelection(etDescripcion.getText().length());
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_formulario);

        etTitulo = findViewById(R.id.etTitulo);
        etDescripcion = findViewById(R.id.etDescripcion);
        ivFoto = findViewById(R.id.ivFoto);
        btnFoto = findViewById(R.id.btnFoto);
        btnVoz = findViewById(R.id.btnVoz);
        btnGuardar = findViewById(R.id.btnGuardar);
        progress = findViewById(R.id.progress);
        layoutError = findViewById(R.id.layoutError);
        tvError = findViewById(R.id.tvError);

        int id = getIntent().getIntExtra(EXTRA_ID, -1);
        idEdicion = id == -1 ? null : Integer.valueOf(id);
        setTitle(idEdicion == null ? R.string.titulo_nuevo : R.string.titulo_editar);

        if (savedInstanceState != null) {
            String ruta = savedInstanceState.getString(KEY_RUTA_FOTO);
            if (ruta != null) archivoFoto = new File(ruta);
            hayFotoNueva = savedInstanceState.getBoolean(KEY_FOTO_NUEVA);
            if (hayFotoNueva) mostrarFotoLocal();
        }

        vm = new ViewModelProvider(this).get(FormularioViewModel.class);

        vm.getCarga().observe(this, new Observer<Estado<RegistroDto>>() {
            @Override
            public void onChanged(Estado<RegistroDto> e) {
                boolean cargando = e.tipo == Estado.Tipo.CARGANDO;
                habilitar(!cargando && e.tipo == Estado.Tipo.EXITO);
                progress.setVisibility(cargando ? View.VISIBLE : View.GONE);
                layoutError.setVisibility(e.tipo == Estado.Tipo.ERROR ? View.VISIBLE : View.GONE);
                if (e.tipo == Estado.Tipo.EXITO && e.datos != null && !hayFotoNueva
                        && etTitulo.getText().length() == 0) {
                    etTitulo.setText(e.datos.titulo);
                    etDescripcion.setText(e.datos.descripcion);
                    fotoBase64Existente = e.datos.fotoBase64;
                    Bitmap bmp = ImagenUtil.decodificarBase64(e.datos.fotoBase64);
                    if (bmp != null) ivFoto.setImageBitmap(bmp);
                } else if (e.tipo == Estado.Tipo.ERROR) {
                    errorEsDeCarga = true;
                    tvError.setText(MensajesError.texto(FormularioActivity.this, e));
                }
            }
        });

        vm.getGuardado().observe(this, new Observer<Estado<RegistroDto>>() {
            @Override
            public void onChanged(Estado<RegistroDto> e) {
                boolean cargando = e.tipo == Estado.Tipo.CARGANDO;
                progress.setVisibility(cargando ? View.VISIBLE : View.GONE);
                habilitar(!cargando);
                layoutError.setVisibility(e.tipo == Estado.Tipo.ERROR ? View.VISIBLE : View.GONE);
                if (e.tipo == Estado.Tipo.EXITO) {
                    Toast.makeText(FormularioActivity.this, R.string.msg_guardado, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else if (e.tipo == Estado.Tipo.ERROR) {
                    errorEsDeCarga = false;
                    // Incluye los mensajes de validación (400) que devuelva el servidor.
                    tvError.setText(MensajesError.texto(FormularioActivity.this, e));
                }
            }
        });

        btnFoto.setOnClickListener(v -> tomarFoto());
        btnVoz.setOnClickListener(v -> dictar());
        btnGuardar.setOnClickListener(v -> guardar());
        findViewById(R.id.btnReintentar).setOnClickListener(v -> reintentar());

        if (idEdicion != null && vm.getCarga().getValue() == null) {
            habilitar(false);
            vm.cargar(idEdicion.intValue());
        }
    }

    private void habilitar(boolean si) {
        btnFoto.setEnabled(si);
        btnVoz.setEnabled(si);
        btnGuardar.setEnabled(si);
        etTitulo.setEnabled(si);
        etDescripcion.setEnabled(si);
    }

    private void tomarFoto() {
        try {
            File carpeta = new File(getCacheDir(), "fotos");
            if (!carpeta.exists()) carpeta.mkdirs();
            archivoFoto = new File(carpeta, "foto_" + System.currentTimeMillis() + ".jpg");
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", archivoFoto);
            lanzadorCamara.launch(uri);
        } catch (Exception ex) {
            Toast.makeText(this, R.string.camara_no_disponible, Toast.LENGTH_LONG).show();
        }
    }

    private void dictar() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES");
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voz_prompt));
        try {
            lanzadorVoz.launch(i);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, R.string.voz_no_disponible, Toast.LENGTH_LONG).show();
        }
    }

    private void mostrarFotoLocal() {
        if (archivoFoto == null) return;
        Bitmap bmp = android.graphics.BitmapFactory.decodeFile(archivoFoto.getAbsolutePath(),
                opcionesMiniatura());
        if (bmp != null) ivFoto.setImageBitmap(bmp);
    }

    private android.graphics.BitmapFactory.Options opcionesMiniatura() {
        android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options();
        o.inSampleSize = 4; // solo para la vista previa
        return o;
    }

    private void guardar() {
        String titulo = etTitulo.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();

        // Validación en el cliente (mismos límites que la API).
        String errT = Validador.validarTitulo(this, titulo);
        String errD = Validador.validarDescripcion(this, descripcion);
        etTitulo.setError(errT);
        etDescripcion.setError(errD);
        if (errT != null || errD != null) return;

        boolean tieneFoto = hayFotoNueva || fotoBase64Existente != null;
        if (!tieneFoto) {
            Toast.makeText(this, R.string.err_foto_requerida, Toast.LENGTH_LONG).show();
            return;
        }

        habilitar(false);
        progress.setVisibility(View.VISIBLE);
        layoutError.setVisibility(View.GONE);

        if (hayFotoNueva) {
            final String ruta = archivoFoto.getAbsolutePath();
            final String t = titulo;
            final String d = descripcion;
            // Reducir + Base64 es pesado: fuera del hilo principal.
            hilo.execute(() -> {
                try {
                    final String b64 = ImagenUtil.reducirABase64(ruta);
                    runOnUiThread(() -> enviar(t, d, b64));
                } catch (Exception ex) {
                    runOnUiThread(() -> {
                        progress.setVisibility(View.GONE);
                        habilitar(true);
                        Toast.makeText(this, R.string.camara_no_disponible, Toast.LENGTH_LONG).show();
                    });
                }
            });
        } else {
            enviar(titulo, descripcion, fotoBase64Existente);
        }
    }

    private void enviar(String titulo, String descripcion, String base64) {
        ultimoDto = new RegistroDto(titulo, descripcion, base64);
        vm.guardar(idEdicion, ultimoDto);
    }

    private void reintentar() {
        layoutError.setVisibility(View.GONE);
        if (errorEsDeCarga && idEdicion != null) {
            habilitar(false);
            vm.cargar(idEdicion.intValue());
        } else if (ultimoDto != null) {
            vm.guardar(idEdicion, ultimoDto);   // la foto ya está reducida; no se repite el proceso
        } else {
            habilitar(true);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (archivoFoto != null) out.putString(KEY_RUTA_FOTO, archivoFoto.getAbsolutePath());
        out.putBoolean(KEY_FOTO_NUEVA, hayFotoNueva);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hilo.shutdown();
    }
}
