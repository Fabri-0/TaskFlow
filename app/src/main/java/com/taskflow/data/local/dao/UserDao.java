package com.taskflow.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.taskflow.data.local.entity.UserEntity;

@Dao
public interface UserDao {
    @Insert
    long insertUser(UserEntity user);

    @Update
    int updateUser(UserEntity user);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    UserEntity getUserByEmail(String email);

    @Query("SELECT * FROM users WHERE email = :identifier OR username = :identifier LIMIT 1")
    UserEntity getUserByEmailOrUsername(String identifier);

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    UserEntity getUserById(long id);

    @Query("SELECT COUNT(*) FROM users WHERE email = :email")
    int countByEmail(String email);

    @Query("SELECT COUNT(*) FROM users WHERE email = :email AND id != :userId")
    int countByEmailForOtherUser(String email, long userId);

    @Query("SELECT COUNT(*) FROM users WHERE username = :username")
    int countByUsername(String username);

    @Query("SELECT COUNT(*) FROM users WHERE username = :username AND id != :userId")
    int countByUsernameForOtherUser(String username, long userId);
}
