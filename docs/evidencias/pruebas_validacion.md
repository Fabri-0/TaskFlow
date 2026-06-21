# Pruebas de validación de autenticación

Se realizaron pruebas manuales en el emulador para verificar el comportamiento de los formularios de login y registro de TaskFlow.

## Casos revisados

| Caso de prueba | Resultado obtenido | Estado |
|---|---|---|
| Login con campos vacíos | Se muestra mensaje de usuario/correo o contraseña incorrectos. | Cumple |
| Login con credenciales incorrectas | Se muestra error en el campo de contraseña y aviso inferior. | Cumple |
| Registro con contraseña menor a 6 caracteres | Se muestra el mensaje: La contraseña debe tener al menos 6 caracteres. | Cumple |
| Registro con confirmación diferente | Se muestra el mensaje: La confirmación no coincide. | Cumple |

## Observación

Las validaciones permiten evitar registros o accesos con datos incompletos, incorrectos o inconsistentes. Los mensajes se muestran dentro del formulario y también mediante avisos breves para que el usuario identifique el error.