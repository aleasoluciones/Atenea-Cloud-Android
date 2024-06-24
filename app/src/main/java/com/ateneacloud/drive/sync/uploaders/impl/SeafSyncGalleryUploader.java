package com.ateneacloud.drive.sync.uploaders.impl;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import com.ateneacloud.drive.SeadroidApplication;
import com.ateneacloud.drive.SeafConnection;
import com.ateneacloud.drive.SeafException;
import com.ateneacloud.drive.account.AccountInfo;
import com.ateneacloud.drive.account.AccountPlans;
import com.ateneacloud.drive.data.DataManager;
import com.ateneacloud.drive.data.SeafDirent;
import com.ateneacloud.drive.data.SeafRepo;
import com.ateneacloud.drive.sync.enums.SeafSyncMode;
import com.ateneacloud.drive.sync.enums.SeafSyncStatus;
import com.ateneacloud.drive.sync.enums.SeafSyncLogStatus;
import com.ateneacloud.drive.sync.fileProvider.SeafSyncFileProviderFactory;
import com.ateneacloud.drive.sync.fileProvider.SeafSyncProviderProtocol;
import com.ateneacloud.drive.sync.fileProvider.syncItems.SeafSyncFileItem;
import com.ateneacloud.drive.sync.logs.SeafSyncLog;
import com.ateneacloud.drive.sync.settings.SeafSyncSettings;
import com.ateneacloud.drive.sync.settings.SeafSyncSettingsService;
import com.ateneacloud.drive.sync.logs.services.SeafSyncLogsService;
import com.ateneacloud.drive.sync.uploaders.SeafUploaderProtocol;
import com.ateneacloud.drive.transfer.SeafSyncUploadFile;
import com.ateneacloud.drive.transfer.TaskState;
import com.ateneacloud.drive.transfer.TransferService;
import com.ateneacloud.drive.transfer.UploadTaskInfo;
import com.ateneacloud.drive.util.Utils;
import com.ateneacloud.drive.util.ValidatesFiles;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class SeafSyncGalleryUploader implements SeafUploaderProtocol {

    private SeafSyncSettings settings;
    private AccountInfo accountInfo;
    private List<Integer> tasksInProgress;
    private HashMap<Integer, SeafSyncFileItem> listSeafSyncFileItemUploads;
    private TransferService transferService;
    private SeafSyncLogsService logService;
    private SeafSyncProviderProtocol fileProvider;
    private SeafSyncSettingsService settingsService;
    private DataManager dataManager;
    private SeafRepo repoCache;
    private boolean isAllValidFiles;
    private boolean notErrorUploadFiles;
    private boolean notErrorUploadFilesForSpace;

    /**
     * Constructs a new SeafSyncGalleryUploader.
     *
     * @param settings The SeafSyncSettings object containing synchronization settings.
     * @param connection The SeafConnection object for the Seafile server connection.
     */
    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // this will run in a foreign thread!

            TransferService.TransferBinder binder = (TransferService.TransferBinder) service;
            synchronized (SeafSyncGalleryUploader.this) {
                transferService = binder.getService();
            }
            // Log.d(DEBUG_TAG, "connected to TransferService");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // this will run in a foreign thread!
            // Log.d(DEBUG_TAG, "disconnected from TransferService, aborting sync");

            synchronized (SeafSyncGalleryUploader.this) {
                transferService = null;
            }
        }
    };

    /**
     * Constructs a new SeafSyncGalleryUploader.
     *
     * @param settings   The SeafSyncSettings object containing synchronization settings.
     * @param connection The SeafConnection object for the Seafile server connection.
     */
    public SeafSyncGalleryUploader(SeafSyncSettings settings, SeafConnection connection) {
        this.settings = settings;
        logService = new SeafSyncLogsService();
        tasksInProgress = new ArrayList<>();
        listSeafSyncFileItemUploads = new HashMap<>();
        settingsService = new SeafSyncSettingsService();
        fileProvider = SeafSyncFileProviderFactory.getProviderFor(settings);
        dataManager = new DataManager(connection.getAccount());
        repoCache = dataManager.getCachedRepoByID(settings.getRepoId());
        Intent bIntent = new Intent(SeadroidApplication.getAppContext(), TransferService.class);
        SeadroidApplication.getAppContext().bindService(bIntent, mConnection, Context.BIND_AUTO_CREATE);
        isAllValidFiles = true;
        notErrorUploadFiles = true;
        notErrorUploadFilesForSpace = true;
        try {
            accountInfo = dataManager.getAccountInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes and configures a new instance of SeafSyncGalleryUploader with the given settings and connection.
     *
     * @param settings   The synchronization settings to be used.
     * @param connection The connection details for the Seafile server.
     * @return An initialized SeafSyncGalleryUploader instance.
     */
    @Override
    public SeafSyncGalleryUploader init(SeafSyncSettings settings, SeafConnection connection) {
        return new SeafSyncGalleryUploader(settings, connection);
    }

    /**
     * Initiates the synchronization process, updates the synchronization state to 'Running', and proceeds to load the target folder for synchronization.
     * If the required conditions are met, the method starts the upload process for the specified folder.
     */
    @Override
    public void upload() {
        settingsService.updateState(settings, SeafSyncStatus.Running);
        loadTargetFolder();
    }

    /**
     * Loads the target folder for synchronization and initiates the file upload process if the required conditions are met.
     * Checks if the remote cloud directory and local folder exist, and waits for the transfer service to be available before starting the upload process.
     */
    private void loadTargetFolder() {
        if (!isCloudDirectoryExists(settings.getRepoPath())) {
            settingsService.updateState(settings, SeafSyncStatus.ErrorNotExistRepoDir);
            settingsService.setLastRunTime(settings, (accountInfo.getPlan() == AccountPlans.Basic));
        } else {
            do {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (transferService == null);
            startDeleteProcess();
            startUploadProcess();
        }
    }

    /**
     * Initiates the process of deleting files based on synchronization logs.
     * Files that have been successfully uploaded and have exceeded their expiration time
     * are deleted from the remote repository, and their status is updated in the synchronization logs.
     */
    private void startDeleteProcess() {

        if (accountInfo != null && accountInfo.getPlan() != AccountPlans.Basic) {
            Date currentDate = new Date();
            // Retrieve synchronization logs for the specified settings, account, and status
            List<SeafSyncLog> logs = logService.findBySettingsAndAccountIdAndStatus(settings.getId(), settings.getAccountId(), SeafSyncLogStatus.Uploaded);

            if (settings.getExpireDate() != null) {
                deleteAllFiles(currentDate, logs);
            } else {
                deleteExpiredFiles(currentDate, logs);
            }
        }

    }

    private void deleteAllFiles(Date currentDate, List<SeafSyncLog> logs) {
        if (settings.isDeletedAllFiles() && currentDate.after(settings.getExpireDate()) || currentDate.equals(settings.getExpireDate())) {
            for (SeafSyncLog log : logs) {
                try {
                    // Construct the full path of the file in the remote repository
                    String path = Utils.pathJoin(log.getRemotePath(), log.getRemoteName());

                    // Delete the file from the remote repository
                    dataManager.delete(null,settings.getRepoId(), path, false);

                    // Update the status of the synchronization log to "Deleted"
                    logService.changeState(log, SeafSyncLogStatus.Deleted);
                } catch (SeafException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void deleteExpiredFiles(Date currentDate, List<SeafSyncLog> logs) {

        if (settings.getTimeExpirationFiles() != -1) {
            long miliseconds = settings.getNumberExpiration() != -1 ? (settings.getNumberExpiration() * settings.getTimeExpirationFiles()) : settings.getTimeExpirationFiles();
            // Iterate through the retrieved logs
            for (SeafSyncLog log : logs) {
                try {

                    // Calculate the expiration date for the file based on the synchronization settings
                    Date logDate = Utils.addTimeToDate(log.getUploadedDate(), miliseconds);

                    if (currentDate.after(logDate) || currentDate.equals(logDate)) {
                        // Construct the full path of the file in the remote repository
                        String path = Utils.pathJoin(log.getRemotePath(), log.getRemoteName());

                        // Delete the file from the remote repository
                        dataManager.delete(null, settings.getRepoId(), path, false);

                        // Update the status of the synchronization log to "Deleted"
                        logService.changeState(log, SeafSyncLogStatus.Deleted);
                    }
                } catch (SeafException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * Initiates the file upload process for all eligible files in the sync queue.
     * Validates and uploads files, waits for the uploads to complete, and checks the upload results.
     */
    private void startUploadProcess() {
        List<SeafSyncFileItem> items = fileProvider.getFiles();
        for (SeafSyncFileItem syncItem : items) {
            try {
                if (!wasUploadedOrQueued(syncItem) && isMustBeUpload(syncItem)) {
                    if (ValidatesFiles.isValidFile(SeadroidApplication.getAppContext(), dataManager.getAccountInfo(), Uri.fromFile(syncItem.getPath())) && (settings.isUploadVideos() || !ValidatesFiles.isVideoByFile(syncItem.getPath()))) {
                        if (isSpaceToUploadFile(syncItem)) {
                            try {
                                uploadFile(syncItem);
                            } catch (Exception e) {
                                notErrorUploadFiles = false;
                                e.printStackTrace();
                            }
                        } else {
                            notErrorUploadFilesForSpace = false;
                        }
                    } else {
                        isAllValidFiles = false;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        waitForUploads();
        checkUploadResult();
    }

    /**
     * Uploads a file to the Seafile server. Checks if the file already exists on the server and skips uploading if found.
     *
     * @param item The SeafSyncFileItem representing the file to be uploaded.
     * @throws SeafException if an error occurs during the upload process.
     */
    private void uploadFile(SeafSyncFileItem item) throws SeafException {
        File file = item.getPath();
        Utils.utilsLogInfo(true, "=======uploadFile===");

        List<SeafDirent> list = dataManager.getCachedDirents(repoCache.getID(), settings.getRepoPath());
        if (list == null) {
            Log.e("", "Seadroid dirent cache is empty in uploadFile. Should not happen, aborting.");
            Utils.utilsLogInfo(true, "=======Seadroid dirent cache is empty in uploadFile. Should not happen, aborting.");
            throw SeafException.unknownException;
        }

        String filename = file.getName();
        String prefix;
        String suffix;
        int dotIndex = filename.lastIndexOf(".");

        if (dotIndex > 0) {
            prefix = filename.substring(0, dotIndex);
            suffix = filename.substring(dotIndex);
        } else {
            prefix = filename;
            suffix = "";
        }
        Pattern pattern = Pattern.compile(Pattern.quote(prefix) + "( \\(\\d+\\))?" + Pattern.quote(suffix));
        for (SeafDirent dirent : list) {
            if (pattern.matcher(dirent.name).matches() && dirent.size == file.length()) {
                // Log.d(DEBUG_TAG, "File " + file.getName() + " in bucket " + bucketName + " already exists on the server. Skipping.");
                Utils.utilsLogInfo(true, "====File " + file.getName() + " in bucket " + settings.getRepoPath() + " already exists on the server. Skipping.");
                return;
            }
        }

        // Log.d(DEBUG_TAG, "uploading file " + file.getName() + " to " + serverPath);
        Utils.utilsLogInfo(true, "====uploading file " + file.getName() + " to " + settings.getRepoPath());

        int taskID = 0;
        if (repoCache != null && repoCache.canLocalDecrypt()) {
            taskID = transferService.addTaskToUploadQueBlock(new SeafSyncUploadFile(dataManager.getAccount(), repoCache.getID(), repoCache.name, settings.getRepoPath(), file.getAbsolutePath(), false, false, settings.getId(), settings.isUploadOnlyOverWifi()));
        } else {
            taskID = transferService.addUploadTask(new SeafSyncUploadFile(dataManager.getAccount(), repoCache.getID(), repoCache.name, settings.getRepoPath(), file.getAbsolutePath(), false, false, settings.getId(), settings.isUploadOnlyOverWifi()));
        }

        if (taskID != 0) {
            tasksInProgress.add(taskID);
            listSeafSyncFileItemUploads.put(taskID, item);
            logService.log(item, settings);
        }

    }

    /**
     * Waits for the transfer service to finish uploading tasks and updates the task states in the log service.
     */
    private void waitForUploads() {
        // Log.d(DEBUG_TAG, "wait for transfer service to finish our tasks");
        boolean stop = false;
        WAITLOOP:
        while (!stop) {
            try {
                Thread.sleep(100); // wait
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            for (int id : tasksInProgress) {
                UploadTaskInfo info = transferService.getUploadTaskInfo(id);
                if (info.state == TaskState.INIT || info.state == TaskState.TRANSFERRING) {
                    Enum<SeafSyncLogStatus> status = logService.getState(listSeafSyncFileItemUploads.get(id), settings);

                    if (status != null && (status != SeafSyncLogStatus.Uploaded || status != SeafSyncLogStatus.InQueue)) {
                        logService.changeState(listSeafSyncFileItemUploads.get(id), settings, SeafSyncLogStatus.InQueue);
                    }

                    continue WAITLOOP;
                }
            }
            stop = true;
        }
    }

    /**
     * Checks the results of the upload tasks, logs their status, and updates the synchronization status.
     * - Iterates through the list of ongoing tasks to verify if they completed successfully.
     * - Changes the status of each task in the log.
     * - Determines the overall synchronization status based on the results.
     * - Updates the last run time and unbinds from the TransferService.
     */
    private void checkUploadResult() {
        boolean error = false;

        if (!notErrorUploadFilesForSpace) {
            settingsService.updateState(settings, SeafSyncStatus.ErrorOutSpace);
        } else {
            if (isAllValidFiles) {
                for (int id : tasksInProgress) {
                    UploadTaskInfo info = transferService.getUploadTaskInfo(id);
                    if (info.err != null || info.state != TaskState.FINISHED) {
                        error = true;
                    }

                    switch (info.state) {
                        case FINISHED:
                            logService.markAsUploaded(listSeafSyncFileItemUploads.get(id), settings, info.newFileId);
                            break;
                        case CANCELLED:
                            logService.changeState(listSeafSyncFileItemUploads.get(id), settings, SeafSyncLogStatus.Canceled);
                            break;
                        case FAILED:
                            logService.changeState(listSeafSyncFileItemUploads.get(id), settings, SeafSyncLogStatus.Errored);
                            break;
                    }
                }

                if (!notErrorUploadFiles) {
                    error = true;
                }

                if (error) {
                    settingsService.updateState(settings, SeafSyncStatus.Error);
                } else {
                    settingsService.updateState(settings, SeafSyncStatus.Complete);
                }

            } else {
                settingsService.updateState(settings, SeafSyncStatus.ErrorNotValidAllFile);
            }

        }

        settingsService.setLastRunTime(settings, (accountInfo.getPlan() == AccountPlans.Basic));

        SeadroidApplication.getAppContext().unbindService(mConnection);

    }

    /**
     * Checks if a file has either been uploaded or queued for upload.
     *
     * @param syncFileItem The file item to check.
     * @return True if the file has been uploaded or queued, false otherwise.
     */
    private boolean wasUploadedOrQueued(SeafSyncFileItem syncFileItem) {
        return uploadFileIsInLog(syncFileItem) || fileNamed(syncFileItem.getPath().getName()) || fileIdentifierInUploadQueueFor(syncFileItem);
    }

    /**
     * Checks if a file is already recorded as uploaded in the synchronization log.
     *
     * @param syncFileItem The file item to check.
     * @return True if the file is already recorded as uploaded, false otherwise.
     */
    private boolean uploadFileIsInLog(SeafSyncFileItem syncFileItem) {
        return logService.isAlreadyUploaded(syncFileItem, settings);
    }

    /**
     * Checks if a file with a specific name exists in a remote directory.
     *
     * @param fileName The name of the file to check.
     * @return True if a file with the given name exists in the remote directory, false otherwise.
     */
    private boolean fileNamed(String fileName) {
        List<SeafDirent> matchingFiles = new ArrayList<>();
        try {
            List<SeafDirent> dir = dataManager.getDirentsFromServer(settings.getRepoId(), settings.getRepoPath());
            for (SeafDirent uploadFile : dir) {
                if (uploadFile.getTitle().equals(fileName)) {
                    matchingFiles.add(uploadFile);
                }
            }
        } catch (SeafException e) {
            e.printStackTrace();
        }
        return matchingFiles.size() > 0;
    }

    /**
     * Checks if a file with the same identifier is already present in the upload queue.
     *
     * @param item The SeafSyncFileItem to check for its identifier in the upload queue.
     * @return True if a matching file is found in the upload queue, otherwise false.
     */
    private boolean fileIdentifierInUploadQueueFor(SeafSyncFileItem item) {
        List<SeafSyncFileItem> matchingFiles = new ArrayList<>();
        listSeafSyncFileItemUploads.forEach((key, value) -> {
            if (value.getIdentifier().equals(item.getIdentifier())) {
                matchingFiles.add(value);
            }
        });
        return matchingFiles.size() > 0;
    }

    /**
     * Checks if a cloud directory exists on the server at the specified path.
     *
     * @param cloudDirPath The path of the cloud directory to check for existence on the server.
     * @return True if the cloud directory exists on the server; otherwise, false.
     */
    private boolean isCloudDirectoryExists(String cloudDirPath) {
        try {
            if (dataManager.getDirentsFromServer(settings.getRepoId(), cloudDirPath) != null) {
                return true;
            }
        } catch (SeafException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Checks whether a file represented by the given SeafSyncFileItem must be uploaded based on creation dates and synchronization mode.
     *
     * @param syncFileItem The SeafSyncFileItem to check for upload requirement.
     * @return True if the file must be uploaded according to the synchronization mode; otherwise, false.
     */
    private boolean isMustBeUpload(SeafSyncFileItem syncFileItem) {
        Date itemDate = syncFileItem.getCreationDate();
        Date settingsDate = settings.getCreationDate();

        boolean uploadRequired = itemDate.before(settingsDate) || itemDate.equals(settingsDate);

        if (settings.getMode().equals(SeafSyncMode.Incremental)) {
            return !uploadRequired;
        }

        // If the synchronization mode is not Incremental, always upload the file.
        return true;

    }

    /**
     * Checks if there is sufficient space on the server to upload a file.
     *
     * @param seafSyncFileItem The file to be uploaded.
     * @return True if there is enough space, false otherwise.
     */
    private boolean isSpaceToUploadFile(SeafSyncFileItem seafSyncFileItem) {
        try {
            AccountInfo info = dataManager.getAccountInfo();
            return (seafSyncFileItem.getSizeInBytes() + info.getUsage()) <= info.getTotal();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

}
