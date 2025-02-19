package com.ateneacloud.drive.ui.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.constraintlayout.widget.ConstraintLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.common.collect.Lists;
import com.ateneacloud.drive.R;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.ateneacloud.drive.SeafConnection;
import com.ateneacloud.drive.SeafException;
import com.ateneacloud.drive.SettingsManager;
import com.ateneacloud.drive.account.Account;
import com.ateneacloud.drive.account.AccountInfo;
import com.ateneacloud.drive.account.AccountManager;
import com.ateneacloud.drive.account.AccountPlans;
import com.ateneacloud.drive.cameraupload.CameraUploadManager;
import com.ateneacloud.drive.data.CheckUploadServiceEvent;
import com.ateneacloud.drive.data.DataManager;
import com.ateneacloud.drive.data.DatabaseHelper;
import com.ateneacloud.drive.data.SeafDirent;
import com.ateneacloud.drive.data.SeafDirentTrash;
import com.ateneacloud.drive.data.SeafRepo;
import com.ateneacloud.drive.data.SeafRepoTrash;
import com.ateneacloud.drive.data.SeafStarredFile;
import com.ateneacloud.drive.data.ServerInfo;
import com.ateneacloud.drive.data.StorageManager;
import com.ateneacloud.drive.fileschooser.MultiFileChooserActivity;
import com.ateneacloud.drive.folderbackup.FolderBackupService;
import com.ateneacloud.drive.folderbackup.FolderBackupService.FileBackupBinder;
import com.ateneacloud.drive.notification.DownloadNotificationProvider;
import com.ateneacloud.drive.notification.UploadNotificationProvider;
import com.ateneacloud.drive.play.exoplayer.ExoVideoPlayerActivity;
import com.ateneacloud.drive.sync.clock.SyncWorker;
import com.ateneacloud.drive.transfer.DownloadTaskInfo;
import com.ateneacloud.drive.transfer.DownloadTaskManager;
import com.ateneacloud.drive.transfer.PendingUploadInfo;
import com.ateneacloud.drive.transfer.SeafUploadFile;
import com.ateneacloud.drive.transfer.TaskState;
import com.ateneacloud.drive.transfer.TransferManager;
import com.ateneacloud.drive.transfer.TransferService;
import com.ateneacloud.drive.transfer.TransferService.TransferBinder;
import com.ateneacloud.drive.transfer.UploadTaskInfo;
import com.ateneacloud.drive.transfer.UploadTaskManager;
import com.ateneacloud.drive.ui.CopyMoveContext;
import com.ateneacloud.drive.ui.NavContext;
import com.ateneacloud.drive.ui.WidgetUtils;
import com.ateneacloud.drive.ui.activity.search.Search2Activity;
import com.ateneacloud.drive.ui.adapter.SeafItemAdapter;
import com.ateneacloud.drive.ui.dialog.AppChoiceDialog;
import com.ateneacloud.drive.ui.dialog.AppChoiceDialog.CustomAction;
import com.ateneacloud.drive.ui.dialog.ContinueDialog;
import com.ateneacloud.drive.ui.dialog.CopyMoveDialog;
import com.ateneacloud.drive.ui.dialog.DeleteFileDialog;
import com.ateneacloud.drive.ui.dialog.DeleteRepoDialog;
import com.ateneacloud.drive.ui.dialog.FetchFileDialog;
import com.ateneacloud.drive.ui.dialog.NewDirDialog;
import com.ateneacloud.drive.ui.dialog.NewFileDialog;
import com.ateneacloud.drive.ui.dialog.NewRepoDialog;
import com.ateneacloud.drive.ui.dialog.PasswordDialog;
import com.ateneacloud.drive.ui.dialog.RenameFileDialog;
import com.ateneacloud.drive.ui.dialog.RenameRepoDialog;
import com.ateneacloud.drive.ui.dialog.SortFilesDialogFragment;
import com.ateneacloud.drive.ui.dialog.SslConfirmDialog;
import com.ateneacloud.drive.ui.dialog.StepRecycleBin;
import com.ateneacloud.drive.ui.dialog.TaskDialog;
import com.ateneacloud.drive.ui.dialog.UploadChoiceDialog;
import com.ateneacloud.drive.ui.fragment.ReposFragment;
import com.ateneacloud.drive.ui.fragment.StarredFragment;
import com.ateneacloud.drive.ui.dialog.ChangePlanDialog;
import com.ateneacloud.drive.util.ConcurrentAsyncTask;
import com.ateneacloud.drive.ui.dialog.RequestStorageDialog;
import com.ateneacloud.drive.util.FileExtension;
import com.ateneacloud.drive.util.Utils;
import com.ateneacloud.drive.util.UtilsJellyBean;
import com.ateneacloud.drive.util.ValidatesFiles;

import org.apache.commons.io.IOUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class BrowserActivity extends BaseActivity implements ReposFragment.OnFileSelectedListener, StarredFragment.OnStarredFileSelectedListener, FragmentManager.OnBackStackChangedListener, Toolbar.OnMenuItemClickListener, SortFilesDialogFragment.SortItemClickListener {
    private static final String DEBUG_TAG = "BrowserActivity";
    public static final String ACTIONBAR_PARENT_PATH = "/";

    public static final String OPEN_FILE_DIALOG_FRAGMENT_TAG = "openfile_fragment";
    public static final String PASSWORD_DIALOG_FRAGMENT_TAG = "password_fragment";
    public static final String CHOOSE_APP_DIALOG_FRAGMENT_TAG = "choose_app_fragment";
    public static final String CHARE_LINK_PASSWORD_FRAGMENT_TAG = "share_link_password_fragment";
    public static final String PICK_FILE_DIALOG_FRAGMENT_TAG = "pick_file_fragment";
    public static final int REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 1;

    public static final String TAG_NEW_REPO_DIALOG_FRAGMENT = "NewRepoDialogFragment";
    public static final String TAG_DELETE_REPO_DIALOG_FRAGMENT = "DeleteRepoDialogFragment";
    public static final String TAG_DELETE_FILE_DIALOG_FRAGMENT = "DeleteFileDialogFragment";
    public static final String TAG_DELETE_FILES_DIALOG_FRAGMENT = "DeleteFilesDialogFragment";
    public static final String TAG_RENAME_REPO_DIALOG_FRAGMENT = "RenameRepoDialogFragment";
    public static final String TAG_RENAME_FILE_DIALOG_FRAGMENT = "RenameFileDialogFragment";
    public static final String TAG_COPY_MOVE_DIALOG_FRAGMENT = "CopyMoveDialogFragment";
    public static final String TAG_SORT_FILES_DIALOG_FRAGMENT = "SortFilesDialogFragment";

    public static final int INDEX_LIBRARY_TAB = 0;
    public static final int INDEX_STARRED_TAB = 1;
    public static final int INDEX_TRASH_TAB = 2;

    private static final int[] ICONS = new int[]{R.drawable.tab_library, R.drawable.tab_starred, R.drawable.tab_activity};

    private FetchFileDialog fetchFileDialog = null;
    //private SeafileTabsAdapter adapter;
    private View mLayout;
    private FrameLayout mContainer;
    private TabLayout mTabLayout;
    //private ViewPager mViewPager;
    private ConstraintLayout constraintLoadingContainer;
    private NavContext navContext = new NavContext();
    private CopyMoveContext copyMoveContext;
    private Menu overFlowMenu;
    private MenuItem menuSearch;

    private DataManager dataManager = null;
    private TransferService txService = null;
    private FolderBackupService mFolderBackupService = null;
    private TransferReceiver mTransferReceiver;
    private AccountManager accountManager;

    private int currentPosition = 0;
    private Intent copyMoveIntent;
    private Account account;

    private AccountInfo accountInfo;

    private Intent mediaObserver;
    private Intent monitorIntent;

    private BrowserActivity mActivity;

    private ReposFragment reposFragment;
    private StarredFragment starredFragment;

    private boolean creatingFiles;

    public DataManager getDataManager() {
        return dataManager;
    }

    public void addUpdateTask(String repoID, String repoName, String targetDir, String localFilePath) {
        if (txService != null) {
            txService.addTaskToUploadQue(new SeafUploadFile(account, repoID, repoName, targetDir, localFilePath, true, true));
        } else {
            PendingUploadInfo info = new PendingUploadInfo(repoID, repoName, targetDir, localFilePath, true, true);
            pendingUploads.add(info);
        }
    }

    public void addUpdateBlocksTask(String repoID, String repoName, String targetDir, String localFilePath) {
        if (txService != null) {
            txService.addTaskToUploadQueBlock(new SeafUploadFile(account, repoID, repoName, targetDir, localFilePath, true, true));
        } else {
            PendingUploadInfo info = new PendingUploadInfo(repoID, repoName, targetDir, localFilePath, true, true);
            pendingUploads.add(info);
        }
    }

    private int addUploadTask(String repoID, String repoName, String targetDir, String localFilePath) {
        if (txService != null) {
            return txService.addTaskToUploadQue(new SeafUploadFile(account, repoID, repoName, targetDir, localFilePath, false, true));
        } else {
            PendingUploadInfo info = new PendingUploadInfo(repoID, repoName, targetDir, localFilePath, false, true);
            pendingUploads.add(info);
            return 0;
        }
    }

    private int addUploadBlocksTask(String repoID, String repoName, String targetDir, String localFilePath) {
        if (txService != null) {
            return txService.addTaskToUploadQueBlock(new SeafUploadFile(account, repoID, repoName, targetDir, localFilePath, false, true));
        } else {
            PendingUploadInfo info = new PendingUploadInfo(repoID, repoName, targetDir, localFilePath, false, true);
            pendingUploads.add(info);
            return 0;
        }
    }

    private ArrayList<PendingUploadInfo> pendingUploads = Lists.newArrayList();

    public TransferService getTransferService() {
        return txService;
    }

    public Account getAccount() {
        return account;
    }

    public NavContext getNavContext() {
        return navContext;
    }

    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, "Permiso para notificaciones denegado", Toast.LENGTH_SHORT).show();
                }
            });


    private void askNotificationPermission() {
        // Solo necesitamos solicitar permiso si estamos en Android 13 o superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                Toast.makeText(this, "Activa las notificaciones para recibir alertas importantes.", Toast.LENGTH_LONG).show();
            }
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);

        mActivity = this;

        accountManager = new AccountManager(this);

        creatingFiles = false;

        askNotificationPermission();

        // restart service should it have been stopped for some reason
//        Intent mediaObserver = new Intent(this, MediaObserverService.class);
//        startService(mediaObserver);

//        Intent dIntent = new Intent(this, FolderBackupService.class);
//        startService(dIntent);
//        Log.d(DEBUG_TAG, "----start FolderBackupService");
//
//        Intent dirIntent = new Intent(this, FolderBackupService.class);
//        bindService(dirIntent, folderBackupConnection, Context.BIND_AUTO_CREATE);
//        Log.d(DEBUG_TAG, "----try bind FolderBackupService");

        if (!isTaskRoot()) {
            final Intent intent = getIntent();
            final String intentAction = getIntent().getAction();
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && intentAction != null && intentAction.equals(Intent.ACTION_MAIN)) {
                finish();
                return;
            }
        }

        setContentView(R.layout.tabs_main);
        mLayout = findViewById(R.id.main_layout);
        mContainer = (FrameLayout) findViewById(R.id.bottom_sheet_container);
        setSupportActionBar(getActionBarToolbar());
        // enable ActionBar app icon to behave as action back
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBarToolbar().setTitle("");

        findViewById(R.id.view_toolbar_bottom_line).setVisibility(View.GONE);
        // Get the message from the intent
        Intent intent = getIntent();


        account = accountManager.getCurrentAccount();


        if (account == null || !account.hasValidToken()) {
            finishAndStartAccountsActivity();
            return;
        }


        // Log.d(DEBUG_TAG, "browser activity onCreate " + account.server + " " + account.email);
        dataManager = new DataManager(account);

        getSupportFragmentManager().addOnBackStackChangedListener(this);

        unsetRefreshing();
        disableUpButton();

        reposFragment = new ReposFragment();
        starredFragment = new StarredFragment();
        // Otros fragmentos...

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .add(R.id.frameLayoutContainer, reposFragment, "reposFragment")
                .show(reposFragment)
                .add(R.id.frameLayoutContainer, starredFragment, "starredFragment")
                .hide(starredFragment)
                .commit();

        mTabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        mTabLayout.addTab(mTabLayout.newTab().setText(getResources().getString(R.string.tabs_library).toUpperCase()));
        mTabLayout.addTab(mTabLayout.newTab().setText(getResources().getString(R.string.tabs_starred).toUpperCase()));
        mTabLayout.addTab(mTabLayout.newTab().setText(getResources().getString(R.string.recycle_bin).toUpperCase()));
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                mActivity.setCurrentPosition(tab.getPosition());
                switch (tab.getPosition()) {
                    case INDEX_LIBRARY_TAB:
                        if (navContext.inRepo() && navContext.inDirentsTrash()) {
                            SeafRepo repo = dataManager.getCachedRepoByID(getNavContext().getRepoID());
                            String parentPath = navContext.getDirNavigateRoot();
                            navContext.setDir(parentPath, null);
                            navContext.setDirentsTrash(false);
                            navContext.setNavigateToDirentsTrash(false);
                            navContext.setDirNavigatePath(null);
                            navContext.setDirCommitID(null);
                            mActivity.setUpButtonTitle(repo.getName());
                            getReposFragment().clearAdapterData();
                            getReposFragment().refreshView(true, true);
                        } else if (navContext.inRepoTrash()) {
                            navContext.setRepoID(null);
                            navContext.setRepoTrash(false);
                            getActionBarToolbar().setTitle("");
                            getReposFragment().clearAdapterData();
                            getReposFragment().refreshView(true, true);
                        }

                        transaction.show(reposFragment).hide(starredFragment);
                        break;
                    case INDEX_STARRED_TAB:
                        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                        transaction.hide(reposFragment).show(starredFragment);
                        break;
                    case INDEX_TRASH_TAB:
                        if (navContext.inRepo() && !navContext.inDirentsTrash()) {
                            recoveringDirentsTrash();
                            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                        } else if (!navContext.inRepo() && !navContext.inRepoTrash()) {
                            recoveringReposTrash();
                            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                        }
                        transaction.show(reposFragment).hide(starredFragment);
                        break;
                }
                //refresh menu
                //ESTA LINEA ES LA IMPORTANTE
                invalidateOptionsMenu();
                cleanActionMode();
                transaction.commit();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }

        });

        if (savedInstanceState != null) {
            Log.d(DEBUG_TAG, "savedInstanceState is not null");
            fetchFileDialog = (FetchFileDialog) getSupportFragmentManager().findFragmentByTag(OPEN_FILE_DIALOG_FRAGMENT_TAG);

            AppChoiceDialog appChoiceDialog = (AppChoiceDialog) getSupportFragmentManager().findFragmentByTag(CHOOSE_APP_DIALOG_FRAGMENT_TAG);

            if (appChoiceDialog != null) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.detach(appChoiceDialog);
                ft.commit();
            }

            SslConfirmDialog sslConfirmDlg = (SslConfirmDialog) getSupportFragmentManager().findFragmentByTag(SslConfirmDialog.FRAGMENT_TAG);

            if (sslConfirmDlg != null) {
                Log.d(DEBUG_TAG, "sslConfirmDlg is not null");
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.detach(sslConfirmDlg);
                ft.commit();
            } else {
                Log.d(DEBUG_TAG, "sslConfirmDlg is null");
            }

            String repoID = savedInstanceState.getString("repoID");
            String repoName = savedInstanceState.getString("repoName");
            String path = savedInstanceState.getString("path");
            String dirID = savedInstanceState.getString("dirID");
            String permission = savedInstanceState.getString("permission");
            boolean inRepoTrash = savedInstanceState.getBoolean("inRepoTrash", false);
            boolean inDirentTrash = savedInstanceState.getBoolean("inDirentTrash", false);
            if (repoID != null) {
                navContext.setRepoID(repoID);
                navContext.setRepoName(repoName);
                navContext.setDir(path, dirID);
                navContext.setDirPermission(permission);
                navContext.setDirentsTrash(inDirentTrash);
            }
            navContext.setRepoTrash(inRepoTrash);
        }

        String repoID = intent.getStringExtra("repoID");
        String repoName = intent.getStringExtra("repoName");
        String path = intent.getStringExtra("path");
        String dirID = intent.getStringExtra("dirID");
        String permission = intent.getStringExtra("permission");
        if (repoID != null) {
            navContext.setRepoID(repoID);
            navContext.setRepoName(repoName);
            navContext.setDir(path, dirID);
            navContext.setDirPermission(permission);
        }

        Intent txIntent = new Intent(this, TransferService.class);
        startService(txIntent);
        Log.d(DEBUG_TAG, "start TransferService");

        // bind transfer service
        Intent bIntent = new Intent(this, TransferService.class);
        bindService(bIntent, mConnection, Context.BIND_AUTO_CREATE);
        Log.d(DEBUG_TAG, "try bind TransferService");

