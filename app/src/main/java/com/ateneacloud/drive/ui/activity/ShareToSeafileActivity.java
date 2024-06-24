package com.ateneacloud.drive.ui.activity;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore.Images;

import androidx.appcompat.app.AlertDialog;

import android.util.Log;

import com.google.common.collect.Lists;
import com.ateneacloud.drive.R;
import com.ateneacloud.drive.SeafException;
import com.ateneacloud.drive.account.Account;
import com.ateneacloud.drive.account.AccountInfo;
import com.ateneacloud.drive.data.DataManager;
import com.ateneacloud.drive.data.SeafDirent;
import com.ateneacloud.drive.data.SeafDirentTrash;
import com.ateneacloud.drive.data.SeafRepo;
import com.ateneacloud.drive.notification.UploadNotificationProvider;
import com.ateneacloud.drive.transfer.SeafUploadFile;
import com.ateneacloud.drive.transfer.TransferService;
import com.ateneacloud.drive.transfer.TransferService.TransferBinder;
import com.ateneacloud.drive.ui.dialog.StepRecycleBin;
import com.ateneacloud.drive.util.ConcurrentAsyncTask;
import com.ateneacloud.drive.util.Utils;
import com.ateneacloud.drive.util.ValidatesFiles;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class ShareToSeafileActivity extends BaseActivity {
    private static final String DEBUG_TAG = "ShareToSeafileActivity";

    public static final String PASSWORD_DIALOG_FRAGMENT_TAG = "password_dialog_fragment_tag";
    private static final int CHOOSE_COPY_MOVE_DEST_REQUEST = 1;

    private TransferService mTxService;
    private ServiceConnection mConnection;
    private ArrayList<String> localPathList;
    private Intent dstData;
    private Boolean isFinishActivity = false;

    private AccountInfo accountInfo;

    private Boolean isLoadingAccountInfo = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        Bundle extras = intent.getExtras();
        if (extras != null) {
            Object extraStream = extras.get(Intent.EXTRA_STREAM);
            if (localPathList == null) localPathList = Lists.newArrayList();
            loadSharedFiles(extraStream);
        }

    }

    private void loadSharedFiles(Object extraStream) {
        if (extraStream instanceof ArrayList) {
            ConcurrentAsyncTask.execute(new LoadSharedFileTask(),
                    ((ArrayList<Uri>) extraStream).toArray(new Uri[]{}));
        } else if (extraStream instanceof Uri) {
            ConcurrentAsyncTask.execute(new LoadSharedFileTask(),
                    (Uri) extraStream);
        }

    }

    private String getSharedFilePath(Uri uri) {
        if (uri == null) {
            return null;
        }

        if (uri.getScheme().equals("file")) {
            return uri.getPath();
        } else {
            ContentResolver contentResolver = getContentResolver();
            Cursor cursor = contentResolver.query(uri, null, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) {
                return null;
            }
            String filePath = cursor.getString(cursor.getColumnIndex(Images.Media.DATA));
            return filePath;
        }
    }

    class LoadSharedFileTask extends AsyncTask<Uri, Void, File[]> {

        @Override
        protected File[] doInBackground(Uri... uriList) {
            if (uriList == null)
                return null;

            List<File> fileList = new ArrayList<File>();
            for (Uri uri : uriList) {
                InputStream in = null;
                OutputStream out = null;

                try {
                    File tempDir = DataManager.createTempDir();
                    File tempFile = new File(tempDir, Utils.getFilenamefromUri(ShareToSeafileActivity.this, uri));

                    if (!tempFile.createNewFile()) {
                        throw new RuntimeException("could not create temporary file");
                    }

                    in = getContentResolver().openInputStream(uri);
                    out = new FileOutputStream(tempFile);
                    IOUtils.copy(in, out);

                    fileList.add(tempFile);

                } catch (IOException e) {
                    Log.e(DEBUG_TAG, "Could not open requested document", e);
                } catch (RuntimeException e) {
                    Log.e(DEBUG_TAG, "Could not open requested document", e);
                } finally {
                    IOUtils.closeQuietly(in);
                    IOUtils.closeQuietly(out);
                }
            }
            return fileList.toArray(new File[]{});
        }

        @Override
        protected void onPostExecute(File... fileList) {
            for (File file : fileList) {
                if (file == null) {
                    showShortToast(ShareToSeafileActivity.this, R.string.saf_upload_path_not_available);
                } else {
                    localPathList.add(file.getAbsolutePath());
                }
            }

            if (localPathList == null || localPathList.size() == 0) {
                showShortToast(ShareToSeafileActivity.this, R.string.not_supported_share);
                finish();
                return;
            }

            // Log.d(DEBUG_TAG, "share " + localPathList);
            Intent chooserIntent = new Intent(ShareToSeafileActivity.this, SeafilePathChooserActivity.class);
            startActivityForResult(chooserIntent, CHOOSE_COPY_MOVE_DEST_REQUEST);
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(DEBUG_TAG, "onDestroy is called");
        if (mTxService != null) {
            unbindService(mConnection);
            mTxService = null;
        }

        super.onDestroy();
    }

    /**
     * update the file to its latest version to avoid duplicate files
     *
     * @param account
     * @param repoName
     * @param repoID
     * @param targetDir
     * @param localFilePaths
     */
    private void addUpdateTask(Account account, String repoName, String repoID, String targetDir, ArrayList<String> localFilePaths) {

        DataManager dataManager = new DataManager(account);
        accountInfo = null;

        ExecutorService service = Executors.newSingleThreadExecutor();
        service.execute(() -> {
            try {
                accountInfo = dataManager.getAccountInfo();
            } catch (Exception e) {
                e.printStackTrace();
            }
            isLoadingAccountInfo = true;
        });

        while (!isLoadingAccountInfo) {

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }

        for (Iterator<String> i = localFilePaths.iterator(); i.hasNext(); ) {
            String localFilePath = i.next();

            if (accountInfo != null && !ValidatesFiles.isValidFile(this, accountInfo, Uri.fromFile(new File(localFilePath)))) {
                i.remove();
            }
        }

        if (!localFilePaths.isEmpty() && accountInfo != null) {
            bindTransferService(account, repoName, repoID, targetDir, localFilePaths, true);
        }

    }

    /**
     * upload the file, which may lead up to duplicate files
     *
     * @param account
     * @param repoName
     * @param repoID
     * @param targetDir
     * @param localFilePaths
     */
    private void addUploadTask(Account account, String repoName, String repoID, String targetDir, ArrayList<String> localFilePaths) {

        DataManager dataManager = new DataManager(account);
        accountInfo = null;

        ExecutorService service = Executors.newSingleThreadExecutor();
        service.execute(() -> {
            try {
                accountInfo = dataManager.getAccountInfo();
            } catch (Exception e) {
                e.printStackTrace();
            }
            isLoadingAccountInfo = true;
        });

        while (!isLoadingAccountInfo) {

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }

        for (Iterator<String> i = localFilePaths.iterator(); i.hasNext(); ) {
            String localFilePath = i.next();

            if (accountInfo != null && !ValidatesFiles.isValidFile(this, accountInfo, Uri.fromFile(new File(localFilePath)))) {
                i.remove();
            }
        }

        if (!localFilePaths.isEmpty() && accountInfo != null) {
            bindTransferService(account, repoName, repoID, targetDir, localFilePaths, false);
        }

    }

    /**
     * Trying to bind {@link TransferService} in order to upload files after the service was connected
     *
     * @param account
     * @param repoName
     * @param repoID
     * @param targetDir
     * @param localPaths
     * @param update     update the file to avoid duplicates if true,
     *                   upload directly, otherwise.
     */
    private void bindTransferService(final Account account, final String repoName, final String repoID,
                                     final String targetDir, final ArrayList<String> localPaths, final boolean update) {
        // start transfer service
        Intent txIntent = new Intent(this, TransferService.class);
        startService(txIntent);
        Log.d(DEBUG_TAG, "start TransferService");

        // bind transfer service
        Intent bIntent = new Intent(this, TransferService.class);
        final DataManager dataManager = new DataManager(account);

        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                TransferBinder binder = (TransferBinder) service;
                mTxService = binder.getService();
                for (String path : localPaths) {

                    final SeafRepo repo = dataManager.getCachedRepoByID(repoID);
                    if (repo != null && repo.canLocalDecrypt()) {

                        mTxService.addTaskToUploadQueBlock(new SeafUploadFile(account, repoID, repoName, targetDir, path, update, false));
                    } else {

                        mTxService.addUploadTask(new SeafUploadFile(account, repoID, repoName, targetDir, path, update, true));
                    }
                    Log.d(DEBUG_TAG, path + (update ? " updated" : " uploaded"));
                }
                showShortToast(ShareToSeafileActivity.this, R.string.upload_started);
                if (mTxService == null)
                    return;

                if (!mTxService.hasUploadNotifProvider()) {
                    UploadNotificationProvider provider = new UploadNotificationProvider(
                            mTxService.getUploadTaskManager(),
                            mTxService);
                    mTxService.saveUploadNotifProvider(provider);
                }
                finish();
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                mTxService = null;
            }
        };
        bindService(bIntent, mConnection, Context.BIND_AUTO_CREATE);
        Log.d(DEBUG_TAG, "try bind TransferService");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != CHOOSE_COPY_MOVE_DEST_REQUEST) {
            return;
        }

        isFinishActivity = true;

        if (resultCode == RESULT_OK) {
            if (!Utils.isNetworkOn()) {
                showShortToast(this, R.string.network_down);
                return;
            }
            dstData = data;
            String dstRepoId, dstRepoName, dstDir;
            Account account;
            dstRepoName = dstData.getStringExtra(SeafilePathChooserActivity.DATA_REPO_NAME);
            dstRepoId = dstData.getStringExtra(SeafilePathChooserActivity.DATA_REPO_ID);
            dstDir = dstData.getStringExtra(SeafilePathChooserActivity.DATA_DIR);
            account = dstData.getParcelableExtra(SeafilePathChooserActivity.DATA_ACCOUNT);
            notifyTrashOverwriting(account, dstRepoId, dstDir);
            if (!localPathList.isEmpty()) {
                notifyFileOverwriting(account, dstRepoName, dstRepoId, dstDir);
            } else if (isFinishActivity) {
                finish();
            }
            Log.i(DEBUG_TAG, "CHOOSE_COPY_MOVE_DEST_REQUEST returns");
        } else {
            finish();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        /*if (dstData != null) {

            String dstRepoId, dstRepoName, dstDir;
            Account account;
            dstRepoName = dstData.getStringExtra(SeafilePathChooserActivity.DATA_REPO_NAME);
            dstRepoId = dstData.getStringExtra(SeafilePathChooserActivity.DATA_REPO_ID);
            dstDir = dstData.getStringExtra(SeafilePathChooserActivity.DATA_DIR);
            account = (Account)dstData.getParcelableExtra(SeafilePathChooserActivity.DATA_ACCOUNT);
            addUploadTask(account, dstRepoName, dstRepoId, dstDir, localPath);
            Log.d(DEBUG_TAG, "dstRepoName: " + dstRepoName);
            Log.d(DEBUG_TAG, "dstDir: " + dstDir);
        }

        if(isFinishActivity) {
            Log.d(DEBUG_TAG, "finish!");
            isFinishActivity = false;
            finish();
        }*/
    }

    private void notifyTrashOverwriting(final Account account,
                                        final String repoID,
                                        final String targetDir) {

        DataManager dataManager = new DataManager(account);
        AtomicReference<List<SeafDirentTrash>> direntTrashList = new AtomicReference<>(new ArrayList<>());
        List<String> inDirentsTrash = new ArrayList<>();
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message mesg) {
                throw new RuntimeException();
            }

        };

        ExecutorService service = Executors.newSingleThreadExecutor();
        service.execute(() -> {
            try {
                direntTrashList.set(dataManager.getTrashDirentsFromServer(repoID, targetDir));
            } catch (SeafException e) {
                e.printStackTrace();
            }
            handler.sendMessage(handler.obtainMessage());
        });

        try {
            Looper.loop();
        } catch (RuntimeException e) {
        }

        if (!direntTrashList.get().isEmpty()) {
            for (SeafDirentTrash direntTrash : direntTrashList.get()) {

                String direntPath = direntTrash.path.endsWith("/") ? direntTrash.path : direntTrash.path + "/";
                String repoPath = targetDir.endsWith("/") ? targetDir : targetDir + "/";

                if (direntPath.equals(repoPath) && direntTrash.isDir() == false) {
                    for (String path : localPathList) {
                        String nameFile = Utils.fileNameFromPath(path);
                        if (nameFile.equals(direntTrash.getTitle())) {
                            inDirentsTrash.add(path);

                        }
                    }
                }
            }

            if (!inDirentsTrash.isEmpty()) {
                int type = inDirentsTrash.size() > 1 ? StepRecycleBin.MULTIPLE_FILES_IN_TRASH : StepRecycleBin.SINGLE_FILE_IN_TRASH;

                final Handler handler2 = new Handler() {
                    @Override
                    public void handleMessage(Message mesg) {
                        throw new RuntimeException();
                    }
                };

                StepRecycleBin.build(this, type).setOnReplaceTrash(new StepRecycleBin.ReplaceTrashTask() {
                    @Override
                    public void continueTask() {
                        handler2.sendMessage(handler2.obtainMessage());
                    }

                    @Override
                    public void cancelTask() {
                        localPathList.removeAll(inDirentsTrash);
                        handler2.sendMessage(handler2.obtainMessage());
                    }
                }).show();

                try {
                    Looper.loop();
                } catch (RuntimeException e) {
                }
            }
        }
    }

    /**
     * Popup a dialog to notify user if allow to overwrite the file.
     * There are two buttons in the dialog, "Allow duplicate" and "Overwrite".
     * "Allow duplicate" will upload the file again, while "Overwrite" will update (overwrite) the file.
     *
     * @param account
     * @param repoName
     * @param repoID
     * @param targetDir
     */
    private void notifyFileOverwriting(final Account account,
                                       final String repoName,
                                       final String repoID,
                                       final String targetDir) {
        boolean fileExistent = false;
        DataManager dm = new DataManager(account);
        List<SeafDirent> dirents = dm.getCachedDirents(repoID, targetDir);
        if (dirents != null) {
            for (String path : localPathList) {
                for (SeafDirent dirent : dirents) {
                    if (dirent.isDir())
                        continue;
                    if (Utils.fileNameFromPath(path).equals(dirent.getTitle())) {
                        fileExistent = true;
                        break;
                    }
                }
            }

            if (fileExistent) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.overwrite_existing_file_title))
                        .setMessage(getString(R.string.overwrite_existing_file_msg))
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                addUpdateTask(account, repoName, repoID, targetDir, localPathList);
                                if (isFinishActivity) {
                                    Log.d(DEBUG_TAG, "finish!");
                                    finish();
                                }
                            }
                        })
                        .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (isFinishActivity) {
                                    Log.d(DEBUG_TAG, "finish!");
                                    finish();
                                }
                            }
                        })
                        .setNegativeButton(R.string.no,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        addUploadTask(account, repoName, repoID, targetDir, localPathList);
                                        if (isFinishActivity) {
                                            Log.d(DEBUG_TAG, "finish!");
                                            finish();
                                        }
                                    }
                                });
                builder.show();
            } else {
                if (!Utils.isNetworkOn()) {
                    showShortToast(this, R.string.network_down);
                    return;
                }

                // asynchronously check existence of the file from server
                asyncCheckDrientFromServer(account, repoName, repoID, targetDir);
            }
        }
    }

    private void asyncCheckDrientFromServer(Account account,
                                            String repoName,
                                            String repoID,
                                            String targetDir) {

        CheckDirentExistentTask task = new CheckDirentExistentTask(account, repoName, repoID, targetDir);
        ConcurrentAsyncTask.execute(task);
    }

    class CheckDirentExistentTask extends AsyncTask<Void, Void, Void> {

        private Account account;
        private String repoName;
        private String repoID;
        private String targetDir;
        private DataManager dm;
        private boolean fileExistent = false;

        public CheckDirentExistentTask(Account account,
                                       String repoName,
                                       String repoID,
                                       String targetDir) {
            this.account = account;
            this.repoName = repoName;
            this.repoID = repoID;
            this.targetDir = targetDir;
            dm = new DataManager(account);
        }

        @Override
        protected Void doInBackground(Void... params) {
            List<SeafDirent> dirents = null;
            try {
                dirents = dm.getDirentsFromServer(repoID, targetDir);
            } catch (SeafException e) {
                Log.e(DEBUG_TAG, e.getMessage() + e.getCode());
            }
            boolean existent = false;
            if (dirents != null) {
                for (String path : localPathList) {
                    for (SeafDirent dirent : dirents) {
                        if (dirent.isDir())
                            continue;
                        if (Utils.fileNameFromPath(path).equals(dirent.getTitle())) {
                            existent = true;
                            break;
                        }
                    }
                }
            }
            if (!existent)
                // upload the file directly
                addUploadTask(account, repoName, repoID, targetDir, localPathList);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (fileExistent)
                showShortToast(ShareToSeafileActivity.this, R.string.overwrite_existing_file_exist);

            finish();
        }
    }
}
