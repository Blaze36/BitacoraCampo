package com.bitacora.campo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bitacora.campo.R;
import com.bitacora.campo.data.remote.RegistroDto;
import com.bitacora.campo.viewmodel.Estado;
import com.bitacora.campo.viewmodel.ListaViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ListaViewModel vm;
    private RegistroAdapter adapter;
    private SwipeRefreshLayout swipe;
    private ProgressBar progress;
    private TextView tvVacio;
    private View layoutError;
    private TextView tvError;
    private FloatingActionButton fab;

    private final ActivityResultLauncher<Intent> lanzador = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Se creó, editó o eliminó algo: refrescar desde el servidor.
                if (result.getResultCode() == RESULT_OK) vm.cargar();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.titulo_lista);

        swipe = findViewById(R.id.swipe);
        progress = findViewById(R.id.progress);
        tvVacio = findViewById(R.id.tvVacio);
        layoutError = findViewById(R.id.layoutError);
        tvError = findViewById(R.id.tvError);
        fab = findViewById(R.id.fabNuevo);
        RecyclerView rv = findViewById(R.id.recycler);

        adapter = new RegistroAdapter(new RegistroAdapter.OnClick() {
            @Override
            public void alTocar(RegistroDto r) {
                if (r.id == null) return;
                Intent i = new Intent(MainActivity.this, DetalleActivity.class);
                i.putExtra(DetalleActivity.EXTRA_ID, r.id.intValue());
                lanzador.launch(i);
            }
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        vm = new ViewModelProvider(this).get(ListaViewModel.class);
        vm.getEstado().observe(this, new Observer<Estado<List<RegistroDto>>>() {
            @Override
            public void onChanged(Estado<List<RegistroDto>> e) {
                pintar(e);
            }
        });

        swipe.setOnRefreshListener(() -> vm.cargar());
        findViewById(R.id.btnReintentar).setOnClickListener(v -> vm.cargar());
        fab.setOnClickListener(v -> lanzador.launch(new Intent(this, FormularioActivity.class)));

        if (vm.getEstado().getValue() == null) vm.cargar();
    }

    private void pintar(Estado<List<RegistroDto>> e) {
        boolean cargando = e.tipo == Estado.Tipo.CARGANDO;
        // El círculo de "tirar para actualizar" se usa si ya hay datos; si no, la ProgressBar central.
        boolean hayDatos = adapter.getItemCount() > 0;
        progress.setVisibility(cargando && !hayDatos ? View.VISIBLE : View.GONE);
        swipe.setRefreshing(cargando && hayDatos);
        fab.setEnabled(!cargando);                       // botones deshabilitados al cargar

        tvVacio.setVisibility(e.tipo == Estado.Tipo.VACIO ? View.VISIBLE : View.GONE);
        layoutError.setVisibility(e.tipo == Estado.Tipo.ERROR ? View.VISIBLE : View.GONE);

        switch (e.tipo) {
            case EXITO:
                adapter.actualizar(e.datos);
                break;
            case VACIO:
                adapter.actualizar(null);
                break;
            case ERROR:
                tvError.setText(MensajesError.texto(this, e));
                break;
            default:
                break;
        }
    }
}
