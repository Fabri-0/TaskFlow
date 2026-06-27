Copia y reemplaza tu `README.md` con este contenido. Ya incluye CRUD, notificaciones, cómo probar el flujo y documentación para el Entregable 10.

# TaskFlow

TaskFlow es una aplicación Android nativa en Java para gestionar tareas personales y académicas de forma local. La aplicación no depende de Firebase, backend remoto, PostgreSQL, API externa obligatoria ni sincronización en la nube. Su funcionamiento se centra en el almacenamiento local, la gestión de tareas, recordatorios y seguimiento de actividades dentro del dispositivo.

## Problema que resuelve

TaskFlow ayuda a estudiantes universitarios y usuarios personales a registrar, organizar y dar seguimiento a sus actividades diarias. La aplicación permite crear tareas, agregar subtareas, asignar fechas límite, configurar recordatorios, clasificar por proyectos o etiquetas, aplicar filtros y revisar el progreso diario desde el teléfono.

## Tecnologías utilizadas

* Android nativo con XML layouts.
* Java como lenguaje principal de la aplicación.
* Gradle Wrapper 9.4.1.
* Android Gradle Plugin 9.2.0.
* Java Toolchain 21.
* Room sobre SQLite local.
* Arquitectura MVVM simple.
* ViewModel y LiveData.
* Repository + DAO.
* AppCompat.
* Material Components.
* ConstraintLayout.
* RecyclerView.
* SharedPreferences para sesión local.
* AlarmManager y NotificationChannel para recordatorios locales.

## Java 21 y SDK local

El proyecto está configurado con:

* `java.toolchain.languageVersion = 21`.
* `sourceCompatibility = JavaVersion.VERSION_21`.
* `targetCompatibility = JavaVersion.VERSION_21`.

Verificación local realizada:

* `java -version` del PATH global mostraba JDK 25.
* El proyecto se verificó ejecutando Gradle con `JAVA_HOME=C:\Program Files\Java\jdk-21`.
* `./gradlew -version` confirmó Launcher JVM 21.0.8.
* El SDK instalado en esta máquina contiene `platforms/android-36.1`, por eso `app/build.gradle` usa `compileSdkVersion 'android-36.1'`.

## Arquitectura general

TaskFlow utiliza una arquitectura MVVM simple para separar responsabilidades dentro de la aplicación.

* **Activities:** muestran la interfaz y reciben acciones del usuario.
* **ViewModel:** administra el estado de pantalla y comunica la interfaz con la lógica de datos.
* **Repository:** coordina las operaciones entre la interfaz y los DAO.
* **DAO:** contiene las consultas y operaciones CRUD sobre Room.
* **Room/SQLite:** almacena usuarios, tareas, subtareas, proyectos, secciones, etiquetas y relaciones locales.
* **SharedPreferences:** conserva datos simples como la sesión activa.

Esta estructura permite que la aplicación sea más ordenada, mantenible y coherente con el alcance local del proyecto.

## Funcionalidades implementadas

TaskFlow cuenta con funcionalidades principales para el flujo local de autenticación, gestión de tareas, planificación, recordatorios y organización personal.

### Autenticación local

* Registro local de usuario con nombre, correo, nombre de usuario y contraseña.
* Inicio de sesión local con usuario/correo y contraseña.
* Conservación de sesión activa mediante `SharedPreferences`.
* Cierre de sesión local desde el perfil del usuario.
* Validación de credenciales desde la base de datos local.
* Hash de contraseña mediante `PasswordUtils`, evitando guardar contraseñas en texto plano.
* Navegación automática hacia la pantalla principal cuando existe una sesión activa.

### Validaciones de formularios

* Validación de campos obligatorios en login y registro.
* Validación de longitud mínima de contraseña.
* Validación de confirmación de contraseña en el registro.
* Mensajes de error visibles dentro del formulario.
* Avisos breves cuando los datos ingresados no cumplen las condiciones requeridas.

## CRUD principal de tareas

La entidad principal del proyecto es la **tarea**. Sobre esta entidad se implementa el CRUD principal de la aplicación.

### Create

