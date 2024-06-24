package com.ateneacloud.drive.sync.settings.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.ateneacloud.drive.sync.settings.SeafSyncSettings;

@Database(entities = {SeafSyncSettings.class}, version = 1, exportSchema = false)
@TypeConverters({SeafSyncSettingsConverter.class})
public abstract class SeafSyncSettingsRoomDatabase extends RoomDatabase {

    public abstract SeafSyncSettingsDao seafSyncSettingsDao();

    private static SeafSyncSettingsRoomDatabase instance;

    public static SeafSyncSettingsRoomDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            SeafSyncSettingsRoomDatabase.class,
                            "SeafSyncSettingsDatabase")
                    .build();
        }
        return instance;
    }
}
