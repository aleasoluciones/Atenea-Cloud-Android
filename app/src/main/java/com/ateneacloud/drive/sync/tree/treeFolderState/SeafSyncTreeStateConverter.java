package com.ateneacloud.drive.sync.tree.treeFolderState;

import android.net.Uri;

import androidx.room.TypeConverter;

import com.ateneacloud.drive.sync.tree.SeafSyncTreeType;

public class SeafSyncTreeStateConverter {

    @TypeConverter
    public static Enum<SeafSyncTreeType> treeTypeEnumFromString(String value) {
        return SeafSyncTreeType.valueOf(value);
    }

    @TypeConverter
    public static String treeTypeEnumToString(Enum<SeafSyncTreeType> type) {
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
