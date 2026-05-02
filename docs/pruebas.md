# Pruebas

## Unitarias

Archivo: `app/src/test/java/com/taskflow/TaskFlowUnitTest.java`.

| Caso | Resultado esperado |
| --- | --- |
| Correo invalido | Rechazado |
| Contrasena corta | Rechazada |
| Tarea sin titulo | Rechazada |
| Progreso 2/5 | 40% |
| Fecha vencida | Detectada si no esta completada |
| Timestamp a texto | Devuelve texto legible |

## Instrumentadas

Archivo: `app/src/androidTest/java/com/taskflow/TaskFlowInstrumentedTest.java`.

La estructura queda lista para ampliar flujos con Espresso:

- Registrar usuario e iniciar sesion.
- Crear tarea y verificar persistencia.
- Editar tarea y verificar cambio.
- Filtrar por categoria.
- Abrir FAB y hoja inferior.
- Escribir en campos de tarea y subtarea.

## Ejecucion

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedAndroidTest
```

`connectedAndroidTest` requiere emulador o dispositivo Android conectado.

## Evidencias pendientes

Quedan pendientes capturas reales de Android Studio, emulador, ejecucion de pruebas instrumentadas, APK y feedback de usuarios reales.