//        monitorIntent = new Intent(this, FileMonitorService.class);
//        startService(monitorIntent);

        constraintLoadingContainer = findViewById(R.id.constraint_loading_container);

        requestServerInfo();

        //requestReadExternalStoragePermission();
        //Utils.startCameraSyncJob(this);
        //syncCamera();
        //checkManagerStoragePermission();
        syncCamera();
        loadAccountInfo();

        Utils.migrateSyncs(dataManager.getAccount());

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .build();

        WorkManager.getInstance(this).enqueue(workRequest);

    }

    private void cleanActionMode() {
        if (getReposFragment() != null) {
            getReposFragment().clearActionMode();
        }
        if (getStarredFragment() != null) {
            getStarredFragment().clearActionMode();
        }
    }


    public FrameLayout getmContainer() {
        return mContainer;
    }

    private void finishAndStartAccountsActivity() {
        Intent newIntent = new Intent(this, AccountsActivity.class);
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        finish();
        startActivity(newIntent);
    }

    private void requestServerInfo() {
        if (!checkServerProEdition()) {
            /*adapter.notifyDataSetChanged();
            mTabLayout.setupWithViewPager(mViewPager);*/
        }

        if (!checkSearchEnabled()) {
            // hide search menu
            if (menuSearch != null) menuSearch.setVisible(false);
        }

        if (!Utils.isNetworkOn()) return;

        ConcurrentAsyncTask.execute(new RequestServerInfoTask());
    }

    public void completeRemoteWipe() {
        ConcurrentAsyncTask.execute(new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... objects) {
                // clear local caches
                StorageManager storageManager = StorageManager.getInstance();
                storageManager.clearCache();

                // clear cached data from database
                DatabaseHelper dbHelper = DatabaseHelper.getDatabaseHelper();
                dbHelper.delCaches();

                try {
                    // response to server when finished cleaning caches
                    getDataManager().completeRemoteWipe();
                } catch (SeafException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void o) {
                // sign out current account
                logoutWhenTokenExpired();

            }
        });
    }

    /**
     * Token expired, clear current authorized info and redirect user to login page
     */
    public void logoutWhenTokenExpired() {
        AccountManager accountMgr = new AccountManager(this);

        // sign out current account
        Account account = accountMgr.getCurrentAccount();
        accountMgr.signOutAccount(account);

        // then redirect to AccountsActivity
        Intent intent = new Intent(this, AccountsActivity.class);
        startActivity(intent);

        // finish current Activity
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (navContext.inRepo() && currentPosition == INDEX_LIBRARY_TAB) {
                    onBackPressed();
                } else if (navContext.inRepoTrash() || navContext.inDirentsTrash()) {
                    onBackPressed();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sort:
                showSortFilesDialog();
                return true;
            case R.id.search:
                Intent searchIntent = new Intent(this, Search2Activity.class);
                startActivity(searchIntent);
                return true;
            case R.id.create_repo:
                showNewRepoDialog();
                return true;
            case R.id.clear_trash_repo:
                cleanTrashRepo();
                return true;
            case R.id.add:
                addFile();
                return true;
            case R.id.transfer_tasks:
                Intent newIntent = new Intent(this, TransferActivity.class);
                newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(newIntent);
                return true;
            case R.id.accounts:
                newIntent = new Intent(this, AccountsActivity.class);
                newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(newIntent);
                return true;
            case R.id.edit:
                // start action mode for selecting multiple files/folders

                if (!Utils.isNetworkOn()) {
                    showShortToast(this, R.string.network_down);
                    return true;
                }
                if (currentPosition == INDEX_LIBRARY_TAB) {
                    if (navContext.inRepo()) {
                        SeafRepo repo = dataManager.getCachedRepoByID(navContext.getRepoID());
                        if (repo.encrypted && !dataManager.getRepoPasswordSet(repo.id)) {
                            String password = dataManager.getRepoPassword(repo.id);
                            showPasswordDialog(repo.name, repo.id, new TaskDialog.TaskDialogListener() {
                                @Override
                                public void onTaskSuccess() {
                                    getReposFragment().startContextualActionMode();
                                }
                            }, password);

                            return true;
                        }
                    }

                    getReposFragment().startContextualActionMode();
                }

                return true;
            case R.id.settings:
                Intent settingsIntent = new Intent(BrowserActivity.this, SettingsActivity.class);
                settingsIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(settingsIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * If the user is running Android 6.0 (API level 23) or later, the user has to grant your app its permissions while they are running the app
     * <p>
     * Requests the WRITE_EXTERNAL_STORAGE permission.
     * If the permission has been denied previously, a SnackBar will prompt the user to grant the
     * permission, otherwise it is requested directly.
     */
    private void requestReadExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                Snackbar.make(mLayout, R.string.permission_read_exteral_storage_rationale, Snackbar.LENGTH_INDEFINITE).setAction(R.string.settings, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ActivityCompat.requestPermissions(BrowserActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
                    }
                }).show();

            } else {

                // No explanation needed, we can request the permission.

                // WRITE_EXTERNAL_STORAGE permission has not been granted yet. Request it directly.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        // Log.i(DEBUG_TAG, "Received response for permission request.");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE: {
                // Check if the only required permission has been granted
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                } else {
                    // permission denied
                }
            }
        }
    }


    class RequestServerInfoTask extends AsyncTask<Void, Void, ServerInfo> {
        private SeafException err;

        @Override
        protected ServerInfo doInBackground(Void... params) {
            try {
                return dataManager.getServerInfo();
            } catch (SeafException e) {
                err = e;
            } catch (JSONException e) {
                Log.e(DEBUG_TAG, "JSONException " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(ServerInfo serverInfo) {
            // Check to see whether this activity is in the process of finishing
            // to avoid IllegalStateException when AsyncTasks continue to run after the activity has been destroyed
            // http://stackoverflow.com/a/35729068/3962551
            if (isFinishing()) return;

            if (serverInfo == null) {
                if (err != null) showShortToast(BrowserActivity.this, err.getMessage());
                return;
            }

            if (serverInfo.isProEdition()) {
                /*adapter.notifyDataSetChanged();
                mTabLayout.setupWithViewPager(mViewPager);*/
            }

            if (serverInfo.isSearchEnabled()) {
                // show search menu
                if (menuSearch != null) menuSearch.setVisible(true);
            }

            accountManager.setServerInfo(account, serverInfo);
        }
    }

    /**
     * check if server is pro edition
     *
     * @return true, if server is pro edition
     * false, otherwise.
     */
    private boolean checkServerProEdition() {
        if (account == null) return false;

        ServerInfo serverInfo = accountManager.getServerInfo(account);

        return serverInfo.isProEdition();
    }

    /**
     * check if server supports searching feature
     *
     * @return true, if search enabled
     * false, otherwise.
     */
    private boolean checkSearchEnabled() {
        if (account == null) return false;

        ServerInfo serverInfo = accountManager.getServerInfo(account);

        return serverInfo.isSearchEnabled();
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(int currentPosition) {
        this.currentPosition = currentPosition;
        mTabLayout.setScrollPosition(currentPosition, 0, true);
        setUpButtonTitleOnSlideTabs(currentPosition);
        //refreshViewOnSlideTabs(currentPosition);
    }


    public ReposFragment getReposFragment() {
        return (ReposFragment) reposFragment;
    }

    public StarredFragment getStarredFragment() {
        return (StarredFragment) starredFragment;
    }

    public ReposFragment getActivitiesFragment() {
        return (ReposFragment) reposFragment;
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            TransferBinder binder = (TransferBinder) service;
            txService = binder.getService();
            Log.d(DEBUG_TAG, "bind TransferService");

            for (PendingUploadInfo info : pendingUploads) {

                txService.addTaskToUploadQue(
                        new SeafUploadFile(
                                account,
                                info.repoID,
                                info.repoName,
                                info.targetDir,
                                info.localFilePath,
                                info.isUpdate,
                                info.isCopyToLocal));
            }
            pendingUploads.clear();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            txService = null;
        }
    };

    private final ServiceConnection folderBackupConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            FileBackupBinder binder = (FileBackupBinder) service;
            mFolderBackupService = binder.getService();
            Log.d(DEBUG_TAG, "-----bind FolderBackupService");
            boolean dirAutomaticUpload = SettingsManager.instance().isFolderAutomaticBackup();
            String backupEmail = SettingsManager.instance().getBackupEmail();
            if (dirAutomaticUpload && mFolderBackupService != null && !TextUtils.isEmpty(backupEmail)) {
                mFolderBackupService.backupFolder(backupEmail);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mFolderBackupService = null;
        }
    };

    @Override
    public void onStart() {
        Log.d(DEBUG_TAG, "onStart");
        super.onStart();
        EventBus.getDefault().register(this);

        if (android.os.Build.VERSION.SDK_INT < 14 && SettingsManager.instance().isGestureLockRequired()) {
            Intent intent = new Intent(this, UnlockGesturePasswordActivity.class);
            startActivity(intent);
        }

        if (mTransferReceiver == null) {
            mTransferReceiver = new TransferReceiver();
        }

        IntentFilter filter = new IntentFilter(TransferManager.BROADCAST_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(mTransferReceiver, filter);
    }

    @Override
    protected void onPause() {
        Log.d(DEBUG_TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onRestart() {
        Log.d(DEBUG_TAG, "onRestart");
        super.onRestart();

        if (accountManager.getCurrentAccount() == null
                || !accountManager.getCurrentAccount().equals(this.account)
                || !accountManager.getCurrentAccount().getToken().equals(this.account.getToken())) {
            finishAndStartAccountsActivity();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(DEBUG_TAG, "onNewIntent");

        // if the user started the Seadroid app from the Launcher, keep the old Activity
        final String intentAction = intent.getAction();
        if (intent.hasCategory(Intent.CATEGORY_LAUNCHER)
                && intentAction != null
                && intentAction.equals(Intent.ACTION_MAIN)) {
            return;
        }

        Account selectedAccount = accountManager.getCurrentAccount();
        Log.d(DEBUG_TAG, "Current account: " + selectedAccount);
        if (selectedAccount == null
                || !account.equals(selectedAccount)
                || !account.getToken().equals(selectedAccount.getToken())) {
            Log.d(DEBUG_TAG, "Account switched, restarting activity.");
            finish();
            startActivity(intent);
        }
    }

    @Override
    protected void onStop() {
        Log.d(DEBUG_TAG, "onStop");
        super.onStop();
        EventBus.getDefault().unregister(this);

        if (mTransferReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mTransferReceiver);
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(DEBUG_TAG, "onDestroy is called");

        if (txService != null) {
            unbindService(mConnection);
            txService = null;
        }
        if (mFolderBackupService != null) {
            unbindService(folderBackupConnection);
            mFolderBackupService = null;
        }
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
        //outState.putInt("tab", getActionBarToolbar().getSelectedNavigationIndex());
        if (navContext.getRepoID() != null) {
            outState.putString("repoID", navContext.getRepoID());
            outState.putString("repoName", navContext.getRepoName());
            outState.putString("path", navContext.getDirPath());
            outState.putString("dirID", navContext.getDirID());
            outState.putString("permission", navContext.getDirPermission());
            outState.putBoolean("inDirentTrash", navContext.inDirentsTrash());
        } else if (navContext.inRepoTrash()) {
            outState.putBoolean("inRepoTrash", navContext.inRepoTrash());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        overFlowMenu = menu;
        Toolbar toolbar = getActionBarToolbar();
        toolbar.inflateMenu(R.menu.browser_menu);
        toolbar.setOnMenuItemClickListener(this);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menuSearch = menu.findItem(R.id.search);
        MenuItem menuSort = menu.findItem(R.id.sort);
        MenuItem menuAdd = menu.findItem(R.id.add);
        MenuItem menuCreateRepo = menu.findItem(R.id.create_repo);
        MenuItem menuEdit = menu.findItem(R.id.edit);
        MenuItem menuCleanTrashRepo = menu.findItem(R.id.clear_trash_repo);

        // Libraries Tab
        if (currentPosition == INDEX_LIBRARY_TAB) {
            if (navContext.inRepo()) {

                if (navContext.inDirentsTrash()) {
                    menuCreateRepo.setVisible(false);
                    menuAdd.setVisible(false);
                    menuEdit.setVisible(false);
                    menuAdd.setEnabled(false);
                    menuEdit.setEnabled(false);
                    if (navContext.isNavigateToDirentsTrash() || !navContext.getDirPath().equals("/")) {
                        menuCleanTrashRepo.setVisible(false);
                    } else {
                        menuCleanTrashRepo.setVisible(true);
                    }
                } else {
                    menuCreateRepo.setVisible(false);
                    menuAdd.setVisible(true);
                    menuEdit.setVisible(true);
                    menuCleanTrashRepo.setVisible(false);
                    if (hasRepoWritePermission()) {
                        menuAdd.setEnabled(true);
                        menuEdit.setEnabled(true);
                    } else {
                        menuAdd.setEnabled(false);
                        menuEdit.setEnabled(false);
                    }

                    SeafRepo repo = dataManager.getCachedRepoByID(navContext.getRepoID());
                    if (repo.isSharedRepo) {
                        menuAdd.setEnabled(false);
                        menuEdit.setEnabled(false);
                    }
                }


                getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            } else if (navContext.inRepoTrash()) {
                menuAdd.setVisible(false);
                menuEdit.setVisible(false);
                menuCreateRepo.setVisible(false);
                menuCleanTrashRepo.setVisible(false);
            } else {
                menuCreateRepo.setVisible(true);
                menuAdd.setVisible(false);
                menuEdit.setVisible(false);
                menuCleanTrashRepo.setVisible(false);
            }


        } else if (currentPosition == INDEX_TRASH_TAB) {
            menuSort.setVisible(false);
            menuCreateRepo.setVisible(false);
            menuAdd.setVisible(false);
            menuEdit.setVisible(false);
            menuCleanTrashRepo.setVisible(false);

            if(navContext.isNavigateToDirentsTrash()){
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            if (navContext.inDirentsTrash() && !navContext.isNavigateToDirentsTrash()) {
                menuCleanTrashRepo.setVisible(true);
            }

        } else {
            menuSort.setVisible(false);
            menuCreateRepo.setVisible(false);
            menuAdd.setVisible(false);
            menuEdit.setVisible(false);
            menuCleanTrashRepo.setVisible(false);
        }

        // Global menus, e.g. Accounts, TransferTasks, Settings, are visible by default.
        // So nothing need to be done here.

        // Though search menu is also a global menu, its state was maintained dynamically at runtime.
        if (!checkServerProEdition()) menuSearch.setVisible(false);

        return true;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        // We can't show the CopyMoveDialog in onActivityResult, this is a
        // workaround found in
        // http://stackoverflow.com/questions/16265733/failure-delivering-result-onactivityforresult/18345899#18345899
        if (copyMoveIntent != null) {
            String dstRepoId, dstDir;
            dstRepoId = copyMoveIntent.getStringExtra(SeafilePathChooserActivity.DATA_REPO_ID);
            dstDir = copyMoveIntent.getStringExtra(SeafilePathChooserActivity.DATA_DIR);
            copyMoveContext.setDest(dstRepoId, dstDir);
            doCopyMove();
            copyMoveIntent = null;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void showSortFilesDialog() {
        SortFilesDialogFragment dialog = new SortFilesDialogFragment();
        dialog.show(getSupportFragmentManager(), TAG_SORT_FILES_DIALOG_FRAGMENT);
    }

    @Override
    public void onSortFileItemClick(DialogFragment dialog, int position) {
        switch (position) {
            case 0: // sort by name, ascending
                sortFiles(SeafItemAdapter.SORT_BY_NAME, SeafItemAdapter.SORT_ORDER_ASCENDING);
                break;
            case 1: // sort by name, descending
                sortFiles(SeafItemAdapter.SORT_BY_NAME, SeafItemAdapter.SORT_ORDER_DESCENDING);
                break;
            case 2: // sort by last modified time, ascending
                sortFiles(SeafItemAdapter.SORT_BY_LAST_MODIFIED_TIME, SeafItemAdapter.SORT_ORDER_ASCENDING);
                break;
            case 3: // sort by last modified time, descending
                sortFiles(SeafItemAdapter.SORT_BY_LAST_MODIFIED_TIME, SeafItemAdapter.SORT_ORDER_DESCENDING);
                break;
            default:
                return;
        }
    }

    /**
     * Sort files by type and order
     *
     * @param type
     */
    private void sortFiles(final int type, final int order) {
        if (currentPosition == INDEX_LIBRARY_TAB) {
            if (navContext.inRepo()) {
                SeafRepo repo = dataManager.getCachedRepoByID(navContext.getRepoID());
                if (repo.encrypted && !dataManager.getRepoPasswordSet(repo.id)) {
                    String password = dataManager.getRepoPassword(repo.id);
                    showPasswordDialog(repo.name, repo.id, new TaskDialog.TaskDialogListener() {
                        @Override
                        public void onTaskSuccess() {
                            getReposFragment().sortFiles(type, order);
                        }
                    }, password);
                }
            }
            getReposFragment().sortFiles(type, order);
        }
    }

    /**
     * create a new repo
     */
    private void showNewRepoDialog() {
        final NewRepoDialog dialog = new NewRepoDialog();
        dialog.init(account);
        dialog.setTaskDialogLisenter(new TaskDialog.TaskDialogListener() {
            @Override
            public void onTaskSuccess() {
                showShortToast(BrowserActivity.this, String.format(getResources().getString(R.string.create_new_repo_success), dialog.getRepoName()));
                ReposFragment reposFragment = getReposFragment();
                if (currentPosition == INDEX_LIBRARY_TAB && reposFragment != null) {
                    reposFragment.refreshView(true, true);
                }
            }
        });
        dialog.show(getSupportFragmentManager(), TAG_NEW_REPO_DIALOG_FRAGMENT);
    }

    /**
     * add new file/files
     */
    private void addFile() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.add_file));
        builder.setItems(R.array.add_file_options_array, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) // create file
                    showNewFileDialog();
                else if (which == 1) // create folder
                    showNewDirDialog();
                else if (which == 2) // upload file
                    pickFile();
                else if (which == 3) // take a photo
                    cameraTakePhoto();
            }
        }).show();
    }

    private void showNewDirDialog() {
        if (!creatingFiles) {
            creatingFiles = true;
            ExecutorService service = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            service.execute(() -> {

                List<SeafDirentTrash> direntTrashList = new ArrayList<>();
                try {
                    direntTrashList = dataManager.getTrashDirentsFromServer(navContext.getRepoID(), navContext.getDirPath());
                } catch (SeafException e) {
                    e.printStackTrace();
                }

                List<SeafDirentTrash> finalDirentTrashList = direntTrashList;
                handler.post(() -> {
                    if (!hasRepoWritePermission()) {
                        showShortToast(this, R.string.library_read_only);
                        return;
                    }

                    final NewDirDialog dialog = new NewDirDialog();
                    dialog.init(navContext.getRepoID(), navContext.getDirPath(), account, finalDirentTrashList);
                    dialog.setTaskDialogLisenter(new TaskDialog.TaskDialogListener() {
                        @Override
                        public void onTaskSuccess() {
                            if (dialog.isRunTask()) {
                                final String message = String.format(getString(R.string.create_new_folder_success), dialog.getNewDirName());
                                showShortToast(BrowserActivity.this, message);
                                ReposFragment reposFragment = getReposFragment();
                                if (currentPosition == INDEX_LIBRARY_TAB && reposFragment != null) {
                                    reposFragment.refreshView();
                                }
                            }
                            creatingFiles = false;
                        }

                        @Override
                        public void onTaskFailed(SeafException e) {
                            creatingFiles = false;

                        }

                        @Override
                        public void onTaskCancelled() {
                            creatingFiles = false;
                        }
                    });
                    dialog.show(getSupportFragmentManager(), "NewDirDialogFragment");
                });

            });

        }
    }


    private void showNewFileDialog() {
        if (!creatingFiles) {
            creatingFiles = true;
            ExecutorService service = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            service.execute(() -> {

                List<SeafDirentTrash> direntTrashList = new ArrayList<>();
                try {
                    direntTrashList = dataManager.getTrashDirentsFromServer(navContext.getRepoID(), navContext.getDirPath());
                } catch (SeafException e) {
                    e.printStackTrace();
                }

                List<SeafDirentTrash> finalDirentTrashList = direntTrashList;
                handler.post(() -> {
                    if (!hasRepoWritePermission()) {
                        showShortToast(this, R.string.library_read_only);
                        return;
                    }

                    final NewFileDialog dialog = new NewFileDialog();
                    dialog.init(navContext.getRepoID(), navContext.getDirPath(), account, finalDirentTrashList);
                    dialog.setTaskDialogLisenter(new TaskDialog.TaskDialogListener() {
                        @Override
                        public void onTaskSuccess() {
                            final String message = String.format(getString(R.string.create_new_file_success), dialog.getNewFileName());
                            if (dialog.isRunTask()) {
                                showShortToast(BrowserActivity.this, message);
                                ReposFragment reposFragment = getReposFragment();
                                if (currentPosition == INDEX_LIBRARY_TAB && reposFragment != null) {
                                    reposFragment.refreshView();
                                }
                            }
                            creatingFiles = false;
                        }

                        @Override
                        public void onTaskFailed(SeafException e) {
                            creatingFiles = false;
                        }

                        @Override
                        public void onTaskCancelled() {
                            creatingFiles = false;
                        }
                    });
                    dialog.show(getSupportFragmentManager(), "NewFileDialogFragment");
                });
            });
        }
    }

    public void setRefreshing() {
        setSupportProgressBarIndeterminateVisibility(Boolean.TRUE);
    }

    public void unsetRefreshing() {
        setSupportProgressBarIndeterminateVisibility(Boolean.FALSE);
    }

    private File takeCameraPhotoTempFile;

    private void cameraTakePhoto() {
        Intent imageCaptureIntent = new Intent("android.media.action.IMAGE_CAPTURE");

        try {
            File ImgDir = DataManager.createTempDir();

            String fileName = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".jpg";
            takeCameraPhotoTempFile = new File(ImgDir, fileName);

            Uri photo = null;
            if (android.os.Build.VERSION.SDK_INT > 23) {
                photo = FileProvider.getUriForFile(this, getApplicationContext().getPackageName(), takeCameraPhotoTempFile);
            } else {
                photo = Uri.fromFile(takeCameraPhotoTempFile);
            }
            imageCaptureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photo);
            startActivityForResult(imageCaptureIntent, TAKE_PHOTO_REQUEST);

        } catch (IOException e) {
            showShortToast(BrowserActivity.this, R.string.unknow_error);
        }
    }

    public void enableUpButton() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setSupportActionBar(getActionBarToolbar());
        //getActionBarToolbar().setLogo(getResources().getDrawable(R.color.transparent));
    }

    public void disableUpButton() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        //getActionBarToolbar().setEnabled(false);
        //getActionBarToolbar().setLogo(R.drawable.icon);
    }

    public void setUpButtonTitle(String title) {
        getActionBarToolbar().setTitle(title);
    }

    /**
     * update up button title when sliding among tabs
     *
     * @param position
     */
    private void setUpButtonTitleOnSlideTabs(int position) {
        if (navContext == null) return;

        if (position == INDEX_LIBRARY_TAB) {
            if (navContext.inRepo()) {

                if (navContext.inDirentsTrash()) {
                    setUpButtonTitle(getResources().getString(R.string.deleted_files).toUpperCase());
                } else {
                    if (navContext.getDirPath().equals(BrowserActivity.ACTIONBAR_PARENT_PATH)) {
                        setUpButtonTitle(navContext.getRepoName());
                    } else {
                        setUpButtonTitle(navContext.getDirPath().substring(navContext.getDirPath().lastIndexOf(BrowserActivity.ACTIONBAR_PARENT_PATH) + 1));
                    }
                }
            } else if (navContext.inRepoTrash()) {
                setUpButtonTitle(getResources().getString(R.string.deleted_repositories).toUpperCase());
            } else setUpButtonTitle(getString(R.string.tabs_library).toUpperCase());
        } else {
            setUpButtonTitle(currentPosition == INDEX_STARRED_TAB ?
                    getString(R.string.tabs_starred).toUpperCase() :
                    (navContext.inRepo() ?
                            getString(R.string.deleted_files) :
                            getString(R.string.deleted_repositories)).toUpperCase());
        }

    }

    /**
     * refresh view when sliding among tabs
     *
     * @param position
     */
    private void refreshViewOnSlideTabs(int position) {
        if (navContext == null) return;

        if (position == INDEX_LIBRARY_TAB) {
            if (navContext.inRepo()) {
                getReposFragment().refreshView();
            }
        }

    }

    /***********  Start other activity  ***************/

    public static final int PICK_FILES_REQUEST = 1;
    public static final int PICK_PHOTOS_VIDEOS_REQUEST = 2;
    public static final int PICK_FILE_REQUEST = 3;
    public static final int TAKE_PHOTO_REQUEST = 4;
    public static final int CHOOSE_COPY_MOVE_DEST_REQUEST = 5;
    public static final int DOWNLOAD_FILE_REQUEST = 6;

    public boolean hasRepoWritePermission() {
        if (navContext == null) {
            return false;
        }
        if (navContext.getDirPermission() == null || navContext.getDirPermission().indexOf('w') == -1) {
            return false;
        }
        return true;
    }

    void pickFile() {
        if (!hasRepoWritePermission()) {
            showShortToast(this, R.string.library_read_only);
            return;
        }

        // Starting with kitkat (or earlier?), the document picker has integrated image and local file support
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            UploadChoiceDialog dialog = new UploadChoiceDialog();
            dialog.show(getSupportFragmentManager(), PICK_FILE_DIALOG_FRAGMENT_TAG);
        } else {
            Intent target = Utils.createGetContentIntent();
            Intent intent = Intent.createChooser(target, getString(R.string.choose_file));
            startActivityForResult(intent, BrowserActivity.PICK_FILE_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PICK_FILES_REQUEST:
                if (resultCode == RESULT_OK) {
                    String[] paths = data.getStringArrayExtra(MultiFileChooserActivity.MULTI_FILES_PATHS);
                    if (paths == null) return;
                    showShortToast(this, getString(R.string.added_to_upload_tasks));

                    List<SeafDirent> list = dataManager.getCachedDirents(navContext.getRepoID(), navContext.getDirPath());
                    if (list == null) return;

                    for (String path : paths) {
                        boolean duplicate = false;
                        for (SeafDirent dirent : list) {
                            if (dirent.name.equals(Utils.fileNameFromPath(path))) {
                                duplicate = true;
                                break;
                            }
                        }
                        if (!duplicate) {
                            showShortToast(BrowserActivity.this, getString(R.string.added_to_upload_tasks));
                            final SeafRepo repo = dataManager.getCachedRepoByID(navContext.getRepoID());

                            uploadFiles(Arrays.asList(path), repo, false);

//                            if (repo != null && repo.canLocalDecrypt()) {
//                                addUploadBlocksTask(navContext.getRepoID(), navContext.getRepoName(), navContext.getDirPath(), path);
//                            } else {
//                                addUploadTask(navContext.getRepoID(), navContext.getRepoName(), navContext.getDirPath(), path);
//                            }
                        } else {
                            showFileExistDialog(path);
                        }
                    }
                }
                break;
            case PICK_PHOTOS_VIDEOS_REQUEST:
                if (resultCode == RESULT_OK) {
                    ArrayList<String> paths = data.getStringArrayListExtra("photos");
                    if (paths == null) return;
                    showShortToast(this, getString(R.string.added_to_upload_tasks));

                    List<SeafDirent> list = dataManager.getCachedDirents(navContext.getRepoID(), navContext.getDirPath());
                    if (list == null) return;

                    for (String path : paths) {
                        boolean duplicate = false;
                        for (SeafDirent dirent : list) {
                            if (dirent.name.equals(Utils.fileNameFromPath(path))) {
                                duplicate = true;
                                break;
                            }
                        }
                        if (!duplicate) {
                            showShortToast(BrowserActivity.this, getString(R.string.added_to_upload_tasks));
                            final SeafRepo repo = dataManager.getCachedRepoByID(navContext.getRepoID());

                            uploadFiles(Arrays.asList(path), repo, false);

//                            if (repo != null && repo.canLocalDecrypt()) {
//                                addUploadBlocksTask(navContext.getRepoID(), navContext.getRepoName(), navContext.getDirPath(), path);
//                            } else {
//                                addUploadTask(navContext.getRepoID(), navContext.getRepoName(), navContext.getDirPath(), path);
//                            }
                        } else {
                            showFileExistDialog(path);
                        }
                    }
                }
                break;
            case PICK_FILE_REQUEST:
                if (resultCode == RESULT_OK) {
                    if (!Utils.isNetworkOn()) {
                        showShortToast(this, R.string.network_down);
                        return;
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        List<Uri> uriList = UtilsJellyBean.extractUriListFromIntent(data);
                        if (uriList.size() > 0) {
                            if (accountInfo != null) {
                                ArrayList<Uri> notValidUris = ValidatesFiles.getNotValidFiles(this, accountInfo, uriList);
                                if (notValidUris.size() > 0) {
                                    int typeChangeDialog = uriList.size() == 1 ? ChangePlanDialog.NOT_ALLOWED_FILE : ChangePlanDialog.NOT_ALLOWED_FILES;
                                    ChangePlanDialog.build(mActivity, typeChangeDialog).setResponse(new ChangePlanDialog.ChangePlanDialogResponse() {
                                        @Override
                                        public void changePlanDialogResponseYes() {
                                            Utils.openWebPlans(mActivity);
                                        }

                                        @Override
                                        public void changePlanDialogResponseNo() {
                                            uriList.removeAll(notValidUris);
                                            if (uriList.size() > 0) {
                                                ContinueDialog.build(
                                                                mActivity,
                                                                ContinueDialog.UPLOAD_FILES)
                                                        .setOnContinueTask(new ContinueDialog.ContinueDialogContinueTask() {
                                                            @Override
                                                            public void continueTask() {
                                                                ConcurrentAsyncTask.execute(new SAFLoadRemoteFileTask(), uriList.toArray(new Uri[]{}));
                                                            }

                                                            @Override
                                                            public void cancelTask() {

                                                            }
                                                        }).show();
                                            }
                                        }

                                        @Override
                                        public void changePlanDialogResponseNeither() {
                                            uriList.removeAll(notValidUris);
                                            if (uriList.size() > 0) {
                                                ContinueDialog.build(mActivity, ContinueDialog.UPLOAD_FILES).setOnContinueTask(new ContinueDialog.ContinueDialogContinueTask() {
                                                    @Override
                                                    public void continueTask() {
                                                        ConcurrentAsyncTask.execute(new SAFLoadRemoteFileTask(), uriList.toArray(new Uri[]{}));
                                                    }

                                                    @Override
                                                    public void cancelTask() {

                                                    }
                                                }).show();
                                            }
                                        }
                                    }).show();
                                } else {
                                    ConcurrentAsyncTask.execute(new SAFLoadRemoteFileTask(), uriList.toArray(new Uri[]{}));
                                }
                            } else {
                                showShortToast(BrowserActivity.this, getResources().getString(R.string.error_not_account_info));
                            }
                        } else {
                            showShortToast(BrowserActivity.this, R.string.saf_upload_path_not_available);
                        }
                    } else {
                        Uri uri = data.getData();
                        if (uri != null) {
                            if (accountInfo != null) {
                                if (ValidatesFiles.isValidFile(this, accountInfo, uri)) {
                                    ConcurrentAsyncTask.execute(new SAFLoadRemoteFileTask(), uri);
                                } else {
                                    ChangePlanDialog.build(mActivity, ChangePlanDialog.NOT_ALLOWED_FILE).setResponse(new ChangePlanDialog.ChangePlanDialogResponse() {
                                        @Override
                                        public void changePlanDialogResponseYes() {
                                            Utils.openWebPlans(mActivity);
                                        }

                                        @Override
                                        public void changePlanDialogResponseNo() {

                                        }

                                        @Override
                                        public void changePlanDialogResponseNeither() {

                                        }
                                    }).show();
                                }
                            } else {
                                showShortToast(BrowserActivity.this, getResources().getString(R.string.error_not_account_info));
                            }
                        } else {
                            showShortToast(BrowserActivity.this, R.string.saf_upload_path_not_available);
                        }
                    }
                }
                break;
            case CHOOSE_COPY_MOVE_DEST_REQUEST:
                if (resultCode == RESULT_OK) {
                    if (!Utils.isNetworkOn()) {
                        showShortToast(this, R.string.network_down);
                        return;
                    }

                    copyMoveIntent = data;
                }
                break;
            case TAKE_PHOTO_REQUEST:
                if (resultCode == RESULT_OK) {
                    showShortToast(this, getString(R.string.take_photo_successfully));
                    if (!Utils.isNetworkOn()) {
                        showShortToast(this, R.string.network_down);
                        return;
                    }

                    if (takeCameraPhotoTempFile == null) {
                        showShortToast(this, getString(R.string.saf_upload_path_not_available));
                        Log.i(DEBUG_TAG, "Pick file request did not return a path");
                        return;
                    }
                    showShortToast(this, getString(R.string.added_to_upload_tasks));
                    final SeafRepo repo = dataManager.getCachedRepoByID(navContext.getRepoID());

                    uploadFiles(Arrays.asList(takeCameraPhotoTempFile.getAbsolutePath()), repo, false);

//                    if (repo != null && repo.canLocalDecrypt()) {
//                        addUploadBlocksTask(navContext.getRepoID(), navContext.getRepoName(), navContext.getDirPath(), takeCameraPhotoTempFile.getAbsolutePath());
//                    } else {
//                        addUploadTask(navContext.getRepoID(), navContext.getRepoName(), navContext.getDirPath(), takeCameraPhotoTempFile.getAbsolutePath());
//                    }
                }
                break;
            case DOWNLOAD_FILE_REQUEST:
                if (resultCode == RESULT_OK) {
                    File file = new File(data.getStringExtra("path"));
                    String repoDir = data.getStringExtra("repoDir");
                    boolean isOpenWith = data.getBooleanExtra("is_open_with", false);
                    //SeafRepo repo = dataManager.getCachedRepoByID(getNavContext().getRepoID());
                    SeafRepo repo = dataManager.getCachedRepoByID(data.getStringExtra("repoID"));
                    WidgetUtils.showFile(BrowserActivity.this, file, repo.getID(), repo.getName(), repoDir, isOpenWith, repo.isSharedRepo);
                }
            default:
                break;
        }
    }

    private class SAFLoadRemoteFileTask extends AsyncTask<Uri, Void, File[]> {

        @Override
        protected File[] doInBackground(Uri... uriList) {
            if (uriList == null) return null;

            List<Uri> listUris = new ArrayList<Uri>();
            for (Uri uri : uriList) {
                listUris.add(uri);
            }
            checkItemsInRecycleBin(listUris);
            List<File> fileList = new ArrayList<File>();
            for (Uri uri : listUris) {
                // Log.d(DEBUG_TAG, "Uploading file from uri: " + uri);
                InputStream in = null;
                OutputStream out = null;

                try {
                    File tempDir = DataManager.createTempDir();
                    File tempFile = new File(tempDir, Utils.getFilenamefromUri(BrowserActivity.this, uri));

                    if (!tempFile.createNewFile()) {
                        throw new RuntimeException("could not create temporary file");
                    }

                    in = getContentResolver().openInputStream(uri);
                    out = new FileOutputStream(tempFile);
                    IOUtils.copy(in, out);

                    fileList.add(tempFile);

                } catch (IOException e) {
                    Log.d(DEBUG_TAG, "Could not open requested document", e);
                } catch (RuntimeException e) {
                    Log.d(DEBUG_TAG, "Could not open requested document", e);
                } finally {
                    IOUtils.closeQuietly(in);
                    IOUtils.closeQuietly(out);
                }
            }
            return fileList.toArray(new File[]{});
        }

        @Override
        protected void onPostExecute(File... fileList) {
            if (fileList == null) return;

            List<SeafDirent> list = dataManager.getCachedDirents(navContext.getRepoID(), navContext.getDirPath());

            for (final File file : fileList) {
                if (file == null) {
                    showShortToast(BrowserActivity.this, R.string.saf_upload_path_not_available);
                } else {
                    if (list == null) {
                        Log.e(DEBUG_TAG, "Seadroid dirent cache is empty in uploadFile. Should not happen, aborting.");
                        return;
                    }

                    boolean duplicate = false;
                    for (SeafDirent dirent : list) {
                        if (dirent.name.equals(file.getName())) {
                            duplicate = true;
                            break;
                        }
                    }

                    if (!duplicate) {
                        final SeafRepo repo = dataManager.getCachedRepoByID(navContext.getRepoID());
                        showShortToast(BrowserActivity.this, getString(R.string.added_to_upload_tasks));

                        if (repo != null && repo.canLocalDecrypt()) {
                            addUploadBlocksTask(repo.id, repo.name, navContext.getDirPath(), file.getAbsolutePath());
                        } else {
                            addUploadTask(navContext.getRepoID(), navContext.getRepoName(), navContext.getDirPath(), file.getAbsolutePath());
                        }
                    } else {
                        showFileExistDialog(file);
                    }
                }
            }

            if (txService == null) return;

            if (!txService.hasUploadNotifProvider()) {
                UploadNotificationProvider provider = new UploadNotificationProvider(txService.getUploadTaskManager(), txService);
                txService.saveUploadNotifProvider(provider);
            }
        }

        private void checkItemsInRecycleBin(List<Uri> uris) {
            List<Uri> elementsInTrahs = new ArrayList<>();

            List<SeafDirentTrash> direntTrashList = new ArrayList<>();

            try {
                direntTrashList = dataManager.getTrashDirentsFromServer(navContext.getRepoID(), navContext.getDirPath());
            } catch (SeafException e) {
                e.printStackTrace();
            }

            for (SeafDirentTrash direntTrash : direntTrashList) {
                String parentPath = direntTrash.path.endsWith("/") ? direntTrash.path : direntTrash.path + "/";
                String contextPath = navContext.getDirPath().endsWith("/") ? navContext.getDirPath() : navContext.getDirPath() + "/";
                for (Uri file : uris) {
                    String name = Utils.getFilenamefromUri(mActivity, file);
                    if (contextPath.equals(parentPath) && direntTrash.isDir() == false && direntTrash.getTitle().equals(name)) {
                        elementsInTrahs.add(file);
                    }
                }
            }

            if (!elementsInTrahs.isEmpty()) {
                int type = elementsInTrahs.size() > 1 ? StepRecycleBin.MULTIPLE_FILES_IN_TRASH : StepRecycleBin.SINGLE_FILE_IN_TRASH;
                final boolean[] wait = {true};
                final boolean[] remove = {false};
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() -> {
                    StepRecycleBin.build(mActivity, type).setOnReplaceTrash(new StepRecycleBin.ReplaceTrashTask() {
                        @Override
                        public void continueTask() {
                            wait[0] = false;
                        }

                        @Override
                        public void cancelTask() {
                            wait[0] = false;
                            remove[0] = true;
                        }
                    }).show();
                });

                while (wait[0]) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (remove[0]) {
                    uris.removeAll(elementsInTrahs);
                }
            }
        }
    }

    private void showFileExistDialog(final String filePath) {
        showFileExistDialog(new File(filePath));
    }

    private void showFileExistDialog(final File file) {
        final SeafRepo repo = dataManager.getCachedRepoByID(navContext.getRepoID());
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.upload_file_exist));
        builder.setMessage(String.format(getString(R.string.upload_duplicate_found), file.getName()));
        builder.setPositiveButton(getString(R.string.upload_replace), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showShortToast(BrowserActivity.this, getString(R.string.added_to_upload_tasks));
                uploadFiles(Arrays.asList(file.getAbsolutePath()), repo, true);
//                if (repo != null && repo.canLocalDecrypt()) {
//                    addUpdateBlocksTask(repo.id, repo.name, navContext.getDirPath(), file.getAbsolutePath());
//                } else {
//                    addUpdateTask(navContext.getRepoID(), navContext.getRepoName(), navContext.getDirPath(), file.getAbsolutePath());
//                }
            }
        });
        builder.setNeutralButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.setNegativeButton(getString(R.string.upload_keep_both), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                uploadFiles(Arrays.asList(file.getAbsolutePath()), repo, false);

//                if (repo != null && repo.canLocalDecrypt()) {
//                    addUploadBlocksTask(repo.id, repo.name, navContext.getDirPath(), file.getAbsolutePath());
//                } else {
//                    addUploadTask(navContext.getRepoID(), navContext.getRepoName(), navContext.getDirPath(), file.getAbsolutePath());
//                }
            }
        });
        builder.show();
    }

    public void onItemSelected() {
        // update contextual action bar (CAB) title
        getReposFragment().updateContextualActionBar();
    }

    /***************  Navigation *************/

    public void onFileSelected(SeafDirent dirent, boolean isOpenWith) {
        final String fileName = dirent.name;
        final long fileSize = dirent.size;
        final String repoName = navContext.getRepoName();
        final String repoID = navContext.getRepoID();
        final String dirPath = navContext.getDirPath();
        final String filePath = Utils.pathJoin(navContext.getDirPath(), fileName);
        final SeafRepo repo = dataManager.getCachedRepoByID(repoID);

        // Encrypted repo doesn\`t support gallery,
        // because pic thumbnail under encrypted repo was not supported at the server side
        if (Utils.isViewableImage(fileName) && repo != null && !repo.encrypted) {
            WidgetUtils.startGalleryActivity(this, repoName, repoID, dirPath, fileName, account, accountInfo);
            return;
        }

        final File localFile = dataManager.getLocalCachedFile(repoName, repoID, filePath, null);
        if (localFile != null) {
            WidgetUtils.showFile(this, localFile, repoID, repoName, dirPath, isOpenWith, repo.isSharedRepo);
            return;
        }
        boolean videoFile = Utils.isVideoFile(fileName);
        if (videoFile) {// is video file
            List<SeafDirent> dirents = new ArrayList<>();
            dirents.add(dirent);

            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setItems(R.array.video_download_array, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) // create file
                        startPlayActivity(fileName, repoID, filePath);
                    else if (which == 1) // create folder
                        downloadFiles(repoID, repoName, dirPath, dirents);
                    //startFileActivity(repoName, repoID, filePath, fileSize, isOpenWith);
                }
            }).show();
            return;
        }
        startFileActivity(repoName, repoID, filePath, fileSize, isOpenWith);
    }

    @Override
    public void onFileSelected(SeafDirent dirent) {
        onFileSelected(dirent, false);
    }

    /**
     * Download a file
     *
     * @param dirent
     * @param repoID
     * @param repoPath
     * @param repoName
     */
    public void downloadFile(SeafDirent dirent, String repoID, String repoPath, String repoName) {

        // txService maybe null if layout orientation has changed
        if (txService == null) {
            return;
        }

        List<SeafDirent> dirents = new ArrayList<>();
        dirents.add(dirent);

        downloadFiles(dirents, repoID, repoPath, repoName);
    }

    /**
     * Download all files (folders) under a given folder
     *
     * @param dirent
     */
    public void downloadDir(SeafDirent dirent) {
        if (!Utils.isNetworkOn()) {
            showShortToast(this, R.string.network_down);
            return;
        }

        List<SeafDirent> dirents = new ArrayList<>();
        dirents.add(dirent);

        downloadFiles(dirents, navContext.getRepoID(), navContext.getDirPath(), navContext.getRepoName());
    }

    private class DownloadDirTask extends AsyncTask<String, Void, List<SeafDirent>> {

        private String repoName;
        private String repoID;
        private String fileName;
        private String dirPath;
        private int fileCount;
        private boolean recurse;
        private ArrayList<String> dirPaths = Lists.newArrayList();
        private SeafException err = null;

        @Override
        protected List<SeafDirent> doInBackground(String... params) {
            if (params.length != 5) {
                Log.d(DEBUG_TAG, "Wrong params to LoadDirTask");
                return null;
            }

            repoName = params[0];
            repoID = params[1];
            dirPath = params[2];
            recurse = Boolean.valueOf(params[3]);
            fileName = params[4];


            ArrayList<SeafDirent> dirents = Lists.newArrayList();

            dirPaths.add(Utils.pathJoin(dirPath, fileName));

            // don`t use for each loop here
            for (int i = 0; i < dirPaths.size(); i++) {

                List<SeafDirent> currentDirents;
                try {
                    currentDirents = dataManager.getDirentsFromServer(repoID, dirPaths.get(i));
                } catch (SeafException e) {
                    err = e;
                    e.printStackTrace();
                    return null;
                }

                if (currentDirents == null) continue;

                for (SeafDirent seafDirent : currentDirents) {
                    if (seafDirent.isDir()) {
                        if (recurse) {
                            dirPaths.add(Utils.pathJoin(dirPaths.get(i), seafDirent.name));
                        }
                    } else {
                        File localCachedFile = dataManager.getLocalCachedFile(repoName, repoID, Utils.pathJoin(dirPaths.get(i), seafDirent.name), seafDirent.id);
                        if (localCachedFile != null) {
                            continue;
                        }

                        // txService maybe null if layout orientation has changed
                        // e.g. landscape and portrait switch
                        if (txService == null) return null;

                        txService.addTaskToDownloadQue(account, repoName, repoID, Utils.pathJoin(dirPaths.get(i), seafDirent.name));

                        fileCount++;
                    }
                }
            }

            return dirents;
        }

        @Override
        protected void onPostExecute(List<SeafDirent> dirents) {
            if (dirents == null) {
                if (err != null) {
                    showShortToast(BrowserActivity.this, R.string.transfer_list_network_error);
                }
                return;
            }

            if (fileCount == 0)
                showShortToast(BrowserActivity.this, R.string.transfer_download_no_task);
            else {
                showShortToast(BrowserActivity.this, getResources().getQuantityString(R.plurals.transfer_download_started, fileCount, fileCount));
                if (!txService.hasDownloadNotifProvider()) {
                    DownloadNotificationProvider provider = new DownloadNotificationProvider(txService.getDownloadTaskManager(), txService);
                    txService.saveDownloadNotifProvider(provider);
                }
            }

            // set download tasks info to adapter in order to update download progress in UI thread
            getReposFragment().getAdapter().setDownloadTaskList(txService.getDownloadTaskInfosByPath(repoID, dirPath));
        }
    }

    private void startFileActivity(String repoName, String repoID, String filePath, long fileSize) {
        startFileActivity(repoName, repoID, filePath, fileSize);
    }

    private void startFileActivity(String repoName, String repoID, String filePath, long fileSize, boolean isOpenWith) {
        // txService maybe null if layout orientation has changed
        if (txService == null) {
            return;
        }
        int taskID = txService.addDownloadTask(account, repoName, repoID, filePath, fileSize);
        Intent intent = new Intent(this, FileActivity.class);
        intent.putExtra("repoName", repoName);
        intent.putExtra("repoID", repoID);
        intent.putExtra("filePath", filePath);
        intent.putExtra("account", account);
        intent.putExtra("taskID", taskID);
        intent.putExtra("is_open_with", isOpenWith);
        startActivityForResult(intent, DOWNLOAD_FILE_REQUEST);
    }

    private void startPlayActivity(String fileName, String repoID, String filePath) {

        try {
            String extension = FileExtension.getExtensionFromUri(mActivity, Uri.fromFile(new File(fileName)));
            if (ValidatesFiles.isValidType(accountInfo, extension.toUpperCase())) {
                Intent intent = new Intent(this, ExoVideoPlayerActivity.class);
                intent.putExtra("fileName", fileName);
                intent.putExtra("repoID", repoID);
                intent.putExtra("filePath", filePath);
                intent.putExtra("account", account);
                intent.putExtra("accountInfo", accountInfo);
                //DOWNLOAD_PLAY_REQUEST
                startActivity(intent);
            } else {
                ChangePlanDialog.build(mActivity, ChangePlanDialog.NOT_ALLOWED_FILE).setResponse(new ChangePlanDialog.ChangePlanDialogResponse() {
                    @Override
                    public void changePlanDialogResponseYes() {
                        Utils.openWebPlans(mActivity);
                    }

                    @Override
                    public void changePlanDialogResponseNo() {

                    }

                    @Override
                    public void changePlanDialogResponseNeither() {

                    }
                }).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void onStarredFileSelected(final SeafStarredFile starredFile, boolean isOpenWith) {
        final long fileSize = starredFile.getSize();
        final String repoID = starredFile.getRepoID();
        final SeafRepo repo = dataManager.getCachedRepoByID(repoID);
        if (repo == null) return;

        if (repo.encrypted && !dataManager.getRepoPasswordSet(repo.id)) {
            String password = dataManager.getRepoPassword(repo.id);
            showPasswordDialog(repo.name, repo.id, new TaskDialog.TaskDialogListener() {
                @Override
                public void onTaskSuccess() {
                    onStarredFileSelected(starredFile);
                }
            }, password);

            return;
        }

        final String repoName = repo.getName();
        final String filePath = starredFile.getPath();
        final String dirPath = Utils.getParentPath(filePath);

        // Encrypted repo doesn\`t support gallery,
        // because pic thumbnail under encrypted repo was not supported at the server side
        if (Utils.isViewableImage(starredFile.getTitle()) && !repo.encrypted) {
            WidgetUtils.startGalleryActivity(this, repoName, repoID, dirPath, starredFile.getTitle(), account, accountInfo);
            return;
        }

        final File localFile = dataManager.getLocalCachedFile(repoName, repoID, filePath, null);
        if (localFile != null) {
            WidgetUtils.showFile(this, localFile, repoID, repoName, dirPath, repo.isSharedRepo);
            return;
        }

        startFileActivity(repoName, repoID, filePath, fileSize, isOpenWith);
    }

    @Override
    public void onStarredFileSelected(SeafStarredFile starredFile) {
        onStarredFileSelected(starredFile, false);
    }

    @Override
    public void onBackPressed() {
        if (constraintLoadingContainer.getVisibility() != View.VISIBLE) {

            if (getSupportFragmentManager().getBackStackEntryCount() != 0) {
                getSupportFragmentManager().popBackStack();
                return;
            }

            if (currentPosition == INDEX_LIBRARY_TAB) {
                if (navContext.inRepo()) {
                    if (navContext.isRepoRoot()) {
                        navContext.setRepoID(null);
                        getActionBarToolbar().setTitle("");
                        mActivity.getmTabLayout().getTabAt(INDEX_TRASH_TAB).view.setVisibility(View.VISIBLE);
                    } else {
                        String parentPath = Utils.getParentPath(navContext.getDirPath());
                        navContext.setDir(parentPath, null);
                        if (parentPath.equals(ACTIONBAR_PARENT_PATH)) {
                            getActionBarToolbar().setTitle(navContext.getRepoName());
                        } else {
                            getActionBarToolbar().setTitle(parentPath.substring(parentPath.lastIndexOf(ACTIONBAR_PARENT_PATH) + 1));
                        }
                    }

                    getReposFragment().clearAdapterData();
                    getReposFragment().refreshView(true);

                } else super.onBackPressed();
            } else if (currentPosition == INDEX_TRASH_TAB) {
                if (navContext.inRepo()) {
                    if (navContext.isNavigateToDirentsTrash()) {
                        String parentPath = Utils.getParentPath(navContext.getDirPath());
                        parentPath = parentPath.endsWith("/") && parentPath.length() > 1 ? parentPath.substring(0, parentPath.length() - 1) : parentPath;
                        if (navContext.getDirNavigatePath().equals(parentPath)) {
                            navContext.setNavigateToDirentsTrash(false);
                            navContext.setDirNavigatePath(null);
                            navContext.setDirCommitID(null);
                            parentPath = navContext.getDirNavigateRoot();
                            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                        }
                        navContext.setDir(parentPath, null);
                        getReposFragment().clearAdapterData();
                        getReposFragment().refreshView(true, true);
                    } else {
                        super.onBackPressed();
                    }
                } else if (navContext.inRepoTrash()) {
                    super.onBackPressed();
                }
            } else super.onBackPressed();
        }
    }

    @Override
    public void onBackStackChanged() {
    }


    /************  Files ************/

    /**
     * Export a file.
     * 1. first ask the user to choose an app
     * 2. then download the latest version of the file
     * 3. start the choosen app
     *
     * @param fileName The name of the file to share in the current navcontext
     */
    public void exportFile(String fileName, long fileSize) {
        String repoName = navContext.getRepoName();
        String repoID = navContext.getRepoID();
        String dirPath = navContext.getDirPath();
        String fullPath = Utils.pathJoin(dirPath, fileName);
        chooseExportApp(repoName, repoID, fullPath, fileSize);
    }

    private void chooseExportApp(final String repoName, final String repoID, final String path, final long fileSize) {
        final File file = dataManager.getLocalRepoFile(repoName, repoID, path);
        Uri uri = null;
        if (android.os.Build.VERSION.SDK_INT > 23) {
            uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName(), file);
        } else {
            uri = Uri.fromFile(file);
        }
        final Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.setType(Utils.getFileMimeType(file));
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);

        // Get a list of apps
        List<ResolveInfo> infos = Utils.getAppsByIntent(sendIntent);

        if (infos.isEmpty()) {
            showShortToast(this, R.string.no_app_available);
            return;
        }

        AppChoiceDialog dialog = new AppChoiceDialog();
        dialog.init(getString(R.string.export_file), infos, new AppChoiceDialog.OnItemSelectedListener() {
            @Override
            public void onCustomActionSelected(CustomAction action) {
            }

            @Override
            public void onAppSelected(ResolveInfo appInfo) {
                String className = appInfo.activityInfo.name;
                String packageName = appInfo.activityInfo.packageName;
                sendIntent.setClassName(packageName, className);

                if (!Utils.isNetworkOn() && file.exists()) {
                    startActivity(sendIntent);
                    return;
                }
                fetchFileAndExport(appInfo, sendIntent, repoName, repoID, path, fileSize);
            }

        });
        dialog.show(getSupportFragmentManager(), CHOOSE_APP_DIALOG_FRAGMENT_TAG);
    }

    public void fetchFileAndExport(final ResolveInfo appInfo, final Intent intent, final String repoName, final String repoID, final String path, final long fileSize) {

        fetchFileDialog = new FetchFileDialog();
        fetchFileDialog.init(repoName, repoID, path, fileSize, new FetchFileDialog.FetchFileListener() {
            @Override
            public void onSuccess() {
                startActivity(intent);
            }

            @Override
            public void onDismiss() {
                fetchFileDialog = null;
            }

            @Override
            public void onFailure(SeafException err) {
            }
        });
        fetchFileDialog.show(getSupportFragmentManager(), OPEN_FILE_DIALOG_FRAGMENT_TAG);
    }

    public void renameRepo(String repoID, String repoName) {
        final RenameRepoDialog dialog = new RenameRepoDialog();
        dialog.init(repoID, repoName, account);
        dialog.setTaskDialogLisenter(new TaskDialog.TaskDialogListener() {
            @Override
            public void onTaskSuccess() {
                showShortToast(BrowserActivity.this, R.string.rename_successful);
                ReposFragment reposFragment = getReposFragment();
                if (currentPosition == INDEX_LIBRARY_TAB && reposFragment != null) {
                    reposFragment.refreshView(true, true);
                }
            }
        });
        dialog.show(getSupportFragmentManager(), TAG_RENAME_REPO_DIALOG_FRAGMENT);
    }

    public void deleteRepo(String repoID) {
        final DeleteRepoDialog dialog = new DeleteRepoDialog();
        dialog.init(repoID, account);
        dialog.setTaskDialogLisenter(new TaskDialog.TaskDialogListener() {
            @Override
            public void onTaskSuccess() {
                showShortToast(BrowserActivity.this, R.string.delete_successful);
                ReposFragment reposFragment = getReposFragment();
                if (currentPosition == INDEX_LIBRARY_TAB && reposFragment != null) {
                    reposFragment.refreshView(true, true);
                }
            }
        });
        dialog.show(getSupportFragmentManager(), TAG_DELETE_REPO_DIALOG_FRAGMENT);
    }

    /**
     * Share a file. Generating a file share link and send the link or file to someone
     * through some app.
     *
     * @param repoID
     * @param path
     */
    public void showShareDialog(String repoID, String path, boolean isDir, long fileSize, String fileName) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        boolean inChina = Utils.isInChina();
        String[] strings;
        //if user  in China ，system  add  WeChat  share
        if (inChina) {
            strings = getResources().getStringArray(R.array.file_action_share_array_zh);
        } else {
            strings = getResources().getStringArray(R.array.file_action_share_array);
        }
        builder.setItems(strings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!inChina) {
                    which++;
                }
                switch (which) {
                    case 0:
                        WidgetUtils.ShareWeChat(BrowserActivity.this, account, repoID, path, fileName, fileSize, isDir);
                        break;
                    case 1:
                        // need input password
                        WidgetUtils.chooseShareApp(BrowserActivity.this, repoID, path, isDir, account, null, null);
                        break;
                    case 2:
                        WidgetUtils.inputSharePassword(BrowserActivity.this, repoID, path, isDir, account);
                        break;
                }
            }
        }).show();
    }

    public void renameFile(String repoID, String repoName, String path) {
        doRename(repoID, repoName, path, false);
    }

    public void renameDir(String repoID, String repoName, String path) {
        doRename(repoID, repoName, path, true);
    }

    private void doRename(String repoID, String repoName, String path, boolean isdir) {
        ExecutorService services = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        services.execute(() -> {
            List<SeafDirentTrash> direntTrashList = new ArrayList<>();

            try {
                direntTrashList = dataManager.getTrashDirentsFromServer(repoID, Utils.getParentPath(path));
            } catch (SeafException e) {
                e.printStackTrace();
            }

            List<SeafDirentTrash> finalDirentTrashList = direntTrashList;
            handler.post(() -> {
                final RenameFileDialog dialog = new RenameFileDialog();
                dialog.init(repoID, path, isdir, account, finalDirentTrashList);
                dialog.setTaskDialogLisenter(new TaskDialog.TaskDialogListener() {
                    @Override
                    public void onTaskSuccess() {
                        showShortToast(BrowserActivity.this, R.string.rename_successful);
                        ReposFragment reposFragment = getReposFragment();
                        if (currentPosition == INDEX_LIBRARY_TAB && reposFragment != null) {
                            reposFragment.refreshView(true, true);
                        }
                    }
                });
                dialog.show(getSupportFragmentManager(), TAG_RENAME_FILE_DIALOG_FRAGMENT);
            });

        });

    }

    public void deleteFile(String repoID, String repoName, String path) {
        doDelete(repoID, repoName, path, false);
    }

    public void deleteDir(String repoID, String repoName, String path) {
        doDelete(repoID, repoName, path, true);
    }

    private void doDelete(final String repoID, String repoName, String path, boolean isdir) {
        final DeleteFileDialog dialog = new DeleteFileDialog();
        dialog.init(repoID, path, isdir, account);
        dialog.setTaskDialogLisenter(new TaskDialog.TaskDialogListener() {
            @Override
            public void onTaskSuccess() {
                showShortToast(BrowserActivity.this, R.string.delete_successful);
                List<SeafDirent> cachedDirents = getDataManager().getCachedDirents(repoID, getNavContext().getDirPath());
                getReposFragment().getAdapter().setItems(cachedDirents);
                getReposFragment().getAdapter().notifyDataSetChanged();
            }
        });
        dialog.show(getSupportFragmentManager(), TAG_DELETE_FILE_DIALOG_FRAGMENT);
    }

    public void copyFile(String srcRepoId, String srcRepoName, String srcDir, String srcFn, boolean isdir) {
        chooseCopyMoveDest(srcRepoId, srcRepoName, srcDir, srcFn, isdir, CopyMoveContext.OP.COPY);
    }

    public void moveFile(String srcRepoId, String srcRepoName, String srcDir, String srcFn, boolean isdir) {
        chooseCopyMoveDest(srcRepoId, srcRepoName, srcDir, srcFn, isdir, CopyMoveContext.OP.MOVE);
    }

    public void starFile(String srcRepoId, String srcDir, String srcFn) {
        getStarredFragment().doStarFile(srcRepoId, srcDir, srcFn);
    }

    private void chooseCopyMoveDest(String repoID, String repoName, String path, String filename, boolean isdir, CopyMoveContext.OP op) {
        copyMoveContext = new CopyMoveContext(repoID, repoName, path, filename, isdir, op);
        Intent intent = new Intent(this, SeafilePathChooserActivity.class);
        intent.putExtra(SeafilePathChooserActivity.DATA_ACCOUNT, account);
        SeafRepo repo = dataManager.getCachedRepoByID(repoID);
        boolean isShowEncryptDir = false;
        if (repo.encrypted) {
            isShowEncryptDir = true;
            intent.putExtra(SeafilePathChooserActivity.ENCRYPTED_REPO_ID, repoID);
        }
        intent.putExtra(SeafilePathChooserActivity.SHOW_ENCRYPTED_REPOS, isShowEncryptDir);
        startActivityForResult(intent, CHOOSE_COPY_MOVE_DEST_REQUEST);
        return;
    }

    private void doCopyMove() {
        if (!copyMoveContext.checkCopyMoveToSubfolder()) {
            showShortToast(this, copyMoveContext.isCopy() ? R.string.cannot_copy_folder_to_subfolder : R.string.cannot_move_folder_to_subfolder);
            return;
        }
        final CopyMoveDialog dialog = new CopyMoveDialog();
        dialog.init(account, copyMoveContext);
        dialog.setCancelable(false);
        dialog.setTaskDialogLisenter(new TaskDialog.TaskDialogListener() {
            @Override
            public void onTaskSuccess() {
                showShortToast(BrowserActivity.this, copyMoveContext.isCopy() ? R.string.copied_successfully : R.string.moved_successfully);
                if (copyMoveContext.batch) {
                    List<SeafDirent> cachedDirents = getDataManager().getCachedDirents(getNavContext().getRepoID(), getNavContext().getDirPath());

                    // refresh view
                    if (getReposFragment().getAdapter() != null) {
                        getReposFragment().getAdapter().setItems(cachedDirents);
                        getReposFragment().getAdapter().notifyDataSetChanged();
                    }

                    if (cachedDirents.size() == 0)
                        getReposFragment().getEmptyView().setVisibility(View.VISIBLE);
                    return;
                }

                if (copyMoveContext.isMove()) {
                    ReposFragment reposFragment = getReposFragment();
                    if (currentPosition == INDEX_LIBRARY_TAB && reposFragment != null) {
                        reposFragment.refreshView();
                    }
                }
            }
        });
        dialog.show(getSupportFragmentManager(), TAG_COPY_MOVE_DIALOG_FRAGMENT);
    }

    private void onFileDownloadFailed(int taskID) {
        if (txService == null) {
            return;
        }

        DownloadTaskInfo info = txService.getDownloadTaskInfo(taskID);
        if (info == null) return;

        final SeafException err = info.err;
        final String repoName = info.repoName;
        final String repoID = info.repoID;
        final String path = info.pathInRepo;

        if (err != null && err.getCode() == SeafConnection.HTTP_STATUS_REPO_PASSWORD_REQUIRED) {
            if (currentPosition == INDEX_LIBRARY_TAB && repoID.equals(navContext.getRepoID()) && Utils.getParentPath(path).equals(navContext.getDirPath())) {
                showPasswordDialog(repoName, repoID, new TaskDialog.TaskDialogListener() {
                    @Override
                    public void onTaskSuccess() {
                        txService.addDownloadTask(account, repoName, repoID, path);
                    }
                });
                return;
            }
        }

        showShortToast(this, getString(R.string.download_failed));
    }

    private void onFileUploaded(int taskID) {
        if (txService == null) {
            return;
        }

        UploadTaskInfo info = txService.getUploadTaskInfo(taskID);

        if (info == null) {
            return;
        }

        String repoID = info.repoID;
        String dir = info.parentDir;
        if (currentPosition == INDEX_LIBRARY_TAB && repoID.equals(navContext.getRepoID()) && dir.equals(navContext.getDirPath())) {
            getReposFragment().refreshView(true, true);
            String verb = getString(info.isUpdate ? R.string.updated : R.string.uploaded);
            showShortToast(this, verb + " " + Utils.fileNameFromPath(info.localFilePath));
        }
    }

    private int intShowErrorTime;

    private void onFileUploadFailed(int taskID) {
        if (++intShowErrorTime <= 1) showShortToast(this, getString(R.string.upload_failed));
    }

    public PasswordDialog showPasswordDialog(String repoName, String repoID, TaskDialog.TaskDialogListener listener) {
        return showPasswordDialog(repoName, repoID, listener, null);
    }

    public PasswordDialog showPasswordDialog(String repoName, String repoID, TaskDialog.TaskDialogListener listener, String password) {
        PasswordDialog passwordDialog = new PasswordDialog();
        passwordDialog.setRepo(repoName, repoID, account);
        if (password != null) {
            passwordDialog.setPassword(password);
        }
        passwordDialog.setTaskDialogLisenter(listener);
        passwordDialog.show(getSupportFragmentManager(), PASSWORD_DIALOG_FRAGMENT_TAG);
        return passwordDialog;
    }

    /************  Multiple Files ************/

    /**
     * Delete multiple fiels
     *
     * @param repoID
     * @param path
     * @param dirents
     */
    public void deleteFiles(final String repoID, String path, List<SeafDirent> dirents) {
        final DeleteFileDialog dialog = new DeleteFileDialog();
        dialog.init(repoID, path, dirents, account);
        dialog.setCancelable(false);
        dialog.setTaskDialogLisenter(new TaskDialog.TaskDialogListener() {
            @Override
            public void onTaskSuccess() {
                showShortToast(BrowserActivity.this, R.string.delete_successful);
                if (getDataManager() != null) {
                    List<SeafDirent> cachedDirents = getDataManager().getCachedDirents(repoID, getNavContext().getDirPath());
                    getReposFragment().getAdapter().setItems(cachedDirents);
                    getReposFragment().getAdapter().notifyDataSetChanged();
                    // update contextual action bar (CAB) title
                    getReposFragment().updateContextualActionBar();
                    if (cachedDirents.size() == 0)
                        getReposFragment().getEmptyView().setVisibility(View.VISIBLE);
                }
            }
        });
        dialog.show(getSupportFragmentManager(), TAG_DELETE_FILES_DIALOG_FRAGMENT);
    }

    /**
     * Copy multiple files
     *
     * @param srcRepoId
     * @param srcRepoName
     * @param srcDir
     * @param dirents
     */
    public void copyFiles(String srcRepoId, String srcRepoName, String srcDir, List<SeafDirent> dirents) {
        chooseCopyMoveDestForMultiFiles(srcRepoId, srcRepoName, srcDir, dirents, CopyMoveContext.OP.COPY);
    }

    /**
     * Move multiple files
     *
     * @param srcRepoId
     * @param srcRepoName
     * @param srcDir
     * @param dirents
     */
    public void moveFiles(String srcRepoId, String srcRepoName, String srcDir, List<SeafDirent> dirents) {
        chooseCopyMoveDestForMultiFiles(srcRepoId, srcRepoName, srcDir, dirents, CopyMoveContext.OP.MOVE);
    }

    /**
     * Choose copy/move destination for multiple files
     *
     * @param repoID
     * @param repoName
     * @param dirPath
     * @param dirents
     * @param op
     */
    private void chooseCopyMoveDestForMultiFiles(String repoID, String repoName, String dirPath, List<SeafDirent> dirents, CopyMoveContext.OP op) {
        copyMoveContext = new CopyMoveContext(repoID, repoName, dirPath, dirents, op);
        Intent intent = new Intent(this, SeafilePathChooserActivity.class);
        intent.putExtra(SeafilePathChooserActivity.DATA_ACCOUNT, account);
        SeafRepo repo = getDataManager().getCachedRepoByID(repoID);
        boolean isShowEncryptDir = true;
        if (repo.encrypted) {
            intent.putExtra(SeafilePathChooserActivity.ENCRYPTED_REPO_ID, repoID);
        }
        intent.putExtra(SeafilePathChooserActivity.REPO_ENCRYPTED, repo.encrypted);
        intent.putExtra(SeafilePathChooserActivity.SHOW_ENCRYPTED_REPOS, isShowEncryptDir);
        startActivityForResult(intent, BrowserActivity.CHOOSE_COPY_MOVE_DEST_REQUEST);
    }


    /**
     * Recover multiple Dirents
     *
     * @param dirents
     */
    public void recoverFiles(List<SeafDirentTrash> dirents) {
        retrieveDirents(dirents);
    }

    /**
     * Add selected files (folders) to downloading queue,
     * folders with subfolder will be downloaded recursively.
     *
     * @param repoID
     * @param repoName
     * @param dirPath
     * @param dirents
     */
    public void downloadFiles(String repoID, String repoName, String dirPath, List<SeafDirent> dirents) {
        if (!Utils.isNetworkOn()) {
            showShortToast(this, R.string.network_down);
            return;
        }

        List<SeafDirent> direntsAll = new ArrayList<>();
        direntsAll.addAll(dirents);

        if (getReposFragment().getAdapter() != null) {
            getReposFragment().closeActionMode();
        }

        downloadFiles(direntsAll, repoID, dirPath, repoName);

    }

    /**
     * Task for asynchronously downloading selected files (folders),
     * files wont be added to downloading queue if they have already been cached locally.
     */
    private class DownloadFilesTask extends AsyncTask<Void, Void, Void> {
        private String repoID, repoName, dirPath;
        private List<SeafDirent> dirents;
        private List<String> direntsNotValidID;
        private SeafException err;
        private int fileCount;

        public DownloadFilesTask(String repoID, String repoName, String dirPath, List<SeafDirent> dirents, List<String> direntsNotValidID) {
            this.repoID = repoID;
            this.repoName = repoName;
            this.dirPath = dirPath;
            this.dirents = dirents;
            this.direntsNotValidID = direntsNotValidID;
        }

        @Override
        protected void onPreExecute() {
            getReposFragment().showLoading(true);
        }

        @Override
        protected Void doInBackground(Void... params) {
            ArrayList<String> dirPaths = Lists.newArrayList(dirPath);
            for (int i = 0; i < dirPaths.size(); i++) {
                if (i > 0) {
                    try {
                        dirents = getDataManager().getDirentsFromServer(repoID, dirPaths.get(i));
                    } catch (SeafException e) {
                        err = e;
                        Log.e(DEBUG_TAG, e.getMessage() + e.getCode());
                    }
                }

                if (dirents == null) continue;

                for (SeafDirent seafDirent : dirents) {
                    if (seafDirent.isDir()) {
                        // download files recursively
                        dirPaths.add(Utils.pathJoin(dirPaths.get(i), seafDirent.name));
                    } else {
                        File localCachedFile = getDataManager().getLocalCachedFile(repoName, repoID, Utils.pathJoin(dirPaths.get(i), seafDirent.name), seafDirent.id);
                        if (localCachedFile != null) {
                            continue;
                        }

                        // txService maybe null if layout orientation has changed
                        // e.g. landscape and portrait switch
                        if (txService == null) return null;

                        if (!direntsNotValidID.contains(seafDirent.id)) {
                            txService.addTaskToDownloadQue(account, repoName, repoID, Utils.pathJoin(dirPaths.get(i), seafDirent.name));
                            fileCount++;
                        }


                    }

                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {


            // update ui
            getReposFragment().showLoading(false);

            if (err != null) {
                showShortToast(BrowserActivity.this, R.string.transfer_list_network_error);
                return;
            }

            if (fileCount == 0)
                showShortToast(BrowserActivity.this, R.string.transfer_download_no_task);
            else {
                showShortToast(BrowserActivity.this, getResources().getQuantityString(R.plurals.transfer_download_started, fileCount, fileCount));

                if (!txService.hasDownloadNotifProvider()) {
                    DownloadNotificationProvider provider = new DownloadNotificationProvider(txService.getDownloadTaskManager(), txService);
                    txService.saveDownloadNotifProvider(provider);
                }

            }

        }
    }

    @Override
    public boolean onKeyUp(int keycode, KeyEvent e) {
        switch (keycode) {
            case KeyEvent.KEYCODE_MENU:
                if (overFlowMenu != null) {
                    overFlowMenu.performIdentifierAction(R.id.menu_overflow, 0);
                }
        }

        return super.onKeyUp(keycode, e);
    }

    // for receive broadcast from TransferService
    private class TransferReceiver extends BroadcastReceiver {

        private TransferReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra("type");
            if (type.equals(DownloadTaskManager.BROADCAST_FILE_DOWNLOAD_FAILED)) {
                int taskID = intent.getIntExtra("taskID", 0);
                onFileDownloadFailed(taskID);
            } else if (type.equals(UploadTaskManager.BROADCAST_FILE_UPLOAD_SUCCESS)) {
                int taskID = intent.getIntExtra("taskID", 0);
                onFileUploaded(taskID);
            } else if (type.equals(UploadTaskManager.BROADCAST_FILE_UPLOAD_FAILED)) {
                int taskID = intent.getIntExtra("taskID", 0);
                onFileUploadFailed(taskID);
            }
        }

    } // TransferReceiver


    public void showRepoBottomSheet(SeafRepo repo) {
        getReposFragment().showRepoBottomSheet(repo);
    }

    /**
     * Displays a bottom sheet for a repository in the trash.
     *
     * @param repo The repository object representing the item in the trash.
     */
    public void showRepoTrashBottomSheet(SeafRepoTrash repo) {
        getReposFragment().showRepoTrashBottomSheet(repo);
    }

    public void showFileBottomSheet(String title, final SeafDirent dirent) {
        getReposFragment().showFileBottomSheet(title, dirent);
    }

    public void showDirBottomSheet(String title, final SeafDirent dirent) {
        getReposFragment().showDirBottomSheet(title, dirent);
    }

    /**
     * Displays a bottom sheet for a deleted directory or file item in the trash.
     *
     * @param title  The title or description for the deleted item.
     * @param dirent The SeafDirentTrash object representing the deleted directory or file.
     */
    public void showDirentTrashBottomSheet(String title, final SeafDirentTrash dirent) {
        getReposFragment().showDirentTrashBottomSheet(title, dirent);
    }

    private void syncCamera() {
        SettingsManager settingsManager = SettingsManager.instance();
        CameraUploadManager cameraManager = new CameraUploadManager(getApplicationContext());
        if (cameraManager.isCameraUploadEnabled() && settingsManager.isVideosUploadAllowed()) {
            cameraManager.performFullSync();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(CheckUploadServiceEvent result) {

//        if (!Utils.isServiceRunning(BrowserActivity.this, "com.seafile.seadroid2.cameraupload.MediaObserverService")) {
//            mediaObserver = new Intent(this, MediaObserverService.class);
//            startService(mediaObserver);
//            syncCamera();
//            Log.d(DEBUG_TAG, "onEvent============false ");
//        } else {
//            Log.d(DEBUG_TAG, "onEvent============true ");
//        }
//
//        if (!Utils.isServiceRunning(BrowserActivity.this, "com.seafile.seadroid2.monitor.FileMonitorService")) {
//            monitorIntent = new Intent(this, FileMonitorService.class);
//            startService(monitorIntent);
//            Log.d(DEBUG_TAG, "FileMonitorService============false ");
//        }
//
//        if (!Utils.isServiceRunning(BrowserActivity.this, "com.seafile.seadroid2.folderbackup.FolderBackupService")) {
//            monitorIntent = new Intent(this, FolderBackupService.class);
//            startService(monitorIntent);
//            Log.d(DEBUG_TAG, "FolderBackupService============false ");
//        }
    }

    private void loadAccountInfo() {
        ExecutorService service = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        service.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    accountInfo = dataManager.getAccountInfo();
                } catch (SeafException e) {
                    throw new RuntimeException(e);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public AccountInfo getAccountInfo() {
        return accountInfo;
    }

    private void getNotValidFilesDownload(List<SeafDirent> dirents, List<String> notValidDirentsID, String repoID, String path) {

        for (int i = 0; i < dirents.size(); i++) {
            SeafDirent dirent = dirents.get(i);
            String direntPath = path.endsWith("/") ? (path + dirent.name) : (path + "/" + dirent.name);
            if (dirent.isDir()) {

                try {
                    List<SeafDirent> subdirents = dataManager.getDirentsFromServer(repoID, direntPath);
                    getNotValidFilesDownload(subdirents, notValidDirentsID, repoID, direntPath);
                } catch (SeafException e) {
                    e.printStackTrace();
                }

            } else {
                if (!ValidatesFiles.isValidFileCloud(account, accountInfo, dirent.name, direntPath, dirent.getFileSize(), repoID)) {
                    notValidDirentsID.add(dirent.id);
                }
            }
        }

    }

    private void getAllIDFilesDownload(List<SeafDirent> dirents, List<String> allDirentsID, String repoID, String path) {

        for (int i = 0; i < dirents.size(); i++) {
            SeafDirent dirent = dirents.get(i);
            String direntPath = path.endsWith("/") ? (path + dirent.name) : (path + "/" + dirent.name);
            if (dirent.isDir()) {

                try {
                    List<SeafDirent> subdirents = dataManager.getDirentsFromServer(repoID, direntPath);
                    getAllIDFilesDownload(subdirents, allDirentsID, repoID, direntPath);
                } catch (SeafException e) {
                    e.printStackTrace();
                }

            } else {
                allDirentsID.add(dirent.id);
            }
        }

    }


    private void downloadFiles(List<SeafDirent> dirents, String repoID, String repoPath, String repoName) {

        constraintLoadingContainer.setVisibility(View.VISIBLE);
        ExecutorService service = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        service.execute(() -> {

            SeafException err = null;
            int fileCount = 0;
            AtomicBoolean uploading = new AtomicBoolean(false);

            List<String> allIDDownloadDirents = new ArrayList<>();
            List<String> direntsNotValidID = new ArrayList<>();

            getAllIDFilesDownload(dirents, allIDDownloadDirents, repoID, repoPath);
            getNotValidFilesDownload(dirents, direntsNotValidID, repoID, repoPath);

            ContinueDialog continueDialog = generatedDownloadContinueDialog(
                    uploading, repoPath, repoID, repoName,
                    dirents, direntsNotValidID, err, fileCount);

            int ChangeType;

            if (allIDDownloadDirents.size() > 1) {
                ChangeType = ChangePlanDialog.NOT_ALLOWED_FILES;
            } else {
                ChangeType = ChangePlanDialog.NOT_ALLOWED_FILE;
            }

            if (!direntsNotValidID.isEmpty()) {
                handler.post(() -> {
                    try {
                        ChangePlanDialog
                                .build(mActivity, ChangeType)
                                .setResponse(new ChangePlanDialog.ChangePlanDialogResponse() {
                                    @Override
                                    public void changePlanDialogResponseYes() {
                                        finishAddTaskDownloads(uploading, err, fileCount);
                                        Utils.openWebPlans(mActivity);
                                    }

                                    @Override
                                    public void changePlanDialogResponseNo() {
                                        if ((allIDDownloadDirents.size() - direntsNotValidID.size()) > 0) {
                                            continueDialog.show();
                                        } else {
                                            finishAddTaskDownloads(uploading, err, fileCount);
                                        }
                                    }

                                    @Override
                                    public void changePlanDialogResponseNeither() {
                                        if ((allIDDownloadDirents.size() - direntsNotValidID.size()) > 0) {
                                            continueDialog.show();
                                        } else {
                                            finishAddTaskDownloads(uploading, err, fileCount);
                                        }
                                    }
                                }).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } else {

                uploading.set(true);
                addFilesToDownloadTask(repoPath, repoID, repoName, dirents, direntsNotValidID, err, fileCount);
                handler.post(() -> {
                    finishAddTaskDownloads(uploading, err, fileCount);
                });

            }
        });
    }

    private void addFilesToDownloadTask(
            String dirPath, String repoID, String repoName, List<SeafDirent> dirents,
            List<String> direntsNotValidID, SeafException err, int fileCount
    ) {

        ArrayList<String> dirPaths = Lists.newArrayList(dirPath);
        for (int i = 0; i < dirPaths.size(); i++) {
            if (i > 0) {
                try {
                    dirents = getDataManager().getDirentsFromServer(repoID, dirPaths.get(i));
                } catch (SeafException e) {
                    err = e;
                    Log.e(DEBUG_TAG, e.getMessage() + e.getCode());
                }
            }

            if (dirents == null) continue;

            for (SeafDirent seafDirent : dirents) {
                if (seafDirent.isDir()) {
                    // download files recursively
                    dirPaths.add(Utils.pathJoin(dirPaths.get(i), seafDirent.name));
                } else {
                    File localCachedFile = getDataManager().getLocalCachedFile(repoName, repoID, Utils.pathJoin(dirPaths.get(i), seafDirent.name), seafDirent.id);
                    if (localCachedFile != null) {
                        continue;
                    }

                    // txService maybe null if layout orientation has changed
                    // e.g. landscape and portrait switch
                    if (txService == null) return;

                    if (!direntsNotValidID.contains(seafDirent.id)) {
                        txService.addTaskToDownloadQue(account, repoName, repoID, Utils.pathJoin(dirPaths.get(i), seafDirent.name));
                        fileCount++;
                    }

                }

            }
        }

    }

    private void finishAddTaskDownloads(AtomicBoolean uploading, SeafException err, int fileCount) {

        getReposFragment().showLoading(false);
        constraintLoadingContainer.setVisibility(View.GONE);

        if (uploading.get()) {
            if (err != null) {
                showShortToast(BrowserActivity.this, R.string.transfer_list_network_error);
                return;
            }

            if (fileCount == 0)
                showShortToast(BrowserActivity.this, R.string.transfer_download_no_task);
            else {
                showShortToast(BrowserActivity.this, getResources().getQuantityString(R.plurals.transfer_download_started, fileCount, fileCount));

                if (!txService.hasDownloadNotifProvider()) {
                    DownloadNotificationProvider provider = new DownloadNotificationProvider(txService.getDownloadTaskManager(), txService);
                    txService.saveDownloadNotifProvider(provider);
                }

            }

        }

    }

    private ContinueDialog generatedDownloadContinueDialog(
            AtomicBoolean uploading, String repoPath, String repoID, String repoName,
            List<SeafDirent> dirents, List<String> direntsNotValidID, SeafException err,
            int fileCount) {

        ContinueDialog continueDialog = ContinueDialog.build(mActivity, ContinueDialog.DOWNLOAD_FILES)
                .setOnContinueTask(new ContinueDialog.ContinueDialogContinueTask() {
                    @Override
                    public void continueTask() {
                        ExecutorService serviceDownload = Executors.newSingleThreadExecutor();
                        Handler handlerDownload = new Handler(Looper.getMainLooper());
                        serviceDownload.execute(() -> {
                            addFilesToDownloadTask(repoPath, repoID, repoName, dirents, direntsNotValidID, err, fileCount);
                            uploading.set(true);
                            handlerDownload.post(() -> {
                                finishAddTaskDownloads(uploading, err, fileCount);
                            });
                        });
                    }

                    @Override
                    public void cancelTask() {
                        finishAddTaskDownloads(uploading, err, fileCount);
                    }
                });

        return continueDialog;

    }


    private void uploadFiles(List<String> pathsFiles, SeafRepo repo, boolean replace) {

        List<String> pathsFilesTemp = new ArrayList<>();
        List<String> pathsFilesNotValid = new ArrayList<>();
        pathsFilesTemp.addAll(pathsFiles);

        ContinueDialog continueDialog = generatedUploadContinueDialog(repo, pathsFilesTemp, pathsFilesNotValid);

        for (String pathFile : pathsFilesTemp) {
            if (!ValidatesFiles.isValidFile(mActivity, accountInfo, Uri.fromFile(new File(pathFile)))) {
                pathsFilesNotValid.add(pathFile);
            }
        }

        if (pathsFilesNotValid.isEmpty()) {
            uploadValidFiles(repo, pathsFiles, pathsFilesNotValid, replace);
        } else if (pathsFilesNotValid.size() == pathsFilesTemp.size()) {
            int changeType = pathsFilesTemp.size() > 1 ? ChangePlanDialog.NOT_ALLOWED_FILES : ChangePlanDialog.NOT_ALLOWED_FILE;

            ChangePlanDialog.build(mActivity, changeType)
                    .setResponse(new ChangePlanDialog.ChangePlanDialogResponse() {
                        @Override
                        public void changePlanDialogResponseYes() {
                            Utils.openWebPlans(mActivity);
                        }

                        @Override
                        public void changePlanDialogResponseNo() {

                        }

                        @Override
                        public void changePlanDialogResponseNeither() {

                        }
                    }).show();

        } else {
            ChangePlanDialog.build(mActivity, ChangePlanDialog.NOT_ALLOWED_FILES)
                    .setResponse(new ChangePlanDialog.ChangePlanDialogResponse() {
                        @Override
                        public void changePlanDialogResponseYes() {
                            Utils.openWebPlans(mActivity);
                        }

                        @Override
                        public void changePlanDialogResponseNo() {
                            continueDialog.show();
                        }

                        @Override
                        public void changePlanDialogResponseNeither() {
                            continueDialog.show();
                        }
                    }).show();
        }
    }

    private ContinueDialog generatedUploadContinueDialog(SeafRepo repo, List<String> pathsFiles, List<String> pathsFilesNotValid) {
        ContinueDialog continueDialog = ContinueDialog.build(mActivity, ContinueDialog.UPLOAD_FILES)
                .setOnContinueTask(new ContinueDialog.ContinueDialogContinueTask() {
                    @Override
                    public void continueTask() {
                        uploadValidFiles(repo, pathsFiles, pathsFilesNotValid, false);
                    }

                    @Override
                    public void cancelTask() {

                    }
                });

        return continueDialog;
    }


    private List<SeafDirentTrash> getSyncDirentsTrash() {
        AtomicReference<List<SeafDirentTrash>> direntTrashList = new AtomicReference<>(new ArrayList<>());

        ExecutorService service = Executors.newSingleThreadExecutor();
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message mesg) {
                throw new RuntimeException();
            }

        };

        service.execute(() -> {
            try {
                direntTrashList.set(dataManager.getTrashDirentsFromServer(navContext.getRepoID(), navContext.getDirPath()));
            } catch (SeafException e) {
                throw new RuntimeException(e);
            }

            handler.sendMessage(handler.obtainMessage());
        });


        try {
            Looper.loop();
        } catch (RuntimeException e) {
        }

        return direntTrashList.get();
    }

    private void checkItemsInRecycleBin(List<String> pathsFiles, List<String> pathsFilesNotValid) {
        List<String> elementsInTrahs = new ArrayList<>();

        List<SeafDirentTrash> direntTrashList = getSyncDirentsTrash();

        for (SeafDirentTrash direntTrash : direntTrashList) {
            String parentPath = direntTrash.path.endsWith("/") ? direntTrash.path : direntTrash.path + "/";
            String contextPath = navContext.getDirPath().endsWith("/") ? navContext.getDirPath() : navContext.getDirPath() + "/";

            for (String path : pathsFiles) {
                if (!pathsFilesNotValid.contains(path)) {
                    String nameFile = Utils.fileNameFromPath(path);
                    if (parentPath.equals(contextPath) && direntTrash.isDir() == false && direntTrash.getTitle().equals(nameFile)) {
                        elementsInTrahs.add(path);
                    }
                }
            }
        }

        if (!elementsInTrahs.isEmpty()) {
            int type = elementsInTrahs.size() > 1 ? StepRecycleBin.MULTIPLE_FILES_IN_TRASH : StepRecycleBin.SINGLE_FILE_IN_TRASH;

            final Handler handler = new Handler() {
                @Override
                public void handleMessage(Message mesg) {
                    throw new RuntimeException();
                }

            };

            StepRecycleBin.build(mActivity, type).setOnReplaceTrash(new StepRecycleBin.ReplaceTrashTask() {
                @Override
                public void continueTask() {
                    handler.sendMessage(handler.obtainMessage());
                }

                @Override
                public void cancelTask() {
                    pathsFilesNotValid.addAll(elementsInTrahs);
                    handler.sendMessage(handler.obtainMessage());
                }
            }).show();

            try {
                Looper.loop();
            } catch (RuntimeException e) {
            }
        }
    }

    private void out__uploadValidFiles(SeafRepo repo, List<String> pathsFiles, List<String> pathsFilesNotValid) {
        checkItemsInRecycleBin(pathsFiles, pathsFilesNotValid);
        for (String path : pathsFiles) {
            if (!pathsFilesNotValid.contains(path)) {
                if (repo != null && repo.canLocalDecrypt()) {
                    addUploadBlocksTask(navContext.getRepoID(), navContext.getRepoName(), navContext.getDirPath(), path);
                } else {
                    addUploadTask(navContext.getRepoID(), navContext.getRepoName(), navContext.getDirPath(), path);
                }
            }
        }
    }

    private void uploadValidFiles(SeafRepo repo, List<String> pathsFiles, List<String> pathsFilesNotValid, boolean replace) {
        checkItemsInRecycleBin(pathsFiles, pathsFilesNotValid);
        for (String path : pathsFiles) {
            if (!pathsFilesNotValid.contains(path)) {
                File file = new File(path);
                if (repo != null && repo.canLocalDecrypt()) {
                    addUpdateBlocksTask(repo.id, repo.name, this.navContext.getDirPath(), file.getAbsolutePath());
                } else if (replace) {
                    addUpdateTask(this.navContext.getRepoID(), this.navContext.getRepoName(), this.navContext.getDirPath(), file.getAbsolutePath());
                } else {
                    addUploadTask(this.navContext.getRepoID(), this.navContext.getRepoName(), this.navContext.getDirPath(), file.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Initiates the process of recovering repositories from the trash.
     * This method triggers the refreshing of repositories in the trash.
     * Call this method to recover repositories that have been deleted and moved to the trash.
     */
    private void recoveringReposTrash() {
        getReposFragment().onClickRefreshReposTrash();
    }

    /**
     * Retrieves a single repository from the trash and attempts to restore it.
     *
     * @param repo The SeafRepoTrash object representing the repository in the trash to be retrieved.
     */
    public void retrieveRepository(SeafRepoTrash repo) {
        retrieveRepos(Arrays.asList(repo));
    }

    /**
     * Retrieves repositories from the trash and attempts to restore them.
     *
     * @param repoTrashes The list of SeafRepoTrash objects representing repositories in the trash.
     */

    public void retrieveRepos(List<SeafRepoTrash> repoTrashes) {
        constraintLoadingContainer.setVisibility(View.VISIBLE);
        List<SeafRepoTrash> reposTrashesTemp = new ArrayList<>();
        reposTrashesTemp.addAll(repoTrashes);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executorService.execute(() -> {
            if (dataManager != null) {
                boolean err = false, space = true;
                for (SeafRepoTrash repo : reposTrashesTemp) {
                    try {
                        accountInfo = dataManager.getAccountInfo();
                        if (repo.size + accountInfo.getUsage() < accountInfo.getTotal()) {
                            dataManager.retrieveRepository(repo.getID());
                        } else {
                            space = false;
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        err = true;
                    }
                }

                boolean finalErr = err;
                boolean finalSpace = space;

                List<SeafRepoTrash> cachedReposTrash = new ArrayList<>();

                try {
                    cachedReposTrash = getDataManager().getReposTrashFromServer();
                } catch (SeafException e) {
                    e.printStackTrace();
                }

                List<SeafRepoTrash> finalCachedReposTrash = cachedReposTrash;
                handler.post(() -> {

                    if (getReposFragment().getActionMode() == null) {
                        recoveringReposTrash();
                    } else {
                        getReposFragment().getAdapter().setReposTrash(finalCachedReposTrash);
                        getReposFragment().getAdapter().notifyDataSetChanged();
                        // update contextual action bar (CAB) title
                        getReposFragment().updateContextualActionBar();
                        if (finalCachedReposTrash.size() == 0)
                            getReposFragment().getEmptyView().setVisibility(View.VISIBLE);
                    }

                    try {
                        if (finalErr) {
                            showShortToast(mActivity, getResources().getString(R.string.unknow_error));
                        } else if (!finalSpace) {
                            if (accountInfo.getPlan() != AccountPlans.Platinum) {
                                ChangePlanDialog.build(mActivity, ChangePlanDialog.WITHOUT_STORAGE).setResponse(new ChangePlanDialog.ChangePlanDialogResponse() {
                                    @Override
                                    public void changePlanDialogResponseYes() {
                                        Utils.openWebPlans(mActivity);
                                    }

                                    @Override
                                    public void changePlanDialogResponseNo() {

                                    }

                                    @Override
                                    public void changePlanDialogResponseNeither() {

                                    }
                                }).show();
                            } else {
                                showShortToast(mActivity, getResources().getString(R.string.without_storage));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    constraintLoadingContainer.setVisibility(View.GONE);
                });
            }
        });
    }


    /**
     * Initiates the recovery process of deleted directory items.
     * This method sets the context to indicate that directory items are being recovered.
     * It also triggers the refresh of directory items in the trash.
     */
    private void recoveringDirentsTrash() {
        getNavContext().setDirentsTrash(true);
        getNavContext().setDirNavigateRoot(getNavContext().getDirPath());
        getReposFragment().onClickRefreshDirentsTrash();
    }

    /**
     * Initiates the retrieval of a deleted directory item.
     *
     * @param dirent The SeafDirentTrash object representing the deleted directory item.
     */
    public void retrieveDirent(SeafDirentTrash dirent) {
        retrieveDirents(Arrays.asList(dirent));
    }

    /**
     * Internal method for recovering deleted directory items.
     *
     * @param dirents The SeafDirentTrash object representing the deleted directory item.
     */
    private void retrieveDirents(List<SeafDirentTrash> dirents) {
        constraintLoadingContainer.setVisibility(View.VISIBLE);
        List<SeafDirentTrash> direntsTemp = new ArrayList<>();
        direntsTemp.addAll(dirents);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executorService.execute(() -> {
            if (dataManager != null) {
                boolean err = false, space = true;
                for (SeafDirentTrash dirent : direntsTemp) {
                    try {
                        long uploadsSize = 0;
                        accountInfo = dataManager.getAccountInfo();
                        for (UploadTaskInfo uploadTaskInfo : getTransferService().getAllUploadTaskInfos()) {
                            if (uploadTaskInfo.state == TaskState.TRANSFERRING || uploadTaskInfo.state == TaskState.INIT) {
                                uploadsSize += uploadTaskInfo.totalSize;
                            }
                        }

                        if (dirent.isDir() && (accountInfo.getUsage() + uploadsSize) < accountInfo.getTotal()) {
                            dataManager.retrieveDirent(dirent, getNavContext().getRepoID());
                            accountInfo = dataManager.getAccountInfo();
                            if ((accountInfo.getUsage() + uploadsSize) > accountInfo.getTotal()) {
                                dataManager.delete(dirent.id, navContext.getRepoID(), Utils.pathJoin(dirent.path, dirent.name), true);
                                space = false;
                            }
                        } else if (!dirent.isDir() && (dirent.size + accountInfo.getUsage() + uploadsSize) <= accountInfo.getTotal()) {
                            dataManager.retrieveDirent(dirent, getNavContext().getRepoID());
                        } else {
                            space = false;
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        err = true;
                    }
                }

                boolean finalErr = err;
                boolean finalSpace = space;

                List<SeafDirentTrash> cachedDirents = new ArrayList<>();

                try {
                    cachedDirents = getDataManager().getTrashDirentsFromServer(getNavContext().getRepoID(), getNavContext().getDirPath());
                } catch (SeafException e) {
                    e.printStackTrace();
                }

                List<SeafDirentTrash> finalCachedDirents = cachedDirents;
                handler.post(() -> {

                    if (getReposFragment().getActionMode() == null) {
                        recoveringDirentsTrash();
                    } else {
                        getReposFragment().getAdapter().setItemsTrash(finalCachedDirents);
                        getReposFragment().getAdapter().notifyDataSetChanged();
                        // update contextual action bar (CAB) title
                        getReposFragment().updateContextualActionBar();
                        if (finalCachedDirents.size() == 0)
                            getReposFragment().getEmptyView().setVisibility(View.VISIBLE);
                    }


                    try {
                        if (finalErr) {
                            showShortToast(mActivity, getResources().getString(R.string.unknow_error));
                        } else if (!finalSpace) {
                            if (accountInfo.getPlan() != AccountPlans.Platinum) {
                                ChangePlanDialog.build(mActivity, ChangePlanDialog.WITHOUT_STORAGE).setResponse(new ChangePlanDialog.ChangePlanDialogResponse() {
                                    @Override
                                    public void changePlanDialogResponseYes() {
                                        Utils.openWebPlans(mActivity);
                                    }

                                    @Override
                                    public void changePlanDialogResponseNo() {

                                    }

                                    @Override
                                    public void changePlanDialogResponseNeither() {

                                    }
                                }).show();
                            } else {
                                showShortToast(mActivity, getResources().getString(R.string.without_storage));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    constraintLoadingContainer.setVisibility(View.GONE);
                });
            }
        });
    }

    public boolean isVisibleLoadingContainer() {
        return constraintLoadingContainer.getVisibility() == View.VISIBLE;
    }

    private void cleanTrashRepo() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            dataManager.cleanTrashRepo(navContext.getRepoID());
            handler.post(() -> {
                recoveringDirentsTrash();
            });
        });
    }

    private void checkManagerStoragePermission() {
        if (!XXPermissions.isGranted(BrowserActivity.this, Permission.MANAGE_EXTERNAL_STORAGE)
                && !XXPermissions.isPermanentDenied(BrowserActivity.this, Permission.MANAGE_EXTERNAL_STORAGE)) {

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {

                RequestStorageDialog.build(this)
                        .setIRequestStorageDialog(new RequestStorageDialog.IRequestStorageDialog() {
                            @Override
                            public void acceptRequestStorage() {
                                requestManagerStoragePermission();
                            }

                            @Override
                            public void denieRequestStorage() {
                                Snackbar.make(mLayout, R.string.permission_read_exteral_storage_rationale, Snackbar.LENGTH_INDEFINITE).setAction(R.string.settings, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        XXPermissions.with(BrowserActivity.this).permission(Permission.MANAGE_EXTERNAL_STORAGE).request(new OnPermissionCallback() {
                                            @Override
                                            public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                                            }
                                        });
                                    }
                                }).show();
                            }
                        }).show();


            } else {
                requestManagerStoragePermission();
            }

        }
    }

    private void requestManagerStoragePermission() {
        XXPermissions.with(BrowserActivity.this).permission(Permission.MANAGE_EXTERNAL_STORAGE).request(new OnPermissionCallback() {
            @Override
            public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                if (!allGranted) {
                    Snackbar.make(mLayout, R.string.permission_read_exteral_storage_rationale, Snackbar.LENGTH_INDEFINITE).setAction(R.string.settings, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            XXPermissions.with(BrowserActivity.this).permission(Permission.MANAGE_EXTERNAL_STORAGE).request(new OnPermissionCallback() {
                                @Override
                                public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                                }
                            });
                        }
                    }).show();
                }
            }

            @Override
            public void onDenied(List<String> permissions, boolean never) {
                if (!never) {
                    Snackbar.make(mLayout, R.string.permission_read_exteral_storage_rationale, Snackbar.LENGTH_INDEFINITE).setAction(R.string.settings, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            XXPermissions.with(BrowserActivity.this).permission(Permission.MANAGE_EXTERNAL_STORAGE).request(new OnPermissionCallback() {
                                @Override
                                public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                                }
                            });
                        }
                    }).show();
                }
            }
        });
    }

    public TabLayout getmTabLayout() {
        return mTabLayout;
    }
}
