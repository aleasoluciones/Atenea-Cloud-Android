package com.ateneacloud.drive.sync.settings.database;

import android.net.Uri;

import androidx.room.TypeConverter;

import com.ateneacloud.drive.sync.enums.SeafSyncMode;
import com.ateneacloud.drive.sync.enums.SeafSyncNetwork;
import com.ateneacloud.drive.sync.enums.SeafSyncStatus;
import com.ateneacloud.drive.sync.enums.SeafSyncType;

import java.util.Date;

public class SeafSyncSettingsConverter {

    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static Enum<SeafSyncNetwork> networkEnumFromString(String value) {
        return SeafSyncNetwork.valueOf(value);
    }

    @TypeConverter
    public static String networkEnumToString(Enum<SeafSyncNetwork> type) {
        return type.name();
    }

    @TypeConverter
    public static Enum<SeafSyncMode> modeEnumFromString(String value) {
        return SeafSyncMode.valueOf(value);
    }

    @TypeConverter
    public static String modeEnumToString(Enum<SeafSyncMode> type) {
        return type.name();
    }

    @TypeConverter
    public static Enum<SeafSyncType> typeEnumFromString(String value) {
        return SeafSyncType.valueOf(value);
    }

    @TypeConverter
    public static String typeEnumToString(Enum<SeafSyncType> type) {
        return type.name();
    }

    @TypeConverter
    public static Enum<SeafSyncStatus> statusEnumFromString(String value) {
        return SeafSyncStatus.valueOf(value);
    }

    @TypeConverter
    public static String statusEnumToString(Enum<SeafSyncStatus> type) {
        return type.name();
    }

    @TypeConverter
    public static Uri uriFromString(String value) {
        return value == null ? null : Uri.parse(value);
    }

    @TypeConverter
    public static String uriToString(Uri uri) {
        return uri == null ? null : uri.toString();
    }

}
