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
import com.ateneacloud.drive.sync.tree.SeafSyncTree;
import com.ateneacloud.drive.sync.tree.SeafSyncTreeType;
import com.ateneacloud.drive.sync.tree.services.SeafSyncTreeService;
import com.ateneacloud.drive.sync.tree.treeChangeDetector.SeafSyncTreeChangeDetector;
import com.ateneacloud.drive.sync.tree.treeFolderState.repository.coreData.SeafSyncTreeFolderStateCoreDataRepository;
import com.ateneacloud.drive.sync.tree.treeFolderState.services.SeafSyncFolderStateService;
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

/**
 * The SeafSyncTreeUploader class is responsible for uploading files and directories to a Seafile repository.
 */
public class SeafSyncTreeUploader implements SeafUploaderProtocol {

    private SeafSyncSettings settings;
    private AccountInfo accountInfo;
    private SeafConnection connection;
    private List<Integer> tasksInProgress;
    private SeafSyncTreeService treeService;
    private List<SeafSyncTree> foldersQueueControl;
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
     * A ServiceConnection used to connect and disconnect from the TransferService.
     */
    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // this will run in a foreign thread!

            TransferService.TransferBinder binder = (TransferService.TransferBinder) service;
            synchronized (SeafSyncTreeUploader.this) {
                transferService = binder.getService();
            }
            // Log.d(DEBUG_TAG, "connected to TransferService");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // this will run in a foreign thread!
            // Log.d(DEBUG_TAG, "disconnected from TransferService, aborting sync");

