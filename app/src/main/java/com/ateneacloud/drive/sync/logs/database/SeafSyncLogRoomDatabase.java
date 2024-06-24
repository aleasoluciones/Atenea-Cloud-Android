package com.ateneacloud.drive.sync.logs.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.ateneacloud.drive.sync.logs.SeafSyncLog;

@Database(entities = {SeafSyncLog.class}, version = 1, exportSchema = false)
@TypeConverters({SeafSyncLogConverter.class})
public abstract class SeafSyncLogRoomDatabase extends RoomDatabase {

    public abstract SeafSyncLogDao seafSyncLogDao();

    private static SeafSyncLogRoomDatabase instance;

    public static SeafSyncLogRoomDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            SeafSyncLogRoomDatabase.class,
                            "SeafSyncLogDatabase")
                    .build();
        }
        return instance;
    }
}

