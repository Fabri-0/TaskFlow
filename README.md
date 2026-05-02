# TaskFlow

TaskFlow es una aplicacion Android nativa en Java para gestionar tareas personales o academicas de forma local, sin Firebase, backend remoto, PostgreSQL ni sincronizacion en la nube.

## Problema que resuelve

Ayuda a estudiantes universitarios y usuarios personales a registrar tareas, subtareas, fechas limite, recordatorios, proyectos, etiquetas, filtros y progreso diario dentro del telefono.

## Tecnologias

- Android nativo con XML layouts.
- Java como lenguaje de aplicacion.
- Gradle wrapper 9.4.1.
- Android Gradle Plugin 9.2.0.
- Java toolchain 21.
- Room sobre SQLite local.
- MVVM simple con ViewModel y LiveData.
- Repository + DAO.
- AppCompat, Material Components, ConstraintLayout y RecyclerView.
- Notificaciones locales con AlarmManager y NotificationChannel.

## Java 21 y SDK local

El proyecto esta configurado con:

- `java.toolchain.languageVersion = 21`.
- `sourceCompatibility = JavaVersion.VERSION_21`.
- `targetCompatibility = JavaVersion.VERSION_21`.

Verificacion local realizada:

- `java -version` del PATH global mostraba JDK 25.
- El proyecto se verifico ejecutando Gradle con `JAVA_HOME=C:\Program Files\Java\jdk-21`.
- `./gradlew -version` confirmo Launcher JVM 21.0.8.
- El SDK instalado en esta maquina contiene `platforms/android-36.1`, por eso `app/build.gradle` usa `compileSdkVersion 'android-36.1'`.

## Funciones implementadas

- Registro local de usuario.
- Login local y cierre de sesion con `SharedPreferences`.
- Hash de contrasena con `PasswordUtils`; no se guarda texto plano.
- CRUD de tareas filtradas por `activeUserId`.
- Subtareas persistidas con `SubtaskEntity`.
- Proyectos, secciones, etiquetas y relacion muchos a muchos tarea-etiqueta.
- Busqueda por texto.
- Filtros: todas, hoy, vencidas, completadas y pendientes.
- Calendario basico por fecha.
- Perfil y preferencia de tema.
- Recordatorios locales con manejo de fechas pasadas y permiso de notificaciones.
- Modo oscuro con estilo morado.
- Datos demo controlados al registrar el primer usuario.

## Fuera de alcance

- Sin nube, API externa obligatoria o backend.
- Sin Compose ni Kotlin para la app.
- Sin Firebase ni PostgreSQL.
- La vista PRO del menu lateral es solo visual/informativa.

## Abrir en Android Studio

1. Abrir la carpeta `TaskFlow`.
2. Esperar sincronizacion Gradle.
3. Confirmar que Android Studio use JDK 21 o que `JAVA_HOME` apunte a JDK 21.
4. Ejecutar el modulo `app`.

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

## Documentacion

La carpeta `docs/` contiene manual tecnico, manual de usuario, pruebas, bugs/mejoras, guion de video demo y evidencias pendientes.
