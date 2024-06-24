package com.ateneacloud.drive.sync.tree.treeFolderState.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.ateneacloud.drive.sync.tree.treeFolderState.SeafSyncTreeState;
import com.ateneacloud.drive.sync.tree.treeFolderState.SeafSyncTreeStateConverter;

@Database(entities = {SeafSyncTreeState.class}, version = 1, exportSchema = false)
@TypeConverters({SeafSyncTreeStateConverter.class})
public abstract class SeafSyncTreeFolderStateRoomDatabase extends RoomDatabase {

    public abstract SeafSyncTreeFolderStateDao seafSyncTreeFolderStateDao();

    private static SeafSyncTreeFolderStateRoomDatabase instance;

    public static SeafSyncTreeFolderStateRoomDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            SeafSyncTreeFolderStateRoomDatabase.class,
                            "SeafSyncTreeFolderStateDatabase")
                    .build();
        }
        return instance;
    }
}
