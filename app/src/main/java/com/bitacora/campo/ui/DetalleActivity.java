package com.bitacora.campo.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.bitacora.campo.R;
import com.bitacora.campo.data.remote.RegistroDto;
import com.bitacora.campo.util.ImagenUtil;
import com.bitacora.campo.viewmodel.DetalleViewModel;
import com.bitacora.campo.viewmodel.Estado;

public class DetalleActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "id";

    private DetalleViewModel vm;
    private int id;
    private ImageView ivFoto;
    private TextView tvTitulo, tvFecha, tvDescripcion, tvError;
    private ProgressBar progress;
    private View contenido, layoutError;
    private Button btnEditar, btnEliminar;

    private final ActivityResultLauncher<Intent> lanzadorEditar = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    setResult(RESULT_OK);   // la lista también debe refrescarse
                    vm.cargar(id);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle);
        setTitle(R.string.titulo_detalle);

        id = getIntent().getIntExtra(EXTRA_ID, -1);
        ivFoto = findViewById(R.id.ivFoto);
        tvTitulo = findViewById(R.id.tvTitulo);
        tvFecha = findViewById(R.id.tvFecha);
        tvDescripcion = findViewById(R.id.tvDescripcion);
        tvError = findViewById(R.id.tvError);
        progress = findViewById(R.id.progress);
        contenido = findViewById(R.id.contenido);
        layoutError = findViewById(R.id.layoutError);
        btnEditar = findViewById(R.id.btnEditar);
        btnEliminar = findViewById(R.id.btnEliminar);

        vm = new ViewModelProvider(this).get(DetalleViewModel.class);

        vm.getEstado().observe(this, new Observer<Estado<RegistroDto>>() {
            @Override
            public void onChanged(Estado<RegistroDto> e) {
                pintar(e);
            }
        });

        vm.getEliminacion().observe(this, new Observer<Estado<Void>>() {
            @Override
            public void onChanged(Estado<Void> e) {
                if (e == null) return;
                boolean cargando = e.tipo == Estado.Tipo.CARGANDO;
                progress.setVisibility(cargando ? View.VISIBLE : View.GONE);
                btnEditar.setEnabled(!cargando);
                btnEliminar.setEnabled(!cargando);
                if (e.tipo == Estado.Tipo.EXITO) {
                    vm.consumirEliminacion();
                    Toast.makeText(DetalleActivity.this, R.string.msg_eliminado, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else if (e.tipo == Estado.Tipo.ERROR) {
                    vm.consumirEliminacion();
                    Toast.makeText(DetalleActivity.this, MensajesError.texto(DetalleActivity.this, e),
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        findViewById(R.id.btnReintentar).setOnClickListener(v -> vm.cargar(id));
        btnEditar.setOnClickListener(v -> {
            Intent i = new Intent(this, FormularioActivity.class);
            i.putExtra(FormularioActivity.EXTRA_ID, id);
            lanzadorEditar.launch(i);
        });
        btnEliminar.setOnClickListener(v -> confirmarEliminar());

        if (vm.getEstado().getValue() == null) vm.cargar(id);
    }

    private void confirmarEliminar() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirmar_eliminar_titulo)
                .setMessage(R.string.confirmar_eliminar_msg)
                .setPositiveButton(R.string.btn_eliminar, (d, w) -> vm.eliminar(id))
                .setNegativeButton(R.string.btn_cancelar, null)
                .show();
    }

    private void pintar(Estado<RegistroDto> e) {
        boolean cargando = e.tipo == Estado.Tipo.CARGANDO;
        progress.setVisibility(cargando ? View.VISIBLE : View.GONE);
        btnEditar.setEnabled(e.tipo == Estado.Tipo.EXITO);
        btnEliminar.setEnabled(e.tipo == Estado.Tipo.EXITO);
        contenido.setVisibility(e.tipo == Estado.Tipo.EXITO ? View.VISIBLE : View.GONE);
        layoutError.setVisibility(e.tipo == Estado.Tipo.ERROR ? View.VISIBLE : View.GONE);

        if (e.tipo == Estado.Tipo.EXITO) {
            RegistroDto r = e.datos;
            tvTitulo.setText(r.titulo);
            tvFecha.setText(r.fecha == null ? "" : r.fecha);
            tvDescripcion.setText(r.descripcion);
            Bitmap bmp = ImagenUtil.decodificarBase64(r.fotoBase64);
            if (bmp != null) ivFoto.setImageBitmap(bmp);
            else ivFoto.setImageResource(android.R.drawable.ic_menu_gallery);
        } else if (e.tipo == Estado.Tipo.ERROR) {
            tvError.setText(MensajesError.texto(this, e));
        } else if (e.tipo == Estado.Tipo.VACIO) {
            tvError.setText(R.string.msg_error_desconocido);
            layoutError.setVisibility(View.VISIBLE);
        }
    }
}
