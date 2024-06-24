package com.ateneacloud.drive.sync.settings.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.ateneacloud.drive.sync.settings.SeafSyncSettings;

import java.util.List;

@Dao
public interface SeafSyncSettingsDao {

    @Query("SELECT * FROM seafsyncsettings")
    public List<SeafSyncSettings> all();

    @Insert
    public void insert(SeafSyncSettings settings);

    @Update
    public void update(SeafSyncSettings settings);

    @Delete
    public void remove(SeafSyncSettings settings);

    @Query("SELECT * FROM seafsyncsettings WHERE id = :id LIMIT 1")
    SeafSyncSettings find(String id);

    @Query("SELECT * FROM seafsyncsettings WHERE repoId = :repoId and repoPath = :repoPath and resourceUri = :resourceUri ")
    List<SeafSyncSettings> findByOriginAndDestination(String repoId, String repoPath, String resourceUri);

    @Query("DELETE FROM seafsyncsettings")
    void clear();
}
