package com.ateneacloud.drive.transfer;

import android.util.Log;
import android.widget.Toast;

import com.ateneacloud.drive.R;
import com.ateneacloud.drive.SeadroidApplication;
import com.ateneacloud.drive.SeafException;
import com.ateneacloud.drive.SettingsManager;
import com.ateneacloud.drive.data.DataManager;
import com.ateneacloud.drive.data.ProgressMonitor;
import com.ateneacloud.drive.util.Utils;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Upload task
 */
public class UploadTask extends TransferTask {
    public static final String DEBUG_TAG = "UploadTask";

    private String dir;   // parent dir
    private boolean isUpdate;  // true if update an existing file
    private boolean isCopyToLocal; // false to turn off copy operation
    private boolean byBlock;
    private UploadStateListener uploadStateListener;
    private String source;
    private DataManager dataManager;

    private UploadFile uploadFile;
    private String newFileId;

    public UploadTask(String source, int taskID, UploadFile uploadFile, boolean byBlock,
                      UploadStateListener uploadStateListener) {

        super(source, taskID, uploadFile.getAccount(), uploadFile.getRepoName(), uploadFile.getRepoID(), uploadFile.getFilePath());
        this.uploadFile = uploadFile;
        this.dir = uploadFile.getRepoPath();
        this.isUpdate = uploadFile.isUpdate();
        this.isCopyToLocal = uploadFile.isCopyToLocal();
        this.byBlock = byBlock;
        this.uploadStateListener = uploadStateListener;
        this.totalSize = new File(uploadFile.getFilePath()).length();
        this.finished = 0;
        this.dataManager = new DataManager(account);
        this.source = source;
    }

    public UploadTaskInfo getTaskInfo() {
        UploadTaskInfo info = new UploadTaskInfo(account, taskID, state, repoID,
                repoName, dir, path, newFileId, isUpdate, isCopyToLocal,
                finished, totalSize, err);
        return info;
    }

    public void cancelUpload() {
        if (state != TaskState.INIT && state != TaskState.TRANSFERRING) {
            return;
        }
        state = TaskState.CANCELLED;
        //super.cancel(true);
    }

    @Override
    protected void onPreExecute() {
        state = TaskState.TRANSFERRING;
    }

    @Override
    protected void onProgressUpdate(Long... values) {
        long uploaded = values[0];
        // Log.d(DEBUG_TAG, "Uploaded " + uploaded);
        this.finished = uploaded;
        uploadStateListener.onFileUploadProgress(taskID);
    }

    @Override
    protected File doInBackground(Void... params) {
        try {
            ProgressMonitor monitor = new ProgressMonitor() {
                @Override
                public void onProgressNotify(long uploaded, boolean updateTotal) {
                    publishProgress(uploaded);
                }

                @Override
                public boolean isCancelled() {
                    return UploadTask.this.isCancelled();
                }
            };

            if (!uploadFile.isMustBeCancelled()) {
                if (byBlock) {
                    newFileId = dataManager.uploadByBlocks(repoName, repoID, dir, path, monitor, isUpdate, isCopyToLocal);
                } else {
                    newFileId = dataManager.uploadFile(repoName, repoID, dir, path, monitor, isUpdate, isCopyToLocal);
                }
            } else {
                cancelUpload();
            }

        } catch (SeafException e) {
            Log.e(DEBUG_TAG, "Upload exception " + e.getCode() + " " + e.getMessage());
            e.printStackTrace();
            err = e;
        } catch (NoSuchAlgorithmException | IOException e) {
            Log.e(DEBUG_TAG, "Upload exception " + e.getMessage());
            err = SeafException.unknownException;
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(File file) {

        if (state != TaskState.CANCELLED) {
            state = err == null ? TaskState.FINISHED : TaskState.FAILED;
        }

        if (uploadStateListener != null) {
            if (state == TaskState.CANCELLED) {
                uploadStateListener.onFileUploadCancelled(taskID);
            } else if (err == null) {
                SettingsManager.instance().saveUploadCompletedTime(Utils.getSyncCompletedTime());
                uploadStateListener.onFileUploaded(taskID);
            } else {
                if (err.getCode() == SeafException.HTTP_ABOVE_QUOTA) {
                    try {
                        Toast.makeText(SeadroidApplication.getAppContext(), R.string.above_quota, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                uploadStateListener.onFileUploadFailed(taskID);
            }
        }
    }

    @Override
    protected void onCancelled() {
        if (uploadStateListener != null) {
            uploadStateListener.onFileUploadCancelled(taskID);
        }
    }

    public String getDir() {
        return dir;
    }

    public boolean isCopyToLocal() {
        return isCopyToLocal;
    }

    public boolean isUpdate() {
        return isUpdate;
    }

    public UploadFile getUploadFile() {
        return uploadFile;
    }
}