            synchronized (SeafSyncTreeUploader.this) {
                transferService = null;
            }
        }
    };


    /**
     * Constructs a new SeafSyncTreeUploader instance with the provided settings and connection.
     *
     * @param settings   The SeafSyncSettings containing synchronization settings.
     * @param connection The SeafConnection used for connecting to the Seafile server.
     */
    public SeafSyncTreeUploader(SeafSyncSettings settings, SeafConnection connection) {
        this.settings = settings;
        this.connection = connection;
        logService = new SeafSyncLogsService();
        tasksInProgress = new ArrayList<>();
        listSeafSyncFileItemUploads = new HashMap<>();
        treeService = new SeafSyncTreeService(new SeafSyncFolderStateService(new SeafSyncTreeFolderStateCoreDataRepository()), new SeafSyncTreeChangeDetector());
        foldersQueueControl = new ArrayList<>();
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
     * Initializes the SeafSyncGalleryUploader based on the provided settings and connection.
     *
     * @param settings   The SeafSyncSettings containing synchronization settings.
     * @param connection The SeafConnection used for connecting to the Seafile server.
     * @return A new SeafSyncGalleryUploader instance.
     */
    @Override
    public SeafSyncGalleryUploader init(SeafSyncSettings settings, SeafConnection connection) {
        return new SeafSyncGalleryUploader(settings, connection);
    }

    /**
     * Initiates the file upload process, including loading the target folder and starting the upload tasks.
     */
    @Override
    public void upload() {
        settingsService.updateState(settings, SeafSyncStatus.Running);
        loadTargetFolder();
    }

    /**
     * Loads the target folder for synchronization and initiates the upload process.
     * Checks if the cloud directory and local folder exist, and then starts the upload process.
     * If the necessary TransferService is not available, it waits until it is available.
     */
    private void loadTargetFolder() {
        if (!isCloudDirectoryExists(settings.getRepoPath())) {
            settingsService.updateState(settings, SeafSyncStatus.ErrorNotExistRepoDir);
            settingsService.setLastRunTime(settings, (accountInfo.getPlan() == AccountPlans.Basic));
        } else if (!new File(settings.getResourceUri().getPath()).exists()) {
            settingsService.updateState(settings, SeafSyncStatus.ErrorNotExistLocalFolder);
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
                    dataManager.delete(null, settings.getRepoId(), path, false);

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
                        dataManager.delete(null,settings.getRepoId(), path, false);

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
     * Initiates the upload process for the current synchronization task.
     * - Builds the current synchronization tree based on the local resource URI and settings.
     * - Identifies and tracks changes in the tree since the last synchronization.
     * - Sets the folders queue control for processing.
     * - Creates remote directories on the server if they do not exist.
     * - Saves the current synchronization tree.
     * - Waits for ongoing uploads to complete.
     * - Checks the upload results and updates the synchronization status.
     */
    private void startUploadProcess() {
        SeafSyncTree currentTree = treeService.buildFrom(settings.getResourceUri(), settings);
        SeafSyncTree treeWithChangesSinceLastSync = treeService.changesSinceLastSyncFor(currentTree);
        setFoldersQueueControlFromTree(treeWithChangesSinceLastSync);

        if (treeWithChangesSinceLastSync != null) {
            addFilesInDirectoryToUploadQueue(currentTree, settings.getRepoPath());
            createRemoteDirectories(foldersQueueControl, settings.getRepoPath());
        }

        treeService.saveTree(currentTree);

        try {
            waitForUploads();
        } catch (Exception e) {
            e.printStackTrace();
        }
        checkUploadResult();
    }

    /**
     * Uploads a file to the specified remote path, checking for duplicates on the server.
     *
     * @param item       The SeafSyncFileItem representing the file to upload.
     * @param remotePath The path on the server where the file should be uploaded.
     * @throws SeafException If an error occurs during the upload process.
     */
    private void uploadFile(SeafSyncFileItem item, String remotePath) throws SeafException {
        File file = item.getPath();
        Utils.utilsLogInfo(true, "=======uploadFile===");

        List<SeafDirent> list = dataManager.getCachedDirents(repoCache.getID(), remotePath);
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
        Utils.utilsLogInfo(true, "====uploading file " + file.getName() + " to " + remotePath);

        int taskID = 0;
        if (repoCache != null && repoCache.canLocalDecrypt()) {
            taskID = transferService.addTaskToUploadQueBlock(new SeafSyncUploadFile(dataManager.getAccount(), repoCache.getID(), repoCache.name, remotePath, file.getAbsolutePath(), false, false, settings.getId(), settings.isUploadOnlyOverWifi()));
        } else {
            taskID = transferService.addUploadTask(new SeafSyncUploadFile(dataManager.getAccount(), repoCache.getID(), repoCache.name, remotePath, file.getAbsolutePath(), false, false, settings.getId(), settings.isUploadOnlyOverWifi()));
        }

        if (taskID != 0) {
            tasksInProgress.add(taskID);
            listSeafSyncFileItemUploads.put(taskID, item);
            logService.log(item, settings, remotePath);
        }

    }

    /**
     * Waits for ongoing upload tasks to complete before proceeding.
     * Monitors the status of upload tasks and logs their progress.
     * This method periodically checks the state of tasks and waits for them to finish.
     */
    private void waitForUploads() {
        // Log.d(DEBUG_TAG, "wait for transfer service to finish our tasks");
        boolean stop = false;
        WAITLOOP:
        while (!stop) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            for (int id : tasksInProgress) {
                UploadTaskInfo info = transferService.getUploadTaskInfo(id);
                if (info.state == TaskState.INIT || info.state == TaskState.TRANSFERRING) {

                    switch (info.state) {
                        case INIT:
                        case TRANSFERRING:
                            logService.changeState(listSeafSyncFileItemUploads.get(id), settings, SeafSyncLogStatus.InQueue);
                            break;
                    }

                    // there is still at least one task pending
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

                    while (info.state == TaskState.TRANSFERRING) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

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
     * Checks if a file with the specified name has already been uploaded or queued for synchronization.
     *
     * @param syncFileItem The SeafSyncFileItem to check for upload or queuing status.
     * @param remotePath   The remote path where the file should be uploaded.
     * @return True if the file has already been uploaded, queued, or logged; otherwise, false.
     */
    private boolean wasUploadedOrQueued(SeafSyncFileItem syncFileItem, String remotePath) {
        return uploadFileIsInLog(syncFileItem) || fileNamed(syncFileItem.getPath().getName(), remotePath) || fileIdentifierInUploadQueueFor(syncFileItem);
    }

    /**
     * Checks if a file represented by the given SeafSyncFileItem is already logged as uploaded.
     *
     * @param syncFileItem The SeafSyncFileItem to check for upload status in the log.
     * @return True if the file is logged as uploaded; otherwise, false.
     */
    private boolean uploadFileIsInLog(SeafSyncFileItem syncFileItem) {
        return logService.isAlreadyUploaded(syncFileItem, settings);
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
     * Checks whether a folder represented by the given SeafSyncFileItem must be created based on modification dates and synchronization mode.
     *
     * @param syncFileItem The SeafSyncFileItem representing the folder to check for creation requirement.
     * @return True if the folder must be created according to the synchronization mode; otherwise, false.
     */
    private boolean isMustBeCreateFolder(SeafSyncFileItem syncFileItem) {
        Date itemDate = syncFileItem.getModificationDate();
        Date settingsDate = settings.getCreationDate();

        boolean uploadRequired = itemDate.before(settingsDate) || itemDate.equals(settingsDate);

        if (settings.getMode().equals(SeafSyncMode.Incremental)) {
            return !uploadRequired;
        }

        // If the synchronization mode is not Incremental, always create the folder.
        return true;

    }

    /**
     * Checks if a file with the specified name exists in the specified directory on the server.
     *
     * @param fileName   The name of the file to check for in the server directory.
     * @param remotePath The remote path where the directory is located.
     * @return True if a file with the given name exists in the directory; otherwise, false.
     */
    private boolean fileNamed(String fileName, String remotePath) {
        List<SeafDirent> matchingFiles = new ArrayList<>();
        try {
            List<SeafDirent> dir = dataManager.getDirentsFromServer(settings.getRepoId(), remotePath);
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
     * @return True if a matching file is found in the upload queue, false otherwise.
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
     * Sets the folders queue control list based on the provided SeafSyncTree.
     *
     * @param treeWithChangesSinceLastSync The SeafSyncTree representing changes since the last synchronization.
     */
    public void setFoldersQueueControlFromTree(SeafSyncTree treeWithChangesSinceLastSync) {
        foldersQueueControl = treeWithChangesSinceLastSync.getChildrensOfType(SeafSyncTreeType.Folder);
    }


    /**
     * Recursively creates remote directories on the server for a list of local directories.
     *
     * @param trees          The list of local directories represented as SeafSyncTree objects.
     * @param cloudParentDir The remote parent directory where the directories should be created.
     */
    public void createRemoteDirectories(List<SeafSyncTree> trees, String cloudParentDir) {
        for (SeafSyncTree localDir : trees) {
            File file = new File(localDir.getUri().getPath().toString());
            String localDirName = file.getName();
            String cloudDirPath = cloudParentDir + "/" + localDirName;

            SeafSyncFileItem syncFileItem = new SeafSyncFileItem(file);
            if (!isCloudDirectoryExists(cloudDirPath) && isMustBeCreateFolder(syncFileItem)) {
                try {
                    createDir(cloudParentDir, localDirName);
                } catch (SeafException e) {
                    notErrorUploadFiles = false;
                    e.printStackTrace();
                }
            }

            if (isCloudDirectoryExists(cloudParentDir)) {
                addFilesInDirectoryToUploadQueue(localDir, cloudDirPath);
            }

            createRemoteDirectories(localDir.getChildrensOfType(SeafSyncTreeType.Folder), cloudDirPath);
        }
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
     * Creates a new directory on the server under the specified cloud parent directory.
     *
     * @param cloudParentDir The parent directory where the new directory will be created.
     * @param localDirName   The name of the new directory to be created.
     * @throws SeafException If an error occurs during the directory creation process.
     */
    private void createDir(String cloudParentDir, String localDirName) throws SeafException {
        dataManager.createNewDir(settings.getRepoId(), cloudParentDir, localDirName);
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
            //Log.d(":::DDD",seafSyncFileItem.getSizeInBytes() + " - " + info.getUsage() + " - " + info.getTotal() + " --- " + (seafSyncFileItem.getSizeInBytes() + info.getUsage() < info.getTotal()));
            return (seafSyncFileItem.getSizeInBytes() + info.getUsage()) <= info.getTotal();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Adds files from a directory to the upload queue, ensuring they are eligible for upload.
     *
     * @param treeFolder      The source directory containing files to be added to the upload queue.
     * @param remoteDirectory The remote directory on the server where files will be uploaded.
     */
    public void addFilesInDirectoryToUploadQueue(SeafSyncTree treeFolder, String remoteDirectory) {
        List<SeafSyncTree> filesInFolder = treeFolder.getChildrensOfType(SeafSyncTreeType.File);
        for (SeafSyncTree file : filesInFolder) {
            SeafSyncFileItem syncItem = new SeafSyncFileItem(new File(file.getUri().getPath()));

            try {
                // Check if the file has not been uploaded or queued for upload and should be uploaded
                if (!wasUploadedOrQueued(syncItem, remoteDirectory) && isMustBeUpload(syncItem)) {
                    // Check if the file is valid for upload and not a video (if specified by settings)
                    if (ValidatesFiles.isValidFile(SeadroidApplication.getAppContext(), dataManager.getAccountInfo(), Uri.fromFile(syncItem.getPath())) && (settings.isUploadVideos() || !ValidatesFiles.isVideoByFile(syncItem.getPath()))) {
                        if (isSpaceToUploadFile(syncItem)) {
                            try {
                                uploadFile(syncItem, remoteDirectory);
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
                isAllValidFiles = false;
                e.printStackTrace();
            }
        }
    }
}
