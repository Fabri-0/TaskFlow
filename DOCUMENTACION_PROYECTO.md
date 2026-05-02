# TaskFlow - Documentacion final del proyecto

Ultima revision local: 2026-05-01.

## 1. Estado de revision

| Revision | Resultado |
|---|---|
| Compilacion debug | Correcta con `assembleDebug`. |
| Pruebas unitarias | Correctas con `testDebugUnitTest`. |
| APK de pruebas Android | Correcto con `assembleDebugAndroidTest`. |
| Layouts | Sin ids repetidos dentro del mismo XML. |
| Textos antiguos | Sin referencias a opciones eliminadas como comentarios, donar, siguenos o apps familiares. |
| Limpieza de codigo | Sin `TODO`, `FIXME`, logs de debug ni clases Java duplicadas. |

## 2. Resumen general

TaskFlow es una aplicacion Android nativa en Java para gestionar tareas, subtareas, categorias, etiquetas, recordatorios, calendario y perfil de usuario.

Funciones principales:

| Funcion | Que hace |
|---|---|
| Registro/login | Registra con nombre, correo, usuario y contrasena. Inicia sesion con correo o usuario. |
| Cuenta | En `Mios`, muestra solo el usuario y permite editar nombre, correo, usuario y contrasena en un modal. |
| Tareas | Crear, editar, ver detalle, borrar, completar, destacar y organizar tareas. |
| Subtareas | Se agregan multiples subtareas en crear/editar y se muestran en detalle/resumen. |
| Categorias | Se crean, editan, eliminan y filtran desde menu, pantalla principal y formularios. |
| Etiquetas | Se crean, editan, eliminan y filtran igual que categorias. |
| Calendario | Muestra dias con tareas, vista mensual y listado visual de tareas del mes/dia. |
| Recordatorios | Programa notificaciones con alarma y reprograma tras reinicio. |
| Perfil/Mios | Muestra estadisticas, resumenes clicables, cuenta, tema y notificaciones. |
| Experiencia visual | Usa tema claro/oscuro, modales animados, confeti y feedback al completar/agregar/reabrir. |

## 3. Arquitectura

Flujo principal:

```text
Activity / Adapter / Dialog
        -> ViewModel
        -> Repository
        -> Room DAO
        -> SQLite local
```

Capas:

| Capa | Responsabilidad |
|---|---|
| UI | Pantallas, listas, dialogos, animaciones y eventos del usuario. |
| ViewModel | Estado observable y puente entre UI y repositorios. |
| Repository | Validaciones, reglas de negocio, hilos de trabajo y coordinacion de DAOs. |
| Room | Entidades, relaciones, DAOs y migracion de base local. |
| Notifications | Canales, alarmas, receptores y permisos de recordatorio. |
| Session | Usuario activo y modo de tema en `SharedPreferences`. |
| Utils | Fechas, validaciones, hashing, progreso, constantes y conversores. |

## 4. Estructura de paquetes

| Paquete | Contenido |
|---|---|
| `com.taskflow` | `TaskFlowApplication`. |
| `data.local.dao` | Consultas Room. |
| `data.local.db` | Base de datos singleton. |
| `data.local.entity` | Tablas Room. |
| `data.local.relation` | Modelos con relaciones Room. |
| `data.repository` | Operaciones de negocio. |
| `notifications` | Recordatorios y receptores. |
| `session` | Sesion y tema. |
| `ui.auth` | Login, registro y autenticacion. |
| `ui.calendar` | Calendario mensual y tareas por fecha. |
| `ui.common` | Dialogos reutilizables. |
| `ui.filter` | Bottom sheet de filtros. |
| `ui.main` | Pantalla principal, menu, filtros y celebraciones. |
| `ui.profile` | Pantalla `Mios`, estadisticas y cuenta. |
| `ui.project` | Gestion de categorias y etiquetas. |
| `ui.splash` | Inicio y redireccion por sesion. |
| `ui.task` | Crear, editar, detalle y subtareas. |
| `utils` | Utilidades generales. |

