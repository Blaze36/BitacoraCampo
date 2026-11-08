package com.bitacora.campo.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.bitacora.campo.data.remote.RegistroDto;
import com.bitacora.campo.data.repository.RegistroRepository;
import com.bitacora.campo.data.repository.Respuesta;
import com.bitacora.campo.data.repository.TipoError;

import retrofit2.Call;

/** Crear (POST) y editar (PUT). También carga el registro existente al editar. */
public class FormularioViewModel extends ViewModel {

    private final RegistroRepository repo = new RegistroRepository();
    private final MutableLiveData<Estado<RegistroDto>> carga = new MutableLiveData<>();
    private final MutableLiveData<Estado<RegistroDto>> guardado = new MutableLiveData<>();
    private Call<?> llamadaActual;

    public LiveData<Estado<RegistroDto>> getCarga() {
        return carga;
    }

    public LiveData<Estado<RegistroDto>> getGuardado() {
        return guardado;
    }

    /** Modo edición: trae el registro para rellenar el formulario. */
    public void cargar(int id) {
        carga.setValue(Estado.<RegistroDto>cargando());
        llamadaActual = repo.obtener(id, new Respuesta<RegistroDto>() {
            @Override
            public void onExito(RegistroDto datos) {
                carga.setValue(Estado.exito(datos));
            }

            @Override
            public void onError(TipoError tipo, int codigo, String mensaje) {
                carga.setValue(Estado.<RegistroDto>error(tipo, codigo, mensaje));
            }
        });
    }

    /** id == null crea (POST); id != null actualiza (PUT). */
    public void guardar(Integer id, RegistroDto dto) {
        guardado.setValue(Estado.<RegistroDto>cargando());
        Respuesta<RegistroDto> r = new Respuesta<RegistroDto>() {
            @Override
            public void onExito(RegistroDto datos) {
                guardado.setValue(Estado.exito(datos));
            }

            @Override
            public void onError(TipoError tipo, int codigo, String mensaje) {
                guardado.setValue(Estado.<RegistroDto>error(tipo, codigo, mensaje));
            }
        };
        llamadaActual = (id == null) ? repo.crear(dto, r) : repo.actualizar(id, dto, r);
    }

    @Override
    protected void onCleared() {
        if (llamadaActual != null) llamadaActual.cancel();
    }
}
