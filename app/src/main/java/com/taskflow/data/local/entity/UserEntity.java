package com.taskflow.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "users",
        indices = {
                @Index(value = "email", unique = true),
                @Index(value = "username", unique = true)
        }
)
public class UserEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name = "";

    @NonNull
    public String email = "";

    @NonNull
    @ColumnInfo(defaultValue = "")
    public String username = "";

    @NonNull
    public String passwordHash = "";

    public long createdAt;

    public UserEntity() {
    }

    @Ignore
    public UserEntity(@NonNull String name, @NonNull String email, @NonNull String username, @NonNull String passwordHash, long createdAt) {
        this.name = name;
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }
}
