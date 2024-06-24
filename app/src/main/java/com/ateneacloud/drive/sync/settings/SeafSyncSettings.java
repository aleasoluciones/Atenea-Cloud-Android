package com.ateneacloud.drive.sync.settings;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.ateneacloud.drive.sync.enums.SeafSyncMode;
import com.ateneacloud.drive.sync.enums.SeafSyncNetwork;
import com.ateneacloud.drive.sync.enums.SeafSyncStatus;
import com.ateneacloud.drive.sync.enums.SeafSyncType;

import java.io.File;
import java.util.Date;
import java.util.UUID;

@Entity(tableName = "seafsyncsettings")
public class SeafSyncSettings implements Cloneable, Parcelable {

    @PrimaryKey
    @NonNull
    private String id;
    private String accountId;
    private String repoId;
    private String repoPath;
    private Date creationDate;
    private Date expireDate;
    private Date lastExecution;
    private long timeExpirationFiles;
    private Enum<SeafSyncNetwork> network;
    private Enum<SeafSyncMode> mode;
    private Enum<SeafSyncType> type;
    private Enum<SeafSyncStatus> status;
    private Uri resourceUri;
    private boolean uploadVideos;
    private boolean active;
    private int numberExpiration;
    private boolean deletedAllFiles;

    protected SeafSyncSettings(Parcel in) {
        id = in.readString();
        accountId = in.readString();
        repoId = in.readString();
        creationDate = (Date) in.readSerializable();
        expireDate = (Date) in.readSerializable();
        lastExecution = (Date) in.readSerializable();
        timeExpirationFiles = in.readLong();
        network = SeafSyncNetwork.values()[in.readInt()];
        mode = SeafSyncMode.values()[in.readInt()];
        type = SeafSyncType.values()[in.readInt()];
        status = SeafSyncStatus.values()[in.readInt()];
        resourceUri = in.readParcelable(Uri.class.getClassLoader());
        uploadVideos = in.readByte() != 0;
        active = in.readByte() != 0;
        repoPath = in.readString();
        numberExpiration = in.readInt();
        deletedAllFiles = in.readByte() != 0;

    }

    public SeafSyncSettings() {
        id = UUID.randomUUID().toString();
        status = SeafSyncStatus.Pending;
        timeExpirationFiles = -1;
        numberExpiration = -1;
        deletedAllFiles = false;
        active = true;
    }

    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getRepoId() {
        return repoId;
    }

    public void setRepoId(String repoId) {
        this.repoId = repoId;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Date getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(Date expireDate) {
        this.expireDate = expireDate;
    }

    public Date getLastExecution() {
        return lastExecution;
    }

    public void setLastExecution(Date lastExecution) {
        this.lastExecution = lastExecution;
    }

    public Enum<SeafSyncNetwork> getNetwork() {
        return network;
    }

    public void setNetwork(Enum<SeafSyncNetwork> network) {
        this.network = network;
    }

    public Enum<SeafSyncMode> getMode() {
        return mode;
    }

    public void setMode(Enum<SeafSyncMode> mode) {
        this.mode = mode;
    }

    public Enum<SeafSyncType> getType() {
        return type;
    }

    public void setType(Enum<SeafSyncType> type) {
        this.type = type;
    }

    public Uri getResourceUri() {
        return resourceUri;
    }

    public void setResourceUri(Uri resourceUri) {
        this.resourceUri = resourceUri;
    }

    public boolean isUploadVideos() {
        return uploadVideos;
    }

    public void setUploadVideos(boolean uploadVideos) {
        this.uploadVideos = uploadVideos;
    }

    public Enum<SeafSyncStatus> getStatus() {
        return status;
    }

    public void setStatus(Enum<SeafSyncStatus> status) {
        this.status = status;
    }

    public String getRepoPath() {
        return repoPath;
    }

    public void setRepoPath(String repoPath) {
        this.repoPath = repoPath;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isUploadOnlyOverWifi() {
        return network == SeafSyncNetwork.Wifi;
    }

    public int getNumberExpiration() {
        return numberExpiration;
    }

    public void setNumberExpiration(int numberExpiration) {
        this.numberExpiration = numberExpiration;
    }

    public String getDirNameOfResource() {

        if (!getMode().equals(SeafSyncType.Gallery)) {
            File file = new File(getResourceUri().getPath());
            return file.getName();
        }

        return "";
    }

    public long getTimeExpirationFiles() {
        return timeExpirationFiles;
    }

    public void setTimeExpirationFiles(long timeExpirationFiles) {
        this.timeExpirationFiles = timeExpirationFiles;
    }

    public boolean isDeletedAllFiles() {
        return deletedAllFiles;
    }

    public void setDeletedAllFiles(boolean deletedAllFiles) {
        this.deletedAllFiles = deletedAllFiles;
    }

    @Override
    public SeafSyncSettings clone() {
        SeafSyncSettings clonedSettings = new SeafSyncSettings();

        clonedSettings.setId(new String(this.id));
        clonedSettings.setAccountId(new String(this.accountId));
        clonedSettings.setRepoId(new String(this.repoId));
        clonedSettings.setCreationDate(new Date(this.creationDate.getTime()));

        if (this.expireDate != null) {
            clonedSettings.setExpireDate(new Date(this.expireDate.getTime()));
        }

        if (this.lastExecution != null) {
            clonedSettings.setLastExecution(new Date(this.lastExecution.getTime()));
        }

        if (this.resourceUri != null) {
            clonedSettings.setResourceUri(Uri.parse(this.resourceUri.toString()));
        }

        clonedSettings.setTimeExpirationFiles(this.timeExpirationFiles);
        clonedSettings.setNetwork(this.network);
        clonedSettings.setMode(this.mode);
        clonedSettings.setType(this.type);
        clonedSettings.setUploadVideos(this.uploadVideos);
        clonedSettings.setStatus(this.status);
        clonedSettings.setRepoPath(new String(this.repoPath));
        clonedSettings.setNumberExpiration(this.numberExpiration);
        clonedSettings.setActive(this.active);
        clonedSettings.setDeletedAllFiles(this.deletedAllFiles);

        return clonedSettings;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(accountId);
        dest.writeString(repoId);
        dest.writeSerializable(creationDate);
        dest.writeSerializable(expireDate);
        dest.writeSerializable(lastExecution);
        dest.writeLong(timeExpirationFiles);
        dest.writeInt(network.ordinal());
        dest.writeInt(mode.ordinal());
        dest.writeInt(type.ordinal());
        dest.writeInt(status.ordinal());
        dest.writeParcelable(resourceUri, flags);
        dest.writeByte((byte) (uploadVideos ? 1 : 0));
        dest.writeByte((byte) (active ? 1 : 0));
        dest.writeString(repoPath);
        dest.writeInt(numberExpiration);
        dest.writeByte((byte) (deletedAllFiles ? 1 : 0));
    }

    public static final Parcelable.Creator<SeafSyncSettings> CREATOR = new Parcelable.Creator<SeafSyncSettings>() {
        public SeafSyncSettings createFromParcel(Parcel in) {
            return new SeafSyncSettings(in);
        }

        public SeafSyncSettings[] newArray(int size) {
            return new SeafSyncSettings[size];
        }
    };

}
