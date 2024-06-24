package com.ateneacloud.drive.data;

import com.ateneacloud.drive.R;
import com.ateneacloud.drive.SeadroidApplication;
import com.ateneacloud.drive.SettingsManager;
import com.ateneacloud.drive.util.PinyinUtils;
import com.ateneacloud.drive.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

/**
 * Represents a deleted Seafile repository.
 */
public class SeafRepoTrash implements SeafItem {
    public String id;
    public String name;
    public String owner;
    public long deleteTime;
    public boolean encrypted;
    public long size;

    /**
     * Creates a SeafRepoTrash object from a JSON object.
     *
     * @param obj The JSON object representing the deleted repository.
     * @return A SeafRepoTrash object created from the JSON object.
     * @throws JSONException If there is an error parsing the JSON object.
     */
    static SeafRepoTrash fromJson(JSONObject obj) throws JSONException {
        SeafRepoTrash repo = new SeafRepoTrash();
        repo.id = obj.getString("repo_id");
        repo.owner = obj.getString("owner_email");
        repo.name = obj.getString("repo_name");
        repo.encrypted = obj.getBoolean("encrypted");
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
            Date delTime = dateFormat.parse(obj.getString("del_time"));
            repo.deleteTime = delTime.getTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
        repo.size = obj.getLong("size");
        return repo;
    }

    public SeafRepoTrash() {
    }

    public String getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    @Override
    public String getTitle() {
        return name;
    }

    @Override
    public String getSubtitle() {
        return Utils.translateCommitTime(deleteTime);
    }

    @Override
    public int getIcon() {
        if (encrypted)
            return R.drawable.repo_encrypted;

        return R.drawable.repo;
    }

    /**
     * Checks if local decryption is possible for the repository.
     *
     * @return True if local decryption is possible, false otherwise.
     */
    public boolean canLocalDecrypt() {
        return encrypted && SettingsManager.instance().isEncryptEnabled();
    }

    /**
     * Comparator class for sorting SeafRepoTrash objects by last modified time.
     */
    public static class RepoLastMTimeComparator implements Comparator<SeafRepoTrash> {

        @Override
        public int compare(SeafRepoTrash itemA, SeafRepoTrash itemB) {
            return (int) (itemA.deleteTime - itemB.deleteTime);
        }
    }

    /**
     * Comparator class for sorting SeafRepoTrash objects by name.
     */
    public static class RepoNameComparator implements Comparator<SeafRepoTrash> {

        @Override
        public int compare(SeafRepoTrash itemA, SeafRepoTrash itemB) {
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
