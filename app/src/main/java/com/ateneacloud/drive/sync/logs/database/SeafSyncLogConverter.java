package com.ateneacloud.drive.sync.logs.database;

import androidx.room.TypeConverter;

import com.ateneacloud.drive.sync.enums.SeafSyncLogStatus;

import java.util.Date;

public class SeafSyncLogConverter {

    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static Enum<SeafSyncLogStatus> statusEnumFromString(String value) {
        return SeafSyncLogStatus.valueOf(value);
    }

    @TypeConverter
    public static String statusEnumToString(Enum<SeafSyncLogStatus> type) {
        return type.name();
    }

}