## 5. Base de datos

### `AppDatabase`

| Elemento | Detalle |
|---|---|
| Archivo | `data/local/db/AppDatabase.java` |
| Base | `taskflow.db` |
| Version | `2` |
| Migracion | `1 -> 2` agrega `username` a usuarios sin borrar datos. |
| DAOs | `UserDao`, `TaskDao`, `SubtaskDao`, `ProjectDao`, `SectionDao`, `TagDao`, `TaskTagDao`. |

### Entidades

| Entidad | Tabla | Atributos | Uso |
|---|---|---|---|
| `UserEntity` | `users` | `id`, `name`, `email`, `username`, `passwordHash`, `createdAt` | Usuario local. `email` y `username` son unicos. |
| `TaskEntity` | `tasks` | `id`, `title`, `description`, `isCompleted`, `dueDate`, `reminderDate`, `userId`, `projectId`, `sectionId`, `priority`, `isStarred`, `createdAt`, `updatedAt` | Tarea principal. |
| `SubtaskEntity` | `subtasks` | `id`, `taskId`, `title`, `isCompleted`, `createdAt` | Paso interno de una tarea. |
| `ProjectEntity` | `projects` | `id`, `name`, `color`, `userId` | Categoria. Nombre unico por usuario. |
| `TagEntity` | `tags` | `id`, `name`, `color`, `userId` | Etiqueta. Nombre unico por usuario. |
| `SectionEntity` | `sections` | `id`, `name`, `projectId` | Agrupacion interna por categoria. |
| `TaskTagCrossRef` | `task_tag_cross_ref` | `taskId`, `tagId` | Relacion muchos-a-muchos tarea/etiqueta. |

### Relaciones

| Clase | Atributos | Uso |
|---|---|---|
| `TaskFull` | `task`, `subtasks`, `tags`, `project` | Tarea completa para listas, detalle y perfil. |
| `TaskWithSubtasks` | `task`, `subtasks` | Tarea con pasos. |
| `TaskWithTags` | `task`, `tags` | Tarea con etiquetas. |

## 6. DAOs

### `UserDao`

| Metodo | Que hace |
|---|---|
| `insertUser(UserEntity)` | Inserta usuario. |
| `updateUser(UserEntity)` | Actualiza datos de cuenta. |
| `getUserByEmail(String)` | Busca por correo. |
| `getUserByEmailOrUsername(String)` | Busca por correo o usuario para login. |
| `getUserById(long)` | Busca por id. |
| `countByEmail(String)` | Valida correo repetido en registro. |
| `countByEmailForOtherUser(String, long)` | Valida correo repetido al editar. |
| `countByUsername(String)` | Valida usuario repetido en registro. |
| `countByUsernameForOtherUser(String, long)` | Valida usuario repetido al editar. |

### `TaskDao`

| Metodo | Que hace |
|---|---|
| `insertTask(TaskEntity)` | Crea tarea. |
| `updateTask(TaskEntity)` | Actualiza tarea completa. |
| `deleteTaskById(long, long)` | Borra tarea del usuario. |
| `getTaskById(long, long)` | Obtiene tarea simple. |
| `getTaskFullById(long, long)` | Obtiene tarea con subtareas, etiquetas y categoria. |
| `getAllTasks(long)` / `getAllFullTasks(long)` | Lista tareas del usuario. |
| `getTasksForToday(...)` / `getFullTasksForToday(...)` | Lista tareas de hoy. |
| `getOverdueTasks(...)` / `getFullOverdueTasks(...)` | Lista vencidas. |
| `getCompletedTasks(long)` / `getFullCompletedTasks(long)` | Lista completadas. |
| `getPendingTasks(long)` / `getFullPendingTasks(long)` | Lista pendientes. |
| `getFullStarredTasks(long)` | Lista tareas estrella. |
| `searchTasks(...)` / `searchFullTasks(...)` | Busca por texto. |
| `getTasksByProject(...)` / `getFullTasksByProject(...)` | Filtra por categoria. |
| `getFullTasksByTag(...)` | Filtra por etiqueta. |
| `getTasksByDateRange(...)` / `getFullTasksByDateRange(...)` | Filtra por rango de fecha. |
| `updateTaskCompleted(...)` | Cambia completado. |
| `countTasksForUser(long)` | Cuenta tareas del usuario. |
| `getFutureReminderTasks(long)` | Recupera recordatorios futuros. |

