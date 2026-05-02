package com.taskflow.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.taskflow.data.local.entity.TagEntity;

import java.util.List;

@Dao
public interface TagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertTag(TagEntity tag);

    @Update
    void updateTag(TagEntity tag);

    @Delete
    void deleteTag(TagEntity tag);

    @Query("SELECT * FROM tags WHERE userId = :userId ORDER BY name ASC")
    LiveData<List<TagEntity>> getTagsByUser(long userId);

    @Query("SELECT * FROM tags WHERE userId = :userId ORDER BY name ASC")
    List<TagEntity> getTagsByUserSync(long userId);

    @Query("SELECT * FROM tags WHERE name = :name AND userId = :userId LIMIT 1")
    TagEntity getTagByName(String name, long userId);
}
