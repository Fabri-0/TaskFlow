# Manual tecnico

## Requisitos

- Android Studio reciente.
- JDK 21.
- SDK Android con plataforma `android-36.1` o ajustar `compileSdkVersion` a una plataforma instalada.
- Gradle wrapper incluido.

## Apertura

Abrir la carpeta `TaskFlow` en Android Studio y sincronizar Gradle. El modulo principal es `app`.

## Configuracion Java 21

`app/build.gradle` configura toolchain, sourceCompatibility y targetCompatibility en Java 21. Durante la verificacion local se uso `JAVA_HOME=C:\Program Files\Java\jdk-21`.

## Arquitectura

La app usa MVVM simple:

- Activities y BottomSheet en `ui/`.
- ViewModel con LiveData.
- Repositorios en `data/repository`.
- DAO en `data/local/dao`.
- Entidades Room en `data/local/entity`.
- Base de datos `AppDatabase`.

Los ViewModel no consultan DAO directamente.

## Room

Entidades principales:

- `UserEntity`.
- `TaskEntity`.
- `SubtaskEntity`.
- `ProjectEntity`.
- `SectionEntity`.
- `TagEntity`.
- `TaskTagCrossRef`.

Relaciones:

- Usuario a tareas, proyectos y etiquetas.
- Proyecto a secciones y tareas.
- Tarea a subtareas.
- Tarea a etiquetas mediante tabla cruzada.

## Sesion local

`SessionManager` guarda `activeUserId` en `SharedPreferences`. Todas las consultas visibles filtran por usuario.

## Recordatorios

`ReminderScheduler` usa `AlarmManager`, `ReminderReceiver` muestra la notificacion y `BootReceiver` reprograma recordatorios futuros al reiniciar el dispositivo. Si la fecha es pasada, no se programa. Si falta permiso de notificaciones, la tarea se guarda y se informa al usuario.

## Compilacion

```powershell
.\gradlew.bat clean
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```
