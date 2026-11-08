package com.bitacora.campo.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bitacora.campo.R;
import com.bitacora.campo.data.remote.RegistroDto;

import java.util.ArrayList;
import java.util.List;

/** Solo muestra datos y avisa del clic; no conoce Retrofit. */
public class RegistroAdapter extends RecyclerView.Adapter<RegistroAdapter.Holder> {

    public interface OnClick {
        void alTocar(RegistroDto registro);
    }

    private final List<RegistroDto> items = new ArrayList<>();
    private final OnClick listener;

    public RegistroAdapter(OnClick listener) {
        this.listener = listener;
    }

    public void actualizar(List<RegistroDto> nuevos) {
        items.clear();
        if (nuevos != null) items.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_registro, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        final RegistroDto r = items.get(position);
        h.titulo.setText(r.titulo);
        h.fecha.setText(r.fecha == null ? "" : r.fecha);
        h.descripcion.setText(r.descripcion);
        h.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.alTocar(r);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView titulo;
        final TextView fecha;
        final TextView descripcion;

        Holder(View v) {
            super(v);
            titulo = v.findViewById(R.id.tvTitulo);
            fecha = v.findViewById(R.id.tvFecha);
            descripcion = v.findViewById(R.id.tvDescripcion);
        }
    }
}
