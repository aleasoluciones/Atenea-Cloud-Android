package com.ateneacloud.drive.folderbackup;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.gson.Gson;
import com.ateneacloud.drive.R;
import com.ateneacloud.drive.SettingsManager;
import com.ateneacloud.drive.account.Account;
import com.ateneacloud.drive.data.SeafRepo;
import com.ateneacloud.drive.folderbackup.selectfolder.SelectBackupFolderFragment;
import com.ateneacloud.drive.folderbackup.selectfolder.StringTools;
import com.ateneacloud.drive.ui.activity.BaseActivity;
import com.ateneacloud.drive.ui.activity.SeafilePathChooserActivity;
import com.ateneacloud.drive.ui.fragment.SettingsFragment;
import com.ateneacloud.drive.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class FolderBackupConfigActivity extends BaseActivity {

    public String DEBUG_TAG = FolderBackupConfigActivity.class.getSimpleName();
    public static final String BACKUP_SELECT_REPO = "backup_select_repo";
    public static final String BACKUP_SELECT_PATHS = "backup_select_paths";
    public static final String BACKUP_SELECT_PATHS_SWITCH = "backup_select_paths_switch";

    private SelectBackupFolderFragment mBucketsFragment;
    private CloudLibraryChooserFragment mCloudLibFragment;

    private SeafRepo mSeafRepo;
    private Account mAccount;
    private boolean isChooseFolderPage;
    private boolean isChooseLibPage;
    private FolderBackupDBHelper databaseHelper;
    private FolderBackupService mBackupService;
    private List<String> selectFolderPaths;
    private Activity mActivity;
    private String originalBackupPaths;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.folder_backup_activity_layout);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        isChooseFolderPage = getIntent().getBooleanExtra(SettingsFragment.FOLDER_BACKUP_REMOTE_PATH, false);
        isChooseLibPage = getIntent().getBooleanExtra(SettingsFragment.FOLDER_BACKUP_REMOTE_LIBRARY, false);

        ViewPager mViewPager = (ViewPager) findViewById(R.id.cuc_pager);
        FragmentManager fm = getSupportFragmentManager();
        mViewPager.setAdapter(new FolderBackupConfigAdapter(fm));
        mViewPager.setOffscreenPageLimit(2);

        databaseHelper = FolderBackupDBHelper.getDatabaseHelper();

        //bind service
