package com.ateneacloud.drive.cameraupload;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;
import android.view.View;

import com.ateneacloud.drive.R;
import com.ateneacloud.drive.SettingsManager;
import com.ateneacloud.drive.account.Account;
import com.ateneacloud.drive.data.SeafRepo;
import com.ateneacloud.drive.ui.activity.BaseActivity;
import com.ateneacloud.drive.ui.activity.SeafilePathChooserActivity;
import com.ateneacloud.drive.ui.fragment.SettingsFragment;
import com.ateneacloud.drive.util.SystemSwitchUtils;

import java.util.List;

import me.relex.circleindicator.CircleIndicator;


/**
 * Camera upload configuration helper
 */
public class CameraUploadConfigActivity extends BaseActivity {
    public  String DEBUG_TAG = "CameraUploadConfigActivity";

    private ViewPager mViewPager;

    private CircleIndicator magicIndicator;
    private BucketsFragment mBucketsFragment;
    private CloudLibraryFragment mCloudLibFragment;
    private WhatToUploadFragment whatToUploadFragment;
    private SettingsManager sm;
    private SeafRepo mSeafRepo;
    private Account mAccount;
    /** handling data from configuration helper */
    private boolean isChooseBothPages;
    /** handling data from cloud library page */
    private boolean isChooseLibPage;
    /** handling data from local directory page */
    private boolean isChooseDirPage;
    private int mCurrentPosition;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        setContentView(R.layout.cuc_activity_layout);

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        isChooseBothPages = getIntent().getBooleanExtra(SettingsFragment.CAMERA_UPLOAD_BOTH_PAGES, false);
        isChooseLibPage = getIntent().getBooleanExtra(SettingsFragment.CAMERA_UPLOAD_REMOTE_LIBRARY, false);
        isChooseDirPage = getIntent().getBooleanExtra(SettingsFragment.CAMERA_UPLOAD_LOCAL_DIRECTORIES, false);

        mViewPager = (ViewPager) findViewById(R.id.cuc_pager);

        FragmentManager fm = getSupportFragmentManager();
        mViewPager.setAdapter(new CameraUploadConfigAdapter(fm));
        mViewPager.setOffscreenPageLimit(6);
        mViewPager.addOnPageChangeListener(pageChangeListener);

        magicIndicator = findViewById(R.id.cuc_indicator);
        magicIndicator.setViewPager(mViewPager);

        sm = SettingsManager.instance();

        if (isChooseLibPage || isChooseDirPage) {
            magicIndicator.setVisibility(View.GONE);
        }
    }

    /**
     * Page scroll listener.
     */
    private OnPageChangeListener pageChangeListener = new OnPageChangeListener() {

        @Override
        public void onPageScrollStateChanged(int scrollState) {}

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            mCurrentPosition = position;
        }

        @Override
        public void onPageSelected(int page){}
    };

    public void saveCameraUploadInfo(Account account, SeafRepo seafRepo) {
        mSeafRepo = seafRepo;
        mAccount = account;
    }

    public void saveSettings() {
        SystemSwitchUtils.getInstance(this).syncSwitchUtils();
        if (isChooseBothPages || isChooseDirPage) {

            SettingsManager settingsManager = SettingsManager.instance();
            List<String> selectedBuckets = mBucketsFragment.getSelectedBuckets();
            if (mBucketsFragment.isAutoScanSelected()) {
                selectedBuckets.clear();
            }
            // this is the only setting that is safed here. all other are returned to caller
            // and safed there...
            settingsManager.setCameraUploadBucketList(selectedBuckets);
        }

        Intent intent = new Intent();
        // update cloud library data
        if (mSeafRepo != null && mAccount != null) {
            intent.putExtra(SeafilePathChooserActivity.DATA_REPO_NAME, mSeafRepo.name);
            intent.putExtra(SeafilePathChooserActivity.DATA_REPO_ID, mSeafRepo.id);
            intent.putExtra(SeafilePathChooserActivity.DATA_ACCOUNT, mAccount);
        }

        setResult(RESULT_OK, intent);

    }


    @Override
    public void onBackPressed() {
        if (mCurrentPosition == 0) {
            setResult(RESULT_CANCELED);
            super.onBackPressed();
        } else {
            // navigate to previous page when press back button
            mCurrentPosition -= 1;
            mViewPager.setCurrentItem(mCurrentPosition);
        }
    }

    public boolean isChooseLibPage() {
        return isChooseLibPage;
    }

    public boolean isChooseDirPage() {
        return isChooseDirPage;
    }

    public void saveDataPlanAllowed(boolean isAllowed) {
        sm.saveDataPlanAllowed(isAllowed);
    }

    public void saveVideosAllowed(boolean isAllowed) {
        sm.saveVideosAllowed(isAllowed);
    }

    class CameraUploadConfigAdapter extends FragmentStatePagerAdapter {

        public CameraUploadConfigAdapter(FragmentManager fm) {
            super(fm);
        }

        // This method controls which fragment should be shown on a specific screen.
        @Override
        public Fragment getItem(int position) {

            if (isChooseLibPage) {
                return position == 0 ? new CloudLibraryFragment() : null;
            }

            if (isChooseDirPage) {
                switch (position) {
                    case 0:
                        mBucketsFragment = new BucketsFragment();
                        return mBucketsFragment;
                    default:
                        return null;
                }

            }

            // Assign the appropriate screen to the fragment object, based on which screen is displayed.
            switch (position) {
                case 0:
                    return new ConfigWelcomeFragment();
                case 1:
                    return new HowToUploadFragment();
                case 2:
                    whatToUploadFragment = new WhatToUploadFragment();
                    return whatToUploadFragment;
                case 3:
                    mBucketsFragment = new BucketsFragment();
                    return mBucketsFragment;
                case 4:
                    mCloudLibFragment = new CloudLibraryFragment();
                    return mCloudLibFragment;
                case 5:
                    return new ReadyToScanFragment();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            if (isChooseLibPage || isChooseDirPage)
                return 1;
            else
                return 6;
        }

    }
}
