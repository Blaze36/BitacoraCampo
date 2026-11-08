package com.bitacora.campo.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.bitacora.campo.data.remote.RegistroDto;
import com.bitacora.campo.data.repository.RegistroRepository;
import com.bitacora.campo.data.repository.Respuesta;
import com.bitacora.campo.data.repository.TipoError;

import java.util.List;

import retrofit2.Call;

public class ListaViewModel extends ViewModel {

    private final RegistroRepository repo = new RegistroRepository();
    private final MutableLiveData<Estado<List<RegistroDto>>> estado = new MutableLiveData<>();
    private Call<?> llamadaActual;

    public LiveData<Estado<List<RegistroDto>>> getEstado() {
        return estado;
    }

    public void cargar() {
        cancelar();
        estado.setValue(Estado.<List<RegistroDto>>cargando());
        llamadaActual = repo.listar(new Respuesta<List<RegistroDto>>() {
            @Override
            public void onExito(List<RegistroDto> datos) {
                if (datos == null || datos.isEmpty()) {
                    estado.setValue(Estado.<List<RegistroDto>>vacio());
                } else {
                    estado.setValue(Estado.exito(datos));
                }
            }

            @Override
            public void onError(TipoError tipo, int codigo, String mensaje) {
                estado.setValue(Estado.<List<RegistroDto>>error(tipo, codigo, mensaje));
            }
        });
    }

    private void cancelar() {
        if (llamadaActual != null) llamadaActual.cancel();
    }

    @Override
    protected void onCleared() {
        cancelar();
    }
}