### `SubtaskDao`

| Metodo | Que hace |
|---|---|
| `insertSubtask(SubtaskEntity)` | Crea subtarea. |
| `updateSubtask(SubtaskEntity)` | Actualiza subtarea. |
| `deleteByTask(long)` | Borra subtareas de una tarea. |
| `deleteById(long)` | Borra subtarea individual. |
| `getSubtasksByTask(long)` | Observa subtareas. |
| `getSubtasksByTaskSync(long)` | Lee subtareas sincronicamente. |
| `countTotal(long)` | Cuenta subtareas. |
| `countCompleted(long)` | Cuenta subtareas hechas. |
| `updateCompleted(...)` | Marca subtarea hecha/no hecha. |

### `ProjectDao`

| Metodo | Que hace |
|---|---|
| `insertProject(ProjectEntity)` | Crea categoria. |
| `updateProject(ProjectEntity)` | Edita categoria. |
| `deleteProject(ProjectEntity)` | Borra categoria. |
| `getProjectsByUser(long)` | Observa categorias. |
| `getProjectsByUserSync(long)` | Lee categorias sincronicamente. |
| `getProjectById(long, long)` | Busca por id y usuario. |
| `getProjectByName(String, long)` | Busca por nombre y usuario. |
| `countByName(String, long)` | Evita duplicados. |

### `TagDao`

| Metodo | Que hace |
|---|---|
| `insertTag(TagEntity)` | Crea etiqueta. |
| `updateTag(TagEntity)` | Edita etiqueta. |
| `deleteTag(TagEntity)` | Borra etiqueta. |
| `getTagsByUser(long)` | Observa etiquetas. |
| `getTagsByUserSync(long)` | Lee etiquetas sincronicamente. |
| `getTagByName(String, long)` | Busca por nombre y usuario. |
| `countByName(String, long)` | Evita duplicados. |

### `SectionDao` y `TaskTagDao`

| DAO | Metodo | Que hace |
|---|---|---|
| `SectionDao` | `insertSection(SectionEntity)` | Crea seccion. |
| `SectionDao` | `getSectionsByProject(long)` | Observa secciones de una categoria. |
| `SectionDao` | `getSectionsByProjectSync(long)` | Lee secciones sincronicamente. |
| `TaskTagDao` | `insertRelation(TaskTagCrossRef)` | Une tarea y etiqueta. |
| `TaskTagDao` | `deleteRelationsForTask(long)` | Limpia etiquetas de una tarea. |
| `TaskTagDao` | `getTasksByTag(long, long)` | Consulta tareas por etiqueta. |

## 7. Repositorios

### `AuthRepository`

| Metodo/Atributo | Que hace |
|---|---|
| DAOs + `SessionManager` | Acceso a usuarios y datos iniciales. |
| `register(...)` | Valida, crea usuario, siembra demo generico y abre sesion. |
| `login(...)` | Inicia sesion con correo o usuario. |
| `getUser(long)` | Devuelve usuario actual. |
| `updateAccount(...)` | Edita nombre, correo, usuario y contrasena opcional. |
| `seedDemoDataIfNeeded(long)` | Crea `Tarea 1..3`, `Categoria 1..3`, `Etiqueta 1..3` y subtareas. |
| `postSuccess/postError` | Vuelve al hilo principal. |

