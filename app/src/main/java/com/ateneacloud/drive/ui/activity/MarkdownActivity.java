package com.ateneacloud.drive.ui.activity;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.appcompat.widget.Toolbar;

import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.ateneacloud.drive.R;
import com.ateneacloud.drive.account.Account;
import com.ateneacloud.drive.account.AccountManager;
import com.ateneacloud.drive.editor.EditorActivity;
import com.ateneacloud.drive.transfer.SeafUploadFile;
import com.ateneacloud.drive.transfer.TransferService;
import com.ateneacloud.drive.util.FileMimeUtils;

import java.io.File;

import br.tiagohm.markdownview.MarkdownView;
import br.tiagohm.markdownview.Utils;
import br.tiagohm.markdownview.css.InternalStyleSheet;
import br.tiagohm.markdownview.css.styles.Github;

/**
 * For showing markdown files
 */
public class MarkdownActivity extends BaseActivity implements Toolbar.OnMenuItemClickListener {

    @SuppressWarnings("unused")
    private static final String DEBUG_TAG = "MarkdownActivity";
    private MarkdownView markdownView;
    private TransferService txService = null;
    private Account account;
    private AccountManager accountManager;

    private String repoID, repoName, repoDir;

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

    String path;
    boolean onlyRead;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_markdown);

        Intent intent = getIntent();
        repoID = intent.getStringExtra("repoID");
        repoName = intent.getStringExtra("repoName");
        repoDir = intent.getStringExtra("repoDir");

        path = intent.getStringExtra("path");
        onlyRead = intent.getBooleanExtra("only_read", true);

        if (path == null || repoID == null || repoName == null || repoDir == null) return;

        markdownView = findViewById(R.id.markdownView);
        Toolbar toolbar = getActionBarToolbar();
        toolbar.setOnMenuItemClickListener(this);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        accountManager = new AccountManager(this);
        account = accountManager.getCurrentAccount();

        Intent bIntent = new Intent(this, TransferService.class);
        bindService(bIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onResume() {
        super.onResume();
        File file = new File(path);
        if (!file.exists())
            return;

        InternalStyleSheet css = new Github();
        css.addRule("body", new String[]{"line-height: 1.6", "padding: 0px"});
        css.addRule("a", "color: orange");
        markdownView.addStyleSheet(css);
        try {
            markdownView.loadMarkdownFromFile(file);
        } catch (Exception e) {
            markdownView.loadData(Utils.getStringFromFile(file), "text/plain", "UTF-8");
            e.printStackTrace();
        }

        getSupportActionBar().setTitle(file.getName());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getActionBarToolbar().inflateMenu(R.menu.markdown_view_menu);
        MenuItem editItem = getActionBarToolbar().getMenu().findItem(R.id.edit_markdown);
        if (onlyRead) {
            editItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.edit_markdown:
                edit();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    private void edit() {
        PackageManager pm = getPackageManager();

        // First try to find an activity who can handle markdown edit
        Intent editAsMarkDown = new Intent(Intent.ACTION_EDIT);
        Uri uri = FileProvider.getUriForFile(this, getPackageName(), new File(path));
        editAsMarkDown.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        String mime = FileMimeUtils.getMimeType(new File(path));
        editAsMarkDown.setDataAndType(uri, mime);

        if ("text/plain".equals(mime)) {
            Intent intent = new Intent(this, EditorActivity.class);
            intent.putExtra("path", path);
            startActivityForResult(intent, 828);
        } else if (pm.queryIntentActivities(editAsMarkDown, 0).size() > 0) {
            // Some activity can edit markdown
            startActivity(editAsMarkDown);
        } else {
            // No activity to handle markdown, take it as text
            Intent editAsText = new Intent(Intent.ACTION_EDIT);
            mime = "text/plain";
            editAsText.setDataAndType(uri, mime);

            try {
                startActivity(editAsText);
            } catch (ActivityNotFoundException e) {
                showShortToast(this, getString(R.string.activity_not_found));
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 828 && resultCode == RESULT_OK) {
            addUpdateTask(repoID, repoName, repoDir, path);
        }

    }

    private void addUpdateTask(String repoID, String repoName, String targetDir, String localFilePath) {
        if (txService != null) {
            txService.addTaskToUploadQue(new SeafUploadFile(account, repoID, repoName, targetDir, localFilePath, true, false));
        }
    }

}
