package com.bitacora.campo.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.bitacora.campo.data.remote.RegistroDto;
import com.bitacora.campo.data.repository.RegistroRepository;
import com.bitacora.campo.data.repository.Respuesta;
import com.bitacora.campo.data.repository.TipoError;

import retrofit2.Call;

/** Carga el detalle (GET /registros/{id}) y elimina (DELETE /registros/{id}). */
public class DetalleViewModel extends ViewModel {

    private final RegistroRepository repo = new RegistroRepository();
    private final MutableLiveData<Estado<RegistroDto>> estado = new MutableLiveData<>();
    private final MutableLiveData<Estado<Void>> eliminacion = new MutableLiveData<>();
    private Call<?> llamadaActual;

    public LiveData<Estado<RegistroDto>> getEstado() {
        return estado;
    }

    public LiveData<Estado<Void>> getEliminacion() {
        return eliminacion;
    }

    public void cargar(int id) {
        cancelar();
        estado.setValue(Estado.<RegistroDto>cargando());
        llamadaActual = repo.obtener(id, new Respuesta<RegistroDto>() {
            @Override
            public void onExito(RegistroDto datos) {
                if (datos == null) estado.setValue(Estado.<RegistroDto>vacio());
                else estado.setValue(Estado.exito(datos));
            }

            @Override
            public void onError(TipoError tipo, int codigo, String mensaje) {
                estado.setValue(Estado.<RegistroDto>error(tipo, codigo, mensaje));
            }
        });
    }

    public void eliminar(int id) {
        eliminacion.setValue(Estado.<Void>cargando());
        repo.eliminar(id, new Respuesta<Void>() {
            @Override
            public void onExito(Void datos) {
                eliminacion.setValue(Estado.<Void>exito(null));
            }

            @Override
            public void onError(TipoError tipo, int codigo, String mensaje) {
                eliminacion.setValue(Estado.<Void>error(tipo, codigo, mensaje));
            }
        });
    }

    /** Evita que el estado de eliminación se re-entregue tras rotar la pantalla. */
    public void consumirEliminacion() {
        eliminacion.setValue(null);
    }

    private void cancelar() {
        if (llamadaActual != null) llamadaActual.cancel();
    }

    @Override
    protected void onCleared() {
        cancelar();
    }
}