### `TaskRepository`

| Metodo | Que hace |
|---|---|
| `getVisibleTasks(...)` | Decide consulta segun filtro, busqueda, categoria o etiqueta. |
| `getTasksByDate(...)` | Devuelve tareas de una fecha. |
| `getTaskFull(...)` | Carga una tarea completa. |
| `createTask(...)` | Crea tarea, subtareas, etiqueta y recordatorio. |
| `updateTask(...)` | Actualiza tarea, reemplaza subtareas/etiquetas y recordatorio. |
| `toggleCompleted(...)` | Marca/desmarca y cancela recordatorio si queda hecha. |
| `toggleSubtask(...)` | Marca/desmarca subtarea. |
| `deleteTask(...)` | Borra tarea, subtareas, relaciones y recordatorio. |
| `resolveProjectId(...)` | Reusa o crea categoria por nombre. |
| `linkTagIfPresent(...)` | Reusa o crea etiqueta y la vincula. |
| `saveSubtasks(...)` | Guarda subtareas no vacias. |
| `normalizeReminder(...)` | Ignora recordatorios pasados. |
| `scheduleIfValid(...)` | Programa recordatorio futuro. |

### Otros repositorios

| Clase | Metodos | Que hace |
|---|---|---|
| `ProjectRepository` | `getProjects`, `createProject`, `updateProject`, `deleteProject` | Gestion de categorias. |
| `TagRepository` | `getTags`, `createTag`, `updateTag`, `deleteTag` | Gestion de etiquetas. |
| `SectionRepository` | `getSections` | Lectura de secciones. |
| `ResultCallback<T>` | `onSuccess`, `onError` | Resultado simple para operaciones async. |

## 8. Autenticacion y cuenta

### `AuthViewModel`

| Atributo/Metodo | Que hace |
|---|---|
| `repository` | Acceso a autenticacion. |
| `error`, `loading`, `authSuccess`, `user` | Estado observable de UI. |
| `accountUpdated` | Evento al guardar cuenta. |
| `login(...)` | Login con correo o usuario. |
| `register(...)` | Valida nombre, correo, usuario, contrasena y confirmacion. |
| `loadUser(long)` | Carga usuario activo. |
| `updateAccount(...)` | Valida y guarda cambios de cuenta. |

### Pantallas

| Clase | Atributos principales | Metodos |
|---|---|---|
| `LoginActivity` | Inputs de correo/usuario y contrasena. | `onCreate`, `openMain`, `text`. |
| `RegisterActivity` | Inputs de nombre, correo, usuario, contrasena y confirmacion. | `onCreate`, `clearErrors`, `openMain`, `text`. |
| `SplashActivity` | `SessionManager`. | `onCreate` decide login o principal. |

Notas:

| Detalle | Estado |
|---|---|
| Ojo de contrasena | Activo en login, registro y modal de cuenta. |
| Cabecera de `Mios` | Muestra solo `@usuario`, no correo. |
| Datos privados | Se editan en `Cuenta y seguridad`. |

## 9. Pantalla principal

### `MainActivity`

| Grupo | Atributos |
|---|---|
| Estado | `currentFilter`, `selectedProjectId`, `selectedTagId`, `query`. |
| UI | Recycler, chips, menu lateral, popup de categorias/etiquetas, contadores. |
| Datos | `projects`, `tags`, `visibleTasks`. |
| Animacion | `CelebrationView`, feedback de agregar/completar/reabrir. |

