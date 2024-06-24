package com.ateneacloud.drive.sync.tree.treeFolderState.database;

import android.net.Uri;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.ateneacloud.drive.sync.tree.treeFolderState.SeafSyncTreeState;
import java.util.List;

@Dao
public interface SeafSyncTreeFolderStateDao {

    @Query("SELECT * FROM seafsynctreestate")
    public List<SeafSyncTreeState> all(); 

    @Insert
    public void insert(SeafSyncTreeState state);

    @Update
    public void update(SeafSyncTreeState state);

    @Delete
    public void remove(SeafSyncTreeState state);

    @Query("SELECT * FROM seafsynctreestate WHERE identifier = :identifier") // Cambia "some_column" y ":someValue" según tus criterios de búsqueda
    List<SeafSyncTreeState> find(String identifier);

    @Query("SELECT * FROM seafsynctreestate WHERE syncSettingId = :syncSettingId")
    List<SeafSyncTreeState> findBySyncSetting(String syncSettingId);

    @Query("SELECT * FROM seafsynctreestate WHERE uri = :uri")
    List<SeafSyncTreeState> findByUri(Uri uri);

    @Query("DELETE FROM seafsynctreestate")
    void clear();
}
