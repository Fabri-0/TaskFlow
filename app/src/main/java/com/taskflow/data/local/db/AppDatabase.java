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
import com.taskflow.data.local.dao.TaskTagDao;
import com.taskflow.data.local.dao.UserDao;
import com.taskflow.data.local.entity.ProjectEntity;
import com.taskflow.data.local.entity.SectionEntity;
import com.taskflow.data.local.entity.SubtaskEntity;
import com.taskflow.data.local.entity.TagEntity;
import com.taskflow.data.local.entity.TaskEntity;
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
                TaskTagCrossRef.class
        },
        version = 2,
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

    public abstract UserDao userDao();

    public abstract TaskDao taskDao();

    public abstract SubtaskDao subtaskDao();

    public abstract ProjectDao projectDao();

    public abstract SectionDao sectionDao();

    public abstract TagDao tagDao();

    public abstract TaskTagDao taskTagDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "taskflow.db"
                            )
                            .addMigrations(MIGRATION_1_2)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