| Metodo | Que hace |
|---|---|
| `setupTasks()` | Conecta RecyclerView con `TaskAdapter`. |
| `setupFilters()` | Prepara chips `Todas`, `Hoy`, `Estrella`, categorias y etiquetas. |
| `setupSearch()` | Busca por texto. |
| `setupSwipeToComplete(...)` | Swipe a la derecha para completar con feedback visual. |
| `setupNavigation()` | Configura barra inferior y menu. |
| `renderProjectChips(...)` | Renderiza filtros superiores. |
| `toggleMainFilterPopup(...)` | Abre/cierra popup flotante de categorias/etiquetas. |
| `renderDrawerSections()` | Dibuja menu con secciones expandibles. |
| `renderDrawerProjects()` | Lista categorias en menu. |
| `renderDrawerTags()` | Lista etiquetas en menu. |
| `renderDrawerAccount()` | Lista opciones de cuenta. |
| `applyFilter(...)` | Aplica filtro estatico. |
| `applyProjectFilter(...)` | Filtra por categoria. |
| `applyTagFilter(...)` | Filtra por etiqueta. |
| `updateStaticFilterSelection()` | Marca filtro activo. |
| `updateHomeCounters(...)` | Actualiza Hoy/Pendientes/Progreso. |
| `onTaskClicked(...)` | Abre detalle. |
| `onTaskCompleted(...)` | Marca/desmarca y celebra. |

### Apoyos de la pantalla principal

| Clase | Que hace |
|---|---|
| `MainViewModel` | Mantiene usuario, filtro, busqueda, categoria/etiqueta y progreso visible. |
| `TaskAdapter` | Lista tareas por secciones, muestra progreso, subtareas, categoria y etiquetas. |
| `CelebrationView` | Dibuja celebraciones para agregar, completar y reabrir. |
| `FilterBottomSheet` | Bottom sheet simple para filtros. |

## 10. Crear, editar y detalle de tareas

### `TaskFormActivity`

| Atributos | Uso |
|---|---|
| `editTitle`, `editDescription` | Titulo y descripcion. |
| `dropdownProject`, `dropdownTag` | Categoria y etiqueta ya creadas. |
| `subtaskInputList`, `subtaskInputs` | Subtareas dinamicas. |
| `checkStarred` | Marca tarea estrella. |
| `buttonDueDate`, `buttonReminder` | Fecha y recordatorio. |
| `taskId`, `dueDate`, `reminderDate` | Estado de edicion. |

| Metodo | Que hace |
|---|---|
| `bindViews()` | Conecta vistas. |
| `renderTask(...)` | Carga datos al editar. |
| `save()` | Valida y crea/actualiza. |
| `addSubtaskField(...)` | Agrega input de subtarea. |
| `collectSubtasks()` | Recoge subtareas no vacias. |
| `populateProjectDropdown(...)` | Llena categorias. |
| `populateTagDropdown(...)` | Llena etiquetas. |
| `pickDate(...)` | Abre selector de fecha. |
| `pickDateTime(...)` | Abre selector de fecha/hora. |

### Otras clases de tareas

| Clase | Que hace |
|---|---|
| `QuickTaskBottomSheet` | Crear tarea rapida desde la pantalla principal. |
| `TaskDetailActivity` | Ver detalle, subtareas, progreso, completar, editar y borrar. |
| `TaskViewModel` | Expone tarea, proyectos, etiquetas y acciones CRUD. |
| `SubtaskAdapter` | Lista subtareas y permite completarlas. |

## 11. Calendario

### `CalendarActivity`

| Atributo/Metodo | Que hace |
|---|---|
| `currentMonth`, `selectedDate`, `allTasks` | Estado del calendario. |
| `bindViews()` | Conecta cabecera, grid y lista. |
| `observeAllTasks()` | Observa tareas para pintar dias con actividad. |
| `renderMonth()` | Dibuja mes actual. |
| `taskCountsByDay()` | Cuenta tareas por dia. |
| `tasksInVisibleMonth()` | Cuenta tareas del mes mostrado. |
| `loadDate(long)` | Carga tareas de fecha seleccionada. |
| `onTaskClicked(...)` | Abre detalle. |
| `onTaskCompleted(...)` | Cambia estado desde calendario. |

