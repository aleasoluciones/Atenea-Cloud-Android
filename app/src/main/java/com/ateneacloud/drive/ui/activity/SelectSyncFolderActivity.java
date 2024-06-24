package com.ateneacloud.drive.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ateneacloud.drive.R;
import com.ateneacloud.drive.folderbackup.selectfolder.BeanListManager;
import com.ateneacloud.drive.folderbackup.selectfolder.Constants;
import com.ateneacloud.drive.folderbackup.selectfolder.FileBean;
import com.ateneacloud.drive.folderbackup.selectfolder.FileListAdapter;
import com.ateneacloud.drive.folderbackup.selectfolder.FileTools;
import com.ateneacloud.drive.folderbackup.selectfolder.OnFileItemClickListener;
import com.ateneacloud.drive.folderbackup.selectfolder.SelectOptions;
import com.ateneacloud.drive.folderbackup.selectfolder.TabBarFileBean;
import com.ateneacloud.drive.folderbackup.selectfolder.TabBarFileListAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SelectSyncFolderActivity extends AppCompatActivity {

    private RecyclerView mTabBarFileRecyclerView, mFileRecyclerView;
    private SelectOptions mSelectOptions;
    private List<String> allPathsList;
    private List<String> mShowFileTypes;
    private int mSortType;
    private List<FileBean> mFileList;
    private List<TabBarFileBean> mTabbarFileList;
    private String mCurrentPath;
    private FileListAdapter mFileListAdapter;
    private TabBarFileListAdapter mTabBarFileListAdapter;
    private Button mButton;
    private String initialPath;

    private String resultData;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.folder_selection_fragment);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);


        mButton = (Button) findViewById(R.id.bt_dir_click_to_finish);

        mFileRecyclerView = (RecyclerView) findViewById(R.id.rcv_files_list);
        mFileRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mFileListAdapter = new FileListAdapter(this, mFileList, false);
        mFileRecyclerView.setAdapter(mFileListAdapter);

        mTabBarFileRecyclerView = (RecyclerView) findViewById(R.id.rcv_tabbar_files_list);
        mTabBarFileRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mTabBarFileListAdapter = new TabBarFileListAdapter(this, mTabbarFileList);
        mTabBarFileRecyclerView.setAdapter(mTabBarFileListAdapter);

        init();
        initData();

    }

    private void init() {

        mButton.setVisibility(View.VISIBLE);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Devuelve la ruta seleccionada como resultado
                returnResultAndFinish();
            }
        });

        mFileListAdapter.setOnItemClickListener(new OnFileItemClickListener() {
            @Override
            public void onItemClick(int position) {
                FileBean item = mFileList.get(position);
                if (item.isFile()) {
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.selection_file_type), Toast.LENGTH_SHORT).show();
                } else {
                    mCurrentPath = item.getFilePath();
                    refreshFileAndTabBar(BeanListManager.TYPE_ADD_TAB_BAR);

                    resultData = item.getFilePath();

                }
            }

            @Override
            public void onCheckBoxClick(View view, int position) {
//                FileBean item = mFileList.get(position);
//                for (FileBean fb : mFileList) {
//                    if (item.equals(fb)) {
//                        if (fb.isChecked()) {
//                            for (int i = 0; i < selectPaths.size(); i++) {
//                                if (item.getFilePath().equals(selectPaths.get(i))) {
//                                    selectPaths.remove(i);
//                                    i--;
//                                }
//                            }
//                            fb.setChecked(false);
//
//                        } else {
//                            selectPaths.add(item.getFilePath());
//                            fb.setChecked(true);
//                        }
//                        mActivity.setFolderPathList(selectPaths);
//                    }
//                }
//                view.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        mFileListAdapter.updateListData(mFileList);
//                        mFileListAdapter.notifyDataSetChanged();
//                    }
//                });
            }
        });

        mTabBarFileListAdapter.setOnItemClickListener(new OnFileItemClickListener() {
            @Override
            public void onItemClick(int position) {
                TabBarFileBean item = mTabbarFileList.get(position);
                mCurrentPath = item.getFilePath();

                if (mTabbarFileList.size() > 1) {
                    refreshFileAndTabBar(BeanListManager.TYPE_DEL_TAB_BAR);
                }
            }

            @Override
            public void onCheckBoxClick(View view, int position) {

            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        finish();
    }

    @Override
    public void onPause() {
        super.onPause();
        finish();
    }

    private void initData() {
        mSelectOptions = SelectOptions.getResetInstance(this);
        allPathsList = initRootPath(this);
        mShowFileTypes = Arrays.asList(mSelectOptions.getShowFileTypes());
        mSortType = mSelectOptions.getSortType();
        mFileList = new ArrayList<>();
        mTabbarFileList = new ArrayList<>();
        refreshFileAndTabBar(BeanListManager.TYPE_INIT_TAB_BAR);
    }

    private List<String> initRootPath(Activity activity) {
        List<String> allPaths = FileTools.getAllPaths(activity);
        mCurrentPath = mSelectOptions.rootPath;
        if (mCurrentPath == null) {
            if (allPaths.isEmpty()) {
                mCurrentPath = Constants.DEFAULT_ROOTPATH;
            } else {
                mCurrentPath = allPaths.get(0);
            }
        }
        initialPath = mCurrentPath;
        return allPaths;
    }

    private void refreshFileAndTabBar(int tabbarType) {
        BeanListManager.upDataFileBeanListByAsyn(this, new ArrayList<>(), mFileList, mFileListAdapter,
                mCurrentPath, mShowFileTypes, mSortType);
        BeanListManager.upDataTabbarFileBeanList(mTabbarFileList, mTabBarFileListAdapter,
                mCurrentPath, tabbarType, allPathsList);
    }

    @Override
    public void onBackPressed() {
        if (mCurrentPath.equals(initialPath) || allPathsList.contains(mCurrentPath)) {
            finish();
        } else {
            mCurrentPath = FileTools.getParentPath(mCurrentPath);
            refreshFileAndTabBar(BeanListManager.TYPE_DEL_TAB_BAR);
        }
    }

    private void returnResultAndFinish() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("selected_folder_path", resultData);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

}

