package com.ateneacloud.drive.ui;

public class NavContext {
    String repoID = null;
    String repoName = null;     // for display
    String dirPath = null;
    String dirID = null;
    String dirPermission = null;

    String dirCommitID = null;

    String dirNavigatePath = null;

    String dirNavigateRoot = null;

    boolean repoTrash = false;

    boolean direntsTrash = false;

    boolean navigateToDirentsTrash = false;

    public NavContext() {
    }

    public void setRepoID(String repoID) {
        this.repoID = repoID;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public void setDir(String path, String dirID) {
        this.dirPath = path;
        this.dirID = dirID;
    }

    public void setDirID(String dirID) {
        this.dirID = dirID;
    }

    public boolean inRepo() {
        return repoID != null;
    }

    public String getRepoID() {
        return repoID;
    }

    public String getRepoName() {
        return repoName;
    }

    public boolean isRepoRoot() {
        return "/".equals(dirPath);
    }

    public String getDirPath() {
        return dirPath;
    }

    public String getDirID() {
        return dirID;
    }

    public String getDirPermission() {
        return dirPermission;
    }

    public void setDirPermission(String dirPermission) {
        this.dirPermission = dirPermission;
    }

    public boolean inRepoTrash() {
        return repoTrash;
    }

    public void setRepoTrash(boolean repoTrash) {
        this.repoTrash = repoTrash;
    }

    public boolean inDirentsTrash() {
        return direntsTrash;
    }

    public void setDirentsTrash(boolean direntsTrash) {
        this.direntsTrash = direntsTrash;
    }

    public boolean isNavigateToDirentsTrash() {
        return navigateToDirentsTrash;
    }

    public void setNavigateToDirentsTrash(boolean navigateToDirentsTrash) {
        this.navigateToDirentsTrash = navigateToDirentsTrash;
    }

    public String getDirCommitID() {
        return dirCommitID;
    }

    public void setDirCommitID(String dirCommitID) {
        this.dirCommitID = dirCommitID;
    }

    public String getDirNavigatePath() {
        return dirNavigatePath;
    }

    public void setDirNavigatePath(String dirNavigatePath) {
        this.dirNavigatePath = dirNavigatePath;
    }

    public String getDirNavigateRoot() {
        return dirNavigateRoot;
    }

    public void setDirNavigateRoot(String dirNavigateRoot) {
        this.dirNavigateRoot = dirNavigateRoot;
    }
}