La operación Create permite registrar una nueva tarea desde el formulario principal. El usuario puede ingresar título, categoría, etiqueta, subtareas, fecha, alarma y otros datos relacionados con la organización de la actividad.

La creación se realiza desde la interfaz de usuario y se comunica con el `ViewModel`, el `Repository`, el `TaskDao` y finalmente con Room/SQLite para guardar la información localmente.

### Read

La operación Read permite consultar las tareas almacenadas en Room y mostrarlas en la pantalla principal mediante un `RecyclerView`.

La lista se actualiza usando `LiveData`, lo que permite que los cambios realizados en la base de datos se reflejen automáticamente en la interfaz. La pantalla principal muestra tareas vencidas, tareas sin fecha, pendientes, próximas y otras agrupaciones según los filtros disponibles.

### Update

La operación Update permite modificar una tarea existente. Al seleccionar una tarea, la aplicación abre el formulario de edición con los datos previamente cargados, como título, descripción, categoría, etiqueta, prioridad, tablero y subtareas.

Cuando el usuario guarda los cambios, la información se actualiza en Room y vuelve a mostrarse en la lista principal.

### Delete

La operación Delete se realiza mediante presión prolongada sobre una tarea. La aplicación envía la tarea a la papelera y muestra un aviso confirmando la acción.

La papelera permite mantener un control sobre las tareas eliminadas, ya que muestra los elementos enviados para restaurarlos o borrarlos definitivamente según corresponda.

## Tabla CRUD aplicada a TaskFlow

| Letra | Operación | Aplicación en TaskFlow                                                                                |
| ----- | --------- | ----------------------------------------------------------------------------------------------------- |
| C     | Create    | Crear una nueva tarea desde el formulario con título, categoría, etiqueta, subtareas, fecha y alarma. |
| R     | Read      | Mostrar las tareas guardadas en Room dentro del RecyclerView de la pantalla principal.                |
| U     | Update    | Editar una tarea existente mediante el formulario con datos precargados.                              |
| D     | Delete    | Enviar una tarea a la papelera mediante presión prolongada y mostrar un aviso de eliminación.         |

## Flujo de datos del CRUD

El flujo principal del CRUD en TaskFlow funciona de la siguiente manera:

```text
Usuario
  ↓
Activity / Pantalla
  ↓
ViewModel
  ↓
Repository
  ↓
DAO
  ↓
Room / SQLite
  ↓
LiveData
  ↓
RecyclerView
  ↓
Lista actualizada en pantalla
```

Este flujo evita que la interfaz consulte directamente la base de datos. De esta forma, cada capa cumple una responsabilidad específica y el proyecto mantiene una estructura más clara.

## Notificaciones locales

TaskFlow utiliza notificaciones locales para recordar al usuario sus tareas programadas. Esta decisión es coherente con el alcance del proyecto, ya que la aplicación funciona sin depender de una API externa o conexión permanente a internet.

Las notificaciones se activan cuando una tarea tiene una alarma o recordatorio configurado. Al llegar el momento programado, la aplicación muestra una alerta con el nombre de la tarea y opciones como posponer o apagar.

### Características de las notificaciones

* Recordatorios locales vinculados a tareas.
* Canal de notificaciones mediante `NotificationChannel`.
* Programación de alarmas con `AlarmManager`.
* Pantalla de alarma con información de la tarea.
* Opciones para posponer 5 minutos o apagar la alarma.
* Manejo de permisos de notificación en Android moderno.

### Razones para usar notificaciones locales

1. TaskFlow está diseñado como una aplicación local, sin dependencia de servicios externos.
2. Las historias de usuario del MVP incluyen recordatorios para tareas con fecha y hora.
3. Las notificaciones aportan valor directo al usuario porque ayudan a recordar actividades importantes aunque la aplicación no esté abierta.

## Planificación y seguimiento

