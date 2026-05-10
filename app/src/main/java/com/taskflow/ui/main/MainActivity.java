package com.taskflow.ui.main;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.taskflow.R;
import com.taskflow.data.local.entity.ProjectEntity;
import com.taskflow.data.local.entity.TagEntity;
import com.taskflow.data.local.relation.TaskFull;
import com.taskflow.notifications.PermissionPrefs;
import com.taskflow.notifications.ReminderScheduler;
import com.taskflow.session.SessionManager;
import com.taskflow.ui.auth.LoginActivity;
import com.taskflow.ui.board.BoardActivity;
import com.taskflow.ui.calendar.CalendarActivity;
import com.taskflow.ui.profile.ProfileActivity;
import com.taskflow.ui.productivity.ProductivityActivity;
import com.taskflow.ui.project.ProjectActivity;
import com.taskflow.ui.task.QuickTaskBottomSheet;
import com.taskflow.ui.task.TaskDetailActivity;
import com.taskflow.ui.task.TaskViewModel;
import com.taskflow.ui.trash.TrashActivity;
import com.taskflow.utils.Constants;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements TaskAdapter.Listener {
    private MainViewModel mainViewModel;
    private TaskViewModel taskViewModel;
    private SessionManager sessionManager;
    private TaskAdapter adapter;
    private DrawerLayout drawerLayout;
    private TextView textProgress;
    private TextView textTodayCounter;
    private TextView textPendingCounter;
    private LinearProgressIndicator progressIndicator;
    private TextInputLayout inputSearchContainer;
    private TextInputEditText editSearch;
    private ImageButton buttonSearchToggle;
    private HorizontalScrollView filterScrollView;
    private ChipGroup chipGroupFilters;
    private Chip chipAll;
    private Chip chipToday;
    private Chip chipStarred;
    private Chip chipUpcoming;
    private Chip chipOverdue;
    private Chip chipCompleted;
    private Chip chipPending;
    private Chip chipNoDate;
    private Chip chipArchived;
    private Chip chipTrash;
    private MaterialButton buttonFilterMenu;
    private PopupWindow mainFilterPopup;
    private boolean suppressMainFilterDismissRender;
    private LinearLayout drawerStaticContainer;
    private LinearLayout drawerCategoriesContainer;
    private LinearLayout drawerTagsContainer;
    private LinearLayout drawerAccountContainer;
    private TextView drawerCategoriesHeader;
    private TextView drawerTagsHeader;
    private TextView drawerAccountHeader;
    private List<ProjectEntity> drawerProjects;
    private List<TagEntity> drawerTags;
    private boolean mainCategoriesExpanded;
    private boolean mainTagsExpanded;
    private boolean searchExpanded;
    private int activeFilter = Constants.FILTER_ALL;
    private Long activeProjectId;
    private Long activeTagId;
    private Integer activePriority;
    private boolean categoriesExpanded;
    private boolean tagsExpanded;
    private boolean accountExpanded;
    private AlertDialog permissionDialog;
    private boolean refreshPermissionDialogOnResume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            openLogin();
            return;
        }
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        drawerLayout = findViewById(R.id.drawerLayout);
        textProgress = findViewById(R.id.textProgress);
        textTodayCounter = findViewById(R.id.textTodayCounter);
        textPendingCounter = findViewById(R.id.textPendingCounter);
        setupTasks();
        setupFilters();
        setupNavigation();
        setupSearch();
        setupCreationCelebration();
        requestStartupRuntimePermissions();
        showFirstRunPermissionGuideIfNeeded();
        mainViewModel.setUserId(sessionManager.getActiveUserId());
        mainViewModel.getProjects().observe(this, projects -> {
            drawerProjects = projects;
            renderProjectChips(projects);
            renderDrawerSections();
        });
        mainViewModel.getTags().observe(this, tags -> {
            drawerTags = tags;
            renderProjectChips(drawerProjects);
            renderDrawerSections();
        });
        mainViewModel.getVisibleTasks().observe(this, tasks -> {
            adapter.submit(tasks);
            updateHomeCounters(tasks);
        });
        mainViewModel.getProgress().observe(this, value -> {
            int progress = value == null ? 0 : value;
            textProgress.setText(progress + "% completado");
            progressIndicator.setProgress(progress);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (refreshPermissionDialogOnResume || permissionDialog != null) {
            refreshPermissionDialogOnResume = false;
            drawerLayout.postDelayed(this::showPermissionSettings, 220L);
        }
    }

    private void setupTasks() {
        RecyclerView recyclerTasks = findViewById(R.id.recyclerTasks);
        recyclerTasks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter(this);
        recyclerTasks.setAdapter(adapter);
        setupSwipeToComplete(recyclerTasks);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> new QuickTaskBottomSheet().show(getSupportFragmentManager(), "quick_task"));
        findViewById(R.id.buttonDrawer).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        progressIndicator = findViewById(R.id.progressIndicator);
    }

    private void setupFilters() {
        filterScrollView = findViewById(R.id.filterScrollView);
        chipGroupFilters = findViewById(R.id.chipGroupFilters);
        chipAll = findViewById(R.id.chipAll);
        chipToday = findViewById(R.id.chipToday);
        chipStarred = findViewById(R.id.chipStarred);
        chipUpcoming = findViewById(R.id.chipUpcoming);
        chipOverdue = findViewById(R.id.chipOverdue);
        chipCompleted = findViewById(R.id.chipCompleted);
        chipPending = findViewById(R.id.chipPending);
        chipNoDate = findViewById(R.id.chipNoDate);
        chipArchived = findViewById(R.id.chipArchived);
        chipTrash = findViewById(R.id.chipTrash);
        buttonFilterMenu = findViewById(R.id.buttonFilterMenu);
        chipAll.setOnClickListener(v -> applyFilter(Constants.FILTER_ALL));
        chipToday.setOnClickListener(v -> applyFilter(Constants.FILTER_TODAY));
        chipStarred.setOnClickListener(v -> applyFilter(Constants.FILTER_STARRED));
        chipUpcoming.setOnClickListener(v -> applyFilter(Constants.FILTER_UPCOMING));
        chipOverdue.setOnClickListener(v -> applyFilter(Constants.FILTER_OVERDUE));
        chipCompleted.setOnClickListener(v -> applyFilter(Constants.FILTER_COMPLETED));
        chipPending.setOnClickListener(v -> applyFilter(Constants.FILTER_PENDING));
        chipNoDate.setOnClickListener(v -> applyFilter(Constants.FILTER_NO_DATE));
        chipArchived.setOnClickListener(v -> applyFilter(Constants.FILTER_ARCHIVED));
        chipTrash.setOnClickListener(v -> applyFilter(Constants.FILTER_TRASH));
        buttonFilterMenu.setOnClickListener(this::showFilterMenu);
        updateStaticFilterSelection();
        updateAdvancedFilterButtons();
    }

    private void setupSearch() {
        inputSearchContainer = findViewById(R.id.inputSearchContainer);
        editSearch = findViewById(R.id.editSearch);
        buttonSearchToggle = findViewById(R.id.buttonSearchToggle);
        buttonSearchToggle.setOnClickListener(v -> toggleSearch());
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mainViewModel.setQuery(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void toggleSearch() {
        if (!searchExpanded) {
            searchExpanded = true;
            inputSearchContainer.setVisibility(View.VISIBLE);
            editSearch.requestFocus();
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (manager != null) {
                manager.showSoftInput(editSearch, InputMethodManager.SHOW_IMPLICIT);
            }
            return;
        }
        if (editSearch.getText() != null && editSearch.getText().length() > 0) {
            editSearch.setText("");
            return;
        }
        collapseSearch();
    }

    private void collapseSearch() {
        searchExpanded = false;
        if (inputSearchContainer != null) {
            inputSearchContainer.setVisibility(View.GONE);
        }
        if (editSearch != null) {
            editSearch.clearFocus();
        }
        InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null && editSearch != null) {
            manager.hideSoftInputFromWindow(editSearch.getWindowToken(), 0);
        }
    }

    private void setupCreationCelebration() {
        getSupportFragmentManager().setFragmentResultListener("quick_task_saved", this,
                (requestKey, result) -> playCelebration("Tarea añadida ✨", CelebrationView.addPalette()));
    }

    private void setupSwipeToComplete(RecyclerView recyclerTasks) {
        Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(ContextCompat.getColor(this, R.color.success));
        circlePaint.setColor(ContextCompat.getColor(this, R.color.on_primary));
        textPaint.setColor(ContextCompat.getColor(this, R.color.on_primary));
        textPaint.setTextSize(15f * getResources().getDisplayMetrics().scaledDensity);
        textPaint.setFakeBoldText(true);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.RIGHT) {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getBindingAdapterPosition();
                TaskFull task = adapter.getTaskAt(position);
                int dragFlags = adapter.isTaskRow(position) ? ItemTouchHelper.UP | ItemTouchHelper.DOWN : 0;
                if (!adapter.isTaskRow(position) || task == null || task.task == null || task.task.isCompleted) {
                    return makeMovementFlags(dragFlags, 0);
                }
                return makeMovementFlags(dragFlags, ItemTouchHelper.RIGHT);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getBindingAdapterPosition();
                int to = target.getBindingAdapterPosition();
                if (!adapter.isTaskRow(from) || !adapter.isTaskRow(to)) {
                    return false;
                }
                adapter.moveTaskRow(from, to);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                TaskFull task = adapter.getTaskAt(position);
                if (task != null && task.task != null && !task.task.isCompleted) {
                    onTaskCompleted(task, true);
                }
                if (position != RecyclerView.NO_POSITION) {
                    adapter.notifyItemChanged(position);
                }
            }

            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                return 0.42f;
            }

            @Override
            public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                if (dX > 0) {
                    View itemView = viewHolder.itemView;
                    float right = itemView.getLeft() + dX;
                    RectF background = new RectF(itemView.getLeft(), itemView.getTop(), right, itemView.getBottom());
                    canvas.drawRoundRect(background, dp(16), dp(16), backgroundPaint);
                    float centerY = itemView.getTop() + itemView.getHeight() / 2f;
                    float circleX = itemView.getLeft() + dp(34);
                    canvas.drawCircle(circleX, centerY, dp(15), circlePaint);
                    backgroundPaint.setColor(ContextCompat.getColor(MainActivity.this, R.color.success));
                    Paint checkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    checkPaint.setColor(ContextCompat.getColor(MainActivity.this, R.color.success));
                    checkPaint.setTextAlign(Paint.Align.CENTER);
                    checkPaint.setTextSize(19f * getResources().getDisplayMetrics().scaledDensity);
                    checkPaint.setFakeBoldText(true);
                    canvas.drawText("✓", circleX, centerY + dp(7), checkPaint);
                    String label = dX > itemView.getWidth() * 0.42f ? "Soltar para completar" : "Desliza para completar";
                    canvas.drawText(label, itemView.getLeft() + dp(58), centerY + dp(6), textPaint);
                }
                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerTasks);
    }

    private void setupNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_tasks);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_calendar) {
                startActivity(new Intent(this, CalendarActivity.class));
                return true;
            }
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            if (id == R.id.nav_productivity) {
                startActivity(new Intent(this, ProductivityActivity.class));
                return true;
            }
            return true;
        });

        drawerStaticContainer = findViewById(R.id.drawerStaticContainer);
        drawerCategoriesContainer = findViewById(R.id.drawerCategoriesContainer);
        drawerTagsContainer = findViewById(R.id.drawerTagsContainer);
        drawerAccountContainer = findViewById(R.id.drawerAccountContainer);
        drawerCategoriesHeader = findViewById(R.id.drawerCategoriesHeader);
        drawerTagsHeader = findViewById(R.id.drawerTagsHeader);
        drawerAccountHeader = findViewById(R.id.drawerAccountHeader);
        View permissionButton = findViewById(R.id.buttonPermissionSettings);
        if (permissionButton != null) {
            permissionButton.setOnClickListener(v -> showPermissionSettings());
        }
        drawerCategoriesHeader.setOnClickListener(v -> {
            categoriesExpanded = !categoriesExpanded;
            renderDrawerSections();
        });
        drawerTagsHeader.setOnClickListener(v -> {
            tagsExpanded = !tagsExpanded;
            renderDrawerSections();
        });
        drawerAccountHeader.setOnClickListener(v -> {
            accountExpanded = !accountExpanded;
            renderDrawerSections();
        });
        renderDrawerSections();
    }

    private void renderProjectChips(List<ProjectEntity> projects) {
        if (chipGroupFilters == null) {
            return;
        }
        updateAdvancedFilterButtons();
    }

    private CharSequence categoryToggleText() {
        if (activeProjectId != null) {
            String name = projectName(activeProjectId);
            if (!name.isEmpty()) {
                return "🗂️ " + name + (mainCategoriesExpanded ? " ▾" : " ▸");
            }
        }
        return countLabelText("🗂️ ", categoryCount(), "Categoria", "Categorias", mainCategoriesExpanded ? " ▾" : " ▸");
    }

    private CharSequence tagToggleText() {
        if (activeTagId != null) {
            String name = tagName(activeTagId);
            if (!name.isEmpty()) {
                return "🏷️ " + name + (mainTagsExpanded ? " ▾" : " ▸");
            }
        }
        return countLabelText("🏷️ ", tagCount(), "Etiqueta", "Etiquetas", mainTagsExpanded ? " ▾" : " ▸");
    }

    private int categoryCount() {
        return drawerProjects == null ? 0 : drawerProjects.size();
    }

    private int tagCount() {
        return drawerTags == null ? 0 : drawerTags.size();
    }

    private CharSequence countLabelText(String prefix, int count, String singular, String plural, String suffix) {
        String number = String.valueOf(count);
        String text = prefix + number + " " + (count == 1 ? singular : plural) + suffix;
        SpannableString spannable = new SpannableString(text);
        int start = prefix.length();
        spannable.setSpan(new RelativeSizeSpan(0.78f), start, start + number.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }

    private String projectName(long id) {
        if (drawerProjects == null) {
            return "";
        }
        for (ProjectEntity project : drawerProjects) {
            if (project != null && project.id == id) {
                return project.name;
            }
        }
        return "";
    }

    private String tagName(long id) {
        if (drawerTags == null) {
            return "";
        }
        for (TagEntity tag : drawerTags) {
            if (tag != null && tag.id == id) {
                return tag.name;
            }
        }
        return "";
    }

    private Chip createFilterToggle(CharSequence text, int colorRes) {
        Chip chip = new Chip(this);
        chip.setId(View.generateViewId());
        chip.setTag("filter_dynamic");
        chip.setText(text);
        chip.setCheckable(false);
        chip.setCheckedIconVisible(false);
        chip.setChipBackgroundColorResource(colorRes);
        chip.setTextColor(ContextCompat.getColor(this, R.color.on_primary));
        return chip;
    }

    private Chip createFilterOption(String text) {
        Chip chip = new Chip(this);
        chip.setId(View.generateViewId());
        chip.setTag("filter_dynamic");
        chip.setText(text);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);
        chip.setChipBackgroundColorResource(R.color.chip_background);
        chip.setTextColor(ContextCompat.getColorStateList(this, R.color.chip_text));
        return chip;
    }

    private void showFilterMenu(View anchor) {
        dismissMainFilterPopup(true);
        if (anchor == null || !anchor.isAttachedToWindow()) {
            return;
        }
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundResource(R.drawable.bg_dialog_panel);
        content.setPadding(dp(10), dp(10), dp(10), dp(10));

        LinearLayout[] opened = new LinearLayout[1];
        addFilterDropdown(content, "Prioridad", opened, target -> {
            addFilterOptionRow(target, "Sin filtro", activePriority == null, () -> applyPriorityFilter(null));
            addFilterOptionRow(target, "Baja", activePriority != null && activePriority == Constants.PRIORITY_LOW, () -> applyPriorityFilter(Constants.PRIORITY_LOW));
            addFilterOptionRow(target, "Media", activePriority != null && activePriority == Constants.PRIORITY_MEDIUM, () -> applyPriorityFilter(Constants.PRIORITY_MEDIUM));
            addFilterOptionRow(target, "Alta", activePriority != null && activePriority == Constants.PRIORITY_HIGH, () -> applyPriorityFilter(Constants.PRIORITY_HIGH));
            addFilterOptionRow(target, "Urgente", activePriority != null && activePriority == Constants.PRIORITY_URGENT, () -> applyPriorityFilter(Constants.PRIORITY_URGENT));
        });
        addFilterDropdown(content, "Categorias", opened, target -> {
            addFilterOptionRow(target, "Sin filtro", activeProjectId == null, () -> applyProjectFilter(null));
            if (drawerProjects == null || drawerProjects.isEmpty()) {
                addFilterOptionRow(target, "Sin categorias", false, null);
            } else {
                for (ProjectEntity project : drawerProjects) {
                    addFilterOptionRow(target, project.name, activeProjectId != null && activeProjectId == project.id,
                            () -> applyProjectFilter(project.id));
                }
            }
        });
        addFilterDropdown(content, "Etiquetas", opened, target -> {
            addFilterOptionRow(target, "Sin filtro", activeTagId == null, () -> applyTagFilter(null));
            if (drawerTags == null || drawerTags.isEmpty()) {
                addFilterOptionRow(target, "Sin etiquetas", false, null);
            } else {
                for (TagEntity tag : drawerTags) {
                    addFilterOptionRow(target, tag.name, activeTagId != null && activeTagId == tag.id,
                            () -> applyTagFilter(tag.id));
                }
            }
        });
        addFilterRootRow(content, "Limpiar filtros", this::clearAdvancedFilters);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        scrollView.addView(content);
        int popupWidth = Math.min(getResources().getDisplayMetrics().widthPixels - dp(56), dp(292));
        int popupHeight = Math.min((int) (getResources().getDisplayMetrics().heightPixels * 0.48f), dp(340));
        mainFilterPopup = new PopupWindow(scrollView, popupWidth, popupHeight, true);
        mainFilterPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mainFilterPopup.setOutsideTouchable(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mainFilterPopup.setElevation(dp(12));
        }
        mainFilterPopup.setOnDismissListener(() -> {
            mainFilterPopup = null;
            suppressMainFilterDismissRender = false;
        });
        content.setAlpha(0f);
        content.setTranslationY(-dp(8));
        int[] anchorLocation = new int[2];
        anchor.getLocationOnScreen(anchorLocation);
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int edgePadding = dp(18);
        int xOffset = 0;
        if (anchorLocation[0] + popupWidth > screenWidth - edgePadding) {
            xOffset = screenWidth - edgePadding - (anchorLocation[0] + popupWidth);
        }
        if (anchorLocation[0] + xOffset < edgePadding) {
            xOffset = edgePadding - anchorLocation[0];
        }
        mainFilterPopup.showAsDropDown(anchor, xOffset, dp(8));
        content.animate().alpha(1f).translationY(0f).setDuration(160).start();
    }

    private void addFilterDropdown(LinearLayout parent, String title, LinearLayout[] opened, FilterOptionsBuilder builder) {
        addFilterRootRow(parent, title, null);
        TextView header = (TextView) parent.getChildAt(parent.getChildCount() - 1);
        LinearLayout options = new LinearLayout(this);
        options.setOrientation(LinearLayout.VERTICAL);
        options.setVisibility(View.GONE);
        options.setPadding(dp(8), dp(6), dp(8), dp(2));
        builder.build(options);
        parent.addView(options, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        header.setOnClickListener(v -> {
            if (opened[0] != null && opened[0] != options) {
                opened[0].setVisibility(View.GONE);
            }
            boolean willOpen = options.getVisibility() != View.VISIBLE;
            options.setVisibility(willOpen ? View.VISIBLE : View.GONE);
            opened[0] = willOpen ? options : null;
        });
    }

    private void addFilterRootRow(LinearLayout parent, String text, DrawerAction action) {
        TextView row = new TextView(this);
        row.setText(text);
        row.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        row.setTextSize(15f);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setTypeface(row.getTypeface(), android.graphics.Typeface.BOLD);
        row.setBackgroundResource(R.drawable.bg_drawer_child_item);
        row.setMinHeight(dp(46));
        row.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, parent.getChildCount() == 0 ? 0 : dp(8), 0, 0);
        parent.addView(row, params);
        if (action != null) {
            row.setOnClickListener(v -> action.run());
        }
    }

    private void addFilterOptionRow(LinearLayout parent, String text, boolean active, DrawerAction action) {
        TextView row = new TextView(this);
        row.setText(text);
        row.setTextColor(ContextCompat.getColor(this, active ? R.color.on_primary : R.color.text_primary));
        row.setTextSize(13f);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setTypeface(row.getTypeface(), android.graphics.Typeface.BOLD);
        row.setBackgroundResource(active ? R.drawable.bg_drawer_section : R.drawable.bg_drawer_child_item);
        row.setMinHeight(dp(40));
        row.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, parent.getChildCount() == 0 ? 0 : dp(6), 0, 0);
        parent.addView(row, params);
        if (action != null) {
            row.setOnClickListener(v -> action.run());
        } else {
            row.setAlpha(0.55f);
        }
    }

    private void toggleMainFilterPopup(boolean categories, View anchor) {
        boolean shouldOpen = categories ? !mainCategoriesExpanded : !mainTagsExpanded;
        dismissMainFilterPopup(true);
        mainCategoriesExpanded = categories && shouldOpen;
        mainTagsExpanded = !categories && shouldOpen;
        updateAdvancedFilterButtons();
        if (shouldOpen) {
            anchor.post(() -> showMainFilterPopup(categories, anchor));
        } else {
            updateAdvancedFilterButtons();
        }
    }

    private void showMainFilterPopup(boolean categories, View anchor) {
        if (anchor == null || !anchor.isAttachedToWindow()) {
            return;
        }
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundResource(R.drawable.bg_dialog_panel);
        content.setPadding(dp(10), dp(10), dp(10), dp(10));
        if (categories) {
            addMainPopupRow(content, "🗂️ Sin filtro", activeProjectId == null,
                    () -> applyProjectFilter(null));
            if (drawerProjects != null) {
                for (ProjectEntity project : drawerProjects) {
                    addMainPopupRow(content, "✨ " + project.name, activeProjectId != null && activeProjectId == project.id,
                            () -> applyProjectFilter(project.id));
                }
            }
        } else {
            addMainPopupRow(content, "🏷️ Sin filtro", activeTagId == null,
                    () -> applyTagFilter(null));
            if (drawerTags != null) {
                for (TagEntity tag : drawerTags) {
                    addMainPopupRow(content, "🏷️ " + tag.name, activeTagId != null && activeTagId == tag.id,
                            () -> applyTagFilter(tag.id));
                }
            }
        }
        int popupWidth = Math.min(getResources().getDisplayMetrics().widthPixels - dp(36), dp(380));
        mainFilterPopup = new PopupWindow(content, popupWidth, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mainFilterPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mainFilterPopup.setOutsideTouchable(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mainFilterPopup.setElevation(dp(12));
        }
        mainFilterPopup.setOnDismissListener(() -> {
            mainFilterPopup = null;
            if (suppressMainFilterDismissRender) {
                suppressMainFilterDismissRender = false;
                return;
            }
            if (mainCategoriesExpanded || mainTagsExpanded) {
                mainCategoriesExpanded = false;
                mainTagsExpanded = false;
                renderProjectChips(drawerProjects);
            }
        });
        content.setAlpha(0f);
        content.setTranslationY(-dp(8));
        int[] anchorLocation = new int[2];
        anchor.getLocationOnScreen(anchorLocation);
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int edgePadding = dp(18);
        int xOffset = 0;
        if (anchorLocation[0] + popupWidth > screenWidth - edgePadding) {
            xOffset = screenWidth - edgePadding - (anchorLocation[0] + popupWidth);
        }
        if (anchorLocation[0] + xOffset < edgePadding) {
            xOffset = edgePadding - anchorLocation[0];
        }
        mainFilterPopup.showAsDropDown(anchor, xOffset, dp(6));
        content.animate().alpha(1f).translationY(0f).setDuration(160).start();
    }

    private void addMainPopupRow(LinearLayout parent, String text, boolean active, DrawerAction action) {
        TextView row = new TextView(this);
        row.setText(text);
        row.setTextColor(ContextCompat.getColor(this, active ? R.color.on_primary : R.color.text_primary));
        row.setTextSize(14f);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setTypeface(row.getTypeface(), android.graphics.Typeface.BOLD);
        row.setBackgroundResource(active ? R.drawable.bg_drawer_section : R.drawable.bg_drawer_child_item);
        row.setMinHeight(dp(46));
        int padH = dp(14);
        int padV = dp(10);
        row.setPadding(padH, padV, padH, padV);
        row.setOnClickListener(v -> {
            if (action != null) {
                action.run();
            }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, parent.getChildCount() == 0 ? 0 : dp(8), 0, 0);
        parent.addView(row, params);
    }

    private void dismissMainFilterPopup() {
        dismissMainFilterPopup(false);
    }

    private void dismissMainFilterPopup(boolean suppressRender) {
        if (mainFilterPopup != null && mainFilterPopup.isShowing()) {
            suppressMainFilterDismissRender = suppressRender;
            mainFilterPopup.dismiss();
        } else {
            mainFilterPopup = null;
            suppressMainFilterDismissRender = false;
        }
    }

    private void renderDrawerSections() {
        if (drawerStaticContainer == null) {
            return;
        }
        drawerStaticContainer.removeAllViews();
        addDrawerRow(drawerStaticContainer, "✅ Todas", () -> applyDrawerFilter(Constants.FILTER_ALL));
        addDrawerRow(drawerStaticContainer, "📅 Hoy", () -> applyDrawerFilter(Constants.FILTER_TODAY));
        addDrawerRow(drawerStaticContainer, "🗓️ Proximas", () -> applyDrawerFilter(Constants.FILTER_UPCOMING));
        addDrawerRow(drawerStaticContainer, "⭐ Favoritas", () -> applyDrawerFilter(Constants.FILTER_STARRED));
        addDrawerRow(drawerStaticContainer, "🔥 Vencidas", () -> applyDrawerFilter(Constants.FILTER_OVERDUE));
        addDrawerRow(drawerStaticContainer, "⏳ Pendientes", () -> applyDrawerFilter(Constants.FILTER_PENDING));
        addDrawerRow(drawerStaticContainer, "🎉 Completadas", () -> applyDrawerFilter(Constants.FILTER_COMPLETED));
        addDrawerRow(drawerStaticContainer, "🗓️ Sin fecha", () -> applyDrawerFilter(Constants.FILTER_NO_DATE));
        addDrawerRow(drawerStaticContainer, "📦 Archivadas", () -> applyDrawerFilter(Constants.FILTER_ARCHIVED));
        addDrawerRow(drawerStaticContainer, "🗑️ Papeleria", () -> startActivity(new Intent(this, TrashActivity.class)));
        addDrawerRow(drawerStaticContainer, "🧩 Tablero", () -> startActivity(new Intent(this, BoardActivity.class)));
        addDrawerRow(drawerStaticContainer, "📈 Avance", () -> startActivity(new Intent(this, ProductivityActivity.class)));
        drawerCategoriesHeader.setText(countLabelText("🗂️ ", categoryCount(), "Categoria", "Categorias", categoriesExpanded ? "  ▾" : "  ▸"));
        drawerTagsHeader.setText(countLabelText("🏷️ ", tagCount(), "Etiqueta", "Etiquetas", tagsExpanded ? "  ▾" : "  ▸"));
        drawerAccountHeader.setText(accountExpanded ? "👤 Cuenta  ▾" : "👤 Cuenta  ▸");
        renderDrawerProjects();
        renderDrawerTags();
        renderDrawerAccount();
    }

    private void renderDrawerProjects() {
        drawerCategoriesContainer.removeAllViews();
        drawerCategoriesContainer.setVisibility(categoriesExpanded ? View.VISIBLE : View.GONE);
        if (!categoriesExpanded) {
            return;
        }
        if (drawerProjects == null || drawerProjects.isEmpty()) {
            addDrawerRow(drawerCategoriesContainer, "Sin categorias", null);
        } else {
            for (ProjectEntity project : drawerProjects) {
                addDrawerRow(drawerCategoriesContainer, "✨ " + project.name, () -> {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    applyProjectFilter(project.id);
                });
            }
        }
        addDrawerRow(drawerCategoriesContainer, "➕ Crear o editar", () -> startActivity(new Intent(this, ProjectActivity.class)));
    }

    private void renderDrawerTags() {
        drawerTagsContainer.removeAllViews();
        drawerTagsContainer.setVisibility(tagsExpanded ? View.VISIBLE : View.GONE);
        if (!tagsExpanded) {
            return;
        }
        if (drawerTags == null || drawerTags.isEmpty()) {
            addDrawerRow(drawerTagsContainer, "Sin etiquetas", null);
        } else {
            for (TagEntity tag : drawerTags) {
                addDrawerRow(drawerTagsContainer, "🏷️ " + tag.name, () -> {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    applyTagFilter(tag.id);
                });
            }
        }
        addDrawerRow(drawerTagsContainer, "➕ Crear o editar", () -> startActivity(new Intent(this, ProjectActivity.class)));
    }

    private void renderDrawerAccount() {
        drawerAccountContainer.removeAllViews();
        drawerAccountContainer.setVisibility(accountExpanded ? View.VISIBLE : View.GONE);
        if (!accountExpanded) {
            return;
        }
        addDrawerRow(drawerAccountContainer, "⚙️ Ajustes", () -> startActivity(new Intent(this, ProfileActivity.class)));
        addDrawerRow(drawerAccountContainer, "🚪 Cerrar sesion", () -> {
            sessionManager.clearSession();
            openLogin();
        });
    }

    private void addDrawerRow(LinearLayout target, String title, DrawerAction action) {
        TextView row = new TextView(this);
        row.setText(title);
        row.setTextColor(ContextCompat.getColor(this, action == null ? R.color.text_secondary : R.color.text_primary));
        row.setTextSize(target == drawerStaticContainer ? 15f : 14f);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setTypeface(row.getTypeface(), android.graphics.Typeface.BOLD);
        row.setBackgroundResource(target == drawerStaticContainer ? R.drawable.bg_drawer_item : R.drawable.bg_drawer_child_item);
        row.setMinHeight(dp(48));
        int padH = dp(target == drawerStaticContainer ? 15 : 13);
        int padV = dp(11);
        row.setPadding(padH, padV, padH, padV);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(target == drawerStaticContainer ? 9 : 7));
        target.addView(row, params);
        row.setEnabled(action != null);
        if (action != null) {
            row.setOnClickListener(v -> action.run());
        } else {
            row.setAlpha(0.55f);
        }
    }

    private void applyDrawerFilter(int filter) {
        drawerLayout.closeDrawer(GravityCompat.START);
        applyFilter(filter);
    }

    private void updateHomeCounters(List<TaskFull> tasks) {
        int today = 0;
        int pending = 0;
        if (tasks != null) {
            for (TaskFull task : tasks) {
                if (task == null || task.task == null) {
                    continue;
                }
                if (!task.task.isCompleted) {
                    pending++;
                }
                if (task.task.dueDate != null
                        && task.task.dueDate >= com.taskflow.utils.DateUtils.startOfToday()
                        && task.task.dueDate <= com.taskflow.utils.DateUtils.endOfToday()) {
                    today++;
                }
            }
        }
        textTodayCounter.setText("📅 Hoy\n" + today);
        textPendingCounter.setText("⏳ Pendientes\n" + pending);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void requestStartupRuntimePermissions() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), 40);
        }
    }

    private void showFirstRunPermissionGuideIfNeeded() {
        if (!hasMissingSystemPermission()) {
            return;
        }
        drawerLayout.postDelayed(this::showPermissionSettings, 650L);
    }

    private void showPermissionSettings() {
        if (permissionDialog != null && permissionDialog.isShowing()) {
            permissionDialog.dismiss();
        }
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(8), dp(8), dp(8), dp(4));
        addPermissionSwitch(content, "⏰ Alarmas", PermissionPrefs.isAlarmsEnabled(this), hasNotificationSystemAccess(),
                this::enableAlarms, () -> disableAppPermission("Alarmas", () -> PermissionPrefs.setAlarmsEnabled(this, false)));
        addPermissionSwitch(content, "🎯 Hora exacta", PermissionPrefs.isExactEnabled(this), hasExactAlarmAccess(),
                this::enableExactAlarms, () -> disableAppPermission("Hora exacta", () -> PermissionPrefs.setExactEnabled(this, false)));
        addPermissionSwitch(content, "📲 Pantalla completa", PermissionPrefs.isFullscreenEnabled(this), hasFullScreenIntentAccess(),
                this::enableFullScreenAlerts, () -> disableAppPermission("Pantalla completa", () -> PermissionPrefs.setFullscreenEnabled(this, false)));
        addPermissionSwitch(content, "🔋 Segundo plano", PermissionPrefs.isBackgroundEnabled(this), hasBatteryAccess(),
                this::enableBackgroundAccess, () -> disableAppPermission("Segundo plano", () -> PermissionPrefs.setBackgroundEnabled(this, false)));
        addPermissionSwitch(content, "📳 Vibracion", PermissionPrefs.isVibrationEnabled(this), true,
                () -> {
                    PermissionPrefs.setVibrationEnabled(this, true);
                    showPermissionSettings();
                },
                () -> disableAppPermission("Vibracion", () -> PermissionPrefs.setVibrationEnabled(this, false)));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Permisos de TaskFlow")
                .setView(content)
                .setPositiveButton("Cerrar", null)
                .create();
        permissionDialog = dialog;
        dialog.setOnDismissListener(d -> {
            if (permissionDialog == dialog) {
                permissionDialog = null;
            }
        });
        permissionDialog.show();
    }

    private void addPermissionSwitch(LinearLayout parent, String label, boolean appEnabled, boolean systemReady,
                                     DrawerAction enableAction, DrawerAction disableAction) {
        boolean enabled = appEnabled && systemReady;
        SwitchMaterial permissionSwitch = new SwitchMaterial(this);
        String status = enabled ? "Activo" : (!appEnabled ? "Desactivado en TaskFlow" : "Falta permiso del sistema");
        permissionSwitch.setText(label + "\n" + status);
        permissionSwitch.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        permissionSwitch.setTextSize(15f);
        permissionSwitch.setChecked(enabled);
        permissionSwitch.setPadding(dp(10), dp(8), dp(10), dp(8));
        ColorStateList thumb = new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{ContextCompat.getColor(this, R.color.success), ContextCompat.getColor(this, R.color.text_secondary)}
        );
        ColorStateList track = new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{0x5522C55E, 0x226F6780}
        );
        permissionSwitch.setThumbTintList(thumb);
        permissionSwitch.setTrackTintList(track);
        permissionSwitch.setOnClickListener(v -> {
            permissionSwitch.setChecked(enabled);
            if (enabled) {
                if (disableAction != null) {
                    disableAction.run();
                }
            } else if (enableAction != null) {
                enableAction.run();
            }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, parent.getChildCount() == 0 ? 0 : dp(8), 0, 0);
        parent.addView(permissionSwitch, params);
    }

    private boolean hasMissingSystemPermission() {
        return (PermissionPrefs.isAlarmsEnabled(this) && !hasNotificationSystemAccess())
                || (PermissionPrefs.isExactEnabled(this) && !hasExactAlarmAccess())
                || (PermissionPrefs.isFullscreenEnabled(this) && !hasFullScreenIntentAccess())
                || (PermissionPrefs.isBackgroundEnabled(this) && !hasBatteryAccess());
    }

    private boolean hasNotificationSystemAccess() {
        return (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                && NotificationManagerCompat.from(this).areNotificationsEnabled();
    }

    private void enableAlarms() {
        PermissionPrefs.setAlarmsEnabled(this, true);
        ReminderScheduler.ensureChannel(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            refreshPermissionDialogOnResume = true;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 40);
            return;
        }
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            openPermissionSystemSettings(notificationSettingsIntent());
            return;
        }
        showPermissionSettings();
    }

    private void enableExactAlarms() {
        PermissionPrefs.setExactEnabled(this, true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasExactAlarmAccess()) {
            openPermissionSystemSettings(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri()));
            return;
        }
        showPermissionSettings();
    }

    private void enableFullScreenAlerts() {
        PermissionPrefs.setFullscreenEnabled(this, true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !hasFullScreenIntentAccess()) {
            openPermissionSystemSettings(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri()));
            return;
        }
        showPermissionSettings();
    }

    private void enableBackgroundAccess() {
        PermissionPrefs.setBackgroundEnabled(this, true);
        if (!hasBatteryAccess()) {
            openPermissionSystemSettings(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri()));
            return;
        }
        showPermissionSettings();
    }

    private void disableAppPermission(String label, DrawerAction action) {
        new AlertDialog.Builder(this)
                .setTitle("Desactivar " + label)
                .setMessage("Se desactivara dentro de TaskFlow sin salir de la app. Puedes volver a activarlo cuando quieras.")
                .setPositiveButton("Aceptar", (dialog, which) -> {
                    if (action != null) {
                        action.run();
                    }
                    showPermissionSettings();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> showPermissionSettings())
                .show();
    }

    private boolean hasExactAlarmAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        return alarmManager == null || alarmManager.canScheduleExactAlarms();
    }

    private boolean hasFullScreenIntentAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return true;
        }
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        return notificationManager == null || notificationManager.canUseFullScreenIntent();
    }

    private boolean hasBatteryAccess() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        return powerManager == null || powerManager.isIgnoringBatteryOptimizations(getPackageName());
    }

    private void openSystemSettings(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            startActivity(appSettingsIntent());
        }
    }

    private void openPermissionSystemSettings(Intent intent) {
        refreshPermissionDialogOnResume = true;
        openSystemSettings(intent);
    }

    private Intent notificationSettingsIntent() {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        return intent;
    }

    private Intent appSettingsIntent() {
        return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri());
    }

    private Uri packageUri() {
        return Uri.parse("package:" + getPackageName());
    }

    private void applyFilter(int filter) {
        clearSearch();
        activeFilter = filter;
        mainCategoriesExpanded = false;
        mainTagsExpanded = false;
        dismissMainFilterPopup();
        updateAdvancedFilterButtons();
        updateStaticFilterSelection();
        mainViewModel.setFilter(filter);
    }

    private void applyProjectFilter(Long projectId) {
        clearSearch();
        activeProjectId = projectId;
        mainCategoriesExpanded = false;
        mainTagsExpanded = false;
        dismissMainFilterPopup();
        updateAdvancedFilterButtons();
        mainViewModel.setProjectFilter(projectId);
    }

    private void applyTagFilter(Long tagId) {
        clearSearch();
        activeTagId = tagId;
        mainCategoriesExpanded = false;
        mainTagsExpanded = false;
        dismissMainFilterPopup();
        updateAdvancedFilterButtons();
        mainViewModel.setTagFilter(tagId);
    }

    private void showPriorityPopup(View anchor) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundResource(R.drawable.bg_dialog_panel);
        content.setPadding(dp(10), dp(10), dp(10), dp(10));
        addMainPopupRow(content, "Sin filtro", activePriority == null, () -> applyPriorityFilter(null));
        addMainPopupRow(content, "Baja", activePriority != null && activePriority == Constants.PRIORITY_LOW, () -> applyPriorityFilter(Constants.PRIORITY_LOW));
        addMainPopupRow(content, "Media", activePriority != null && activePriority == Constants.PRIORITY_MEDIUM, () -> applyPriorityFilter(Constants.PRIORITY_MEDIUM));
        addMainPopupRow(content, "Alta", activePriority != null && activePriority == Constants.PRIORITY_HIGH, () -> applyPriorityFilter(Constants.PRIORITY_HIGH));
        addMainPopupRow(content, "Urgente", activePriority != null && activePriority == Constants.PRIORITY_URGENT, () -> applyPriorityFilter(Constants.PRIORITY_URGENT));
        int popupWidth = Math.min(getResources().getDisplayMetrics().widthPixels - dp(36), dp(320));
        PopupWindow popup = new PopupWindow(content, popupWidth, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mainFilterPopup = popup;
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popup.setElevation(dp(12));
        }
        content.setAlpha(0f);
        content.setTranslationY(-dp(8));
        popup.showAsDropDown(anchor, 0, dp(6));
        content.animate().alpha(1f).translationY(0f).setDuration(160).start();
    }

    private void applyPriorityFilter(Integer priority) {
        activePriority = priority;
        updateAdvancedFilterButtons();
        mainViewModel.setPriorityFilter(priority);
        dismissMainFilterPopup();
    }

    private void updateAdvancedFilterButtons() {
        if (buttonFilterMenu == null) {
            return;
        }
        buttonFilterMenu.setText("Filtros");
    }

    private void clearAdvancedFilters() {
        activeProjectId = null;
        activeTagId = null;
        activePriority = null;
        mainCategoriesExpanded = false;
        mainTagsExpanded = false;
        dismissMainFilterPopup();
        updateAdvancedFilterButtons();
        mainViewModel.clearAdvancedFilters();
    }

    private String priorityLabel(int priority) {
        if (priority >= Constants.PRIORITY_URGENT) {
            return "Urgente";
        }
        if (priority >= Constants.PRIORITY_HIGH) {
            return "Alta";
        }
        if (priority >= Constants.PRIORITY_MEDIUM) {
            return "Media";
        }
        return "Baja";
    }

    private void updateStaticFilterSelection() {
        if (chipGroupFilters == null) {
            return;
        }
        int checkedId;
        switch (activeFilter) {
            case Constants.FILTER_TODAY:
                checkedId = R.id.chipToday;
                break;
            case Constants.FILTER_STARRED:
                checkedId = R.id.chipStarred;
                break;
            case Constants.FILTER_UPCOMING:
                checkedId = R.id.chipUpcoming;
                break;
            case Constants.FILTER_OVERDUE:
                checkedId = R.id.chipOverdue;
                break;
            case Constants.FILTER_COMPLETED:
                checkedId = R.id.chipCompleted;
                break;
            case Constants.FILTER_PENDING:
                checkedId = R.id.chipPending;
                break;
            case Constants.FILTER_NO_DATE:
                checkedId = R.id.chipNoDate;
                break;
            case Constants.FILTER_ARCHIVED:
                checkedId = R.id.chipArchived;
                break;
            case Constants.FILTER_TRASH:
                checkedId = R.id.chipTrash;
                break;
            case Constants.FILTER_ALL:
            default:
                checkedId = R.id.chipAll;
                break;
        }
        chipGroupFilters.check(checkedId);
    }

    private void clearSearch() {
        if (editSearch != null && editSearch.getText() != null && editSearch.getText().length() > 0) {
            editSearch.setText("");
        }
        collapseSearch();
    }

    @Override
    public void onTaskClicked(TaskFull task) {
        Intent intent = new Intent(this, TaskDetailActivity.class);
        intent.putExtra(Constants.EXTRA_TASK_ID, task.task.id);
        startActivity(intent);
    }

    @Override
    public void onTaskLongPressed(TaskFull task) {
        if (task == null || task.task == null) {
            return;
        }
        if (task.task.isDeleted) {
            String[] actions = {"Restaurar", "Eliminar definitivamente"};
            new AlertDialog.Builder(this)
                    .setTitle(task.task.title)
                    .setItems(actions, (dialog, which) -> {
                        if (which == 0) {
                            taskViewModel.restoreTask(task.task.id);
                            applyFilter(Constants.FILTER_ALL);
                            Toast.makeText(this, "Tarea restaurada.", Toast.LENGTH_SHORT).show();
                        } else {
                            taskViewModel.deleteTaskPermanently(task.task.id);
                            Toast.makeText(this, "Tarea eliminada definitivamente.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .show();
            return;
        }
        if (task.task.isArchived) {
            String[] actions = {"Ver detalle", "Desarchivar", "Duplicar", "Mover a papelera"};
            new AlertDialog.Builder(this)
                    .setTitle(task.task.title)
                    .setItems(actions, (dialog, which) -> {
                        if (which == 0) {
                            onTaskClicked(task);
                        } else if (which == 1) {
                            taskViewModel.archiveTask(task.task.id, false);
                            applyFilter(Constants.FILTER_ALL);
                            Toast.makeText(this, "Tarea desarchivada.", Toast.LENGTH_SHORT).show();
                        } else if (which == 2) {
                            taskViewModel.duplicateTask(task.task.id);
                            Toast.makeText(this, "Tarea duplicada.", Toast.LENGTH_SHORT).show();
                        } else {
                            taskViewModel.deleteTask(task.task.id);
                            startActivity(new Intent(this, TrashActivity.class));
                            Toast.makeText(this, "Tarea enviada a papelera.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .show();
            return;
        }
        String completeLabel = task.task.isCompleted ? "Reabrir" : "Completar";
        String[] actions = {"Ver detalle", completeLabel, "Editar", "Temporizador", "Duplicar", "Archivar", "Mover a papelera"};
        new AlertDialog.Builder(this)
                .setTitle(task.task.title)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        onTaskClicked(task);
                    } else if (which == 1) {
                        onTaskCompleted(task, !task.task.isCompleted);
                    } else if (which == 2) {
                        openTaskForm(task.task.id);
                    } else if (which == 3) {
                        openFocus(task.task.id);
                    } else if (which == 4) {
                        taskViewModel.duplicateTask(task.task.id);
                        Toast.makeText(this, "Tarea duplicada.", Toast.LENGTH_SHORT).show();
                    } else if (which == 5) {
                        taskViewModel.archiveTask(task.task.id, true);
                        applyFilter(Constants.FILTER_ARCHIVED);
                        Toast.makeText(this, "Tarea archivada.", Toast.LENGTH_SHORT).show();
                    } else {
                        taskViewModel.deleteTask(task.task.id);
                        startActivity(new Intent(this, TrashActivity.class));
                        Toast.makeText(this, "Tarea enviada a papelera.", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void openTaskForm(long taskId) {
        Intent intent = new Intent(this, com.taskflow.ui.task.TaskFormActivity.class);
        intent.putExtra(Constants.EXTRA_TASK_ID, taskId);
        startActivity(intent);
    }

    private void openFocus(long taskId) {
        Intent intent = new Intent(this, com.taskflow.ui.task.FocusActivity.class);
        intent.putExtra(Constants.EXTRA_TASK_ID, taskId);
        startActivity(intent);
    }

    @Override
    public void onTaskCompleted(TaskFull task, boolean completed) {
        taskViewModel.toggleCompleted(task.task.id, completed);
        if (completed) {
            playCelebration("Completada 🎉", CelebrationView.completePalette());
        } else {
            playCelebration("Reabierta 🔁", CelebrationView.reopenPalette());
        }
    }

    @Override
    public void onTaskMoved(TaskFull task, int adapterPosition) {
        if (task != null && task.task != null) {
            taskViewModel.moveTask(task.task.id, System.currentTimeMillis() + adapterPosition);
        }
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void playCelebration(String message, CelebrationView.Palette palette) {
        CelebrationView view = new CelebrationView(this);
        addContentView(view, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        view.bringToFront();
        view.play(message, palette);
    }

    private interface DrawerAction {
        void run();
    }

    private interface FilterOptionsBuilder {
        void build(LinearLayout target);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 40 && grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Las tareas se guardan aunque el aviso quede sin notificacion.", Toast.LENGTH_LONG).show();
        }
        if (requestCode == 40 && permissionDialog != null) {
            drawerLayout.postDelayed(this::showPermissionSettings, 180L);
        }
    }
}
