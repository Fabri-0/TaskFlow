# Bugs y mejoras

| Tipo | Descripcion | Estado | Mejora aplicada |
| --- | --- | --- | --- |
| Bug | Gradle 9.3.1 no era compatible con AGP 9.2.0 | Corregido | Wrapper actualizado a Gradle 9.4.1 |
| Bug | SDK local no tenia `android-36-ext20` | Corregido | `compileSdkVersion 'android-36.1'` |
| Mejora | Manejar notificaciones sin permiso | Aplicado | Se guarda la tarea y se informa al usuario |
| Mejora | Evitar contrasena en texto plano | Aplicado | Hash SHA-256 con sal local |

## Optimizacion futura

- Agregar edicion avanzada de multiples subtareas.
- Mejorar selectors visuales de proyecto y etiqueta.
- Agregar pruebas Espresso completas con ActivityScenario.
- Agregar migraciones Room cuando cambie el esquema.
- Agregar widget real si el alcance del curso lo permite.
