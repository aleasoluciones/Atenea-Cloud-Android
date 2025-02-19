package com.ateneacloud.drive.ui.activity.search;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.CollectionUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.QuickAdapterHelper;
import com.chad.library.adapter.base.loadState.LoadState;
import com.chad.library.adapter.base.loadState.trailing.TrailingLoadStateAdapter;
import com.ateneacloud.drive.R;
import com.ateneacloud.drive.SeafException;
import com.ateneacloud.drive.account.Account;
import com.ateneacloud.drive.account.AccountInfo;
import com.ateneacloud.drive.account.AccountManager;
import com.ateneacloud.drive.data.DataManager;
import com.ateneacloud.drive.data.SeafRepo;
import com.ateneacloud.drive.data.SearchedFile;
import com.ateneacloud.drive.play.exoplayer.ExoVideoPlayerActivity;
import com.ateneacloud.drive.transfer.TransferService;
import com.ateneacloud.drive.ui.WidgetUtils;
import com.ateneacloud.drive.ui.activity.BaseActivity;
import com.ateneacloud.drive.ui.activity.FileActivity;
import com.ateneacloud.drive.ui.base.adapter.CustomLoadMoreAdapter;
import com.ateneacloud.drive.ui.dialog.ChangePlanDialog;
import com.ateneacloud.drive.util.ConcurrentAsyncTask;
import com.ateneacloud.drive.util.FileExtension;
import com.ateneacloud.drive.util.Utils;
import com.ateneacloud.drive.util.ValidatesFiles;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Search Activity
 */
public class Search2Activity extends BaseActivity implements View.OnClickListener, Toolbar.OnMenuItemClickListener {
    private static final String DEBUG_TAG = "SearchActivity";

    private static final String STATE_SEARCHED_RESULT = "searched_result";
    private String mSearchedRlt;
    private EditText mTextField;
    private ImageView mTextClearBtn;
    private View mSearchBtn;
    private RecyclerView mRecyclerView;

    private QuickAdapterHelper helper;
    private SearchRecyclerViewAdapter mAdapter;
    private DataManager dataManager;
    private TransferService txService = null;
    private Account account;

    private AccountInfo accountInfo;

    private Search2Activity search2Activity;

    private ConstraintLayout constraintSearchLoadingContainer;

