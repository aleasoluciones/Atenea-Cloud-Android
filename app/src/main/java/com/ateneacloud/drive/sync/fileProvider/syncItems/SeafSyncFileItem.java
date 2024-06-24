package com.ateneacloud.drive.sync.fileProvider.syncItems;

import com.ateneacloud.drive.sync.SeafSyncUtils;
import com.ateneacloud.drive.sync.enums.SeafSyncItemType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.Date;

public class SeafSyncFileItem {

    private File path;
    private Date creationDate;
    private Date modificationDate;
    private Enum<SeafSyncItemType> itemType;
    private boolean isDirectory;
    private long sizeInBytes;
    private String fileHash;
    private String identifier;
    private String extension;

    public SeafSyncFileItem(File path) {
        this.path = path;
        fillProperties();
    }

    private void fillProperties() {
        if (path.exists()) {

            extension = path.getName().substring(path.getName().lastIndexOf(".") + 1).toLowerCase();
            identifier = String.valueOf(path.hashCode());
            isDirectory = path.isDirectory();
            itemType = isDirectory ? SeafSyncItemType.Folder : SeafSyncItemType.File;

            try {
                FileTime creationTime = (FileTime) Files.getAttribute(path.getAbsoluteFile().toPath(), "creationTime");
                creationDate = new Date(creationTime.toMillis());
            } catch (IOException e) {
                creationDate = new Date(path.lastModified());
                e.printStackTrace();
            }

            modificationDate = new Date(path.lastModified());

            sizeInBytes = path.length();
            fileHash = SeafSyncUtils.calculateHash(identifier + "," + sizeInBytes);
        }
    }

    public File getPath() {
        return path;
    }

    public void setPath(File path) {
        this.path = path;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Date getModificationDate() {
        return modificationDate;
    }

    public void setModificationDate(Date modificationDate) {
        this.modificationDate = modificationDate;
    }

    public Enum<SeafSyncItemType> getItemType() {
        return itemType;
    }

    public void setItemType(Enum<SeafSyncItemType> itemType) {
        this.itemType = itemType;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    public void setSizeInBytes(long sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }
}
