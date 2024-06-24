package com.ateneacloud.drive.data;

import android.util.Log;

import com.ateneacloud.drive.R;
import com.ateneacloud.drive.SeadroidApplication;
import com.ateneacloud.drive.util.PinyinUtils;
import com.ateneacloud.drive.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

/**
 * Represents a deleted file or directory in the trash.
 */
public class SeafDirentTrash implements SeafItem, Serializable {

    /**
     * Serial version UID for serialization.
     */
    public static final long serialVersionUID = 0L;

    private static final String DEBUG_TAG = "SeafTrashDirent";

    /**
     * Enumeration representing the type of the deleted item (Directory or File).
     */
    public enum DirentType {DIR, FILE}

    public String id;
    public String commitID;
    public String path;
    public DirentType type;
    public String name;
    public boolean isRoot;
    public long size;
    public long deletedTime;

    /**
     * Creates a SeafDirentTrash object from a JSON object.
     *
     * @param obj The JSON object representing the deleted item.
     * @return A SeafDirentTrash object created from the JSON object.
     */
    static SeafDirentTrash fromJson(JSONObject obj) {
        SeafDirentTrash dirent = new SeafDirentTrash();
        try {
            dirent.id = obj.getString("commit_id");
            dirent.commitID = obj.getString("commit_id");
            dirent.name = obj.getString("obj_name");
            dirent.path = obj.getString("parent_dir");
            dirent.isRoot = true;
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
                Date delTime = dateFormat.parse(obj.getString("deleted_time"));
                dirent.deletedTime = delTime.getTime();
            } catch (Exception e) {
                e.printStackTrace();
            }
            boolean isDir = obj.getBoolean("is_dir");
            if (obj.get("size") instanceof String) {
                dirent.size = 0;
            } else {
                dirent.size = obj.getLong("size");
            }

            if (!isDir) {
                dirent.type = DirentType.FILE;
            } else {
                dirent.type = DirentType.DIR;
            }
            return dirent;
        } catch (JSONException e) {
            Log.d(DEBUG_TAG, e.getMessage());
            return null;
        }
    }

    static SeafDirentTrash fromJsonNavigation(JSONObject obj, String commitID) {
        SeafDirentTrash dirent = new SeafDirentTrash();
        try {
            dirent.id = obj.getString("obj_id");
            dirent.commitID = commitID;
            dirent.name = obj.getString("name");
            dirent.path = obj.getString("parent_dir");
            dirent.isRoot = false;
            boolean isDir = obj.getString("type").equals("file") ? false : true;

            if (obj.has("size")) {
                if (obj.get("size") instanceof String) {
                    dirent.size = 0;
                } else {
                    dirent.size = obj.getLong("size");
                }
            } else {
                dirent.size = 0;
            }


            if (!isDir) {
                dirent.type = DirentType.FILE;
            } else {
                dirent.type = DirentType.DIR;
            }

            return dirent;
        } catch (JSONException e) {
            Log.d(DEBUG_TAG, e.getMessage());
            return null;
        }
    }

    /**
     * Checks if the deleted item is a directory.
     *
     * @return True if the deleted item is a directory, false otherwise.
     */
    public boolean isDir() {
        return (type == DirentType.DIR);
    }

    /**
     * Gets the file size of the deleted item.
     *
     * @return The file size in bytes.
     */
    public long getFileSize() {
        return size;
    }

    /**
     * Gets the path of the deleted item.
     *
     * @return The path of the deleted item.
     */
    public String getPath() {
        return path;
    }

    @Override
    public String getTitle() {
        return name;
    }

    @Override
    public String getSubtitle() {
        String timestamp = deletedTime == 0 ? "" : Utils.translateCommitTime(deletedTime);
        if (isDir())
            return timestamp;
        String fileSize = size == 0 ? "" : Utils.readableFileSize(size);
        String finalText = "";

        if (!fileSize.equals("")) {
            finalText += fileSize;
        }

        if (!finalText.equals("") && !timestamp.equals("")) {
            finalText += ", " + timestamp;
        } else if (!timestamp.equals("")) {
            finalText += timestamp;
        }

        return finalText;
    }

    @Override
    public int getIcon() {
        if (isDir()) {
            return R.drawable.folder;
        }
        return Utils.getFileIcon(name);
    }

    /**
     * Comparator class for sorting SeafDirentTrash objects by last modified time.
     */
    public static class DirentLastMTimeComparator implements Comparator<SeafDirentTrash> {

        @Override
        public int compare(SeafDirentTrash itemA, SeafDirentTrash itemB) {
            return (int) (itemA.deletedTime - itemB.deletedTime);
        }
    }

    /**
     * Comparator class for sorting SeafDirentTrash objects by name.
     */
    public static class DirentNameComparator implements Comparator<SeafDirentTrash> {

        @Override
        public int compare(SeafDirentTrash itemA, SeafDirentTrash itemB) {
            // get the first character unicode from each file name
            int unicodeA = itemA.name.codePointAt(0);
            int unicodeB = itemB.name.codePointAt(0);

            String strA, strB;

            // both are Chinese words
            if ((19968 < unicodeA && unicodeA < 40869) && (19968 < unicodeB && unicodeB < 40869)) {
                strA = PinyinUtils.toPinyin(SeadroidApplication.getAppContext(), itemA.name).toLowerCase();
                strB = PinyinUtils.toPinyin(SeadroidApplication.getAppContext(), itemB.name).toLowerCase();
            } else if ((19968 < unicodeA && unicodeA < 40869) && !(19968 < unicodeB && unicodeB < 40869)) {
                // itemA is Chinese and itemB is English
                return 1;
            } else if (!(19968 < unicodeA && unicodeA < 40869) && (19968 < unicodeB && unicodeB < 40869)) {
                // itemA is English and itemB is Chinese
                return -1;
            } else {
                // both are English words
                strA = itemA.name.toLowerCase();
                strB = itemB.name.toLowerCase();
            }

            return strA.compareTo(strB);
        }
    }
}
