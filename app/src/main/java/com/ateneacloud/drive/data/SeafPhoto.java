package com.ateneacloud.drive.data;

import java.io.Serializable;

/**
 * Photo entity for displaying photos in gallery
 */
public class SeafPhoto implements Serializable {
    public static final long serialVersionUID = 0L;

    /** download shows tatus */
    private boolean downloaded;
    /** display name */
    private String name;
    /** repo name */
    private String repoName;
    /** repo id */
    private String repoID;
    /** dir path */
    private String dirPath;
    /** related {@link SeafDirent} */
    private SeafDirent dirent;

    public SeafPhoto(String repoName, String repoID, String dirPath, SeafDirent dirent) {
        this.repoName = repoName;
        this.repoID = repoID;
        this.dirPath = dirPath;
        this.dirent = dirent;
        this.name = dirent.name;
    }

    public boolean getDownloaded() {
        return downloaded;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }

    public String getName() {
        return name;
    }

    public String getDirPath() {
        return dirPath;
    }

    public String getRepoID() {
        return repoID;
    }

    public String getRepoName() {
        return repoName;
    }

    public SeafDirent getDirent() {
        return dirent;
    }

}