### `CalendarDayAdapter`

| Metodo | Que hace |
|---|---|
| `submit(...)` | Recibe dias del calendario. |
| `onCreateViewHolder(...)` | Crea celda. |
| `onBindViewHolder(...)` | Pinta dia, seleccion y tareas. |
| `getItemCount()` | Total de celdas. |

## 12. Perfil / Mios

### `ProfileActivity`

| Grupo | Atributos |
|---|---|
| Sesion | `sessionManager`, `authViewModel`, `currentUser`. |
| Estadisticas | `textCompletionRate`, `textTodayCount`, `textTotalTasks`, `textPendingTasks`, `textCompletedTasks`, `textBusyDay`, `textStarredTasks`. |
| Datos | `dashboardTasks`, `dashboardProjects`, `dashboardTags`, `busiestDayMillis`. |
| Cuenta | `accountDialog`, `accountDialogContent`. |
| Ajustes | `switchDarkMode`, `textThemeStatus`, `textNotificationStatus`, `buttonNotificationPermission`. |

| Metodo | Que hace |
|---|---|
| `onCreate(...)` | Configura perfil, usuario, tema, permisos y accesos. |
| `observeDashboard()` | Observa tareas, categorias y etiquetas. |
| `renderTaskStats(...)` | Calcula total, hoy, pendientes, completadas, estrella y dia cargado. |
| `renderProjectStats(...)` | Muestra categorias. |
| `renderTagStats(...)` | Muestra etiquetas. |
| `setupInfoCards()` | Hace clicables las tarjetas. |
| `visibleUsername(...)` | Muestra solo usuario en cabecera. |
| `showAccountSettingsDialog()` | Modal para editar nombre, correo, usuario y contrasena. |
| `showTasksDialog(...)` | Modal de tareas solo lectura. |
| `subtaskPreviewLine(...)` | Muestra solo subtareas existentes y maximo 2 palabras. |
| `showTextDialog(...)` | Modal animado para informacion. |
| `busiestDayText(...)` | Dia con mas carga contando tareas y subtareas. |
| `updateThemeStatus(...)` | Texto del tema actual. |
| `updateNotificationStatus()` | Estado de permisos de notificacion. |
| `requestNotificationPermission()` | Pide permiso Android 13+. |

## 13. Categorias y etiquetas

### `ProjectActivity`

| Atributo/Metodo | Que hace |
|---|---|
| `ProjectViewModel`, `SessionManager` | Estado y usuario activo. |
| Inputs categoria/etiqueta | Crear nuevas entradas. |
| Listas | Muestran editar/borrar. |
| `addProjectRow(...)` | Fila de categoria. |
| `addTagRow(...)` | Fila de etiqueta. |
| `showEditDialog(...)` | Modal de renombrar. |
| `confirmDelete(...)` | Modal de confirmacion. |

### `ProjectViewModel`

| Metodo | Que hace |
|---|---|
| `getProjects(long)` | Observa categorias. |
| `getTags(long)` | Observa etiquetas. |
| `createProject(...)` / `createTag(...)` | Crea entradas. |
| `updateProject(...)` / `updateTag(...)` | Edita entradas. |
| `deleteProject(...)` / `deleteTag(...)` | Borra entradas. |
| `getError()` | Expone errores. |

## 14. UI comun y recursos visuales

### `TaskFlowPickerDialogs`

| Metodo | Que hace |
|---|---|
| `showDatePicker(...)` | Selector visual de fecha. |
| `showDateTimePicker(...)` | Selector visual de fecha + hora. |
| `showDateDialog(...)` | Dialogo de calendario. |
| `showTimeDialog(...)` | Dialogo de hora. |
| `showPolished(...)` | Aplica estilo/animacion. |

### Layouts principales