* Calendario básico por fecha.
* Tablero local con estados: por hacer, en progreso y listo.
* Tareas recurrentes diarias, semanales o mensuales.
* Hasta tres recordatorios por tarea.
* Recordatorios locales con manejo de fechas pasadas y permisos de notificación.
* Acciones de notificación para completar o posponer tareas.
* Temporizador de tarea con cuenta regresiva.
* Estadísticas locales de productividad.
* Exportación local a JSON.
* Widget de pantalla de inicio con resumen de tareas del día.
* Perfil de usuario y preferencia de tema.
* Modo oscuro con estilo morado.
* Datos demo controlados al registrar el primer usuario.

## Cómo probar el CRUD

1. Abrir TaskFlow en Android Studio.
2. Ejecutar la aplicación en un emulador o dispositivo físico.
3. Iniciar sesión o registrar un usuario local.
4. En la pantalla principal, presionar el botón flotante `+`.
5. Crear una tarea con título, categoría, etiqueta, fecha o alarma.
6. Verificar que la tarea aparezca en la lista principal.
7. Seleccionar una tarea para abrir el formulario de edición.
8. Modificar datos como título, descripción, prioridad, subtareas o tablero.
9. Guardar los cambios y confirmar que la lista se actualice.
10. Mantener presionada una tarea para enviarla a la papelera.
11. Abrir la papelera y comprobar que la tarea eliminada aparece registrada.

## Cómo probar las notificaciones

1. Crear o editar una tarea.
2. Asignar una alarma o recordatorio a una hora cercana.
3. Guardar la tarea.
4. Esperar a que llegue la hora configurada.
5. Verificar que aparezca la notificación “Alarma de tarea”.
6. Abrir la notificación para revisar la pantalla de alarma.
7. Probar las opciones “Posponer 5 minutos” o “Apagar”.

## Evidencia visual

Captura de la pantalla de inicio de sesión de TaskFlow:

![Pantalla de inicio de sesión de TaskFlow](docs/evidencias/login_taskflow.png)

Captura de la lista principal con tareas:

![Lista principal de tareas](docs/evidencias/lista_tareas.png)

Captura del formulario de creación de tarea:

![Formulario de nueva tarea](docs/evidencias/formulario_creacion_tarea.png)

Captura del formulario de edición de tarea:

![Formulario de edición de tarea](docs/evidencias/formulario_edicion_tarea.png)

Captura de tarea actualizada en la lista:

![Tarea actualizada en la lista](docs/evidencias/tarea_actualizada.png)

Captura de tarea enviada a papelera:

![Tarea enviada a papelera](docs/evidencias/papelera_tarea.png)

Captura de notificación local:

![Notificación local de tarea](docs/evidencias/notificacion_tarea.png)

Captura de pantalla de alarma:

![Pantalla de alarma de tarea](docs/evidencias/pantalla_alarma.png)

## Video de demostración

[Video de demostración del flujo de autenticación de TaskFlow](https://uceedu-my.sharepoint.com/:f:/g/personal/fmtopon_uce_edu_ec/IgB8A32SzpaYRry5ZTlx1ZgLAeqVNTqYwRHheBZQzy152Ks?e=z8sQl2)

## Fuera de alcance

* Sin nube, API externa obligatoria o backend.
* Sin Compose ni Kotlin para la aplicación.
* Sin Firebase ni PostgreSQL.
* Sin sincronización entre dispositivos.
* La vista PRO del menú lateral es solo visual e informativa.

## Abrir en Android Studio

1. Abrir la carpeta `TaskFlow`.
2. Esperar la sincronización de Gradle.
3. Confirmar que Android Studio use JDK 21 o que `JAVA_HOME` apunte a JDK 21.
4. Ejecutar el módulo `app`.

## Comandos

Desde la carpeta `TaskFlow`:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat clean
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

Para pruebas instrumentadas se requiere emulador o dispositivo:

```powershell
.\gradlew.bat connectedAndroidTest
```

## Documentación

La carpeta `docs/` contiene documentación técnica, manual de usuario, pruebas, bugs, mejoras, guion de video demo y evidencias del proyecto.

## Estado actual del proyecto

El proyecto cuenta con autenticación local, CRUD principal de tareas, persistencia con Room/SQLite, filtros, subtareas, papelera, recordatorios locales, pantalla de alarma, tablero, calendario y documentación inicial en GitHub.
