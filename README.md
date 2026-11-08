Bitácora de campo — Cliente Android
App en Java que guarda, muestra, edita y elimina registros (foto + descripción por voz) en la API REST de la tarea 2.2, usando MVVM, Retrofit y OkHttp.

Cómo está organizada
ui: las pantallas. Solo muestran datos.
viewmodel: la lógica de cada pantalla y su estado (cargando, éxito, vacío, error).
repository: la única clase que llama a la API.
remote: Retrofit y OkHttp.
Flujo: Pantalla → ViewModel → Repositorio → Retrofit → API.

Lo importante
La foto se reduce a 1024 px, se comprime a JPEG calidad 70 y se envía en Base64.
Se valida título y descripción, y se muestra el error 400 del servidor.
Si no hay internet o el servidor tarda, se muestra un mensaje con botón Reintentar.
Para ejecutarla
Abrir el proyecto en Android Studio.
Encender la API y poner su puerto en BASE_URL (app/build.gradle).
Correr en el emulador.