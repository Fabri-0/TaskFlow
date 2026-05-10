package com.taskflow.data.local.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.taskflow.data.local.dao.ProjectDao;
import com.taskflow.data.local.dao.SectionDao;
import com.taskflow.data.local.dao.SubtaskDao;
import com.taskflow.data.local.dao.TagDao;
import com.taskflow.data.local.dao.TaskDao;
import com.taskflow.data.local.dao.TaskTemplateDao;
import com.taskflow.data.local.dao.TaskTagDao;
import com.taskflow.data.local.dao.UserDao;
import com.taskflow.data.local.entity.ProjectEntity;
import com.taskflow.data.local.entity.SectionEntity;
import com.taskflow.data.local.entity.SubtaskEntity;
import com.taskflow.data.local.entity.TagEntity;
import com.taskflow.data.local.entity.TaskEntity;
import com.taskflow.data.local.entity.TaskTemplateEntity;
import com.taskflow.data.local.entity.TaskTagCrossRef;
import com.taskflow.data.local.entity.UserEntity;
import com.taskflow.utils.Converters;

@Database(
        entities = {
                UserEntity.class,
                TaskEntity.class,
                SubtaskEntity.class,
                ProjectEntity.class,
                SectionEntity.class,
                TagEntity.class,
                TaskTagCrossRef.class,
                TaskTemplateEntity.class
        },
        version = 4,
        exportSchema = false
)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE users ADD COLUMN username TEXT NOT NULL DEFAULT ''");
            database.execSQL("UPDATE users SET username = LOWER(REPLACE(REPLACE(REPLACE(SUBSTR(email, 1, CASE WHEN INSTR(email, '@') > 1 THEN INSTR(email, '@') - 1 ELSE LENGTH(email) END), '-', '_'), '+', '_'), ' ', '_')) || '_' || id WHERE username = ''");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_users_username ON users(username)");
        }
    };
    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE tasks ADD COLUMN reminderDate2 INTEGER");
            database.execSQL("ALTER TABLE tasks ADD COLUMN reminderDate3 INTEGER");
            database.execSQL("ALTER TABLE tasks ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE tasks ADD COLUMN recurrenceType TEXT NOT NULL DEFAULT ''");
            database.execSQL("ALTER TABLE tasks ADD COLUMN recurrenceInterval INTEGER NOT NULL DEFAULT 1");
            database.execSQL("ALTER TABLE tasks ADD COLUMN boardColumn TEXT NOT NULL DEFAULT 'TODO'");
            database.execSQL("ALTER TABLE tasks ADD COLUMN attachment TEXT NOT NULL DEFAULT ''");
            database.execSQL("ALTER TABLE tasks ADD COLUMN estimatedMinutes INTEGER NOT NULL DEFAULT 25");
            database.execSQL("ALTER TABLE tasks ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE tasks ADD COLUMN completedAt INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE tasks ADD COLUMN archivedAt INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE tasks ADD COLUMN lastFocusedAt INTEGER NOT NULL DEFAULT 0");
            database.execSQL("UPDATE tasks SET sortOrder = createdAt WHERE sortOrder = 0");
            database.execSQL("UPDATE tasks SET completedAt = updatedAt WHERE isCompleted = 1 AND completedAt = 0");
            database.execSQL("UPDATE tasks SET boardColumn = 'DONE' WHERE isCompleted = 1");
            database.execSQL("CREATE TABLE IF NOT EXISTS task_templates (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userId INTEGER NOT NULL, name TEXT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, projectName TEXT NOT NULL, tagsCsv TEXT NOT NULL, subtasksCsv TEXT NOT NULL, recurrenceType TEXT NOT NULL, boardColumn TEXT NOT NULL, attachment TEXT NOT NULL, priority INTEGER NOT NULL, estimatedMinutes INTEGER NOT NULL, createdAt INTEGER NOT NULL, FOREIGN KEY(userId) REFERENCES users(id) ON UPDATE NO ACTION ON DELETE CASCADE)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_task_templates_userId ON task_templates(userId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_isArchived ON tasks(isArchived)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_priority ON tasks(priority)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_boardColumn ON tasks(boardColumn)");
        }
    };
    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE tasks ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE tasks ADD COLUMN deletedAt INTEGER NOT NULL DEFAULT 0");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_isDeleted ON tasks(isDeleted)");
        }
    };

    public abstract UserDao userDao();

    public abstract TaskDao taskDao();

    public abstract SubtaskDao subtaskDao();

    public abstract ProjectDao projectDao();

    public abstract SectionDao sectionDao();

    public abstract TagDao tagDao();

    public abstract TaskTagDao taskTagDao();

    public abstract TaskTemplateDao taskTemplateDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "taskflow.db"
                            )
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                            .build();
                }
            }
        }
        return instance;
    }
}