    public static final int DOWNLOAD_FILE_REQUEST = 0;
    private int page = 1;
    private int PAGE_SIZE = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search2);
        mSearchBtn = findViewById(R.id.btn_search);
        mSearchBtn.setOnClickListener(this);
        mTextClearBtn = findViewById(R.id.btn_clear);
        mTextClearBtn.setOnClickListener(this);

        mTextField = findViewById(R.id.et_content);
        mTextField.setOnClickListener(this);
        mTextField.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        mTextField.addTextChangedListener(new SearchTextWatcher());
        mTextField.setOnEditorActionListener(new EditorActionListener());
        mTextField.requestFocus();

        setSupportActionBar(getActionBarToolbar());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.search_menu_item);

        mRecyclerView = findViewById(R.id.lv_search);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        constraintSearchLoadingContainer = findViewById(R.id.constraint_search_loading_container);

        initAdapter();

        initData();

        search2Activity = this;
    }

    private void initAdapter() {
        mAdapter = new SearchRecyclerViewAdapter(this);
        View t = findViewById(R.id.ll_message_content);
        mAdapter.setEmptyView(t);
        mAdapter.setEmptyViewEnable(true);
        mAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener<SearchedFile>() {
            @Override
            public void onClick(@NotNull BaseQuickAdapter<SearchedFile, ?> baseQuickAdapter, @NotNull View view, int i) {
                onSearchedFileSelected(mAdapter.getItems().get(i));
            }
        });

        CustomLoadMoreAdapter customLoadMoreAdapter = new CustomLoadMoreAdapter();
        customLoadMoreAdapter.setOnLoadMoreListener(new TrailingLoadStateAdapter.OnTrailingListener() {
            @Override
            public void onLoad() {
                loadNext(false);
            }

            @Override
            public void onFailRetry() {
                loadNext(false);
            }

            @Override
            public boolean isAllowLoading() {
                return true;
            }
        });


        helper = new QuickAdapterHelper.Builder(mAdapter)
                .setTrailingLoadStateAdapter(customLoadMoreAdapter)
                .build();
        mRecyclerView.setAdapter(helper.getAdapter());
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the searched result
        savedInstanceState.putString(STATE_SEARCHED_RESULT, mSearchedRlt);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);

        // Restore state members from saved instance
        mSearchedRlt = savedInstanceState.getString(STATE_SEARCHED_RESULT);

        // update ui
        if (dataManager != null) {
            ArrayList<SearchedFile> files = dataManager.parseSearchResult(mSearchedRlt);
            if (files != null) {
                mAdapter.submitList(files);
            }
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(DEBUG_TAG, "onDestroy is called");
        if (txService != null) {
            unbindService(mConnection);
            txService = null;
        }

        super.onDestroy();
    }

    private void initData() {
        AccountManager accountManager = new AccountManager(this);
        account = accountManager.getCurrentAccount();
        dataManager = new DataManager(account);
        loadAccountInfo();

        // bind transfer service
        Intent bIntent = new Intent(this, TransferService.class);
        bindService(bIntent, mConnection, Context.BIND_AUTO_CREATE);
        Log.d(DEBUG_TAG, "try bind TransferService");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getActionBarToolbar().setOnMenuItemClickListener(this);
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        final int id = v.getId();
        if (id == R.id.btn_search) {
            loadNext(true);
        } else if (id == R.id.btn_clear) {
            mTextField.getText().clear();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case DOWNLOAD_FILE_REQUEST:
                if (resultCode == RESULT_OK) {
                    File file = new File(data.getStringExtra("path"));
                    String repoDir = data.getStringExtra("repoDir");
                    SeafRepo repo = dataManager.getCachedRepoByID(data.getStringExtra("repoID"));
                    WidgetUtils.showFile(this, file, repo.getID(), repo.getName(), repoDir, repo.isSharedRepo);
                }
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if(constraintSearchLoadingContainer.getVisibility() != View.VISIBLE){
            super.onBackPressed();
        }
    }

    public void increasePage() {
        this.page++;
    }

    private void loadNext(boolean isRefresh) {
        if (!Utils.isNetworkOn()) {
            showShortToast(this, R.string.network_down);
            return;
        }

        if (isRefresh) {
            page = 1;
        }

        String searchText = mTextField.getText().toString().trim();
        if (!TextUtils.isEmpty(searchText)) {

            search(searchText, page, PAGE_SIZE);

            Utils.hideSoftKeyboard(mTextField);
        } else {
            showShortToast(this, R.string.search_txt_empty);
        }
    }

    private void search(String content, int page, int pageSize) {
        // start asynctask
        ConcurrentAsyncTask.execute(new SearchLibrariesTask(dataManager, content, page, pageSize));
    }

    private class SearchLibrariesTask extends AsyncTask<Void, Void, ArrayList<SearchedFile>> {

        private DataManager dataManager;
        private String query;
        private int pageSize;
        private int page;

        private SeafException seafException;

        @Override
        protected void onPreExecute() {
            // show loading view
            mSearchBtn.setEnabled(false);
        }

        public SearchLibrariesTask(DataManager dataManager, String query, int page, int pageSize) {
            this.dataManager = dataManager;
            this.query = query;
            this.pageSize = pageSize;
            this.page = page;
        }

        @Override
        protected ArrayList<SearchedFile> doInBackground(Void... params) {
            try {
                mSearchedRlt = dataManager.search(query, page, pageSize);
                return dataManager.parseSearchResult(mSearchedRlt);
            } catch (SeafException e) {
                seafException = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<SearchedFile> result) {
            // stop loading view
            mSearchBtn.setEnabled(true);

            if (result == null) {
                if (seafException != null) {
                    if (seafException.getCode() == 404)
                        showShortToast(Search2Activity.this, R.string.search_server_not_support);

                    Log.d(DEBUG_TAG, seafException.getMessage() + " code " + seafException.getCode());
                }

                return;
            }

            if (result.size() == 0) {
                showShortToast(Search2Activity.this, R.string.search_content_empty);
            }

            if (page == 1) {
                mAdapter.submitList(result);
            } else {
                mAdapter.addAll(result);
            }

            if (CollectionUtils.isEmpty(result) || result.size() < PAGE_SIZE) {
                helper.setTrailingLoadState(new LoadState.NotLoading(true));
                if (helper.getTrailingLoadStateAdapter() != null) {
                    helper.getTrailingLoadStateAdapter().checkDisableLoadMoreIfNotFullPage();
                }
            } else {
                helper.setTrailingLoadState(new LoadState.NotLoading(false));
            }

            increasePage();
        }
    }

    public DataManager getDataManager() {
        if (dataManager == null) {
            AccountManager accountManager = new AccountManager(this);
            account = accountManager.getCurrentAccount();
            dataManager = new DataManager(account);
        }
        return dataManager;
    }

    class SearchTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (mTextField.getText().toString().length() > 0) {
                mTextClearBtn.setVisibility(View.VISIBLE);
                mSearchBtn.setVisibility(View.VISIBLE);
            } else {
                mTextClearBtn.setVisibility(View.GONE);
                mSearchBtn.setVisibility(View.GONE);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }

    class EditorActionListener implements TextView.OnEditorActionListener {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                // pass 0 to disable page loading
                loadNext(true);
                return true;
            }
            return false;
        }
    }

    public void onSearchedFileSelected(SearchedFile searchedFile) {
        final String repoID = searchedFile.getRepoID();
        final String fileName = searchedFile.getTitle();
        final SeafRepo repo = dataManager.getCachedRepoByID(repoID);
        final String repoName = repo.getName();
        final String filePath = searchedFile.getPath();
        final long fileSize = searchedFile.getSize();
        final String repoDir = Utils.getParentPath(filePath);

        if (searchedFile.isDir()) {
            if (repo == null) {
                showShortToast(this, R.string.search_library_not_found);
                return;
            }
            WidgetUtils.showRepo(this, repoID, repoName, filePath, null);
            return;
        }

        // Encrypted repo doesn\`t support gallery,
        // because pic thumbnail under encrypted repo was not supported at the server side
        if (Utils.isViewableImage(searchedFile.getTitle())
                && repo != null && !repo.encrypted) {
            WidgetUtils.startGalleryActivity(this, repoName, repoID, Utils.getParentPath(filePath), searchedFile.getTitle(), account, accountInfo);
            return;
        }

        final File localFile = dataManager.getLocalCachedFile(repoName, repoID, filePath, null);
        if (localFile != null) {
            WidgetUtils.showFile(this, localFile, repoName, repoID, repoDir, repo.isSharedRepo);
            return;
        }
        boolean videoFile = Utils.isVideoFile(fileName);
        if (videoFile) { // is video file
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setItems(R.array.video_download_array, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) // create file
                        startPlayActivity(fileName, repoID, filePath);
                    else if (which == 1) // create folder
                        startFileActivity(fileName, fileSize, repoName, repoID, filePath);
                }
            }).show();
            return;
        }

        startFileActivity(fileName, fileSize, repoName, repoID, filePath);
    }

    private void startFileActivity(String fileName, long fileSize, String repoName, String repoID, String filePath) {
        constraintSearchLoadingContainer.setVisibility(View.VISIBLE);
        ExecutorService service = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        service.execute(() -> {

            if (ValidatesFiles.isValidFileCloud(account, accountInfo, fileName, filePath, fileSize, repoID)) {
                handler.post(() -> {
                    constraintSearchLoadingContainer.setVisibility(View.GONE);
                    final int taskID = txService.addDownloadTask(account, repoName, repoID, filePath);
                    Intent intent = new Intent(this, FileActivity.class);
                    intent.putExtra("repoName", repoName);
                    intent.putExtra("repoID", repoID);
                    intent.putExtra("filePath", filePath);
                    intent.putExtra("account", account);
                    intent.putExtra("taskID", taskID);
                    startActivityForResult(intent, DOWNLOAD_FILE_REQUEST);
                });

            } else {
                handler.post(() -> {
                    constraintSearchLoadingContainer.setVisibility(View.GONE);
                    try {
                        ChangePlanDialog
                                .build(search2Activity, ChangePlanDialog.NOT_ALLOWED_FILES)
                                .setResponse(new ChangePlanDialog.ChangePlanDialogResponse() {
                                    @Override
                                    public void changePlanDialogResponseYes() {
                                        Utils.openWebPlans(search2Activity);
                                    }

                                    @Override
                                    public void changePlanDialogResponseNo() {

                                    }

                                    @Override
                                    public void changePlanDialogResponseNeither() {

                                    }
                                }).show();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        });

    }

    private void startPlayActivity(String fileName, String repoID, String filePath) {

        try {
            String extension = FileExtension.getExtensionFromUri(search2Activity, Uri.fromFile(new File(fileName)));
            if (ValidatesFiles.isValidType(accountInfo, extension.toUpperCase())) {
                Intent intent = new Intent(this, ExoVideoPlayerActivity.class);
                intent.putExtra("fileName", fileName);
                intent.putExtra("repoID", repoID);
                intent.putExtra("filePath", filePath);
                intent.putExtra("account", account);
                intent.putExtra("accountInfo", accountInfo);
                //        DOWNLOAD_PLAY_REQUEST
                startActivity(intent);
            } else {
                ChangePlanDialog.build(search2Activity, ChangePlanDialog.NOT_ALLOWED_FILE).setResponse(new ChangePlanDialog.ChangePlanDialogResponse() {
                    @Override
                    public void changePlanDialogResponseYes() {
                        Utils.openWebPlans(search2Activity);
                    }

                    @Override
                    public void changePlanDialogResponseNo() {

                    }

                    @Override
                    public void changePlanDialogResponseNeither() {

                    }
                }).show();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            TransferService.TransferBinder binder = (TransferService.TransferBinder) service;
            txService = binder.getService();
            Log.d(DEBUG_TAG, "bind TransferService");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            txService = null;
        }
    };

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
}