//        Intent bindIntent = new Intent(this, FolderBackupService.class);
//        bindService(bindIntent, mFolderBackupConnection, Context.BIND_AUTO_CREATE);

        mActivity = this;

        originalBackupPaths = SettingsManager.instance().getBackupPaths();

        if (isChooseFolderPage && !TextUtils.isEmpty(originalBackupPaths)) {
            selectFolderPaths = StringTools.getJsonToList(originalBackupPaths);
        }
    }

    public void saveBackupLibrary(Account account, SeafRepo seafRepo) {
        mSeafRepo = seafRepo;
        mAccount = account;
    }

    public FolderBackupDBHelper getDatabaseHelper() {
        return databaseHelper;
    }

    public void setFolderPathList(List<String> selectFileList) {
        this.selectFolderPaths = selectFileList;
    }

    public List<String> getSelectFolderPath() {
        return selectFolderPaths;
    }

    public void saveRepoConfig() {
        if (!isChooseLibPage) {
            return;
        }

        //FIX an issue: When no folder or library is selected, a crash occurs
        if (null == mSeafRepo || null == mAccount) {
            Utils.utilsLogInfo(false, "----------No repo is selected");
            return;
        }

        Intent intent = new Intent();

        // update cloud library data
        if (mSeafRepo != null && mAccount != null) {
            intent.putExtra(SeafilePathChooserActivity.DATA_REPO_NAME, mSeafRepo.name);
            intent.putExtra(SeafilePathChooserActivity.DATA_REPO_ID, mSeafRepo.id);
            intent.putExtra(SeafilePathChooserActivity.DATA_ACCOUNT, mAccount);
            intent.putExtra(BACKUP_SELECT_REPO, true);
            SettingsManager.instance().saveBackupEmail(mAccount.getEmail());
            try {
                RepoConfig repoConfig = databaseHelper.getRepoConfig(mAccount.getEmail());
                if (repoConfig != null) {
                    databaseHelper.updateRepoConfig(mAccount.getEmail(), mSeafRepo.getID(), mSeafRepo.getName());
                } else {
                    databaseHelper.saveRepoConfig(mAccount.getEmail(), mSeafRepo.getID(), mSeafRepo.getName());
                }
                Toast.makeText(mActivity, mActivity.getString(R.string.folder_backup_select_repo_update), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Utils.utilsLogInfo(true, "=saveRepoConfig=======================" + e.toString());
            }
        }

        setResult(RESULT_OK, intent);

        boolean automaticBackup = SettingsManager.instance().isFolderAutomaticBackup();
        if (automaticBackup && mBackupService != null) {
            mBackupService.backupFolder(mAccount.getEmail());
        }
    }

    public void saveFolderConfig() {
        if (!isChooseFolderPage) {
            return;
        }

        //FIX an issue: When no folder or library is selected, a crash occurs
        if (selectFolderPaths == null || selectFolderPaths.isEmpty()) {
            Utils.utilsLogInfo(false, "----------No folder is selected");

            //clear local storage
            SettingsManager.instance().saveBackupPaths("");

            Intent intent = new Intent();
            intent.putExtra(BACKUP_SELECT_PATHS_SWITCH, true);
            setResult(RESULT_OK, intent);
            return;
        }

        String backupEmail = SettingsManager.instance().getBackupEmail();
        String strJsonPath = new Gson().toJson(selectFolderPaths);

        if ((TextUtils.isEmpty(originalBackupPaths) && !TextUtils.isEmpty(strJsonPath)) || !originalBackupPaths.equals(strJsonPath)) {
            mBackupService.startFolderMonitor(selectFolderPaths);
            Utils.utilsLogInfo(false, "----------Restart monitoring FolderMonitor");
        }

        if (!TextUtils.isEmpty(originalBackupPaths) && TextUtils.isEmpty(strJsonPath)) {
            mBackupService.stopFolderMonitor();
        }

        SettingsManager.instance().saveBackupPaths(strJsonPath);
        Intent intent = new Intent();
        if (selectFolderPaths != null) {
            intent.putStringArrayListExtra(BACKUP_SELECT_PATHS, (ArrayList<String>) selectFolderPaths);
            intent.putExtra(BACKUP_SELECT_PATHS_SWITCH, true);
        }

        setResult(RESULT_OK, intent);

        boolean folderAutomaticBackup = SettingsManager.instance().isFolderAutomaticBackup();
        if (folderAutomaticBackup && mBackupService != null) {
            mBackupService.backupFolder(backupEmail);
        }
    }

    @Override
    public void onBackPressed() {
        if (mBucketsFragment != null && mBucketsFragment.onBackPressed()) {
            return;
        }
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    public boolean isChooseDirPage() {
        return isChooseFolderPage;
    }

    @Override
    protected void onDestroy() {
        if (mBackupService != null) {
            unbindService(mFolderBackupConnection);
            mBackupService = null;
        }
        super.onDestroy();
    }

    private final ServiceConnection mFolderBackupConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
//            FolderBackupService.FileBackupBinder fileBackupBinder = (FolderBackupService.FileBackupBinder) binder;
//            mBackupService = fileBackupBinder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mBackupService = null;
        }

    };

    private class FolderBackupConfigAdapter extends FragmentStatePagerAdapter {

        public FolderBackupConfigAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            if (isChooseLibPage) {
                return position == 0 ? new CloudLibraryChooserFragment() : null;
            }
            if (isChooseFolderPage) {
                switch (position) {
                    case 0:
                        mBucketsFragment = new SelectBackupFolderFragment();
                        return mBucketsFragment;
                    default:
                        return null;
                }

            }
            switch (position) {
                case 0:
                    mCloudLibFragment = new CloudLibraryChooserFragment();
                    return mCloudLibFragment;
                case 1:
                    mBucketsFragment = new SelectBackupFolderFragment();
                    return mBucketsFragment;
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            if (isChooseLibPage || isChooseFolderPage)
                return 1;
            else
                return 2;
        }
    }
}