| Layout | Uso |
|---|---|
| `activity_splash.xml` | Inicio. |
| `activity_login.xml` | Login con correo/usuario. |
| `activity_register.xml` | Registro con usuario. |
| `activity_main.xml` | Pantalla principal. |
| `activity_calendar.xml` | Calendario. |
| `activity_profile.xml` | `Mios`. |
| `activity_project.xml` | Categorias/etiquetas. |
| `activity_task_form.xml` | Crear/editar tarea. |
| `activity_task_detail.xml` | Detalle. |

### Dialogos y componentes

| Layout | Uso |
|---|---|
| `bottom_sheet_quick_task.xml` | Crear tarea rapida. |
| `drawer_header.xml` | Cabecera del menu. |
| `dialog_account_settings.xml` | Cuenta y seguridad. |
| `dialog_confirm_delete.xml` | Confirmar borrado. |
| `dialog_edit_name.xml` | Editar nombre de categoria/etiqueta. |
| `dialog_profile_info.xml` | Modal informativo de `Mios`. |
| `dialog_taskflow_date_picker.xml` | Selector de fecha. |
| `dialog_taskflow_time_picker.xml` | Selector de hora. |
| `item_task.xml` | Fila de tarea. |
| `item_subtask.xml` | Fila de subtarea. |
| `item_subtask_input.xml` | Input de subtarea. |
| `item_calendar_day.xml` | Celda del calendario. |
| `item_manage_entry.xml` | Fila editar/borrar. |
| `item_dropdown_option.xml` | Opcion de dropdown. |

### Estilos visuales

| Recurso | Uso |
|---|---|
| `colors.xml` / `values-night/colors.xml` | Paleta claro/oscuro. |
| `themes.xml` | Tema Material, inputs, chips, dialogos y picker. |
| `bg_gradient_dark` | Fondo general. |
| `bg_home_panel`, `bg_header_panel` | Paneles principales. |
| `bg_card_dark`, `bg_card_soft` | Tarjetas. |
| `bg_profile_panel`, `bg_profile_glass` | Perfil/Mios. |
| `bg_dialog_panel`, `bg_dialog_edit_header`, `bg_picker_shell` | Dialogos. |
| `bg_calendar_*` | Estados del calendario. |
| `bg_drawer_*` | Menu lateral. |
| `ic_launcher`, `ic_launcher_round` | Icono de app con referencia a gestor de tareas y conejo. |

## 15. Notificaciones

### `ReminderScheduler`

| Metodo | Que hace |
|---|---|
| `ensureChannel(Context)` | Crea canal de alta importancia con sonido/vibracion. |
| `canNotify()` | Verifica permisos y estado de notificaciones. |
| `schedule(...)` | Programa alarma exacta si se puede; si no, alternativa segura. |
| `cancel(long)` | Cancela alarma de una tarea. |
| `pendingIntent(...)` | Construye intent de alarma. |
| `showIntent(long)` | Abre detalle al tocar notificacion. |

### Receptores

| Clase | Que hace |
|---|---|
| `ReminderReceiver` | Muestra la notificacion cuando dispara la alarma. |
| `BootReceiver` | Reprograma recordatorios futuros al reiniciar el dispositivo. |

## 16. Sesion y utilidades

### `SessionManager`

| Metodo | Que hace |
|---|---|
| `saveSession(long)` | Guarda usuario activo. |
| `getActiveUserId()` | Lee usuario activo. |
| `isLoggedIn()` | Indica si hay sesion. |
| `clearSession()` | Cierra sesion. |
| `saveThemeMode(int)` | Guarda tema. |
| `getThemeMode(int)` | Lee tema. |

### Utilidades

