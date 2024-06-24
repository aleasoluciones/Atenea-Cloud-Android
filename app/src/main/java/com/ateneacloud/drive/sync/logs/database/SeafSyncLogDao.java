package com.ateneacloud.drive.sync.logs.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.ateneacloud.drive.sync.enums.SeafSyncLogStatus;
import com.ateneacloud.drive.sync.logs.SeafSyncLog;

import java.util.List;

@Dao
public interface SeafSyncLogDao {

    @Query("SELECT * FROM seafsynclogs")
    public List<SeafSyncLog> all();

    @Insert
    public void insert(SeafSyncLog seafSyncLog);

    @Update
    public void update(SeafSyncLog seafSyncLog);

    @Delete
    public void remove(SeafSyncLog seafSyncLog);

    @Query("SELECT * FROM seafsynclogs WHERE accountId = :accountId and resourceId = :resourceId and repoId = :repoId and settingsId = :settingsId LIMIT 1")
    SeafSyncLog find(String accountId, String resourceId, String repoId, String settingsId);

    @Query("SELECT * FROM seafsynclogs WHERE accountId = :accountId and settingsId = :settingsId")
    List<SeafSyncLog> findBySettingsAndAccountId(String settingsId, String accountId);

    @Query("SELECT * FROM seafsynclogs WHERE accountId = :accountId and settingsId = :settingsId and state = :status")
    List<SeafSyncLog> findBySettingsAndAccountIdAndStatus(String settingsId, String accountId, Enum<SeafSyncLogStatus> status);

    @Query("DELETE FROM seafsynclogs")
    void clear();
}
