package com.taskflow.ui.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.taskflow.R;
import com.taskflow.data.local.entity.ProjectEntity;
import com.taskflow.data.local.entity.TagEntity;
import com.taskflow.data.local.relation.TaskFull;
import com.taskflow.session.SessionManager;
import com.taskflow.ui.auth.LoginActivity;
import com.taskflow.ui.calendar.CalendarActivity;
import com.taskflow.ui.profile.ProfileActivity;
import com.taskflow.ui.project.ProjectActivity;
import com.taskflow.ui.task.QuickTaskBottomSheet;
import com.taskflow.ui.task.TaskDetailActivity;
import com.taskflow.ui.task.TaskViewModel;
import com.taskflow.utils.Constants;

import java.util.HashMap;
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
    private TextInputEditText editSearch;
    private HorizontalScrollView filterScrollView;
    private ChipGroup chipGroupFilters;
    private Chip chipAll;
    private Chip chipToday;
    private Chip chipStarred;
    private Chip chipOverdue;
    private Chip chipCompleted;
    private Chip chipPending;
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
    private int activeFilter = Constants.FILTER_ALL;
    private Long activeProjectId;
    private Long activeTagId;
    private boolean categoriesExpanded;
    private boolean tagsExpanded;
    private boolean accountExpanded;

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
        requestNotificationPermissionIfNeeded();
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
        chipOverdue = findViewById(R.id.chipOverdue);
        chipCompleted = findViewById(R.id.chipCompleted);
        chipPending = findViewById(R.id.chipPending);
        chipAll.setOnClickListener(v -> applyFilter(Constants.FILTER_ALL));
        chipToday.setOnClickListener(v -> applyFilter(Constants.FILTER_TODAY));
        chipStarred.setOnClickListener(v -> applyFilter(Constants.FILTER_STARRED));
        chipOverdue.setOnClickListener(v -> applyFilter(Constants.FILTER_OVERDUE));
        chipCompleted.setOnClickListener(v -> applyFilter(Constants.FILTER_COMPLETED));
        chipPending.setOnClickListener(v -> applyFilter(Constants.FILTER_PENDING));
        updateStaticFilterSelection();
    }

    private void setupSearch() {
        editSearch = findViewById(R.id.editSearch);
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

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getBindingAdapterPosition();
                TaskFull task = adapter.getTaskAt(position);
                if (!adapter.isTaskRow(position) || task == null || task.task == null || task.task.isCompleted) {
                    return makeMovementFlags(0, 0);
                }
                return makeMovementFlags(0, ItemTouchHelper.RIGHT);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
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
            return true;
        });

        drawerStaticContainer = findViewById(R.id.drawerStaticContainer);
        drawerCategoriesContainer = findViewById(R.id.drawerCategoriesContainer);
        drawerTagsContainer = findViewById(R.id.drawerTagsContainer);
        drawerAccountContainer = findViewById(R.id.drawerAccountContainer);
        drawerCategoriesHeader = findViewById(R.id.drawerCategoriesHeader);
        drawerTagsHeader = findViewById(R.id.drawerTagsHeader);
        drawerAccountHeader = findViewById(R.id.drawerAccountHeader);
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
        int filterScrollX = filterScrollView == null ? 0 : filterScrollView.getScrollX();
        for (int i = chipGroupFilters.getChildCount() - 1; i >= 0; i--) {
            View child = chipGroupFilters.getChildAt(i);
            if ("filter_dynamic".equals(child.getTag())) {
                chipGroupFilters.removeViewAt(i);
            }
        }
        if (projects != null && !projects.isEmpty()) {
            Chip toggle = createFilterToggle(categoryToggleText(), R.color.accent_teal);
            toggle.setOnClickListener(v -> toggleMainFilterPopup(true, v));
            chipGroupFilters.addView(toggle);
        }
        if (drawerTags != null && !drawerTags.isEmpty()) {
            Chip toggle = createFilterToggle(tagToggleText(), R.color.accent_blue);
            toggle.setOnClickListener(v -> toggleMainFilterPopup(false, v));
            chipGroupFilters.addView(toggle);
        }
        if (filterScrollView != null) {
            filterScrollView.post(() -> filterScrollView.scrollTo(filterScrollX, 0));
        }
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

    private void toggleMainFilterPopup(boolean categories, View anchor) {
        boolean shouldOpen = categories ? !mainCategoriesExpanded : !mainTagsExpanded;
        dismissMainFilterPopup(true);
        mainCategoriesExpanded = categories && shouldOpen;
        mainTagsExpanded = !categories && shouldOpen;
        if (anchor instanceof Chip) {
            ((Chip) anchor).setText(categories ? categoryToggleText() : tagToggleText());
        }
        if (shouldOpen) {
            anchor.post(() -> showMainFilterPopup(categories, anchor));
        } else {
            renderProjectChips(drawerProjects);
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
            if (drawerProjects == null || drawerProjects.isEmpty()) {
                return;
            }
            for (ProjectEntity project : drawerProjects) {
                addMainPopupRow(content, "✨ " + project.name, activeProjectId != null && activeProjectId == project.id,
                        () -> applyProjectFilter(project.id));
            }
        } else {
            if (drawerTags == null || drawerTags.isEmpty()) {
                return;
            }
            for (TagEntity tag : drawerTags) {
                addMainPopupRow(content, "🏷️ " + tag.name, activeTagId != null && activeTagId == tag.id,
                        () -> applyTagFilter(tag.id));
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
        addDrawerRow(drawerStaticContainer, "⭐ Tareas estrella", () -> applyDrawerFilter(Constants.FILTER_STARRED));
        addDrawerRow(drawerStaticContainer, "🔥 Vencidas", () -> applyDrawerFilter(Constants.FILTER_OVERDUE));
        addDrawerRow(drawerStaticContainer, "⏳ Pendientes", () -> applyDrawerFilter(Constants.FILTER_PENDING));
        addDrawerRow(drawerStaticContainer, "🎉 Completadas", () -> applyDrawerFilter(Constants.FILTER_COMPLETED));
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

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 40);
        }
    }

    private void applyFilter(int filter) {
        clearSearch();
        activeFilter = filter;
        activeProjectId = null;
        activeTagId = null;
        mainCategoriesExpanded = false;
        mainTagsExpanded = false;
        dismissMainFilterPopup();
        renderProjectChips(drawerProjects);
        updateStaticFilterSelection();
        mainViewModel.setFilter(filter);
    }

    private void applyProjectFilter(long projectId) {
        clearSearch();
        activeFilter = -1;
        activeProjectId = projectId;
        activeTagId = null;
        mainCategoriesExpanded = false;
        mainTagsExpanded = false;
        dismissMainFilterPopup();
        renderProjectChips(drawerProjects);
        chipGroupFilters.clearCheck();
        mainViewModel.setProjectFilter(projectId);
    }

    private void applyTagFilter(long tagId) {
        clearSearch();
        activeFilter = -1;
        activeProjectId = null;
        activeTagId = tagId;
        mainCategoriesExpanded = false;
        mainTagsExpanded = false;
        dismissMainFilterPopup();
        renderProjectChips(drawerProjects);
        chipGroupFilters.clearCheck();
        mainViewModel.setTagFilter(tagId);
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
            case Constants.FILTER_OVERDUE:
                checkedId = R.id.chipOverdue;
                break;
            case Constants.FILTER_COMPLETED:
                checkedId = R.id.chipCompleted;
                break;
            case Constants.FILTER_PENDING:
                checkedId = R.id.chipPending;
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
    }

    @Override
    public void onTaskClicked(TaskFull task) {
        Intent intent = new Intent(this, TaskDetailActivity.class);
        intent.putExtra(Constants.EXTRA_TASK_ID, task.task.id);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 40 && grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Las tareas se guardan aunque el aviso quede sin notificacion.", Toast.LENGTH_LONG).show();
        }
    }
}