| Clase | Metodos | Que hace |
|---|---|---|
| `Constants` | Constantes de prefs, filtros, extras y canal. | Evita strings/ids magicos. |
| `DateUtils` | `startOfToday`, `endOfToday`, `startOfDay`, `endOfDay`, `formatDateTime`, `formatDate`, `isOverdue` | Manejo de fechas. |
| `PasswordUtils` | `hashPassword`, `matches` | Hash local de contrasena. |
| `ProgressUtils` | `calculatePercentage`, `formatCounter`, `completedSubtasks`, `taskCompletionPercentage`, `visibleTasksProgress` | Progreso de tareas. |
| `Validators` | `isValidName`, `isValidEmail`, `isValidUsername`, `isValidPassword`, `isValidTaskTitle` | Validaciones compartidas. |
| `Converters` | `fromTimestamp`, `dateToTimestamp` | Conversores Room. |

## 17. Manifest

| Elemento | Estado |
|---|---|
| Permisos | `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`. |
| Application | `TaskFlowApplication`. |
| Launcher | `SplashActivity`. |
| Actividades internas | Login, registro, principal, formulario, detalle, calendario, categorias y `Mios`. |
| Receivers | `ReminderReceiver`, `BootReceiver`. |
| Iconos | `@drawable/ic_launcher` y `@drawable/ic_launcher_round`. |

## 18. Flujos principales

### Registro

1. `RegisterActivity` recoge nombre, correo, usuario y contrasena.
2. `AuthViewModel.register(...)` valida formulario.
3. `AuthRepository.register(...)` verifica duplicados, crea usuario y demo generico.
4. `SessionManager` guarda sesion.
5. UI navega a `MainActivity`.

### Login

1. `LoginActivity` recibe correo o usuario.
2. `AuthRepository.login(...)` usa `getUserByEmailOrUsername`.
3. Si la contrasena coincide, guarda sesion.
4. UI abre la pantalla principal.

### Editar cuenta

1. `ProfileActivity` abre `dialog_account_settings.xml`.
2. Usuario cambia nombre/correo/usuario y opcionalmente contrasena.
3. `AuthViewModel.updateAccount(...)` valida.
4. `AuthRepository.updateAccount(...)` verifica duplicados y contrasena actual si aplica.
5. UI actualiza `@usuario` y cierra el modal.

### Crear/editar tarea

1. `TaskFormActivity` o `QuickTaskBottomSheet` recoge datos.
2. Categoria y etiqueta salen de las creadas.
3. Usuario agrega las subtareas que quiera.
4. `TaskRepository` guarda tarea, subtareas, etiquetas y recordatorio.
5. Room actualiza `LiveData`; UI se refresca.

### Completar tarea

1. Usuario toca checkbox o desliza a la derecha.
2. `MainActivity` llama `TaskViewModel.toggleCompleted`.
3. `TaskRepository` actualiza Room y cancela recordatorio si queda completada.
4. `CelebrationView` muestra animacion segun completar/desmarcar.

### Filtrar

1. Usuario toca filtro, menu, categoria o etiqueta.
2. `MainViewModel` guarda filtro activo.
3. `TaskRepository.getVisibleTasks(...)` selecciona consulta correcta.
4. Lista y contador se actualizan.

### Recordatorio

1. Usuario elige fecha/hora en picker visual.
2. `TaskRepository.scheduleIfValid(...)` programa.
3. `ReminderScheduler` crea alarma.
4. `ReminderReceiver` muestra notificacion.
5. `BootReceiver` reprograma si el telefono reinicia.

## 19. Notas de mantenimiento

| Tema | Criterio actual |
|---|---|
| Duplicacion visual | Los estilos se concentran en drawables, `colors.xml` y `themes.xml`. |
| Duplicacion de datos | DAOs validan nombres unicos por usuario para categorias/etiquetas y correo/usuario unicos. |
| Formularios | Inputs de contrasena usan `password_toggle`. |
| Datos sensibles | El correo no se muestra en la cabecera de `Mios`; va en modal privado. |
| Ejemplos nuevos | Las tareas demo son genericas para no parecer datos reales. |
| Compatibilidad local | Migracion 1->2 conserva datos al agregar `username`. |

